package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.State;
import org.jetbrains.annotations.Nullable;

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
    private Map<String, CompletableFuture<Message>> futureResultMap = new ConcurrentHashMap<>();
    private Map<String, Call> callBackMap = new ConcurrentHashMap<>();

    @Override
    public void process(AioPipe<Message> pipe, Message msg) {
        System.err.println(" Client,收到消息: ");
        switch (msg.getListen()) {
            case CALLBACK:
                callBackMap.get(msg.getSubscribeId()).back(msg);
                break;
            case FUTURE_AND_ONCE:
                futureResultMap.get(msg.getSubscribeId()).complete(msg);
                break;
        }
    }

    @Override
    public void subscribe(String topic, Call callBack) {
        subscribe(topic, null, callBack);
    }

    @Override
    public <V> void subscribe(String topic, V v, Call callBack) {
        Message subscribe = Message.buildSubscribe(topic, v, getNode(), Message.Life.ALL_COMFIRM, Message.Listen.CALLBACK);
        this.pipe.write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), callBack);
    }

    @Override
    public <V> Message publishJob(String topic, V v) {
        Message job = Message.buildPublishJob(topic, v, getNode());
        this.pipe.write(job);
        Message jobResultListen = Message.buildJobResultListen(pipe.getId(), job);
        return sendAndRecvOfFuture(jobResultListen);
    }

    @Override
    public <V> Message acceptJob(String topic) {
        Message subscribe = Message.buildAcceptJob(topic, getNode());
        return sendAndRecvOfFuture(subscribe);
    }

    @Override
    public <V> boolean finishJob(String topic, V v, Message acceptJob) {
        try {
            Message finishJob = Message.buildFinishJob(getJobid(acceptJob), topic, v, getNode());
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

    /**
     * 实际上是任务发布者的 pipeId
     * @param job
     * @return
     */
    private Integer getJobid(Message job) {
        Integer jobId = job.getK().getSendNode();
        return jobId;
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


    @Nullable
    private Message sendAndRecvOfFuture(Message subscribe) {
        this.pipe.write(subscribe);
        CompletableFuture<Message> future = new CompletableFuture<>();
        futureResultMap.put(subscribe.getSubscribeId(), future);
        Message result = future.join();
        if (MqConfig.mq_auto_acked && result != null) { // autoAck
            ack(subscribe.getK().getId(), Message.Life.SPARK);
        }
        return result;
    }


}
