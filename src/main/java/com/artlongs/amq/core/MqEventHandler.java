package com.artlongs.amq.core;

import com.artlongs.amq.disruptor.BatchStartAware;
import com.artlongs.amq.disruptor.EventHandler;

import java.nio.channels.SelectionKey;

/**
 * Func : Action EVENT
 * Created by leeton on 2018/12/20.
 */
public class MqEventHandler implements EventHandler<MqAcceptEvent>, BatchStartAware {

    private Message message;

    @Override
    public void onEvent(MqAcceptEvent event, long sequence, boolean endOfBatch) throws Exception {
        runJob(event.getVal());
    }

    @Override
    public void onBatchStart(long batchSize) {

    }

    /**
     * 任务分派器的进阶版，耦合性降低，拓展性增强
     * 子类只需要实现Runnable接口，并重写run()方法，就可以实现多种任务的无差别分派
     *
     * @param taskSelectionKey
     */
    private void runJob(SelectionKey taskSelectionKey) {
        Runnable runnable = (Runnable)taskSelectionKey.attachment();
        if (runnable != null) {
            runnable.run();
        }
    }
}
