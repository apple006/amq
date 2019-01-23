package com.artlongs.amq.server.disruptor;

public interface TimeoutHandler
{
    void onTimeout(long sequence) throws Exception;
}
