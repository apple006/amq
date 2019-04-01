package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.tools.IOUtils;
import org.osgl.util.C;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqClientProcessor extends AioBaseProcessor<ByteBuffer> implements MqClientAction {

    private AioPipe<ByteBuffer> pipe;

    /**
     * 客户端返回的消息包装
     */
    private static Map<String, Call> callBackMap = new ConcurrentHashMap<>();
    /**
     * 客户端返回的消息包装
     */
    private static Map<String, CompletableFuture<Message>> futureResultMap = new ConcurrentHashMap<>();
    //
    ISerializer serializer = ISerializer.Serializer.INST.of();

    @Override
    public void process0(AioPipe<ByteBuffer> pipe, ByteBuffer buffer) {
        try {
            Message message = serializer.getObj(buffer);
            if (null != message) {
                String subscribeId = message.getSubscribeId();
                if (C.notEmpty(callBackMap) && null != callBackMap.get(subscribeId)) {
                    callBackMap.get(subscribeId).back(message);
                }
                if (C.notEmpty(futureResultMap) && null != futureResultMap.get(subscribeId)) {
                    futureResultMap.get(subscribeId).complete(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public <V>void subscribe(String topic, Call<V> callBack) {
        subscribe(topic, null, callBack);
    }

    @Override
    public <V> void subscribe(String topic, V v, Call<V> callBack) {
        Message subscribe = Message.buildSubscribe(topic, v, getNode(), Message.Life.ALL_ACKED, Message.Listen.CALLBACK);
        write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), callBack);
    }

    @Override
    public <V> Message publishJob(String topic, V v) {
        Message job = Message.buildPublishJob(topic, v, getNode());
        String jobId = job.getK().getId();
        // 发布一个 future ,等任务完成后读取结果.
        futureResultMap.put(jobId, new CompletableFuture<Message>());
        write(job);
        Message result = futureResultMap.get(jobId).join();
        if (null != result) {
            removeFutureResultMap(result.getSubscribeId());
            if (MqConfig.inst.mq_auto_acked) {
                ack(result.getSubscribeId(), Message.Life.SPARK);
            }
        }

        return result;
    }

    @Override
    public void acceptJob(String topic, Call acceptJobThenExecute) {
        Message subscribe = Message.buildAcceptJob(topic, getNode());
        write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), acceptJobThenExecute);
    }

    public <V> boolean finishJob(String topic, V v, Message acceptJob) {
        try {
            Message<Message.Key, V> finishJob = Message.buildFinishJob(acceptJob.getK().getId(), topic, v, getNode());
            return write(finishJob);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public <M> boolean onlyPublish(String topic, M data) {
        try {
            Message message = Message.buildCommonMessage(topic, data, getNode());
            return write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean ack(String messageId, Message.Life life) {
        Message message = Message.buildAck(messageId, life);
        return write(message);
    }

    private boolean write(Message obj) {
        return this.pipe.write(IOUtils.wrap(obj));
    }

    @Override
    public void stateEvent0(AioPipe<ByteBuffer> pipe, State state, Throwable throwable) {
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

    private Integer getNode() {
        return this.pipe.getId();
    }


}
