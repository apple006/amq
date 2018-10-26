package com.artlongs.amq.net.http.aio.handler;

import com.artlongs.amq.net.http.HttpEvent;
import com.artlongs.amq.net.http.HttpResolver;
import com.artlongs.amq.net.http.HttpServerState;
import com.artlongs.amq.net.http.Writer;
import com.artlongs.amq.net.http.aio.AioHttpServer;
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

	public SocketReadHandler(AioHttpServer httpServer, AsynchronousSocketChannel client) {
		this.client = client;
		this.server = httpServer;
		httpServer.writer(new Worker(client));
		resolver = new HttpResolver(httpServer);
		state = httpServer.getState();
	}

	/**
	 * 苦力
	 */
	class Worker implements Writer{
		private AsynchronousSocketChannel client;
		private final CompletionHandler writeHandler = new CompletionHandler() {
			@Override
			public void completed(Object result, Object attachment) {

			}

			@Override
			public void failed(Throwable exc, Object attachment) {

			}
		};

		public Worker(AsynchronousSocketChannel client) {
			this.client = client;
		}

		@Override
		public void write(ByteBuffer byteBuffer) {
			client.write(byteBuffer, null, writeHandler);
		}

		@Override
		public void close() {
			try {
				if(this.client != null && client.isOpen()) {
					logger.debug("close socket:{}",client.getRemoteAddress().toString());
					this.client.close();
				}
			} catch (IOException e) {
				logger.error("close client happen exception", e);
			}
		}
	}



	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		if(result == -1) {
			closeConn();
			return;
		}
//		attachment.flip();
/*		byte[] buffer = new byte[result];
		attachment.get(buffer, 0, result);
        attachment.clear();*/

        resolver.excute(attachment);
//            send(attachment);

//		this.client.read(attachment,50,TimeUnit.MINUTES,attachment,this);
	}
	@Override
	public void failed(Throwable ex, ByteBuffer attachment) {
		logger.debug("read failed,maybe resource not exist. exception msg:{}",ex.getMessage());
		closeConn();
	}

    private void send(ByteBuffer data){

		server.ringBuffer.publishEvent(HttpEvent::translateTo,data,resolver);
    }

	
	private void closeConn() {
		try {
			state.decrementAndGet(HttpServerState.FIELD_CONNECTION);
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
