package com.artlongs.amq.demo;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.State;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqClientProcessor implements AioProcessor<Message> {
    @Override
    public void process(AioPipe<Message> pipe, Message msg) {
        System.err.println(msg);

    }

    @Override
    public void stateEvent(AioPipe<Message> session, State state, Throwable throwable) {

    }
}
