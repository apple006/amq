package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqServerProcessor extends AioBaseProcessor<Message> {
    private static Logger logger = LoggerFactory.getLogger(MqServerProcessor.class);

    @Override
    public void process0(AioPipe pipe, Message msg) {
        logger.debug("[S]:"+msg);
        ProcessorImpl.INST.onMessage(pipe, msg);
    }

    @Override
    public void stateEvent0(AioPipe session, State state, Throwable throwable) {

    }
}
