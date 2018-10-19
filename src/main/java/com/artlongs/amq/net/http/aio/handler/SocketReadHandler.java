package com.artlongs.amq.net.http.aio.handler;

import com.artlongs.amq.net.http.HttpResolver;
import com.artlongs.amq.net.http.HttpServer;
import com.artlongs.amq.net.http.HttpServerState;
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
	public static HttpServerState state = null;

	
	public SocketReadHandler(HttpServer httpServer, AsynchronousSocketChannel client) {
		this.client = client;
		resolver = new HttpResolver(httpServer,client);
		state = httpServer.getState();
	}

	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		if(result == -1) {
			closeConn();
			return;
		}
		attachment.flip();
		byte[] buffer = new byte[result];
		attachment.get(buffer, 0, result);
        attachment.clear();
        try {
            resolver.append(attachment);
			closeConn();
        } catch (Exception e) {
            e.printStackTrace();
            closeConn();
            return;
        }

//		this.client.read(attachment,50,TimeUnit.MINUTES,attachment,this);
	}
	@Override
	public void failed(Throwable ex, ByteBuffer attachment) {
		logger.debug("read failed,maybe resource not exist. exception msg:{}",ex.getMessage());
		closeConn();
	}
	
	private void closeConn() {
		try {
			logger.debug("close socket:{}",client.getRemoteAddress().toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			state.decrementAndGet(HttpServerState.FIELD_CONNECTION);
			resolver = null;
			if(this.client != null) {
				this.client.close();
			}
		} catch (IOException e) {
			logger.error("close client happen exception", e);
		}
	}
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
