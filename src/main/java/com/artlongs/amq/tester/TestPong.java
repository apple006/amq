package com.artlongs.amq.tester;

import com.artlongs.amq.core.*;
import com.artlongs.amq.serializer.FastJsonSerializer;

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
public class TestPong {
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.client_connect_thread_pool_size);
        AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(20, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(MqConfig.host, MqConfig.port, new MqProtocol(), processor);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);
        client.start(asynchronousChannelGroup);
        //

        FastJsonSerializer.User user = new FastJsonSerializer.User(2, "alice");
        String jobTopc = "topic_get_userById";
        processor.acceptJob(jobTopc, (m)->{
            if (m != null) {
                Message job = (Message)m; // 收到的 JOB
                System.err.println(job);
                // 完成任务 JOB
                if (user.getId().equals(job.getV())) {
                    processor.<FastJsonSerializer.User>finishJob(jobTopc, user,job);
                }
            }
        });




    }


}
