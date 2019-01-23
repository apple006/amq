package com.artlongs.amq.server.core;

/**
 * Func :
 *
 * @author: leeton on 2019/1/15.
 */
public interface MqClient {
    void config();
    void listen();
    void addJob(Message message);
}
