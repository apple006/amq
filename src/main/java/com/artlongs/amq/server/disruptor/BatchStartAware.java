package com.artlongs.amq.server.disruptor;

public interface BatchStartAware
{
    void onBatchStart(long batchSize);
}
