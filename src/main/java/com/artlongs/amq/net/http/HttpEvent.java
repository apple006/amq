package com.artlongs.amq.net.http;

import com.artlongs.amq.disruptor.EventFactory;
import com.artlongs.amq.disruptor.EventHandler;

import java.nio.ByteBuffer;

/**
 * Created by ${leeton} on 2018/10/22.
 */
public class HttpEvent implements EventFactory<HttpEvent>,EventHandler<HttpEvent> {

    private ByteBuffer data;
    private HttpResolver resolver;

    public ByteBuffer getData() {
        return data;
    }

    public HttpResolver getResolver() {
        return resolver;
    }

    @Override
    public HttpEvent newInstance() {
        return this;
    }

    public static void translateTo(HttpEvent event, long sequence, ByteBuffer data,HttpResolver resolver) {
        event.data=data;
        event.resolver=resolver;
    }

    @Override
    public void onEvent(HttpEvent event, long sequence, boolean endOfBatch) throws Exception {
        getResolver().excute(event.getData());
    }
}
