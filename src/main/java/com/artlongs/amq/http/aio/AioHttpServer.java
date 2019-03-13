package com.artlongs.amq.http.aio;


import com.artlongs.amq.http.HttpHandler;
import com.artlongs.amq.http.HttpServer;
import com.artlongs.amq.http.HttpServerConfig;
import com.artlongs.amq.http.HttpServerState;
import com.artlongs.amq.http.aio.handler.SocketAcceptHandler;
import com.artlongs.amq.http.aio.handler.SocketWriteHandler;
import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
*@author leeton
*2018年2月6日
*
*/
public class AioHttpServer implements HttpServer {
	private static Logger LOGGER = LoggerFactory.getLogger(AioHttpServer.class);
	private HttpServerConfig config = null;
	private AsynchronousServerSocketChannel serverSocket = null;
	private HttpHandler handler = null;
	private HttpServerState state = null;
	private Router router;
	private SocketWriteHandler writer;

	public AioHttpServer(HttpServerConfig config) {
		this.config = config;
		state = new HttpServerState(this);
		router = new Router();
		init();
	}
	
	public void init() {

	}

	@Override
	public void setDaemon(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
	}

	public void start() {
		try {
			ExecutorService threadPool = Executors.newFixedThreadPool(config.threadPoolSize);
			AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(threadPool);
			serverSocket = AsynchronousServerSocketChannel.open(group);
			serverSocket.bind(new InetSocketAddress(config.ip, config.port));
			serverSocket.accept(null, new SocketAcceptHandler(this));
			setDaemon(this);

		} catch (IOException e) {
			throw new RuntimeException(" http start on Error:" + e);
		}
		LOGGER.warn("AMQ-HTTP had started,listening {}:{}",config.ip,config.port);
	}


	public void shutdown() {
		
	}

	public void stop() {
		
	}

	public HttpServerState getState() {
		return state;
	}
	
	public HttpServerConfig getConfig() {
		return config;
	}


	public AsynchronousServerSocketChannel getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(AsynchronousServerSocketChannel serverSocket) {
		this.serverSocket = serverSocket;
	}

	public void handler(HttpHandler handler) {
		this.handler = handler;
	}

	public HttpServer addController(Controller... controllers) {
		this.handler= Router.asRouter(controllers);
		return this;
	}

	public HttpHandler getHandler() {
		return handler;
	}

	@Override
	public void writer(SocketWriteHandler writer) {
		this.writer = writer;

	}

	@Override
	public SocketWriteHandler getWriter() {
		return writer;
	}

	@Override
	public void run() {
		this.start();
	}


	public void closeConn(AsynchronousSocketChannel client) {
		try {
			if(null != client && client.isOpen()) {
				client.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
