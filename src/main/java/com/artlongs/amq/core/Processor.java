package com.artlongs.amq.core;

import com.artlongs.amq.tools.FastList;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NetworkChannel;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/18.
 */
public interface Processor {

    ConcurrentMap<String, ByteBuffer> onData(AsynchronousSocketChannel channel ,ByteBuffer buffer);

    Buffer getBuffer(String msgId);

    Message parser(ByteBuffer buffer);

    void publishJobToWorker(Message message);

    FastList<Subscribe> subscribeOfTopic(String topic);

    FastList<Subscribe> subscribeOfDirect(String directTopic);

    void sendMessageToSubcribe(List<Subscribe> subscribeList, Message message);

    void sendMessageOfFanout(Message message);

    NetworkChannel getTargetChannel(String keyOfChannel);



}
