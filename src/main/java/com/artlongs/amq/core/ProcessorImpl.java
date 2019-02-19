package com.artlongs.amq.core;

import com.artlongs.amq.core.event.BuzEventHandler;
import com.artlongs.amq.core.event.JobEvent;
import com.artlongs.amq.core.event.JobEvnetHandler;
import com.artlongs.amq.core.event.StoreEventHandler;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.disruptor.*;
import com.artlongs.amq.disruptor.dsl.Disruptor;
import com.artlongs.amq.disruptor.dsl.ProducerType;
import com.artlongs.amq.disruptor.util.DaemonThreadFactory;
import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.tools.FastList;
import com.artlongs.amq.tools.IOUtils;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NetworkChannel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/22.
 */
public enum ProcessorImpl implements Processor {
    INST;
    private static Logger logger = LoggerFactory.getLogger(ProcessorImpl.class);

    /**
     * 从客户端读取到的数据缓存 map(messageId,ByteBuffer)
     */
    public static ConcurrentMap<String, ByteBuffer> cache_buffer = new ConcurrentHashMap<>(128);
    /**
     * 从客户端读取到的数据缓存 map(messageId,Message)
     */
    public static ConcurrentMap<String, Message> cache_message = new ConcurrentHashMap<>(128);

    /**
     * 客户端订阅的主题 map(messageId,Subscribe)
     */
    public static ConcurrentMap<String, Subscribe> subscribe = new ConcurrentHashMap<>(128);

    public static ISerializer json = ISerializer.Serializer.INST.of();

    // ringBuffer cap setting
    private static final int WORKER_BUFFER_SIZE = 1024 * 32;
    private static final int NUM_WORKERS = 2;

    //创建消息多线程分发器 ringBuffer
    private final RingBuffer<JobEvent> job_ringbuffer;

    // 业务处理 worker pool
    private final RingBuffer<JobEvent> buz_worker;

    // 消息持久化 worker pool
    private final RingBuffer<JobEvent> persistent_worker;

    // 业务处理 worker pool
    private final WorkerPool<JobEvent> buz_worker_pool;

    // 消息持久化 worker pool
    private final WorkerPool<JobEvent> persistent_worker_pool;

    public ProcessorImpl me(){
        return this;
    }

    ProcessorImpl() {
        this.job_ringbuffer = createDisrupter().start();
        this.buz_worker_pool= createWorkerPool(new BuzEventHandler());
        this.persistent_worker_pool = createWorkerPool(new StoreEventHandler());
        this.buz_worker = buz_worker_pool.start(Executors.newFixedThreadPool(NUM_WORKERS, DaemonThreadFactory.INSTANCE));
        this.persistent_worker = persistent_worker_pool.start(Executors.newFixedThreadPool(NUM_WORKERS, DaemonThreadFactory.INSTANCE));
    }

    /**
     * 创建消息 disruptor
     *
     * @return
     */
    private Disruptor<JobEvent> createDisrupter() {
        final JobEvnetHandler jobEvnetHandler = new JobEvnetHandler();
        Disruptor<JobEvent> disruptor =
                new Disruptor<JobEvent>(
                        JobEvent.EVENT_FACTORY,
                        WORKER_BUFFER_SIZE,
                        DaemonThreadFactory.INSTANCE,
                        ProducerType.SINGLE,
                        new YieldingWaitStrategy());
        disruptor.handleEventsWith(jobEvnetHandler);
        return disruptor;
    }

    /**
     * 创建工作线程沲
     * @param workHandler 事件处理器
     * @return
     */
    private WorkerPool<JobEvent> createWorkerPool(WorkHandler workHandler) {

        final RingBuffer<JobEvent> ringBuffer = RingBuffer.createSingleProducer(JobEvent.EVENT_FACTORY, WORKER_BUFFER_SIZE, new YieldingWaitStrategy());

        WorkerPool<JobEvent> workerPool = new WorkerPool<JobEvent>(ringBuffer, ringBuffer.newBarrier(), new FatalExceptionHandler(), workHandler);

        //init sequences
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());

