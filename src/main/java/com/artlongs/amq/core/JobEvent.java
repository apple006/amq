package com.artlongs.amq.core;

import com.artlongs.amq.disruptor.EventFactory;

/**
 * Func :
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvent {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public JobEvent setMessage(Message message) {
        this.message = message;
        return this;
    }

    public static final EventFactory<JobEvent> EVENT_FACTORY = new EventFactory<JobEvent>()
    {
        public JobEvent newInstance()
        {
            return new JobEvent();
        }
    };

    public static void translate(JobEvent jobEvent, long sequence, Message msg) {
        jobEvent.setMessage(msg);
    }
}
