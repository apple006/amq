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
 * @author: leeton on 2019/3/1.
 */
public class TestPing {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.inst.client_connect_thread_pool_size);
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(20, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(MqConfig.inst.host, MqConfig.inst.port, new MqProtocol(), processor);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);
        client.start(asynchronousChannelGroup);
        //

        Message message = processor.publishJob("topic_get_userById",2);
        System.err.println(message);





    }

}
