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

    void onMessage(AioPipe<Message> pipe, Message message);

    Message getMessage(String msgId);

    void publishJobToWorker(Message message);

    FastList<Subscribe> subscribeOfTopic(String topic);

    FastList<Subscribe> subscribeOfDirect(String directTopic);

    void sendMessageToSubcribe(List<Subscribe> subscribeList, Message message);

    Runnable delaySendOnScheduled();

    Runnable retrySendOnScheduled();

}
