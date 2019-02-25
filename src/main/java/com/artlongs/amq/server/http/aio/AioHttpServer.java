package com.artlongs.amq.server.http.aio;


import com.artlongs.amq.disruptor.BusySpinWaitStrategy;
import com.artlongs.amq.disruptor.RingBuffer;
import com.artlongs.amq.disruptor.dsl.Disruptor;
import com.artlongs.amq.disruptor.dsl.ProducerType;
import com.artlongs.amq.server.http.*;
import com.artlongs.amq.server.http.routes.Controller;
import com.artlongs.amq.server.http.routes.Router;
import com.artlongs.amq.server.Write;
import com.artlongs.amq.server.http.aio.handler.SocketReadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
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
	private Write writer;
	public  RingBuffer<HttpEventHandler> ringBuffer;

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
			serverSocket.accept(null, new SocketReadHandler(this));
			setDaemon(this);

			this.ringBuffer = buildRingBuffer(); //创建 RingBuffer

		} catch (IOException e) {
			throw new RuntimeException(" http start on Error:" + e);
		}
		LOGGER.warn("AMQ-HTTP had started,listening {}:{}",config.ip,config.port);
	}

	/**
	 * 创建 RingBuffer
	 * @return
	 */
	public RingBuffer<HttpEventHandler> buildRingBuffer(){
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		// Specify the size of the ring buffer, must be power of 2.
		int bufferSize = 1024 * 128;

		HttpEventHandler httpEvent  = new HttpEventHandler();
		// Construct the Disruptor
		Disruptor<HttpEventHandler> disruptor = new Disruptor<>(httpEvent, bufferSize, threadFactory,ProducerType.SINGLE,new BusySpinWaitStrategy());
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
	public void writer(Write writer) {
		this.writer = writer;

	}

	@Override
	public Write getWriter() {
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