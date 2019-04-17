package com.artlongs.amq.core.aio;

import com.artlongs.amq.core.aio.plugin.Monitor;
import com.artlongs.amq.core.event.AioReadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class Reader<T> implements CompletionHandler<Integer, AioPipe<T>> {
    private static final Logger logger = LoggerFactory.getLogger(Reader.class);

    private ExecutorService executorService;
    private ThreadLocal<Object> threadLocal = new ThreadLocal<>();

    public Reader() {
    }

    public Reader(ExecutorService executorService) {
        this.executorService = executorService;
    }

/*    @Override
    public void completed(final Integer result, final AioPipe<T> aioSession) {
        if (executorService != null && threadLocal.get() == null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    threadLocal.set(this);
                    completed0(result, aioSession);
                    threadLocal.remove();
                }
            });
        } else {
            completed0(result, aioSession);
        }
    }*/
    public void completed(final Integer size, final AioPipe<T> aioPipe) {
        try {
            logger.debug("read is completed" );
            if(size>0){
                // 记录流量
                Monitor<T> monitor = aioPipe.getServerConfig().getProcessor().getMonitor();
                if (monitor != null) {
                    monitor.read(aioPipe, size);
                }
            }
            AioServer.readRingBuffer.publishEvent(AioReadEvent::translate, aioPipe, size);
//            aioPipe.readFromChannel(size == -1);
        } catch (Exception e) {
            failed(e, aioPipe);
        }
    }

    @Override
    public void failed(Throwable exc, AioPipe<T> aioPipe) {
        try {
            aioPipe.getServerConfig().getProcessor().stateEvent(aioPipe, State.INPUT_EXCEPTION, exc);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        try {
            aioPipe.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
