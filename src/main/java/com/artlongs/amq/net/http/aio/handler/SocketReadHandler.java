package com.artlongs.amq.net.http.aio.handler;

import com.artlongs.amq.net.http.HttpServerState;
import com.artlongs.amq.net.http.aio.AioHttpServer;
import com.artlongs.amq.net.http.aio.HttpRequestResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
*@author song(mejeesong@qq.com)
*2018年2月6日
*
*/
public class SocketReadHandler implements CompletionHandler<Integer, ByteBuffer>,Cloneable {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private HttpRequestResolver resolver = null;
	private AsynchronousSocketChannel client = null;
	public static HttpServerState state = null;
	
	public SocketReadHandler(AioHttpServer httpServer,AsynchronousSocketChannel client) {
		this.client = client;
		resolver = new HttpRequestResolver(httpServer,client);
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
			resolver.append(buffer);
		} catch (Exception e) {
			e.printStackTrace();
			closeConn();
			return;
		}
		this.client.read(attachment,50,TimeUnit.MINUTES,attachment,this);
	}
	@Override
	public void failed(Throwable exc, ByteBuffer attachment) {
		logger.debug("read failed,exc:"+exc.getStackTrace());
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
