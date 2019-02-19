package com.artlongs.amq.core.event;

import com.artlongs.amq.core.ProcessorImpl;
import com.artlongs.amq.disruptor.EventHandler;

/**
 * Func :
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvnetHandler implements EventHandler<JobEvent> {

    @Override
    public void onEvent(JobEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.err.println(" 执行 JOB 分派 ......");
        ProcessorImpl.INST.publishJobToWorker(event.getMessage());

    }
}
