package com.artlongs.amq.client;

import com.artlongs.amq.core.Message;

/**
 * Func :
 * Created by leeton on 2018/12/24.
 */
public class MqReceiver implements Receiver {

    private MqReceiver me = null;

    @Override
    public void regToMqCPU(MqReceiver me) {
        if (null == me) {
            this.me = me;
        }
    }

    @Override
    public Message subcribe(Message.Key key) {
        return null;
    }
}
