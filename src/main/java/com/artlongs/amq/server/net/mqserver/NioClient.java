package com.artlongs.amq.server.net.mqserver;

import com.artlongs.amq.serializer.FastJsonSerializer;
import com.artlongs.amq.server.core.Message;
import com.artlongs.amq.server.core.MqConfig;
import com.artlongs.amq.server.net.IOUtils;
import com.artlongs.amq.tools.ID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Func :
 * Created by leeton on 2018/12/25.
 */
public class NioClient {
    //管道管理器
    private Selector selector;
    private SocketChannel channel;
    private FastJsonSerializer json = new FastJsonSerializer();

    public NioClient init(String serverIp, int port) throws IOException {
        //获取socket通道
        channel = SocketChannel.open();

        channel.configureBlocking(false);
        //获得通道管理器
        selector = Selector.open();
        channel.connect(new InetSocketAddress(serverIp, port));
        //为该通道注册SelectionKey.OP_CONNECT事件
        channel.register(selector, SelectionKey.OP_CONNECT);
        return this;
    }

    public void listen() throws IOException {
        System.out.println("客户端启动");
        //轮询访问selector
        while (true) {
            int readyChannels = selector.select();
            if (readyChannels == 0) continue;
            Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey key = ite.next();
                if (key.isConnectable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    //客户端连接服务器，需要调用channel.finishConnect(),才能实际完成连接。
                    if (channel.isConnectionPending()) {
                        channel.finishConnect();
                    }
                    Message.Key mKey = new Message.Key(ID.ONLY.id(), "quenu", Message.SPREAD.TOPIC);
                    Message<Message.Key, String> message = Message.ofDef(mKey, " This message from clinet .");
                    IOUtils.write(key, json.toByte(message));
                } else if (key.isValid() && key.isReadable()) { //有可读数据事件。
                    IOUtils.print(IOUtils.read(key));
                }
                // 删除已选的key，防止重复处理
                ite.remove();
            }
        }
    }

    public void send(Object content) {

    }


    public static void main(String[] args) throws IOException {
        NioClient client = new NioClient().init(MqConfig.address, MqConfig.port);
/*
        SocketChannel channel = client.channel;

        //如果正在连接，则完成连接
        if(channel.isConnectionPending()){
            channel.finishConnect();
        }

        //向服务器发送消息
        channel.write(ByteBuffer.wrap(new String(" This message from clinet .").getBytes()));

        //连接成功后，注册接收服务器消息的事件
        channel.register(client.selector, SelectionKey.OP_READ);
*/

        client.listen();


    }
}
