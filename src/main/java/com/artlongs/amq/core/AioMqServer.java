package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioServer;
import com.artlongs.amq.http.AioHttpServer;
import com.artlongs.amq.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :MQ 服务端
 *
 * @author: leeton on 2019/2/22.
 */
public class AioMqServer {
    private static Logger logger = LoggerFactory.getLogger(AioMqServer.class);

    private AioMqServer() {
    }

    public static final AioMqServer instance = new AioMqServer();

    private HttpServer httpServer = null;

    private ExecutorService pool = Executors.newFixedThreadPool(MqConfig.inst.server_connect_thread_pool_size);

    public void start() {
        try {
            AioServer<ByteBuffer> aioServer = new AioServer<>(MqConfig.inst.host, MqConfig.inst.port, new MqProtocol(), new MqServerProcessor());
            aioServer.startCheckAlive(MqConfig.inst.start_check_client_alive);
            aioServer.startMonitorPlugin(MqConfig.inst.start_flow_monitor);
            //
            Thread t = new Thread(aioServer);
            t.setDaemon(true);
            pool.submit(t);
            aioServer.start();
            //
            scheduler();
            //
            startAdmin();
            //
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void scheduler() {
        MqScheduler.inst.start();
    }

    public void startAdmin(){
        if (MqConfig.inst.start_mq_admin) {
            httpServer = AioHttpServer.instance;
//            HttpProcessor processor = httpServer.getHttpProcessor();
//            processor.addController(new QueryController().getControllers());
            httpServer.start();

        }
    }

    public void shutdown() {
        ProcessorImpl.INST.shutdown();
        httpServer.shutdown();
    }



    public static void main(String[] args) throws IOException {
        AioMqServer.instance.start();
    }

}
