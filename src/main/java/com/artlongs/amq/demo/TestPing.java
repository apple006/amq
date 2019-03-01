package com.artlongs.amq.demo;

import com.artlongs.amq.core.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/3/1.
 */
public class TestPing {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.connect_thread_pool_size);
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(MqConfig.host, MqConfig.port, new MqProtocol(), processor);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);
        client.start();
        //

        Message message = processor.publishJob("topic_get_userById",1);
        System.err.println(message);





    }

}
