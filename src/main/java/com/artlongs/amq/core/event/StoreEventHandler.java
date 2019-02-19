package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.disruptor.WorkHandler;

/**
 * Func : 存储事件 Handler
 *
 * @author: leeton on 2019/2/15.
 */
public class StoreEventHandler implements WorkHandler<JobEvent> {
    @Override
    public void onEvent(JobEvent event) throws Exception {
        System.err.println(" 执行消息保存到硬盘 ......");
        Message message = event.getMessage();
        Store.INST.save(message.getK().getId(), message);
    }
}
