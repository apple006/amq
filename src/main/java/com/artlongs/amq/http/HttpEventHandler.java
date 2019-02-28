package com.artlongs.amq.http;

import com.artlongs.amq.disruptor.EventFactory;
import com.artlongs.amq.disruptor.EventHandler;

import java.nio.ByteBuffer;

/**
 *
 * Created by ${leeton} on 2018/10/22.
 */
public class HttpEventHandler implements EventFactory<HttpEventHandler>,EventHandler<HttpEventHandler> {

    private ByteBuffer data;
    private HttpResolver resolver;

    public ByteBuffer getData() {
        return data;
    }

    public HttpResolver getResolver() {
        return resolver;
    }

    @Override
    public HttpEventHandler newInstance() {
        return this;
    }

    public static void buildAndSet(HttpEventHandler event, long sequence, ByteBuffer data, HttpResolver resolver) {
        event.data=data;
        event.resolver=resolver;
    }

    @Override
    public void onEvent(HttpEventHandler event, long sequence, boolean endOfBatch) throws Exception {
        getResolver().excute(event.getData());
    }
}
