package com.artlongs.amq.core.event;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.disruptor.EventFactory;

/**
 * Func : AIO EVENT
 *
 * @author: leeton on 2019/4/17.
 */
public class AioEvent {
    private AioPipe pipe;
    private int readSize;
    private boolean read;

    public static final EventFactory<AioEvent> EVENT_FACTORY = new EventFactory<AioEvent>()
    {
        public AioEvent newInstance()
        {
            return new AioEvent();
        }
    };

    public static void translate(AioEvent event, long sequence, AioPipe pipe,boolean read, int readSize) {
        event.setPipe(pipe);
        event.setReadSize(readSize);
        event.setRead(read);
    }

    ///========================
    public AioPipe getPipe() {
        return pipe;
    }

    public AioEvent setPipe(AioPipe pipe) {
        this.pipe = pipe;
        return this;
    }

    public int getReadSize() {
        return readSize;
    }

    public AioEvent setReadSize(int readSize) {
        this.readSize = readSize;
        return this;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
