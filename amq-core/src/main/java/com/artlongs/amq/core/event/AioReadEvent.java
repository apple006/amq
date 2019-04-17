package com.artlongs.amq.core.event;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.disruptor.EventFactory;

/**
 * Func : AIO EVENT
 *
 * @author: leeton on 2019/4/17.
 */
public class AioReadEvent {
    private AioPipe pipe;
    private int readSize;

    public AioPipe getPipe() {
        return pipe;
    }

    public AioReadEvent setPipe(AioPipe pipe) {
        this.pipe = pipe;
        return this;
    }

    public int getReadSize() {
        return readSize;
    }

    public AioReadEvent setReadSize(int readSize) {
        this.readSize = readSize;
        return this;
    }

    public static final EventFactory<AioReadEvent> EVENT_FACTORY = new EventFactory<AioReadEvent>()
    {
        public AioReadEvent newInstance()
        {
            return new AioReadEvent();
        }
    };

    public static void translate(AioReadEvent event, long sequence, AioPipe pipe, int readSize) {
        event.setPipe(pipe);
        event.setReadSize(readSize);
    }
}
