package com.artlongs.amq.http.aio.handler;

import com.artlongs.amq.http.aio.AioHttpServer;
import com.artlongs.amq.http.HttpServerConfig;
import com.artlongs.amq.http.HttpServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
*@author leeton
*2018年2月6日
*
*/
public class SocketReadHandler implements CompletionHandler<AsynchronousSocketChannel,Void>,Cloneable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private AsynchronousServerSocketChannel serverSocket = null;
	private AioHttpServer httpServer = null;
	private HttpServerState state = null;
	private HttpServerConfig config = null;

	public SocketReadHandler(AioHttpServer httpServer) {
		this.serverSocket = httpServer.getServerSocket();
		this.httpServer = httpServer;
		state = httpServer.getState();
		config = httpServer.getConfig();
	}
	@Override
	public void completed(AsynchronousSocketChannel client, Void attachment) {
		int n = HttpServerState.CONCURRENT_NUMS.getAndIncrement();

//		ByteBuffer byteBuf = ByteBuffer.allocate(256);// 内部堆 heap buffer
		ByteBuffer byteBuf = HttpServerConfig.bufferPool.allocate().getResource(); // 分配外部 direct buffer
		client.read(byteBuf, config.readWait, TimeUnit.SECONDS, byteBuf, new SocketWriteHandler(httpServer,client));

		while(n >= config.maxConnection) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			n = HttpServerState.CONCURRENT_NUMS.decrementAndGet();
		}
		serverSocket.accept(attachment, this);
	}
	@Override
	public void failed(Throwable e, Void attachment) {
		e.printStackTrace();
	}
	

}
