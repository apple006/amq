package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.tools.FastList;

import java.util.List;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/18.
 */
public interface Processor {

    /**
     * 收到消息的逻辑处理
     * @param pipe
     * @param message
     */
    void onMessage(AioPipe<Message> pipe, Message message);

    void publishJobToWorker(Message message);

    FastList<Subscribe> subscribeMatchOfTopic(String topic);

    void sendMessageToSubcribe(Message message, List<Subscribe> subscribeList);

    Runnable delaySendOnScheduled();

    Runnable retrySendOnScheduled();

}
