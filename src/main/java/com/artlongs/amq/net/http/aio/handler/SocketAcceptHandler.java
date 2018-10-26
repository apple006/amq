package com.artlongs.amq.net.http.aio.handler;

import com.artlongs.amq.net.http.HttpServerConfig;
import com.artlongs.amq.net.http.HttpServerState;
import com.artlongs.amq.net.http.aio.AioHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
public class SocketAcceptHandler implements CompletionHandler<AsynchronousSocketChannel,Void>,Cloneable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private AsynchronousServerSocketChannel serverSocket = null;
	private AioHttpServer httpServer = null;
	private HttpServerState state = null;
	private HttpServerConfig config = null;

	public SocketAcceptHandler(AioHttpServer httpServer) {
		this.serverSocket = httpServer.getServerSocket();
		this.httpServer = httpServer;
		state = httpServer.getState();
		config = httpServer.getConfig();
	}
	@Override
	public void completed(AsynchronousSocketChannel client, Void attachment) {
		try {
			logger.debug("accept a socket:"+client.getRemoteAddress().toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		int n = state.incrementAndGet(HttpServerState.FIELD_CONNECTION);
		
//		ByteBuffer byteBuf = ByteBuffer.allocate(256);// 内部堆 heap buffer
		ByteBuffer byteBuf = HttpServerConfig.bufferPool.allocate().getResource(); // 分配外部 direct buffer
		client.read(byteBuf,config.readWait,TimeUnit.SECONDS,byteBuf, new SocketReadHandler(httpServer,client));
		
		while(n >= config.maxConnection) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			n = state.incrementAndGet(HttpServerState.FIELD_CONNECTION);
		}
		serverSocket.accept(attachment, this);
	}
	@Override
	public void failed(Throwable e, Void attachment) {
		e.printStackTrace();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
