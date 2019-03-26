package com.artlongs.amq.tester;

import com.artlongs.amq.core.*;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class TestSend {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.client_connect_thread_pool_size);
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(MqConfig.client_connect_thread_pool_size, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(MqConfig.host, MqConfig.port, new MqProtocol(), processor);
        client.start(asynchronousChannelGroup);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);
        client.start();

        long s = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            TestUser user = new TestUser(i, "alice");
            processor.onlyPublish("topic_hello", user);
            Thread.sleep(0,500);
        }
        System.err.println("Time(ms):"+(System.currentTimeMillis()-s));

    }

}
