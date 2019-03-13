package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.event.BizEventHandler;
import com.artlongs.amq.core.event.JobEvent;
import com.artlongs.amq.core.event.JobEvnetHandler;
import com.artlongs.amq.core.event.StoreEventHandler;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.disruptor.*;
import com.artlongs.amq.disruptor.dsl.Disruptor;
import com.artlongs.amq.disruptor.dsl.ProducerType;
import com.artlongs.amq.disruptor.util.DaemonThreadFactory;
import com.artlongs.amq.tools.FastList;
import com.artlongs.amq.tools.RingBufferQueue;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    private ConcurrentSkipListMap<String, Message> cache_all_message = new ConcurrentSkipListMap();

    /**
     * 发送失败的消息数据缓存 map(messageId,Message)
     */
    private ConcurrentSkipListMap<String, Message> cache_falt_message = new ConcurrentSkipListMap();
    private ConcurrentSkipListMap<String, Message> cache_public_job = new ConcurrentSkipListMap();
    private ConcurrentSkipListMap<String, Message> cache_accept_job = new ConcurrentSkipListMap();

    /**
     * 客户端订阅的主题 RingBufferQueue(Subscribe)
     */
    private RingBufferQueue<Subscribe> cache_subscribe = new RingBufferQueue<>(MqConfig.mq_cache_map_sizes);

    // ringBuffer cap setting
    private final int WORKER_BUFFER_SIZE = 1024 * 32;

    //创建消息多线程任务分发器 ringBuffer
    private final RingBuffer<JobEvent> job_ringbuffer;

    // 业务处理 worker
    private final RingBuffer<JobEvent> biz_worker;
    // 业务处理 worker pool
    private final WorkerPool<JobEvent> biz_worker_pool;

    // 消息持久化 worker
    private RingBuffer<JobEvent> persistent_worker = null;
    // 消息持久化 worker pool
    private WorkerPool<JobEvent> persistent_worker_pool = null;

    // scheduled to resend message
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public ProcessorImpl me() {
        return this;
    }

    ProcessorImpl() {
        this.job_ringbuffer = createDisrupter().start();
        this.biz_worker_pool = createWorkerPool(new BizEventHandler());
        ExecutorService poolExecutor = Executors.newFixedThreadPool(MqConfig.worker_thread_pool_size, DaemonThreadFactory.INSTANCE);
        this.biz_worker = biz_worker_pool.start(poolExecutor);
        if (MqConfig.store_all_message_to_db) {
            this.persistent_worker_pool = createWorkerPool(new StoreEventHandler());
            this.persistent_worker = persistent_worker_pool.start(poolExecutor);
        }
        // 计时任务
        final ScheduledFuture<?> delaySend = scheduler.scheduleWithFixedDelay(
                delaySendOnScheduled(), 5, MqConfig.msg_not_acked_resend_period, SECONDS);
        final ScheduledFuture<?> retrySend = scheduler.scheduleWithFixedDelay(
                retrySendOnScheduled(), 5, MqConfig.msg_falt_message_resend_period, SECONDS);
        final ScheduledFuture<?> checkAlive = scheduler.scheduleWithFixedDelay(
                checkAliveScheduled(), 1, MqConfig.msg_default_alive_tims, SECONDS);

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
     * 创建工作线程池
     *
     * @param workHandler 事件处理器
     * @return
     */
    private WorkerPool<JobEvent> createWorkerPool(WorkHandler workHandler) {
        final RingBuffer<JobEvent> ringBuffer = RingBuffer.createSingleProducer(JobEvent.EVENT_FACTORY, WORKER_BUFFER_SIZE, new BlockingWaitStrategy());
//        final RingBuffer<JobEvent> ringBuffer = RingBuffer.createMultiProducer(JobEvent.EVENT_FACTORY, WORKER_BUFFER_SIZE, new BlockingWaitStrategy());
        WorkerPool<JobEvent> workerPool = new WorkerPool<JobEvent>(ringBuffer, ringBuffer.newBarrier(), new FatalExceptionHandler(), workHandler);
        //init sequences
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        return workerPool;

    }

    @Override
    public void onMessage(AioPipe pipe, Message message) {
        if (null != message) {
            String msgId = message.getK().getId();
            if (message.isAckedMsg()) { // ACK 消息
                if (Message.Life.SPARK == message.getLife()) {
                    removeSubscribeCacheOnAck(msgId);
                    removeDbDataOfDone(msgId);
                } else {
                    Integer clientNode = getNode(pipe);
                    upStatOfACK(clientNode, message);
                }
            } else {
                if (message.isSubscribe()) { // subscribe msg
                    addSubscribeIF(pipe, message);
                    if (isAcceptJob(message)) { // 如果工作任务已经先一步发布了,则触发-->直接把任务发给订阅者
                        triggerDirectSendJobToAcceptor(pipe, message);
                        return;
                    }

                } else {
                    if (isPublishJOb(message)) { // 发布的消息为新的工作任务(pingpong)
                        buildSubscribeOfCatchResult(pipe, message);
                        cachePubliceJobMessage(msgId, message);
                    }
                    cacheAllMessage(msgId, message);
                    // pulish message job to RingBuffer
 /*               for (int i = 0; i < 10; i++) { //test code
                    pulishJobEvent(message);
                }*/
                    pulishJobEvent(message);

                }
            }
        }

    }


    /**
     * 标记消息已经收到
     *
     * @param clientNode 消息的通道 ID
     * @param ack
     */
    private void upStatOfACK(Integer clientNode, Message ack) {
        String ackId = ack.getK().getId();
        Message message = getMessageOfCache(ackId);
        if (null != message) {
            message.upStatOfACK(clientNode);
        }
    }

    /**
     * 分派消息
     *
     * @param message
     */
    private void pulishJobEvent(Message message) {
        message.getStat().setOn(Message.ON.QUENED);
        job_ringbuffer.publishEvent(JobEvent::translate, message);
    }

    /**
     * 把消息放到线程池里去执行,因为消息是在客户端执行耗时比较长.
     *
     * @param message
     */
    @Override
    public void publishJobToWorker(Message message) {
        message.getStat().setOn(Message.ON.SENDING);
        publishJobToWorkerPool(biz_worker, message);
        if (MqConfig.store_all_message_to_db) { // 持久化
            publishJobToWorkerPool(persistent_worker, message);
        }
    }

    private void publishJobToWorkerPool(RingBuffer<JobEvent> ringBuffer, Message message) {
        ringBuffer.publishEvent(JobEvent::translate, message);
    }

    /**
     * 如果是订阅消息,加入到订阅队列
     *
     * @param pipe
     * @param message
     * @return
     */
    private boolean addSubscribeIF(AioPipe pipe, Message message) {
        if (message.isSubscribe()) {
            String clientKey = message.getK().getId();
            Subscribe listen = new Subscribe(clientKey, message.getK().getTopic(), pipe, message.getLife(), message.getListen());
            RingBufferQueue.Result result = cache_subscribe.putIfAbsent(listen);
            listen.setIdx(result.index);
            return true;
        }
        return false;
    }

    /**
     * 消息类型为 {@link Message.Type.PUBLISH_JOB} 时,自动为它创建一个订阅,以收取任务结果
     *
     * @param pipe
     * @param message
     * @return
     */
    private boolean buildSubscribeOfCatchResult(AioPipe pipe, Message message) {
        String jobId = message.getK().getId();
        String jobTopc = Message.buildFinishJobTopic(jobId, message.getK().getTopic());
        Subscribe subscribe = new Subscribe(jobId, jobTopc, pipe, message.getLife(), message.getListen());
        RingBufferQueue.Result result = cache_subscribe.putIfAbsent(subscribe);
        subscribe.setIdx(result.index);
        return true;
    }


    /**
     * 按 TOPIC 前缀式匹配
     *
     * @param topic
     * @return
     */
    public FastList<Subscribe> subscribeOfTopic(String topic) {
        FastList<Subscribe> list = new FastList<>(Subscribe.class);
        Iterator<Subscribe> subscribes = cache_subscribe.iterator();
        while (subscribes.hasNext()) {
            Subscribe listen = subscribes.next();
            if (null != listen && listen.getTopic().startsWith(topic)) {
                list.add(listen);
            }
        }
        return list;
    }

    /**
     * 按 DIRECT 完全相同匹配
     *
     * @param directTopic
     * @return
     */
    public FastList<Subscribe> subscribeOfDirect(String directTopic) {
        FastList<Subscribe> list = new FastList<>(Subscribe.class);
        Iterator<Subscribe> subscribes = cache_subscribe.iterator();
        while (subscribes.hasNext()) {
            Subscribe listen = subscribes.next();
            if (null != listen && listen.getTopic().startsWith(directTopic)) {
                list.add(listen);
            }
        }
        return list;
    }

    public void sendMessageToSubcribe(Message message, List<Subscribe> subscribeList) {
        for (Subscribe subscribe : subscribeList) {
            if (isPipeClosedThenRemove(subscribe)) {
                continue;
            }
            // 当客户端全部 ACK,则 remove 掉缓存
            int ackedSize = message.ackedSize();
            if (ackedSize >= subscribeList.size()) {
                String msgId = message.getK().getId();
                removeCacheOfDone(msgId);
                removeDbDataOfDone(msgId);
                return;
            }
            sendMessageToSubcribe(message, subscribe);
        }
    }

    /**
     * 发送消息给订阅者
     *
     * @param subscribe
     * @param message
     */
    private void sendMessageToSubcribe(Message message, Subscribe subscribe) {
        // 当前的消息,所有客户端都已确认收到
        if (isAcked(message, subscribe)) return;
        // 发送消息给订阅方
        changeMessageOnReply(subscribe, message);
        boolean writed = subscribe.getPipe().write(message);
        if (writed) {
            onSendSuccToOption(subscribe, message);
        } else {
            onSendFailToBackup(message);
        }
    }

    /**
     * 把生产者的消息ID替换为订阅者的消息ID , 以便订阅端读取到对应的消息
     *
     * @param subscribe
     * @param message
     */
    private void changeMessageOnReply(Subscribe subscribe, Message message) {
        message.setSubscribeId(subscribe.getId());
        message.setLife(subscribe.getLife());
        message.setListen(subscribe.getListen());
    }

    /**
     * 通道失效,移除对应在的订阅
     *
     * @param subscribe
     * @return
     */
    private boolean isPipeClosedThenRemove(Subscribe subscribe) {
        try {
            if (subscribe.getPipe().isInvalid()) {
                cache_subscribe.remove(subscribe.getIdx());
                logger.warn("remove subscribe on pipe ({}) is CLOSED.", subscribe.getPipe().getId());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 客户端未确认的消息-->重发
     *
     * @return
     */
    @Override
    public Runnable delaySendOnScheduled() {
        Runnable delay = new Runnable() {
            @Override
            public void run() {
                for (Message message : cache_all_message.values()) {
                    if (MqConfig.msg_not_acked_resend_max_times > message.getStat().getDelay()) {
                        logger.warn("The scheduler task is running delay-send message !");
                        message.incrDelay();
                        pulishJobEvent(message);
                    }
                }
            }
        };
        return delay;
    }

    /**
     * 发送失败的消息-->重发
     *
     * @return
     */
    @Override
    public Runnable retrySendOnScheduled() {
        final Runnable retry = new Runnable() {
            @Override
            public void run() {
                for (Message message : cache_falt_message.values()) {
                    if (MqConfig.msg_falt_message_resend_max_times > message.getStat().getRetry()) {
                        logger.warn("The scheduler task is running retry-send message !");
                        message.incrRetry();
                        pulishJobEvent(message);
                    }
                }
            }
        };
        return retry;
    }

    /**
     * 删除过期的消息
     * @return
     */
    public Runnable checkAliveScheduled() {
        final Runnable retry = new Runnable() {
            @Override
            public void run() {
                for (Message message : cache_all_message.values()) {
                    long cTime = message.getStat().getCtime();
                    long now = System.currentTimeMillis();
                    long ttl = message.getStat().getTtl();
                    if((now - cTime >= ttl) && Message.Life.FOREVER != message.getLife()){
                        String id = message.getK().getId();
                        logger.warn("The scheduler task is running remove message({}) on TTL.",id);
                        removeDbDataOfDone(id);
                        removeCacheOfDone(id);
                    }
                }
            }
        };
        return retry;
    }

    /**
     * 当前的消息,客户端都已确认收到过
     *
     * @param message
     * @param subscribe
     * @return
     */
    private boolean isAcked(Message message, Subscribe subscribe) {
        Set<Integer> comfirmList = message.getStat().getNodesConfirmed();
        if (C.notEmpty(comfirmList)) {
            for (Integer confirm : comfirmList) {
                if (confirm.equals(subscribe.getPipe().getId())) return true;
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

    private void appendSendFaltMessageOfDb(Map<String, Message> targetMap) {
        Store.INST.appendOfRetryMap(targetMap);
    }

    private void onSendSuccToOption(Subscribe listen, Message message) {
        message.upStatOfSended(listen.getPipe().getId());
        if (Message.Life.SPARK == listen.getLife()) {
            removeSubscribeOfCache(listen);
        }
    }

    /**
     * 客户端的通信通道 ID
     *
     * @param pipe
     * @return
     */
    private Integer getNode(AioPipe pipe) {
        return pipe.getId();
    }

    @Override
    public Message getMessage(String msgId) {
        return cache_all_message.get(msgId);
    }

    public Message getMessageOfCache(String id) {
        return cache_all_message.get(id);
    }

    private void cacheAllMessage(String key, Message message) {
        cache_all_message.putIfAbsent(key, message);
    }

    private void cachePubliceJobMessage(String key, Message message) {
        cache_public_job.putIfAbsent(key, message);
    }

    public void removeCacheOfDone(String key) {
        cache_all_message.remove(key);
        cache_falt_message.remove(key);
    }

    public void removeDbDataOfDone(String key) {
        Store.INST.remove(key);
        Store.INST.removeOfRetryList(key);
        Store.INST.removeOfUptime(key);
    }

    private void removeSubscribeCacheOnAck(String ackId) {
        Iterator<Subscribe> iter = cache_subscribe.iterator();
        while (iter.hasNext()) {
            Subscribe subscribe = iter.next();
            if (subscribe != null && ackId.equals(subscribe.getId())) {
                cache_subscribe.remove(subscribe.getIdx());
                break;
            }
        }
    }

    private void removeSubscribeOfCache(Subscribe subscribe) {
        try {
            cache_subscribe.remove(subscribe.getIdx());
        } catch (Exception e) {
            logger.error(" remove subscribe-cache element exception.");
        }
    }

    private boolean isPublishJOb(Message message) {
        return Message.Type.PUBLISH_JOB == message.getType();
    }

    private boolean isAcceptJob(Message message) {
        return Message.Type.ACCEPT_JOB == message.getType();
    }

    /**
     * 直接把任务发给接收者
     *
     * @param pipe
     * @param acceptor
     */
    private void triggerDirectSendJobToAcceptor(AioPipe pipe, Message acceptor) {
        String topic = acceptor.getK().getTopic();
        Message job = matchPublishJob(topic);
        if (null != job) {
            Subscribe subscribe = new Subscribe(acceptor.getK().getId(), topic, pipe, acceptor.getLife(), acceptor.getListen());
            sendMessageToSubcribe(job, subscribe);
        }
    }

    private Message matchPublishJob(String topic) {
        if (cache_public_job.size() == 0) return null;
        final Collection<Message> messageList = cache_public_job.values();
        for (Message message : messageList) {
            if (message.getK().getTopic().startsWith(topic)) return message;
        }
        return null;
    }

    public ConcurrentSkipListMap<String, Message> getCache_all_message() {
        return cache_all_message;
    }

    public ConcurrentSkipListMap<String, Message> getCache_falt_message() {
        return cache_falt_message;
    }

    public ConcurrentSkipListMap<String, Message> getCache_public_job() {
        return cache_public_job;
    }

    public ConcurrentSkipListMap<String, Message> getCache_accept_job() {
        return cache_accept_job;
    }

    public RingBufferQueue<Subscribe> getCache_subscribe() {
        return cache_subscribe;
    }
}
