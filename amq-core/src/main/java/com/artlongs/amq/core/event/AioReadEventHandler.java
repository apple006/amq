package com.artlongs.amq.core.event;

import com.artlongs.amq.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func :
 *
 * @author: leeton on 2019/4/17.
 */
public class AioReadEventHandler implements EventHandler<AioReadEvent> {
    private static Logger logger = LoggerFactory.getLogger(AioReadEventHandler.class);

    @Override
    public void onEvent(AioReadEvent event, long sequence, boolean endOfBatch) throws Exception {
//        logger.debug("read completed evnet.");
        event.getPipe().readFromChannel(event.getReadSize() == -1);
    }
}
