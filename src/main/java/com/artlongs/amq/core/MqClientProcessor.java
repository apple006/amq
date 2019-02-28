package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.State;
import com.artlongs.amq.tools.ID;

import java.util.Map;
import java.util.concurrent.*;

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
        System.err.println(" Client,收到消息: " );
        switch (msg.getListen()) {
            case CALLBACK:
                callBackMap.get(msg.getSubscribeId()).back(msg);
                break;
            case PINGPONG:
                futureResultMap.get(msg.getSubscribeId()).complete(msg);
                break;
        }
    }



    public void subscribeAndWaitBack(String topic) {
/*        Message subscribe = buildSubscribe(topic);
        this.pipe.write(subscribe);

        CompletableFuture<Message> future = new CompletableFuture<>();
        futureResultMap.put(subscribe.getSubscribeId(), future);

        return getMessageOfExecutor(subscribe.getSubscribeId());*/

    }

    private Message getMessageOfExecutor(String subscribeId){
        Message[] message = {null};
        Executor executor = Executors.newCachedThreadPool();
         executor.execute(()->{
           message[0] =  futureResultMap.get(subscribeId).join();
        });

        return message[0];
    }

    @Override
    public void subscribe(String topic, Call callBack) {
        subscribe(topic, null, callBack);
    }

    @Override
    public <V> void subscribe(String topic, V v, Call callBack) {
        Message subscribe = buildSubscribe(topic,v,Subscribe.Life.ALL_COMFIRM,Subscribe.Listen.CALLBACK);
        this.pipe.write(subscribe);
        callBackMap.put(subscribe.getSubscribeId(), callBack);
    }

    @Override
    public <V> Message pingpong(String topic, V v) {
        Message subscribe = buildSubscribe(topic,v,Subscribe.Life.SPARK,Subscribe.Listen.PINGPONG);
        this.pipe.write(subscribe);
        CompletableFuture<Message> future = new CompletableFuture<>();
        futureResultMap.put(subscribe.getSubscribeId(), future);
        Message message = future.join();
        if (message != null) { // autoAck
            ack(subscribe.getK().getId(),Subscribe.Life.SPARK);
        }
        return message;
    }

    @Override
    public <M> void publish(String topic, M data) {
        this.pipe.write(buildCommonMessage(topic, data, Message.SPREAD.TOPIC));
    }

    @Override
    public boolean ack(String messageId, Subscribe.Life life) {
        Message message = buildAck(messageId,life);
        pipe.write(message);
        return false;
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

    private <M> Message buildCommonMessage(String topic, M data, Message.SPREAD spread) {
        Message.Key mKey = new Message.Key(ID.ONLY.id(), topic, spread);
        String node = this.pipe.getLocalAddress().toString();
        mKey.setSendNode(node);
        return Message.ofDef(mKey, data);
    }

    private <V> Message buildSubscribe(String topic, V v, Subscribe.Life life, Subscribe.Listen listen) {
        Message.Key mKey = new Message.Key(ID.ONLY.id(), topic, Message.SPREAD.TOPIC);
        String node = this.pipe.getLocalAddress().toString();
        mKey.setSendNode(node);
        Message message = Message.ofSubscribe(mKey,v,life,listen);
        return message;
    }

    private Message buildAck(String msgId, Subscribe.Life life) {
        Message message = Message.ofAcked(msgId,life);
        return message;
    }


}
