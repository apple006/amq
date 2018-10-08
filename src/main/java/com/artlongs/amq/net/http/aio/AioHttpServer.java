package com.artlongs.amq.net.http.aio;


import com.artlongs.amq.net.http.HttpHandler;
import com.artlongs.amq.net.http.HttpServer;
import com.artlongs.amq.net.http.HttpServerConfig;
import com.artlongs.amq.net.http.HttpServerState;
import com.artlongs.amq.net.http.aio.handler.SocketAcceptHandler;
import com.artlongs.amq.net.http.routes.Controller;
import com.artlongs.amq.net.http.routes.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
*@author leeton
*2018年2月6日
*
*/
public class AioHttpServer implements HttpServer{
	private static Logger LOGGER = LoggerFactory.getLogger(AioHttpServer.class);
	private HttpServerConfig config = null;
	private AsynchronousServerSocketChannel serverSocket = null;
	private HttpHandler handler = null;
	private HttpServerState state = null;
	private Router router;
	
	public AioHttpServer(HttpServerConfig config) {
		this.config = config;
		state = new HttpServerState(this);
		router = new Router();
		init();
	}
	
	public void init() {

	}

	public void start() {
		try {
			ExecutorService threadPool = Executors.newFixedThreadPool(config.threadPoolSize);
			AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(threadPool);
			serverSocket = AsynchronousServerSocketChannel.open(group);
			serverSocket.bind(new InetSocketAddress(config.address, config.port));
			serverSocket.accept(null, new SocketAcceptHandler(this));
		} catch (IOException e) {
			throw new RuntimeException(" http start on Error:" + e);
		}
		LOGGER.info("Httpdoor had started,listening {}:{}",config.address,config.port);
	}

	public void shutdown() {
		
	}

	public void stop() {
		
	}

	public HttpServerState getState() {
		return state;
	}
	
	@Override
	public HttpServerConfig getConfig() {
		return config;
	}


	public AsynchronousServerSocketChannel getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(AsynchronousServerSocketChannel serverSocket) {
		this.serverSocket = serverSocket;
	}

	@Override
	public void setHttpRequestHandler(HttpHandler handler) {
		this.handler = handler;
	}

	public HttpServer accept(Controller controller) {
		this.handler= router;
		Router.addRoutes(controller, router);
		return this;
	}

	@Override
	public HttpHandler getHttpRequestHandler() {
		return handler;
	}

	@Override
	public void run() {
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		this.start();
	}

}
