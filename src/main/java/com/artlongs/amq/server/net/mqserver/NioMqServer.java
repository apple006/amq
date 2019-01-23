package com.artlongs.amq.server.net.mqserver;

import com.artlongs.amq.server.core.Message;
import com.artlongs.amq.server.core.MqConfig;
import com.artlongs.amq.server.net.IOUtils;
import com.artlongs.amq.tools.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ${leeton} on 2018/10/26.
 */
public class NioMqServer implements  Runnable {
    private static Logger logger = LoggerFactory.getLogger(NioMqServer.class);
    private ServerSocketChannel socket;
    private Selector selector = null;
    private MqConfig config;
    private ExecutorService connectThreadPool;

    public NioMqServer(MqConfig config) {
        this.config = config;
        init();
    }

    private void init() {
        try {
            connectThreadPool = Executors.newFixedThreadPool(config.connect_thread_pool_size);
            selector = Selector.open();
            socket = ServerSocketChannel.open();
            socket.bind(new InetSocketAddress(MqConfig.address, config.port));
            socket.configureBlocking(false);
            socket.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            while (true) {
                int readyChannels = selector.select();
                if(readyChannels == 0) continue;
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isValid() && key.isReadable()) {
                        IOUtils.print(IOUtils.read(key));
                        key.interestOps(SelectionKey.OP_WRITE);
                        iter.remove();
                        continue;
                    }
                    if (key.isValid() &&  key.isWritable()) {
                        Message.Key mk = new Message.Key(ID.ONLY.id(), "quene", Message.SPREAD.TOPIC);
                        IOUtils.write(key,Message.ofDef(mk,new String("this message from server")).toString().getBytes());
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (socket != null && socket.isOpen()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssChannel.accept();
        if (sc != null) {
            sc.configureBlocking(false);
            sc.register(key.selector(), SelectionKey.OP_READ,ByteBuffer.allocate(1024));
        }
    }



    public void setDaemon(Runnable runnable) {
       /* Thread r = new Thread(this);
        r.daemon(true);
        threadPool.submit(this);
        r.start();
        System.out.println("d.isDaemon() = " + r.isDaemon() + ".");
*/
    }

    @Override
    public void run() {
    }

    public static void main(String[] args) throws IOException {
        NioMqServer connect = new NioMqServer(new MqConfig());
        connect.start();

    }


}