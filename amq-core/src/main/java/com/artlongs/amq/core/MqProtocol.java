package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.Protocol;

import java.nio.ByteBuffer;

/**
 * Func : Mq 协议
 * NOTE : 原本的编码/解码功能,迁移到 MQ 中心处理,这样效率比较高
 * {@link com.artlongs.amq.core.event.JobEvnetHandler}
 * {@link com.artlongs.amq.core.MqClientProcessor#write(Message)}
 *
 * @author: leeton on 2019/2/22.
 */
public class MqProtocol implements Protocol<ByteBuffer> {
   /*
   private static ISerializer serializer = ISerializer.Serializer.INST.of();

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



}
