package com.artlongs.amq.core;

/**
 * FUNC: MqClientAction
 * Created by leeton on 2019/1/15.
 */
public interface MqClientAction {

    void subscribe(String topic, Call callBack);

    <V> void subscribe(String topic, V v, Call callBack);

    /**
     * 手动拉取消息,只获取一次
     * @param topic
     * @param v
     * @return
     */
    <V> Message pingpong(String topic, V v);

    <V> void publish(String topic, V v);

    /**
     * 确认收到消息
     *
     * @param messageId
     * @param life
     * @return
     */
    boolean ack(String messageId, Subscribe.Life life);


}
