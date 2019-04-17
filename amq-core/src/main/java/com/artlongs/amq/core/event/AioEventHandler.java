package com.artlongs.amq.core.event;

import com.artlongs.amq.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func :
 *
 * @author: leeton on 2019/4/17.
 */
public class AioEventHandler implements EventHandler<AioEvent> {
    private static Logger logger = LoggerFactory.getLogger(AioEventHandler.class);

    @Override
    public void onEvent(AioEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.isRead()) {
            event.getPipe().readFromChannel(event.getReadSize() == -1);
        } else {
            event.getPipe().writeToChannel();
        }
    }
}
