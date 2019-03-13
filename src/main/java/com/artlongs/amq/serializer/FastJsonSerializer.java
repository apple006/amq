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

    public Message getObj(byte[] bytes) {
        return JSON.parseObject(bytes, Message.class);
    }


    @Override
    public Message getObj(ByteBuffer byteBuffer) {
        byte[] bytes = Buffers.take(byteBuffer, false);
        Message obj = JSON.parseObject(bytes, Message.class);
        return obj != null ? obj : Message.empty();
    }


    @Override
    public <T extends Serializable> T getObj(ByteBuffer byteBuffer, Class<T> clzz) {
        T obj = JSON.parseObject(byteBuffer.array(), clzz);
        return obj;
    }

    public static void main(String[] args) {
        User user = new User(1, "alice");
        Message<Message.Key, User> msgEntity = Message.ofDef(new Message.Key("id", "quene"), user);
        byte[] bytes = new FastJsonSerializer().toByte(msgEntity);
        Message entity = new FastJsonSerializer().getObj(ByteBuffer.wrap(bytes));
        System.out.println("entity = [" + entity + "]");

    }

    public static class User {
        private Integer id;
        private String name;

        public User(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public User setId(Integer id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public User setName(String name) {
            this.name = name;
            return this;
        }
    }

}
