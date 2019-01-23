package com.artlongs.amq.server.core;

import java.nio.channels.SocketChannel;

/**
 * FUNC: Action
 * Created by leeton on 2019/1/15.
 */
public interface Action {

    /**
     * 简单的发送消息
     * @param message
     * @return
     */
    boolean send(Message message);

    /**
     * 发送消息,并包含回调
     * @param message
     * @param callback
     */
    void send(Message message, Call callback);

    /**
     * 监听消息
     * @param key
     * @return
     */
    Message listen(Message.Key key);

    /**
     * 确认收到消息
     * @param messageId
     * @return
     */
    boolean ack(String messageId);

    /**
     * 关闭 channel 连接
     * @param channel
     */
    void close(SocketChannel channel);


}
