package com.artlongs;

import com.artlongs.amq.core.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/4/1.
 */
@Component
public class AmqClient extends MqClientProcessor {

    public AmqClient() {
        try {
            ExecutorService pool = Executors.newFixedThreadPool(MqConfig.inst.client_connect_thread_pool_size);
            AioMqClient<Message> client = new AioMqClient(new MqProtocol(), this);
            Thread t = new Thread(client);
            t.setDaemon(true);
            pool.submit(t);
            client.start();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
