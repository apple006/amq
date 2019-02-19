package com.artlongs.amq.serializer;


import java.io.Serializable;
import java.nio.ByteBuffer;

public interface ISerializer {

    public byte[] toByte(Object obj);
    public <T extends Serializable> T getObj(byte[] bytes);
    public <T extends Serializable> T getObj(ByteBuffer byteBuffer);
    public <T extends Serializable> T getObj(ByteBuffer byteBuffer, Class<T> clzz);


    enum Serializer{
        INST;
        public ISerializer of(){
            return new FastJsonSerializer();
        }
    }
}
