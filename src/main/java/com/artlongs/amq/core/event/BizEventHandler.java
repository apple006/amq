package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.ProcessorImpl;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Func : 业务事件 Handler
 *
 * @author: leeton on 2019/2/13.
 */
public class BizEventHandler implements WorkHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(BizEventHandler.class);
    @Override
    public void onEvent(JobEvent event) throws Exception {
        Message message = event.getMessage();
        logger.debug(" 执行业务消息的匹配与发送 ......");
        String topic = message.getK().getTopic();
        switch (message.getK().getSpread()) {
            case TOPIC:
                List<Subscribe> subscribeList = ProcessorImpl.INST.subscribeOfTopic(topic);
                ProcessorImpl.INST.sendMessageToSubcribe(message,subscribeList);
                break;

            case DIRECT:
                List<Subscribe> directList = ProcessorImpl.INST.subscribeOfDirect(topic);
                ProcessorImpl.INST.sendMessageToSubcribe(message,directList);
                break;
        }
    }

}
