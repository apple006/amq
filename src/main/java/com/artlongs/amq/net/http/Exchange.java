package com.artlongs.amq.net.http;

import java.nio.Buffer;

/**
 *
 * Created by leeton on 2018/10/15.
 */
public interface Exchange {

    void handler(HttpHandler httpHandler);

    void data(Buffer in, Buffer out);



}
