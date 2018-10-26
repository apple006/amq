package com.artlongs.amq.net.http;

import java.nio.ByteBuffer;

/**
 * Created by ${leeton} on 2018/10/26.
 */
public interface Writer {
    public void write(ByteBuffer byteBuffer);
    public void close();
}
