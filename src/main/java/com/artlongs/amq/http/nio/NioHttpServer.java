package com.artlongs.amq.http.nio;

import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.Write;
import com.artlongs.amq.http.HttpHandler;
import com.artlongs.amq.http.HttpServer;
import com.artlongs.amq.http.HttpServerConfig;
import com.artlongs.amq.http.HttpServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ${leeton} on 2018/10/26.
 */
public class NioHttpServer implements HttpServer {
    private static Logger logger = LoggerFactory.getLogger(NioHttpServer.class);
    ServerSocketChannel socket;
    Selector selector = null;
    HttpServerConfig config;
    private Executor theadPool;

    public NioHttpServer(HttpServerConfig config) {
        this.config = config;
        init();
    }

    @Override
    public void setDaemon(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        ((ExecutorService) theadPool).submit(t);
    }

    private void init(){
        try {
            theadPool = Executors.newFixedThreadPool(config.threadPoolSize);
            setDaemon(this);
            selector = Selector.open();
            socket = ServerSocketChannel.open();
            socket.bind(new InetSocketAddress(config.ip, config.port));
            socket.configureBlocking(false);
            socket.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        try {
            while (true) {
                if (selector.select(config.connectTimeout) == 0) {
                    System.out.println("==");
                    continue;
                }
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    if (key.isWritable() && key.isValid()) {
                        handleWrite(key);
                    }
                    if (key.isConnectable()) {
                        System.out.println("isConnectable = true");
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void stop() {
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (socket != null && socket.isOpen()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handler(HttpHandler httpHandler) {

    }

    @Override
    public HttpHandler getHandler() {
        return null;
    }

    @Override
    public HttpServerState getState() {
        return null;
    }

    @Override
    public HttpServerConfig getConfig() {
        return null;
    }

    @Override
    public HttpServer addController(Controller... controller) {
        return null;
    }

    @Override
    public void writer(Write writer) {

    }

    @Override
    public Write getWriter() {
        return null;
    }

    public static void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssChannel.accept();
        sc.configureBlocking(false);
        sc.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocateDirect(1024));
    }

    public static void handleRead(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer in = (ByteBuffer) key.attachment();
        long readCount = sc.read(in);
        while (readCount > 0) {
            in.flip();
            while (in.hasRemaining()) {
                in.get();
            }
            System.out.println();
            in.clear();
            readCount = sc.read(in);
        }
        if (readCount == -1) {
            sc.close();
        }
    }

    public static void handleWrite(SelectionKey key) throws IOException {
        ByteBuffer buf = (ByteBuffer) key.attachment();
        buf.flip();
        SocketChannel sc = (SocketChannel) key.channel();
        while (buf.hasRemaining()) {
            sc.write(buf);
        }
        buf.compact();
    }


    @Override
    public void run() {
        this.start();
    }

    public static void main(String[] args) {
        new NioHttpServer(new HttpServerConfig()).start();

    }


}