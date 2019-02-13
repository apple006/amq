package com.artlongs.amq.serializer;

import com.alibaba.fastjson.JSON;
import com.artlongs.amq.core.Message;
import com.artlongs.amq.tools.io.Buffers;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Created by leeton on 8/31/17.
 */
public class FastJsonSerializer implements ISerializer {
    @Override
    public byte[] toByte(Object obj) {
        return JSON.toJSONBytes(obj);
    }


    @Override
    public <T extends Serializable>  T getObj(ByteBuffer byteBuffer) {
        byteBuffer.rewind(); // 重置position,有可能 buffer 在之前已经被读取过
        final int remaining = byteBuffer.remaining();
        byte[] bytes = new byte[remaining];
        if (byteBuffer.isDirect()) {
           bytes = Buffers.take(byteBuffer);
        }else {
            bytes = byteBuffer.array();
        }
        T obj = JSON.parseObject(bytes,Message.class);
        return obj;
    }


    @Override
    public <T extends Serializable>  T getObj(ByteBuffer byteBuffer, Class<T> clzz) {
        T obj = JSON.parseObject(byteBuffer.array(), clzz);
        return obj;
    }


    public static void main(String[] args) {
        Message<Message.Key,String> msgEntity = Message.ofDef(new Message.Key("id","quene",Message.SPREAD.TOPIC),"test content");
        byte[] bytes = new FastJsonSerializer().toByte(msgEntity);
        Message entity =  new FastJsonSerializer().getObj(ByteBuffer.wrap(bytes));
        System.out.println("entity = [" + entity + "]");
    }

}
