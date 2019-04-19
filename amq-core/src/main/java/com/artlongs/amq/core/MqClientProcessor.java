package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.tools.IOUtils;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqClientProcessor extends AioBaseProcessor<ByteBuffer> implements MqClientAction {
    private static Logger logger = LoggerFactory.getLogger(MqClientProcessor.class);

    private AioPipe<ByteBuffer> pipe;

    private Semaphore clientExecLock = new Semaphore(1);

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
        prcessMsg(buffer);
    }

    private void prcessMsg(ByteBuffer buffer) {
        if (tryAcquire(50)) {
            try {
                Message message = serializer.getObj(buffer);
                if (null != message) {
                    clientExecLock.release();
                    String subscribeId = message.getSubscribeId();
                    if (C.notEmpty(callBackMap) && null != callBackMap.get(subscribeId)) {
                        callBackMap.get(subscribeId).back(message);
                    }
                    if (C.notEmpty(futureResultMap) && null != futureResultMap.get(subscribeId)) {
                        futureResultMap.get(subscribeId).complete(message);
                    }
                }
            } catch (Exception e) {
                logger.error("[C] Client prcess decode exception: ", e);
                clientExecLock.release();
            }
        }
    }

    private boolean tryAcquire(int timeout) {
        try {
            return clientExecLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public <V> void subscribe(String topic, Call<V> callBack) {
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
                ack(result.getSubscribeId());
            }
        }

        return result;
    }

    @Override
    public <V> void acceptJob(String topic, Call<V> acceptJobThenExecute) {
        Message subscribe = Message.buildAcceptJob(topic, getNode());
        write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), acceptJobThenExecute);
    }

    public <V> boolean finishJob(String topic, V v, String acceptJobId) {
        try {
            Message<Message.Key, V> finishJob = Message.buildFinishJob(acceptJobId, topic, v, getNode());
            return write(finishJob);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public <M> boolean publish(String topic, M data) {
        try {
            Message message = Message.buildCommonMessage(topic, data, getNode());
            return write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean ack(String messageId) {
        Message message = Message.buildAck(messageId, Message.Life.SPARK);
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
        if (State.NEW_PIPE != state) {
            logger.warn("[C]消息处理,状态:{}, EX:{}", state.toString(), throwable);
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
