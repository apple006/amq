package com.artlongs.amq.core;

import com.artlongs.amq.core.store.IStore;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.tools.RingBufferQueue;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Func : MQ 中心的内部定时任务
 *
 * @author: leeton on 2019/3/15.
 */
public enum MqScheduler {
    inst;
    private static Logger logger = LoggerFactory.getLogger(MqScheduler.class);
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public void start(){
        // 计时任务
        final ScheduledFuture<?> delaySend = scheduler.scheduleWithFixedDelay(
                delaySendOnScheduled(), 5, MqConfig.msg_not_acked_resend_period, SECONDS);
        final ScheduledFuture<?> retrySend = scheduler.scheduleWithFixedDelay(
                retrySendOnScheduled(), 5, MqConfig.msg_falt_message_resend_period, SECONDS);
        final ScheduledFuture<?> checkAlive = scheduler.scheduleWithFixedDelay(
                checkAliveScheduled(), 1, MqConfig.msg_default_alive_tims, SECONDS);

/*        TimeWheelService.instance.schedule(delaySendOnScheduled(), 5, MqConfig.msg_not_acked_resend_period, SECONDS);
        TimeWheelService.instance.schedule(retrySendOnScheduled(), 5, MqConfig.msg_falt_message_resend_period, SECONDS);
        TimeWheelService.instance.schedule(checkAliveScheduled(), 1, MqConfig.msg_default_alive_tims, SECONDS);*/
    }

    /**
     * 客户端未确认的消息-->重发
     *
     * @return
     */
    private Runnable delaySendOnScheduled() {
        final Runnable delay = new Runnable() {
            @Override
            public void run() {
                RingBufferQueue<Subscribe> cache_subscribe = ProcessorImpl.INST.getCache_subscribe();
                if (cache_subscribe.empty()) { // 从 DB 恢复所有订阅
                    final List<Subscribe> retryList = Store.INST.getAll(IStore.mq_subscribe,Subscribe.class);
                    for (Subscribe o : retryList) {
                        cache_subscribe.put(o);
                    }
                }

                ConcurrentSkipListMap<String, Message> cache_common_publish_message = ProcessorImpl.INST.getCache_common_publish_message();
                if (C.isEmpty(cache_common_publish_message)) { //  从 DB 恢复所有未确认的消息
                    final List<Message> retryList = Store.INST.getAll(IStore.mq_all_data,Message.class);
                    for (Message o : retryList) {
                        cache_common_publish_message.put(o.getK().getId(), o);
                    }
                }

                for (Message message : cache_common_publish_message.values()) {
                    if (MqConfig.msg_not_acked_resend_max_times > message.getStat().getDelay()) {
                        logger.warn("The scheduler task is running delay-send message !");
                        message.incrDelay();
                        ProcessorImpl.INST.pulishJobEvent(message);
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
    private Runnable retrySendOnScheduled() {
        final Runnable retry = new Runnable() {
            @Override
            public void run() {
                ConcurrentSkipListMap<String, Message> cache_falt_message = ProcessorImpl.INST.getCache_falt_message();
                if (C.isEmpty(cache_falt_message)) {
                    final List<Message> retryList = Store.INST.getAll(IStore.mq_need_retry,Message.class);
                    for (Message o : retryList) {
                        cache_falt_message.put(o.getK().getId(), o);
                    }
                }
                for (Message message : cache_falt_message.values()) {
                    if (MqConfig.msg_falt_message_resend_max_times > message.getStat().getRetry()) {
                        logger.warn("The scheduler task is running retry-send message !");
                        message.incrRetry();
                        ProcessorImpl.INST.pulishJobEvent(message);
                    }
                }
            }
        };
        return retry;
    }

    /**
     * 删除过期的消息
     *
     * @return
     */
    private Runnable checkAliveScheduled() {
        final Runnable retry = new Runnable() {
            @Override
            public void run() {
                ConcurrentSkipListMap<String, Message> cache_common_publish_message = ProcessorImpl.INST.getCache_common_publish_message();
                if (C.isEmpty(cache_common_publish_message)) { //  从 DB 恢复所有未确认的消息
                    final List<Message> retryList = Store.INST.getAll(IStore.mq_all_data,Message.class);
                    for (Message o : retryList) {
                        cache_common_publish_message.put(o.getK().getId(), o);
                    }
                }
                for (Message message : cache_common_publish_message.values()) {
                    long cTime = message.getStat().getCtime();
                    long now = System.currentTimeMillis();
                    long ttl = message.getStat().getTtl();
                    if ((now - cTime >= ttl) && Message.Life.FOREVER != message.getLife()) {
                        String id = message.getK().getId();
                        logger.warn("The scheduler task is running remove message({}) on TTL.", id);
                        ProcessorImpl.INST.removeDbDataOfDone(id);
                        ProcessorImpl.INST.removeCacheOfDone(id);
                    }
                }
            }
        };
        return retry;
    }
}
