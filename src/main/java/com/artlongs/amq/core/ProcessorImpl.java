package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
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
import com.artlongs.amq.tools.RingBufferQueue;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/22.
 */
public enum ProcessorImpl implements Processor {
    INST;
    private static Logger logger = LoggerFactory.getLogger(ProcessorImpl.class);

    /**
     * 从客户端读取到的数据缓存 map(messageId,Message)
     */
    public static ConcurrentSkipListMap<String, Message> cache_all_message =   new ConcurrentSkipListMap();

    /**
     * 发送失败的消息数据缓存 map(messageId,Message)
     */
    public static ConcurrentSkipListMap<String, Message> cache_falt_message =   new ConcurrentSkipListMap();

    /**
     * 客户端订阅的主题 map(messageId,Subscribe)
     */
    public static RingBufferQueue<Subscribe> subscribe_cache =   new RingBufferQueue<>(MqConfig.mq_cache_map_sizes);

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
    // scheduled to resend message
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    final Runnable beeper = ()-> System.out.println("The scheduler task is running!");




    public ProcessorImpl me(){
        return this;
    }

    ProcessorImpl() {
        this.job_ringbuffer = createDisrupter().start();
        this.buz_worker_pool= createWorkerPool(new BuzEventHandler());
        this.persistent_worker_pool = createWorkerPool(new StoreEventHandler());
        this.buz_worker = buz_worker_pool.start(Executors.newFixedThreadPool(NUM_WORKERS, DaemonThreadFactory.INSTANCE));
        this.persistent_worker = persistent_worker_pool.start(Executors.newFixedThreadPool(NUM_WORKERS, DaemonThreadFactory.INSTANCE));

        final ScheduledFuture<?> delaySend = scheduler.scheduleWithFixedDelay(
                delaySendOnScheduled(), 5, MqConfig.msg_not_acked_resend_period, SECONDS);

        final ScheduledFuture<?> retrySend = scheduler.scheduleWithFixedDelay(
                retrySendOnScheduled(), 5, MqConfig.msg_falt_message_resend_period, SECONDS);
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
                        new SleepingWaitStrategy());
        disruptor.handleEventsWith(jobEvnetHandler);
        return disruptor;
    }

    /**
     * 创建工作线程沲
     * @param workHandler 事件处理器
     * @return
     */
    private WorkerPool<JobEvent> createWorkerPool(WorkHandler workHandler) {

        final RingBuffer<JobEvent> ringBuffer = RingBuffer.createSingleProducer(JobEvent.EVENT_FACTORY,
                WORKER_BUFFER_SIZE, new BlockingWaitStrategy());

        WorkerPool<JobEvent> workerPool = new WorkerPool<JobEvent>(ringBuffer, ringBuffer.newBarrier(), new FatalExceptionHandler(), workHandler);

        //init sequences
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());

        return workerPool;

    }

    @Override
    public void onMessage(AioPipe pipe, Message message) {
        String msgId = message.getK().getId();
        if (message.isAcked()) { // ack msg
            if (Subscribe.Life.SPARK == message.getLife()) {
                removeSubscribeCacheOnAck(msgId);
                removeDbDataOfDone(msgId);
            }else {
                String clientNode = pipe.getLocalAddress().toString();
                flagOnAck(clientNode,message);
            }
        }else {
            if(message.isSubscribe()){ // subscribe msg
                addSubscribeIF(pipe,message);
            }else { // common buz msg
                addCache(msgId, message);
                // pulish message job to RingBuffer
                pulishJobEvent(message);
            }
        }
    }

    @Override
    public Message getMessage(String msgId) {
        return cache_all_message.get(msgId);
    }


    public Message getMessageOfCache(String id) {
       return cache_all_message.get(id);
    }

    private void addCache(String key, Message message) {
        cache_all_message.putIfAbsent(key, message);
    }

    public void removeCacheOfDone(String key){
        cache_all_message.remove(key);
        cache_falt_message.remove(key);
    }

    public void removeDbDataOfDone(String key) {
        Store.INST.remove(key);
        Store.INST.removeOfRetryList(key);
        Store.INST.removeOfUptime(key);
    }

    private void removeSubscribeCacheOnAck(String ackId) {
        Iterator<Subscribe> iter = subscribe_cache.iterator();
        while (iter.hasNext()) {
            Subscribe subscribe = iter.next();
            if(ackId.equals(subscribe.getId())){
                subscribe_cache.remove(subscribe.getIdx());
                break;
            }
        }
    }


    /**
     * 标记消息已经收到
     * @param ack
     */
    private void flagOnAck(String clientNode, Message ack) {
        String ackId = ack.getK().getId();
        Message message = getMessageOfCache(ackId);
        if (null != message) {
            message.upStatOfACK(clientNode);
        }
    }


    private void pulishJobEvent(Message message) {
        try {
            if (!message.isSubscribe()) {
                final CountDownLatch latch = new CountDownLatch(1);
                message.getStat().setOn(Message.ON.QUENED);
                job_ringbuffer.publishEvent(JobEvent::translate, message);
                latch.await();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 如果是订阅消息,加入到订阅队列
     * @param pipe
     * @param message
     * @return
     */
    private boolean addSubscribeIF(AioPipe pipe,Message message) {
        if (message.isSubscribe()) {
            String clientKey = message.getK().getId();
            Subscribe listen = new Subscribe(clientKey, message.getK().getTopic(), pipe, message.getLife(),message.getListen());
            RingBufferQueue.Result result = subscribe_cache.putIfAbsent(listen);
            listen.setIdx(result.index);
            return true;
        }
        return false;
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
        Iterator<Subscribe> subscribes = subscribe_cache.iterator();
        while (subscribes.hasNext()) {
            Subscribe listen = subscribes.next();
            if (null != listen && listen.getTopic().startsWith(topic)) {
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
        Iterator<Subscribe> subscribes = subscribe_cache.iterator();
        while (subscribes.hasNext()) {
            Subscribe listen = subscribes.next();
            if (null != listen && listen.getTopic().startsWith(directTopic)) {
                list.add(listen);
            }
        }
        return list;
    }

    public void sendMessageToSubcribe(List<Subscribe> subscribeList,Message message){
        for (Subscribe subscribe : subscribeList) {
            if (pipeInvalidThenUnsubscribe(subscribe)) {
                subscribe.remove(subscribeList, subscribe);
                continue;
            }
            String msgId = message.getK().getId();
            // 当客户端全部 ACK,则 remove 掉缓存
            int sends = message.ackedSize();
            if (sends >= subscribeList.size()) {
                removeCacheOfDone(msgId);
                removeDbDataOfDone(msgId);
                return;
            }

            String remoteAddressStr = subscribe.getPipe().getRemoteAddress().toString();
            if(isACKED(message, remoteAddressStr)) return;

            changeMessageOnReply(subscribe, message);
            boolean writed = subscribe.getPipe().write(message);
            if (writed) {
                onSendSuccToOption(subscribeList, subscribe, message);
            }else {
                onSendFailToBackup(message);
            }
        }
    }

    private void changeMessageOnReply(Subscribe subscribe,Message message) {
        message.setSubscribeId(subscribe.getId());
        message.setLife(subscribe.getLife());
        message.setListen(subscribe.getListen());
    }

    /**
     * 通道失效,移除对应在的订阅
     * @param subscribe
     * @return
     */
    private boolean pipeInvalidThenUnsubscribe(Subscribe subscribe) {
        try {
            if (subscribe.getPipe().isInvalid()) {
                subscribe_cache.remove(subscribe.getIdx());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Runnable delaySendOnScheduled() {
        System.out.println("The scheduler task is running delay-send message !");
        Runnable delay = new Runnable() {
            @Override
            public void run() {
                for (Message message : cache_all_message.values()) {
                    if(MqConfig.msg_not_acked_resend_max_times > message.getStat().getDelay()){
                        message.incrDelay();
                        pulishJobEvent(message);
                    }
                }
            }
        };
        return delay;
    }

    @Override
    public Runnable retrySendOnScheduled() {
        System.out.println("The scheduler task is running retry-send message !");
        final Runnable retry = new Runnable() {
            @Override
            public void run() {
                for (Message message : cache_falt_message.values()) {
                    if(MqConfig.msg_falt_message_resend_max_times > message.getStat().getRetry()){
                        message.incrRetry();
                        pulishJobEvent(message);
                    }
                }
            }
        };
        return retry;
    }

    /**
     * 当前的消息,客户端已确认
     * @param message
     * @param node
     * @return
     */
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
        String id = message.getK().getId();
        Store.INST.saveToRetryList(id, message);
        cache_all_message.remove(id);
        cache_falt_message.putIfAbsent(id, message);
    }

    private void appendSendFaltMessageOfDb(Map<String ,Message> targetMap) {
        Store.INST.appendOfRetryMap(targetMap);
    }

    public void onSendSuccToOption(List<Subscribe>subscribeList,Subscribe listen,Message message){
        message.upStatOfSended(listen.getId());
        if (Subscribe.Life.SPARK == listen.getLife()) {
            listen.remove(subscribeList, listen);
        }
    }

    public void publishJobToWorkerPool(RingBuffer<JobEvent> ringBuffer,Message message){
        ringBuffer.publishEvent(JobEvent::translate, message);
    }






}
