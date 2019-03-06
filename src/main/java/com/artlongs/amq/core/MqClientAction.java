package com.artlongs.amq.core;

/**
 * FUNC: MqClientAction
 * Created by leeton on 2019/1/15.
 */
public interface MqClientAction {

    void subscribe(String topic, Call callBack);

    <V> void subscribe(String topic, V v, Call callBack);

    <V> Message publishJob(String topic, V v);

    void acceptJob(String topic,Call acceptJobThenExecute);
//    <V> boolean finishJob(String topic, V v,Message acceptJob);

    <V> boolean onlyPublish(String topic, V v);

    /**
     * 确认收到消息
     *
     * @param messageId
     * @param life
     * @return
     */
    boolean ack(String messageId, Message.Life life);


}
