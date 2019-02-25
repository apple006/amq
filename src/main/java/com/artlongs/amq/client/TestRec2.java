package com.artlongs.amq.client;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/2/14.
 */
public class TestRec2 {
    public static void main(String[] args) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.connect_thread_pool_size);

        NioClient client = new NioClient(MqConfig.host, MqConfig.port);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);

        NioClient.RspHandler handler = new NioClient.RspHandler();
//        client.send(client.buildSubscribe("topic_hello",null), handler);
        client.send(client.buildAcked("2019021914534240901"), handler);
        Message message = handler.waitForResponse();
        //

    }
}
