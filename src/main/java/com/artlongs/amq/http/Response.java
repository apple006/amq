package com.artlongs.amq.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author leeton 2018年2月6日
 */
public class Response implements HttpResponse {
    private static final Logger logger = LoggerFactory.getLogger(Response.class);
    // 保证单线程写，不必考虑多线程
    private static final HashMap<String, String> baseHeaders = new HashMap<>();
    private static final byte[] rn = "\r\n".getBytes();
    private static final int STATE_END = 0;
    private static final int STATE_RESP_LINE = 1;
    private static final int STATE_HEADER = 2;
    private static final int STATE_BODY = 3;
    private static final String MSG_OK = "OK";

    static {
        baseHeaders.put("Server", "amq-http");
        baseHeaders.put("Content-Type", "text/html; charset=utf-8");
        baseHeaders.put("Transfer-Encoding", "chunked");
    }

    private int code = 200;
    private int state = STATE_RESP_LINE;
    private String msg = MSG_OK;
    private Map<String, String> headers = null;
    private int bufferLen = 128;
    private ByteBuffer buffer = ByteBuffer.allocate(bufferLen);
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private HttpServer server;

    public Response(HttpServer server) {
        headers = baseHeaders;
        this.server = server;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public int getState() {
        return code;
    }

    @Override
    public void setState(int code) {
        this.code = code;
    }

    @Override
    public void append(String str) {
        flush();
        writeChunk(str.getBytes());
    }

    @Override
    public void write(byte[] data) {
        flush();
        writeChunk(data);
    }

    @Override
    public void end(){
        if (state == STATE_END) {
            return;
        }
        ioWrite(baos, "0\r\n\r\n".getBytes());
        flush();
        state = STATE_END;
        //close
//        server.getWriter().close();
    }


    @Override
    public void flush() {
        if (state == STATE_END) {
            return;
        }
        if (state == STATE_RESP_LINE) {
            buffer.clear();
            String respLine = "HTTP/1.1 " + code + " " + msg + "\r\n";
            ioWrite(baos, respLine.getBytes());
            state = STATE_HEADER;
        }
        if (state == STATE_HEADER) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                String values = headers.get(key);
                ioWrite(baos, (key + ":" + values + "\r\n").getBytes());

            }
            ioWrite(baos, rn);
            state = STATE_BODY;
        }
        int size = baos.size();
        if (size > 0) {
            byte[] data = baos.toByteArray();
            baos.reset();

            int offset = 0;
            int len = data.length;
            while (offset < len) {
                buffer.clear();
                int remain = buffer.remaining();
                if (remain + offset < len) {
                    buffer.put(data, offset, remain);
                    offset += remain;
                    buffer.flip();
                    server.getWriter().write(buffer);
                } else {
                    buffer.put(data, offset, len - offset);
                    buffer.flip();
                    server.getWriter().write(buffer);
                    break;
                }
            }
        }
    }



    private void writeChunk(byte[] data) {
        byte[] hex = Integer.toHexString(data.length).getBytes();
        ioWrite(baos, hex);
        ioWrite(baos, rn);
        ioWrite(baos, data);
        ioWrite(baos, rn);
        flush();
    }

    private void ioWrite(ByteArrayOutputStream baos, byte[] data) {
        try {
            baos.write(data);
        } catch (IOException ex) {
            throw new RuntimeException("IO write on Error:", ex);
        }
    }

    private String getRemoteAddr(AsynchronousSocketChannel channel){
        try {
            if(channel != null && channel.isOpen()){
                return channel.getRemoteAddress().toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


}
