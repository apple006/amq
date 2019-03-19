package com.artlongs.amq.http;

import com.artlongs.amq.core.aio.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * Func :
 *
 * @author: leeton on 2019/3/18.
 */
public class HttpProtocol implements Protocol<ByteBuffer> {

    private ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    private static final byte[] rn = "\r\n".getBytes();
    private static final byte[] end = "0\r\n\r\n".getBytes();

    @Override
    public ByteBuffer encode(ByteBuffer buffer) {
        return null;
    }

    @Override
    public ByteBuffer decode(ByteBuffer buffer) {
        return null;
    }

    private void httpFirstLine(int code, String note) {
        String respLine = "HTTP/1.1 " + code + " " + note + "\r\n";
        write2OutStream(outStream, respLine.getBytes());
    }
    private void httpHeader(Map<String, String> headers) {
        if (headers.size() == 0) return;
        Set<String> keys = headers.keySet();
        for (String key : keys) {
            String values = headers.get(key);
            write2OutStream(outStream, (key + ":" + values + "\r\n").getBytes());
        }
        write2OutStream(outStream, rn);
    }

    private void httpBody(byte[] data) {
        byte[] hex = Integer.toHexString(data.length).getBytes();
        write2OutStream(outStream, hex);
        write2OutStream(outStream, rn);
        write2OutStream(outStream, data);
        write2OutStream(outStream, rn);
    }
    private void httpEnd(){
        write2OutStream(outStream, end);
    }


    private void write2OutStream(ByteArrayOutputStream baos, byte[] data) {
        if (data == null) return;
        try {
            baos.write(data);
        } catch (IOException ex) {
            throw new RuntimeException("IO write on Error:", ex);
        }
    }




}
