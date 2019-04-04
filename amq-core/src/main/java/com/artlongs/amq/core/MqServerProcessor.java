package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioBaseProcessor;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import com.artlongs.amq.serializer.ISerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Func : Mq 消息处理
 *
 * @author: leeton on 2019/2/25.
 */
public class MqServerProcessor extends AioBaseProcessor<ByteBuffer> {
    private static Logger logger = LoggerFactory.getLogger(MqServerProcessor.class);
    ISerializer serializer = ISerializer.Serializer.INST.of();
    @Override
    public void process0(AioPipe<ByteBuffer> pipe, ByteBuffer buffer) {
        ProcessorImpl.INST.onMessage(pipe, buffer);
        setRead(buffer);
    }

    @Override
    public void stateEvent0(AioPipe session, State state, Throwable throwable) {

    }

    private void setRead(ByteBuffer buffer) {
        buffer.position(buffer.limit());
    }

    private Message decode(ByteBuffer buffer) {
        try {
            return serializer.getObj(buffer,Message.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
