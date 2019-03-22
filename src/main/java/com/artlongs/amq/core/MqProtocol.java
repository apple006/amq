package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.DirectBufferUtil;
import com.artlongs.amq.core.aio.Protocol;
import com.artlongs.amq.serializer.ISerializer;

import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class MqProtocol implements Protocol<ByteBuffer> {
    private static ISerializer serializer = ISerializer.Serializer.INST.of();

/*
    @Override
    public ByteBuffer encode(Message message) {
        return wrap(serializer.toByte(message));
    }

    @Override
    public Message decode(ByteBuffer buffer) {
        return serializer.getObj(buffer);
    }
*/

    @Override
    public ByteBuffer encode(ByteBuffer buffer) {
        return buffer;
    }

    @Override
    public ByteBuffer decode(ByteBuffer buffer) {
        return buffer;
    }

    private ByteBuffer wrap(byte[] bytes) {
        ByteBuffer buffer = DirectBufferUtil.allocateDirectBuffer(bytes.length);
        buffer.put(bytes);
        buffer.rewind();
        return buffer;
    }

}
