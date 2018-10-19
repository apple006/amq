package com.artlongs.amq.net.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;

/**
*@author leeton
*2018年2月6日
*
*/
public class HttpResolver {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	public enum ReadState {
		BEGIN, REQ_LINE,HEADERS, BODY, END
	}
	
	private StringBuilder context = new StringBuilder();
	
	Request req = null;
	
	private ReadState state = ReadState.BEGIN;
	private AsynchronousSocketChannel client;
	private HttpServer httpServer = null;
	
	public HttpResolver(HttpServer httpServer, AsynchronousSocketChannel client) {
		this.httpServer = httpServer;
		this.client = client;
	}
	
	public void append(ByteBuffer data) throws Exception{
//		context.append(data);
		CharBuffer buffer = StandardCharsets.UTF_8.decode(data);
		context.append(buffer.toString());
		if(state == ReadState.BEGIN || state == ReadState.REQ_LINE) {
			req = new Request();
			resolveRequestLine();
		}
		if(state == ReadState.HEADERS) {
			resolveHeader();
		}
		if(state == ReadState.BODY) {
			resolveBody(data.array());
		}
		if(state == ReadState.END) {
			logger.debug("receive a new request from {},uri={},req={},socket={}",client.getRemoteAddress(),req.uri,req.hashCode(),client.hashCode());
			HttpResponse resp = new Response(client);
			HttpServerState state = httpServer.getState();
			state.incrementAndGet(HttpServerState.FIELD_CONCURRENT);
			try {
				httpServer.getHandler().handle(req, resp);
				state.decrementAndGet(HttpServerState.FIELD_CONCURRENT);
			}catch(Exception e) {
				state.decrementAndGet(HttpServerState.FIELD_CONCURRENT);
				throw e;
			}finally {
				// free buffer
				HttpServerConfig.bufferPool.allocate().free();
			}
			logger.debug("handle finished the request from {},uri={},req={},socket={}",client.getRemoteAddress(),req.uri,req.hashCode(),client.hashCode());
		}
	}
	private void resolveRequestLine() {
		state = ReadState.REQ_LINE;
		int x = context.indexOf("\r\n");
		if(x >= 0) {
			String firstLine = context.substring(0,x);
			context.delete(0, x+2);
			x = firstLine.indexOf(' ');
			//如果x<0代表请求违法，会抛出异常
			req.method = firstLine.substring(0, x).toUpperCase();
			x++;
			int y = firstLine.indexOf(' ',x);
			String uri = firstLine.substring(x, y);
			x = uri.indexOf('?');
			if(x>0) {
				req.uri = uri.substring(0,x);
				req.query = uri.substring(++x);
			}else {
				req.uri = uri;
			}
		}

		state = ReadState.HEADERS;
	}
	private void resolveHeader() {
		int x = context.indexOf("\r\n");
		while(x>0) {
			int y = context.indexOf(":");
			String key = context.substring(0,y).trim();
			String value = context.substring(y+1, x);
			req.headers.put(key, value);
			context.delete(0, x+2);
			x = context.indexOf("\r\n");
		}
		if(x==0) {
			context.delete(0, 2);
			if(Request.METHOD_POST.equals(req.method)) {
				state = ReadState.BODY;
			}else {
				state = ReadState.END;
			}
			return;
		}
		state = ReadState.END;
	}
	private void resolveBody(byte[] data) {
		if (state != ReadState.BODY) return;
		req.bodyBytes = data;
		this.state = ReadState.END;
	}
}
