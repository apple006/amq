package com.artlongs.amq.http;


import com.artlongs.amq.core.AioMqServer;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.core.aio.AioServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author leeton
 * 2018年2月6日
 */
public class AioHttpServer implements HttpServer  {
    private static Logger logger = LoggerFactory.getLogger(AioHttpServer.class);

    private AioHttpServer() {
        this.httpProcessor = new HttpProcessor();
    }

    public static final AioHttpServer instance = new AioHttpServer();

    private HttpProcessor httpProcessor;
    final ExecutorService pool = Executors.newFixedThreadPool(HttpServerConfig.maxConnection);


    public void start() {
        try {

            AioServer<Http> aioServer = new AioServer<Http>(HttpServerConfig.host, HttpServerConfig.port, new HttpProtocol(), this.httpProcessor);
            //
            Thread t = new Thread(aioServer);
            t.setDaemon(true);
            pool.submit(t);
            aioServer.start();
            //
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public HttpProcessor getHttpProcessor() {
        return this.httpProcessor;
    }

    public static void main(String[] args) {
        AioHttpServer.instance.start();
    }


}
