package com.artlongs.amq.tester;

import com.artlongs.amq.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class TestRecv1 {
    private static Logger logger = LoggerFactory.getLogger(TestRecv1.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(MqConfig.inst.client_connect_thread_pool_size);
        MqClientProcessor processor = new MqClientProcessor();
        AioMqClient<Message> client = new AioMqClient(MqConfig.inst.host, MqConfig.inst.port, new MqProtocol(), processor);
        Thread t = new Thread(client);
        t.setDaemon(true);
        pool.submit(t);
        client.start();

        //
        long s = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
        Call<Message> callback = (msg)->{
            logger.debug("nums :"+ count.incrementAndGet());
            execBack(msg,s);
        };
        processor.subscribe("topic_hello",callback);

    }

    private static void execBack(Message message,long s) {

        logger.debug(message+" Useed Time:"+(message.getStat().getCtime()-s));

    }



}
