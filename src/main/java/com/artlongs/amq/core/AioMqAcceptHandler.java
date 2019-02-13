package com.artlongs.amq.core;

import com.artlongs.amq.tools.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author leeton
 * 2018年2月6日
 */
public class AioMqAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private AsynchronousServerSocketChannel serverSocket;
    private AsynchronousSocketChannel client;
    private MqServer server;
    private MqConfig config;

    public AioMqAcceptHandler(MqServer server) {
        this.serverSocket = (AsynchronousServerSocketChannel) server.getServerChannel();
        this.server = server;
        config = server.getConfig();
    }

    @Override
    public void completed(AsynchronousSocketChannel client, Void attachment) {
        server.regClient(client); // 注册客户端 channel
//		serverSocket.accept(attachment, this);
//		ByteBuffer byteBuf = ByteBuffer.allocate(256);// 内部堆 heap buffer
        ByteBuffer byteBuf = config.mq_buffer_pool.allocate().getResource(); // 分配外部 direct buffer
//		client.read(byteBuf,config.read_wait_timeout,TimeUnit.SECONDS,byteBuf, new AioMqWriteHandler(server,client));
        this.client = client;
        client.read(byteBuf, config.read_wait_timeout, TimeUnit.SECONDS, byteBuf, new Reader(client));
        serverSocket.accept(attachment, this); // 循环调用自身
    }

    @Override
    public void failed(Throwable e, Void attachment) {
        e.printStackTrace();
    }

    private class Reader implements CompletionHandler<Integer, ByteBuffer> {

        private AsynchronousSocketChannel channel;

        public Reader(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void completed(Integer result, ByteBuffer buffer) {
            show(buffer);
            // 把收到的数据加入到消息处理中心
            ProcessorImpl.INST.add(buffer);
            // 测试把消息回传到客户端
            IOUtils.write(client, buffer, writeHandler);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            System.err.println("amq - read on error .");
        }
    }

    private void show(ByteBuffer data) {
        data.flip();
        System.err.println("REC:" + new StringBuilder(StandardCharsets.UTF_8.decode(data)).toString());
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
