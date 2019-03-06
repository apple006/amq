package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioServer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class AioMqServer {

    public static void main(String[] args) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.server_connect_thread_pool_size);

        AioServer<Message> aioServer = new AioServer<>(MqConfig.host, MqConfig.port, new MqProtocol(), new MqServerProcessor());
        Thread t = new Thread(aioServer);
        t.setDaemon(true);
        pool.submit(t);
        aioServer.start();

    }

}
