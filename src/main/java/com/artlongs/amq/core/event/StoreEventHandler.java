package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func : 存储事件 Handler
 *
 * @author: leeton on 2019/2/15.
 */
public class StoreEventHandler implements WorkHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(StoreEventHandler.class);
    @Override
    public void onEvent(JobEvent event) throws Exception {
        logger.debug("[S]执行消息保存到硬盘 ......");
        Message message = event.getMessage();
        Store.INST.save(message.getK().getId(), message);
    }
}
