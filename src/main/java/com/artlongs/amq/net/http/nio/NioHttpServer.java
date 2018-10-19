package com.artlongs.amq.net.http.nio;

import com.artlongs.amq.net.http.*;
import com.artlongs.amq.net.http.routes.Controller;

import java.nio.Buffer;

/**
 * Created by ${leeton} on 2018/10/16.
 */
public class NioHttpServer implements HttpServer {
    private HttpHandler handler = null;

    public NioHttpServer(HttpServerConfig config) {
    }

    @Override
    public void handler(HttpHandler handler) {
        this.handler = handler;
    }
    @Override
    public void data(Buffer in, Buffer out) {
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void stop() {

    }

    @Override
    public HttpHandler getHandler() {
        return this.handler;
    }

    @Override
    public HttpServerState getState() {
        return null;
    }

    @Override
    public HttpServerConfig getConfig() {
        return null;
    }
    public HttpServer addController(Controller... controller) {
        return null;
    }
}
