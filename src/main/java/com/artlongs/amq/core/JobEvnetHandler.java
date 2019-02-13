package com.artlongs.amq.core;

import com.artlongs.amq.disruptor.EventHandler;

/**
 * Func :
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvnetHandler implements EventHandler<JobEvent> {

    @Override
    public void onEvent(JobEvent event, long sequence, boolean endOfBatch) throws Exception {
        // TODO: 消息处理及发送
        System.err.println(" 收到 ringbuffer 的事件");

    }
}
