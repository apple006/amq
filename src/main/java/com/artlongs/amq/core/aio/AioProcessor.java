package com.artlongs.amq.core.aio;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public interface AioProcessor<T> {
    void process(AioPipe<T> pipe, T msg);
    void stateEvent(AioPipe<T> session, State state, Throwable throwable);
}
