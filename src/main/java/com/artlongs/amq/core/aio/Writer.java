package com.artlongs.amq.core.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class Writer<T> implements CompletionHandler<Integer, AioPipe<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Writer.class);

    @Override
    public void completed(Integer result, AioPipe<T> pipe) {
        try {
            // 接收到的消息进行预处理
            Monitor<T> monitor = pipe.getServerConfig().getMonitor();
            if (monitor != null) {
                monitor.write(pipe, result);
            }
            pipe.writeSemaphore.release();
            DirectBufferUtil.freeFirstBuffer(pipe.writeBuffer);
            pipe.writeBuffer = null;
            pipe.writeToChannel();
        } catch (Exception e) {
            failed(e, pipe);
        }
    }

    @Override
    public void failed(Throwable exc, AioPipe<T> pipe) {
        pipe.writeSemaphore.release();
        DirectBufferUtil.freeFirstBuffer(pipe.writeBuffer);
        pipe.writeBuffer = null;

        try {
            pipe.getServerConfig().getProcessor().stateEvent(pipe, State.OUTPUT_EXCEPTION, exc);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        try {
            pipe.close();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }
}
