package com.artlongs.amq.server.net.mqserver;

import com.artlongs.amq.server.core.MqConfig;
import com.artlongs.amq.server.core.MqServer;
import com.artlongs.amq.server.disruptor.BusySpinWaitStrategy;
import com.artlongs.amq.server.disruptor.RingBuffer;
import com.artlongs.amq.server.disruptor.dsl.Disruptor;
import com.artlongs.amq.server.disruptor.dsl.ProducerType;
import com.artlongs.amq.server.net.IOUtils;
import com.artlongs.amq.server.net.Write;
import com.artlongs.amq.server.net.http.HttpEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Func : AioMqServer
 * Created by leeton on 2018/12/14.
 */
public class AioMqServer implements MqServer,Runnable {

    private static Logger logger = LoggerFactory.getLogger(AioMqServer.class);
    protected MqConfig config ;
    private AsynchronousServerSocketChannel serverSocket = null;
    private Write writer;
    public  RingBuffer<HttpEventHandler> ringBuffer;
    private ExecutorService connectThreadPool;

    public AioMqServer(MqConfig config) {
        this.config = config;
    }

    public void start() {
        try {
            connectThreadPool = Executors.newFixedThreadPool(config.connect_thread_pool_size);
            AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(connectThreadPool);
            serverSocket = AsynchronousServerSocketChannel.open(group);
            serverSocket.bind(new InetSocketAddress(config.address, config.port));
            serverSocket.accept(null, new AioMqAcceptHandler(this));
            this.ringBuffer = buildRingBuffer(); //创建 RingBuffer
            //
            daemon(this);

        } catch (IOException e) {
            throw new RuntimeException(" http start on Error:" + e);
        }
        logger.warn("AMQ had started,listening {}:{}",config.address,config.port);
    }

    /**
     * 创建 RingBuffer
     * @return
     */
    public RingBuffer<HttpEventHandler> buildRingBuffer(){
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024 * 128;

        HttpEventHandler httpEvent  = new HttpEventHandler();
        // Construct the Disruptor
        Disruptor<HttpEventHandler> disruptor = new Disruptor<>(httpEvent, bufferSize, threadFactory,ProducerType.SINGLE,new BusySpinWaitStrategy());
        // Connect the handler
        disruptor.handleEventsWith(httpEvent);
        return disruptor.start();
    }

    @Override
    public void regClient(NetworkChannel channel) {
        clientSocketMap.putIfAbsent(IOUtils.getRemoteAddress(channel).toString(), channel);
    }


    public void shutdown() {

    }

    @Override
    public void run() {
        start();
    }

    public void daemon(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        connectThreadPool.submit(t);
    }

    public AsynchronousServerSocketChannel getServerSocket() {
        return serverSocket;
    }

    void closeConn(AsynchronousSocketChannel client) {
        try {
            if(null != client && client.isOpen()) {
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void accept() {

    }

    @Override
    public void assignJob() {

    }

    @Override
    public NetworkChannel getServerChannel() {
        return null;
    }

    @Override
    public MqConfig getConfig() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }

    public static void main(String[] args) {
        new AioMqServer(new MqConfig()).start();;
    }
}
