package com.artlongs.amq.server.mq;

import com.artlongs.amq.server.http.HttpResolver;
import com.artlongs.amq.server.Write;
import com.artlongs.amq.server.http.HttpEventHandler;
import com.artlongs.amq.server.http.HttpServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author leeton
 * 2018年2月6日
 */
public class AioMqWriteHandler implements CompletionHandler<Integer, ByteBuffer> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private HttpResolver resolver = null;
    private AsynchronousSocketChannel client = null;
    private AioMqServer server = null;
    public static HttpServerState state = null;

    public AioMqWriteHandler(AioMqServer server, AsynchronousSocketChannel client) {
        this.client = client;
        this.server = server;
        new Write.Worker(client); // 真正的写操作
    }

    @Override
    public void completed(Integer result, ByteBuffer data) {
        if (result == -1) {
            server.closeConn(client);
            return;
        }

//        send(data);
    }

    @Override
    public void failed(Throwable ex, ByteBuffer data) {
        logger.debug("read failed,maybe resource not exist. exception msg:{}", ex.getMessage());
        server.closeConn(client);
    }

    private void send(ByteBuffer data) {
        server.ringBuffer.publishEvent(HttpEventHandler::buildAndSet, data, resolver);
    }


}
