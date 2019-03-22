package com.artlongs.amq.serializer;


import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.http.Render;
import com.artlongs.amq.tester.TestUser;
import com.artlongs.amq.tools.io.Buffers;
import org.nustaq.serialization.FSTConfiguration;

import java.nio.ByteBuffer;

public enum Fst implements ISerializer {
    inst;
    FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();
    FSTConfiguration fst_json = FSTConfiguration.createJsonConfiguration();
    Fst() {
        fst.registerClass(Message.class);
        fst.registerClass(Subscribe.class);
        fst.registerClass(Render.class);
    }

    public <T> byte[] toByte(T obj) {
        if (obj == null) return null;
        return fst.asByteArray(obj);
    }

    public <T> T getObj(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        T obj = (T) fst.asObject(bytes);
        return obj;
    }

    @Override
    public <T> T getObj(byte[] bytes, Class<T> clzz) {
        return getObj(bytes);
    }

    @Override
    public <T> T getObj(ByteBuffer byteBuffer) {
        byte[] bytes = Buffers.take(byteBuffer);
        return getObj(bytes);
    }

    @Override
    public <T> T getObj(ByteBuffer byteBuffer, Class<T> clzz) {
        byte[] bytes = Buffers.take(byteBuffer);
        return getObj(bytes, clzz);
    }

    public static void main(String[] args) {
        Fst fst = Fst.inst;
        Message msg = Message.buildCommonMessage("hello", new TestUser(1, "alice"), 127);
        byte[] jsonBytes = fst.toByte(msg);
        System.err.println(new String(jsonBytes));
        String jsonStr = fst.fst_json.asJsonString(msg);
        System.err.println(jsonStr);
        //
        long s = System.currentTimeMillis();
        int nums = 1_000;
        for (int i = 0; i < nums; i++) {
            fst.getObj(jsonBytes);
        }
        System.err.println("byte de used times(ms):" + (System.currentTimeMillis() - s));
        //
        byte[] jsonStrBtyes = jsonStr.getBytes();
        long s2 = System.currentTimeMillis();
        for (int i = 0; i < nums; i++) {
            fst.fst_json.asObject(jsonStrBtyes);
        }
        System.err.println("json de used times(ms):" + (System.currentTimeMillis() - s2));
    }

}