        return workerPool;

    }

    @Override
    public ConcurrentMap<String, ByteBuffer> onData(AsynchronousSocketChannel channel,ByteBuffer buffer) {
        Message message = parser(buffer);
        if (message.isAcked()) { // ack
            flagOnAck(channel,message);
        }else {
            addCache(message.getK().getId(), buffer);
            addCache(message.getK().getId(), message);
            addSubscribeIF(message);
            // 发布 JOB 到 RingBuffer
            pulishJobEvent(message);
        }
        return cache_buffer;
    }

    @Override
    public ByteBuffer getBuffer(String id) {
        return cache_buffer.get(id);
    }

    public Message getMessageOfCache(String id) {
       return cache_message.get(id);
    }

    private void addCache(String key, ByteBuffer buffer) {
        cache_buffer.putIfAbsent(key, buffer);
    }

    private void addCache(String key, Message message) {
        cache_message.putIfAbsent(key, message);
    }

    public void removeCacheOfDone(String key){
        cache_buffer.remove(key);
        cache_message.remove(key);
    }

    /**
     * 标记消息已经收到
     * @param ack
     */
    private void flagOnAck(AsynchronousSocketChannel channel, Message ack) {
        String ackId = ack.getK().getId();
        Message message = getMessageOfCache(ackId);
        if (null != message) {
            String node = IOUtils.getRemoteAddressStr(channel);
            message.upStatOfACK(node);
        }
    }

    private void pulishJobEvent(Message message) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            message.getStat().setOn(Message.ON.QUENED);
            job_ringbuffer.publishEvent(JobEvent::translate, message);
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean addSubscribeIF(Message message) {
        if (message.isSubscribe()) {
            String clientKey = message.getK().getSendNode();
            NetworkChannel channel = getTargetChannel(clientKey);
            Subscribe listen = new Subscribe(clientKey, message.getK().getTopic(), channel, message.getLife(), false);
            subscribe.putIfAbsent(clientKey, listen);
            return true;
        }
        return false;
    }

    @Override
    public Message parser(ByteBuffer buffer) {
        return json.getObj(buffer);
    }

    @Override
    public void publishJobToWorker(Message message) {
        message.getStat().setOn(Message.ON.SENDING);
        publishJobToWorkerPool(buz_worker, message);
        publishJobToWorkerPool(persistent_worker, message);
    }

    /**
     * 按 TOPIC 前缀式匹配
     * @param topic
     * @return
     */
    public FastList<Subscribe> subscribeOfTopic(String topic) {
        FastList<Subscribe> list = new FastList<>(Subscribe.class);
        for (Subscribe listen : subscribe.values()) {
            if(listen.getTopic().startsWith(topic)){
                list.add(listen);
            }
        }
        return list;
    }

    /**
     *  按 DIRECT 完全相同匹配
     * @param directTopic
     * @return
     */
    public FastList<Subscribe> subscribeOfDirect(String directTopic) {
        FastList<Subscribe> list = new FastList<>(Subscribe.class);
        for (Subscribe listen : subscribe.values()) {
            if(listen.getTopic().equalsIgnoreCase(directTopic)){
                list.add(listen);
            }
        }
        return list;
    }

    public void sendMessageToSubcribe(List<Subscribe> subscribeList,Message message){
        for (Subscribe listen : subscribeList) {
            String msgId = message.getK().getId();
            // remove 掉缓存当客户端全部 ACK
            int sends = message.getAckedSize();
            if (sends >= subscribeList.size()) {
                removeCacheOfDone(msgId);
                return;
            }

            String remoteAddressStr = IOUtils.getRemoteAddressStr(listen.getChannel());
            if(isACKED(message, remoteAddressStr)) return;

            boolean writed = IOUtils.write((AsynchronousSocketChannel) listen.getChannel(), ProcessorImpl.INST.getBuffer(msgId));
            if (writed) {
                onSendSuccToOption(subscribeList, listen, message);
            }else {
                onSendFailToBackup(message);
            }
        }
    }

    public void sendMessageOfFanout(Message message){
        for (NetworkChannel channel : MqServer.clientSocketMap.values()) {
            String msgId = message.getK().getId();
            // remove 掉缓存当客户端全部 ACK
            int sends = message.getAckedSize();
            if (sends >= MqServer.clientSocketMap.size()) {
                removeCacheOfDone(msgId);
                return;
            }

            String sendNode = IOUtils.getRemoteAddressStr(channel);
            if (message.getK().isSelf(sendNode)) return;
            if(isACKED(message,sendNode)) return;
            //
            boolean writed = IOUtils.write((AsynchronousSocketChannel) channel, ProcessorImpl.INST.getBuffer(msgId));
            if (writed) {
                message.upStatOfSended(sendNode);
            }else {
                onSendFailToBackup(message);
            }

        }
    }

    private boolean isACKED(Message message, String node) {
        Set<String> nodesConfirmed = message.getStat().getNodesConfirmed();
        if (C.notEmpty(nodesConfirmed)) {
            for (String confirm : nodesConfirmed) {
                if(confirm.equalsIgnoreCase(node)) return true;
            }
        }
        return false;
    }

    private void onSendFailToBackup(Message message) {
        message.getStat().setOn(Message.ON.SENDONFAIL);
        Store.INST.saveToRetryList(message.getK().getId(), message);
    }

    public void onSendSuccToOption(List<Subscribe>subscribeList,Subscribe listen,Message message){
        message.upStatOfSended(listen.getId());
        if (Subscribe.Life.SPARK == listen.getLife()) {
            listen.remove(subscribeList, listen);
        }
    }

    @Override
    public NetworkChannel getTargetChannel(String keyOfChannel) {
        return MqServer.clientSocketMap.get(keyOfChannel);
    }

    public void publishJobToWorkerPool(RingBuffer<JobEvent> ringBuffer,Message message){
        ringBuffer.publishEvent(JobEvent::translate, message);
    }


}
