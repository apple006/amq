package com.artlongs.amq.tester;

import com.artlongs.amq.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class TestRecv1 {
    private static Logger logger = LoggerFactory.getLogger(TestRecv1.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        final int threadSize = MqConfig.inst.client_connect_thread_pool_size;
        AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withFixedThreadPool(threadSize, (r)->new Thread(r));
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(new MqProtocol(), processor);
        Thread t = new Thread(client);
        t.setDaemon(true);
        client.start(channelGroup);

        //
        AtomicInteger count = new AtomicInteger(0);
        Call<Message> callback = (msg)->{
            logger.debug("nums :"+ count.incrementAndGet());
            execBack(msg);
        };
        processor.subscribe("topic_hello",callback);

    }

    private static void execBack(Message message) {
        long s = System.currentTimeMillis();
        logger.debug(message.toString());
        logger.debug("Useed Time(ms):"+(s-message.getStat().getCtime()));

    }



}
