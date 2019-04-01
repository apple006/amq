package com.artlongs.mq;

import com.artlongs.amq.core.*;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/4/1.
 */
@Singleton
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

    public static class Module extends org.osgl.inject.Module {
        @Override
        protected void configure() {
            bind(MqClientAction.class).in(Singleton.class).to(new Provider<MqClientAction>() {
                @Override
                public MqClientAction get() {
                    return new AmqClient();
                }
            });

        }
    }

}
