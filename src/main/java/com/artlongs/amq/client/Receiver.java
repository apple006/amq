package com.artlongs.amq.client;

import com.artlongs.amq.server.core.Message;

/**
 * Func :
 * Created by leeton on 2018/12/24.
 */
public interface Receiver {
    void regToMqCPU(MqReceiver me);
    Message subcribe(Message.Key key);
}
