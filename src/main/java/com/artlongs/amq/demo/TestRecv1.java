package com.artlongs.amq.demo;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.core.aio.AioPipe;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class TestRecv1 {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.connect_thread_pool_size);

        AioMqClient<Message> client = new AioMqClient(MqConfig.host, MqConfig.port, new MqProtocol(), new MqClientProcessor());
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);
        client.start();

        AioPipe<Message> pipe = client.getPipe();
        pipe.write(client.buildSubscribe("topic_hello",null));
    }


}
