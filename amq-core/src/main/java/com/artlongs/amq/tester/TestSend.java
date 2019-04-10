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
        final int groupSize = MqConfig.inst.client_channel_event_thread_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(groupSize, (r)->new Thread(r));
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(new MqProtocol(), processor);
//        Thread t = new Thread(client);
//        t.setDaemon(true);
        client.start(channelGroup);

        long s = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) { // 测试时,最好把 aioServer.setWriteQueueSize 的大小设置为 >= 测试次数
            processor.onlyPublish("topic_hello", new TestUser(i, "alice"));
            Thread.sleep(0,100);
        }
        System.err.println("Time(ms):"+(System.currentTimeMillis()-s));

    }

}
