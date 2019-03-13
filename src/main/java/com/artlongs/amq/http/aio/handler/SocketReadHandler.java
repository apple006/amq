package com.artlongs.amq.http.aio.handler;

import com.artlongs.amq.http.aio.AioHttpServer;
import com.artlongs.amq.http.HttpResolver;
import com.artlongs.amq.http.HttpServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
*@author leeton
*2018年2月6日
*
*/
public class SocketReadHandler implements CompletionHandler<Integer, ByteBuffer>,Cloneable {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private HttpResolver resolver = null;
	private AsynchronousSocketChannel client = null;
	private AioHttpServer server = null;
	public static HttpServerState state = null;

	public SocketReadHandler(AioHttpServer httpServer, AsynchronousSocketChannel client, ByteBuffer buffer) {
		this.client = client;
		this.server = httpServer;
		httpServer.writer(new SocketWriteHandler(client, buffer));
		resolver = new HttpResolver(httpServer);
		state = httpServer.getState();
	}

	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		if(result == -1) {
			closeConn();
			return;
		}
        resolver.excute(attachment);
	}
	@Override
	public void failed(Throwable ex, ByteBuffer attachment) {
		logger.debug("write failed,maybe resource not exist. exception msg:{}",attachment.toString());
		closeConn();
	}

	private void closeConn() {
		try {
			HttpServerState.CONNECTION_NUMS.decrementAndGet();
			resolver = null;
			if(null != this.client && this.client.isOpen()) {
                logger.debug("close socket:{}",client.getRemoteAddress().toString());
				this.client.close();
			}
		} catch (IOException e) {
			logger.error("close client happen exception", e);
		}
	}



}
