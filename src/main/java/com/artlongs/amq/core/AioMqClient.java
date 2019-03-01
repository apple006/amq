package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioClient;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.Protocol;
import com.artlongs.amq.serializer.ISerializer;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class AioMqClient<T> extends AioClient<T> {
    private static ISerializer serializer = ISerializer.Serializer.INST.of();

    private AioPipe aioPipe;

    public AioMqClient(String host, int port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(host, port, protocol, messageProcessor);
    }

    @Override
    public AioPipe<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        aioPipe = super.start(asynchronousChannelGroup);
        return aioPipe;
    }

    @Override
    public void run() {
        super.run();
    }


}