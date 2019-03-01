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
public class TestPong {
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.connect_thread_pool_size);
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(MqConfig.host, MqConfig.port, new MqProtocol(), processor);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);
        client.start();
        //

        String jobTopc = "topic_get_userById";
        Message acceptJob = processor.acceptJob(jobTopc);
        System.err.println(acceptJob);
        if (acceptJob != null) {
            User user = new User(1, "alice");
            processor.finishJob(jobTopc, user,acceptJob);
        }


    }

    public static class User{
        private Integer id;
        private String name;

        public User(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
