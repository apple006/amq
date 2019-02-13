package com.artlongs.amq.core;

import com.artlongs.amq.disruptor.EventFactory;

import java.nio.channels.SelectionKey;

/**
 * Func :
 *
 * @author: leeton on 2019/1/15.
 */
public class MqAcceptEvent {

    private SelectionKey val;

    public SelectionKey getVal() {
        return val;
    }

    public MqAcceptEvent setVal(SelectionKey val) {
        this.val = val;
        return this;
    }

    public static final EventFactory<MqAcceptEvent> EVENT_FACTORY = new EventFactory<MqAcceptEvent>() {
        public MqAcceptEvent newInstance()
        {
            return new MqAcceptEvent();
        }
    };
}
