package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.State;

import java.util.concurrent.Future;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqServerProcessor implements AioProcessor<Message> {
    @Override
    public void process(AioPipe pipe, Message msg) {
        System.err.println(msg);
        ProcessorImpl.INST.onMessage(pipe, msg);
    }

    @Override
    public void stateEvent(AioPipe session, State state, Throwable throwable) {

    }
}
