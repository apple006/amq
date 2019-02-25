package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.ProcessorImpl;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.disruptor.WorkHandler;

import java.util.List;

/**
 * Func : 业务事件 Handler
 *
 * @author: leeton on 2019/2/13.
 */
public class BuzEventHandler implements WorkHandler<JobEvent> {
    @Override
    public void onEvent(JobEvent event) throws Exception {
        System.err.println(" 收到业务处理事件 ......");
        Message message = event.getMessage();
        if (!message.isSubscribe()) {
            System.err.println(" 执行业务消息的匹配及发送 ......");
            String topic = message.getK().getTopic();
            switch (message.getK().getSpread()) {
                case TOPIC:
                    List<Subscribe> subscribeList = ProcessorImpl.INST.subscribeOfTopic(topic);
                    ProcessorImpl.INST.sendMessageToSubcribe(subscribeList, message);
                    break;

                case DIRECT:
                    List<Subscribe> directList = ProcessorImpl.INST.subscribeOfDirect(topic);
                    ProcessorImpl.INST.sendMessageToSubcribe(directList, message);
                    break;
            }
        }
    }

}
