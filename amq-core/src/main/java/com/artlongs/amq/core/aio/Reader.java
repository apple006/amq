package com.artlongs.amq.core.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class Reader<T> implements CompletionHandler<Integer, AioPipe<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Reader.class);

    @Override
    public void completed(final Integer size, final AioPipe<T> aioPipe) {
        try {
//            System.err.println("read is completed" );
            // 记录流量
            Monitor<T> monitor = aioPipe.getServerConfig().getProcessor().getMonitor();
            if (monitor != null) {
                monitor.read(aioPipe, size);
            }
            aioPipe.readSemaphore.release();
            aioPipe.readCacheQueue.put(aioPipe.readBuffer);
            aioPipe.readFromChannel(size == -1);
        } catch (Exception e) {
            failed(e, aioPipe);
        }finally {
            aioPipe.readSemaphore.release();
        }
    }

    @Override
    public void failed(Throwable exc, AioPipe<T> aioPipe) {
        whenErrDoWaiting();
        try {
            aioPipe.getServerConfig().getProcessor().stateEvent(aioPipe, State.INPUT_EXCEPTION, exc);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        try {
            aioPipe.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
    private void whenErrDoWaiting(){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}