package com.artlongs.amq.core.aio;

import java.nio.channels.CompletionHandler;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class Writer<T> implements CompletionHandler<Integer, AioPipe<T>> {

    @Override
    public void completed(Integer result, AioPipe<T> attachment) {

    }

    @Override
    public void failed(Throwable exc, AioPipe<T> attachment) {

    }
}
