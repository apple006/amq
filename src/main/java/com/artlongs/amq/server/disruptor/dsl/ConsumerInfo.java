package com.artlongs.amq.server.disruptor.dsl;

import com.artlongs.amq.server.disruptor.Sequence;
import com.artlongs.amq.server.disruptor.SequenceBarrier;

import java.util.concurrent.Executor;

interface ConsumerInfo
{
    Sequence[] getSequences();

    SequenceBarrier getBarrier();

    boolean isEndOfChain();

    void start(Executor executor);

    void halt();

    void markAsUsedInBarrier();

    boolean isRunning();
}
