package com.artlongs.amq.tester;

import com.artlongs.amq.core.*;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class TestSend {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        final int threadSize = MqConfig.inst.client_connect_thread_pool_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(new MqProtocol(), processor);
        Thread t = new Thread(client);
        t.setDaemon(true);
        client.start(channelGroup);

        long s = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            TestUser user = new TestUser(i, "alice");
            processor.onlyPublish("topic_hello", user);
            Thread.sleep(0,500);
        }
        System.err.println("Time(ms):"+(System.currentTimeMillis()-s));

    }

}
