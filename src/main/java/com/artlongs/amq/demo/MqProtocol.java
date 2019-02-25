package com.artlongs.amq.demo;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.aio.Protocol;
import com.artlongs.amq.serializer.ISerializer;

import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class MqProtocol implements Protocol<Message> {
    private static ISerializer serializer = ISerializer.Serializer.INST.of();

    @Override
    public ByteBuffer encode(Message entity) {
        return ByteBuffer.wrap(serializer.toByte(entity));
    }

    @Override
    public Message decode(ByteBuffer buffer) {
        return serializer.getObj(buffer);
    }

}
