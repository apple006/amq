package com.artlongs.amq.core;

import com.artlongs.amq.tools.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.NetworkChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/1/18.
 */
public class AioServer implements MqServer {
    private static Logger logger = LoggerFactory.getLogger(AioServer.class);

    protected MqConfig config;
    private ExecutorService connectThreadPool;
    private AsynchronousServerSocketChannel serverSocket = null;

    public AioServer(MqConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        try {
            connectThreadPool = Executors.newFixedThreadPool(config.connect_thread_pool_size);
            daemon(this);
            //
            AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(connectThreadPool);
            serverSocket = AsynchronousServerSocketChannel.open(group);
            serverSocket.bind(new InetSocketAddress(config.server_ip, config.port), config.max_connection);
            serverSocket.accept(null, new AioMqAcceptHandler(this));

        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.warn("AMQ had started,listening {}:{}", config.server_ip, config.port);
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void daemon(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        connectThreadPool.submit(t);
    }

    @Override
    public void accept() {

    }

    @Override
    public void assignJob() {

    }

    @Override
    public NetworkChannel getServerChannel() {
        return serverSocket;
    }

    @Override
    public void regClient(NetworkChannel channel) {
        clientSocketMap.putIfAbsent(getClientAddr(channel).toString(), channel);
    }

    private InetSocketAddress getClientAddr(NetworkChannel channel) {
        return IOUtils.getRemoteAddress(channel);
    }

    @Override
    public MqConfig getConfig() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return serverSocket.isOpen();
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    public static void main(String[] args) {
        new AioServer(new MqConfig()).start();

    }


}
