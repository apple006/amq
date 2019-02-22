package com.artlongs.amq.client;

import com.artlongs.amq.client.NioClient;
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
public class TestSender1 {

    public static void main(String[] args) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.connect_thread_pool_size);

        NioClient client = new NioClient(MqConfig.server_ip, MqConfig.port);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);

        NioClient.RspHandler handler1 = new NioClient.RspHandler();
        NioClient.RspHandler handler2 = new NioClient.RspHandler();
        client.send(client.buildMessage("topic_hello","hello,leeton",Message.SPREAD.TOPIC), handler1);
        handler1.waitForResponse();


    }

}
