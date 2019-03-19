package com.artlongs.amq.http;

import com.artlongs.amq.core.aio.Protocol;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/3/18.
 */
public class HttpProtocol implements Protocol<Http> {

    private ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    private static final String rn = "\r\n";
    private static final String end = "0\r\n\r\n";

    @Override
    public ByteBuffer encode(Http http) {
        return http.getResponse().build();
    }

    @Override
    public Http decode(ByteBuffer buffer) {
        Http http = new Http();
        http.setRequest(new Request().build(buffer));
        return http;
    }
}
