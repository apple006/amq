package com.artlongs.amq.server.net;

import com.artlongs.amq.server.core.MqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;

/**
 * Func : NIO Untils
 * Created by leeton on 2018/12/26.
 */
public class IOUtils {
    private static Logger logger = LoggerFactory.getLogger(IOUtils.class);
    public static void print(ByteBuffer buf){
        if (null != buf && buf.remaining() >0) {
            buf.flip();
            logger.info("[REC]:"+new StringBuilder(StandardCharsets.UTF_8.decode(buf)).toString());
        }
    }

    public static void print(byte[] bytes) {
        if (null != bytes) {
            logger.info("[REC]:" + new String(bytes));
        }
    }

    /**
     * NIO 读取数据
     * @param key
     * @return
     */
     public static byte[] read(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer in = (ByteBuffer) key.attachment();
        if (in != null) {
            try {
                int size = sc.read(in);
                if (size > 0) {
                    byte[] bytes = new byte[size];
                    in.flip();
                    for (int i = 0; i < size; i++) {
                        byte b = in.get();
                        bytes[i] = b;
                    }
                    in.clear();
                    return bytes;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * NIO 写数据
     * @param key
     * @param attachment
     */
    public static void write(SelectionKey key,byte[] attachment) {
        SocketChannel sc = (SocketChannel)key.channel();
        try {
            if(sc.isOpen()){
//                ByteBuffer buffer = ByteBuffer.allocate(8192);
                ByteBuffer buffer = MqConfig.mq_buffer_pool.allocate().getResource();// 分配外部 direct buffer
                buffer.put(attachment);
                buffer.position(0);
                buffer.limit(attachment.length);
                sc.write(buffer);
                // sc.register 有 BUG , 一定要把buffer传入,并且加上 buffer.clear(),客户端才能收到消息
                sc.register(key.selector(), SelectionKey.OP_READ,buffer);
                buffer.clear();

            }
        } catch (IOException e) {
            logger.error("[x] connection to {} is broken.",getRemoteAddress(sc));
            closeChannel(sc);
            e.printStackTrace();
        }
    }

    /**
     * Aio write data
     * @param client
     * @param buffer
     * @param completionHandler
     */
    public static void write(AsynchronousSocketChannel client, ByteBuffer buffer, CompletionHandler completionHandler) {
        buffer.rewind();
        client.write(buffer, null, completionHandler);
    }

    public static void closeChannel(SocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getRemoteAddress(SocketChannel channel){
        try {
           return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static InetSocketAddress getRemoteAddress(NetworkChannel channel) {
        try {
            return (InetSocketAddress)((AsynchronousSocketChannel)channel).getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
