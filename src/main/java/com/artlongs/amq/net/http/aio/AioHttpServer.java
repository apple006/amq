package com.artlongs.amq.net.http.aio;


import com.artlongs.amq.disruptor.RingBuffer;
import com.artlongs.amq.disruptor.YieldingWaitStrategy;
import com.artlongs.amq.disruptor.dsl.Disruptor;
import com.artlongs.amq.disruptor.dsl.ProducerType;
import com.artlongs.amq.net.http.*;
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
import java.util.concurrent.ThreadFactory;

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
	private Writer writer;
	public  RingBuffer<HttpEvent> ringBuffer;

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

			this.ringBuffer = buildRingBuffer(); //创建 RingBuffer

		} catch (IOException e) {
			throw new RuntimeException(" http start on Error:" + e);
		}
		LOGGER.warn("AMQ-HTTP had started,listening {}:{}",config.address,config.port);
	}

	/**
	 * 创建 RingBuffer
	 * @return
	 */
	public RingBuffer<HttpEvent> buildRingBuffer(){
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		// Specify the size of the ring buffer, must be power of 2.
		int bufferSize = 1024 * 128;

		HttpEvent httpEvent  = new HttpEvent();
		// Construct the Disruptor
		Disruptor<HttpEvent> disruptor = new Disruptor<>(httpEvent, bufferSize, threadFactory,ProducerType.SINGLE,new YieldingWaitStrategy());
		// Connect the handler
		disruptor.handleEventsWith(httpEvent);
		return disruptor.start();
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
	public void writer(Writer writer) {
		this.writer = writer;

	}

	@Override
	public Writer getWriter() {
		return writer;
	}
}
