package com.artlongs.amq.http.aio.handler;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * Func :
 *
 * @author: leeton on 2019/3/12.
 */
public class SocketWriteHandler implements CompletionHandler<Integer, ByteBuffer>,Cloneable{

    private AsynchronousSocketChannel client;

    public SocketWriteHandler(AsynchronousSocketChannel client,ByteBuffer buffer) {
        this.client = client;
    }

    public void write(ByteBuffer buffer) {
        client.write(buffer, null, this);
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {

    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {

    }
}
