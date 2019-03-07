package com.artlongs.amq.core.event;

import com.artlongs.amq.core.ProcessorImpl;
import com.artlongs.amq.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func :
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvnetHandler implements EventHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(JobEvnetHandler.class);

    @Override
    public void onEvent(JobEvent event, long sequence, boolean endOfBatch) throws Exception {
        logger.debug(" 执行消息任务的分派 ......");
        ProcessorImpl.INST.publishJobToWorker(event.getMessage());
    }
}
