package com.artlongs.amq.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 *
 */
public interface Write {
    public void write(ByteBuffer byteBuffer);

    /**
     * 苦力
     */
    class Worker implements Write {
        private Logger logger = LoggerFactory.getLogger(this.getClass());
        private AsynchronousSocketChannel client;
        public Worker(AsynchronousSocketChannel client) {
            this.client = client;
        }

        @Override
        public void write(ByteBuffer byteBuffer) {
            client.write(byteBuffer, null, writeHandler);
        }

        private final CompletionHandler writeHandler = new CompletionHandler() {
            @Override
            public void completed(Object result, Object attachment) {

            }

            @Override
            public void failed(Throwable exc, Object attachment) {

            }
        };

    }
}
