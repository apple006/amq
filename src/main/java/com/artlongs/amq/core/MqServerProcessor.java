package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class MqServerProcessor extends AioBaseProcessor<ByteBuffer> {
    private static Logger logger = LoggerFactory.getLogger(MqServerProcessor.class);

/*    @Override
    public void process0(AioPipe pipe, Message msg) {
        logger.debug("[S]:"+msg);
        ProcessorImpl.INST.onMessage(pipe, msg);
    }*/

    @Override
    public void process0(AioPipe<ByteBuffer> pipe, ByteBuffer buffer) {
        ByteBuffer tempBuffer = ByteBuffer.allocate(buffer.limit());
        tempBuffer.put(buffer);
//        buffer = null; // 这里把buffer清空,以让Aio读取准确的进行下一次读取.
//        CompletableFuture.runAsync(() -> ProcessorImpl.INST.onMessage(pipe, tempBuffer));
        ProcessorImpl.INST.onMessage(pipe, tempBuffer);
//        ProcessorImpl.INST.onMessage(pipe, buffer);
//        setBufferToReaded(buffer);
        return;
    }

    @Override
    public void stateEvent0(AioPipe session, State state, Throwable throwable) {

    }

    private void setBufferToReaded(ByteBuffer buffer) {
        buffer.position(buffer.limit());
    }
}
