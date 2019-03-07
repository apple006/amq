package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.State;
import org.osgl.util.C;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqClientProcessor implements AioProcessor<Message>, MqClientAction {

    private AioPipe<Message> pipe;
    private static Map<String, Call> callBackMap = new ConcurrentHashMap<>();
    /**
     * 客户端返回的消息包装
     */
    private static Map<String, CompletableFuture<Message>> futureResultMap = new ConcurrentHashMap<>();

    @Override
    public void process(AioPipe<Message> pipe, Message msg) {
        System.err.println(" Client,收到消息: ");
        String subscribeId = msg.getSubscribeId();
        if(C.notEmpty(callBackMap) && null != callBackMap.get(subscribeId)){
            callBackMap.get(subscribeId).back(msg);
        }
        if(C.notEmpty(futureResultMap) && null != futureResultMap.get(subscribeId)){
            futureResultMap.get(subscribeId).complete(msg);
        }
    }

    @Override
    public void subscribe(String topic, Call callBack) {
        subscribe(topic, null, callBack);
    }

    @Override
    public <V> void subscribe(String topic, V v, Call callBack) {
        Message subscribe = Message.buildSubscribe(topic, v, getNode(), Message.Life.ALL_ACKED, Message.Listen.CALLBACK);
        this.pipe.write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), callBack);
    }

    @Override
    public <V> Message publishJob(String topic, V v) {
        Message job = Message.buildPublishJob(topic, v, getNode());
        String jobId = job.getK().getId();
        // 发布一个 future ,等任务完成后读取结果.
        futureResultMap.put(jobId, new CompletableFuture<Message>());
        this.pipe.write(job);
        Message result = futureResultMap.get(jobId).join();
        if (null != result) {
            removeFutureResultMap(result.getSubscribeId());
            if (MqConfig.mq_auto_acked) {
                ack(result.getSubscribeId(), Message.Life.SPARK);
            }
        }

        return result;
    }

    @Override
    public void acceptJob(String topic,Call acceptJobThenExecute) {
        Message subscribe = Message.buildAcceptJob(topic, getNode());
        this.pipe.write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), acceptJobThenExecute);
    }

    public <V> boolean finishJob(String topic, V v, Message acceptJob) {
        try {
            Message<Message.Key,V> finishJob = Message.buildFinishJob(acceptJob.getK().getId(), topic, v, getNode());
            this.pipe.write(finishJob);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public <M> boolean onlyPublish(String topic, M data) {
        try {
            this.pipe.write(Message.buildCommonMessage(topic, data, getNode(), Message.SPREAD.TOPIC));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean ack(String messageId, Message.Life life) {
        Message message = Message.buildAck(messageId, life);
        pipe.write(message);
        return false;
    }

    private Integer getNode() {
        return this.pipe.getId();
    }

     @Override
    public void stateEvent(AioPipe<Message> pipe, State state, Throwable throwable) {
        switch (state) {
            case NEW_PIPE:
                this.pipe = pipe;
                break;
        }
        if (null != throwable) {
            throwable.printStackTrace();
        }
    }

    private void removeFutureResultMap(String key) {
        futureResultMap.remove(key);
    }

    private void removeCallbackMap(String key) {
        callBackMap.remove(key);
    }






}
