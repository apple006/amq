package com.artlongs.amq.core.aio;

/**
 * Func : Aio 处理器
 *
 * @author: leeton on 2019/2/22.
 */
public interface AioProcessor<T> {
    void process(AioPipe<T> pipe, T msg);
    void stateEvent(AioPipe<T> pipe, State state, Throwable throwable);
    void addPlugin(Plugin plugin);
    Monitor getMonitor();
}
