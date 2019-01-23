package com.artlongs.amq.server.core;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/18.
 */
public interface Processor {

    Buffer getBuffer(String id);

    ConcurrentMap<String, ByteBuffer> addBuffer(ByteBuffer buffer);

    Message doParser(ByteBuffer buffer);

    void doMatch(Message message);

    Future toStore(Message message);

    void buildJob(Message message);

    void sendJobToRingBuffer(Message message);

    Future ackJob(Message message);


}
