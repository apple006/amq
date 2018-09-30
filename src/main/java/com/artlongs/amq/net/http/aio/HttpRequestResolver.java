package com.artlongs.amq.net.http.aio;

import com.artlongs.amq.net.http.HttpResponse;
import com.artlongs.amq.net.http.HttpServer;
import com.artlongs.amq.net.http.HttpServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousSocketChannel;

/**
*@author song(mejeesong@qq.com)
*2018年2月6日
*
*/
public class HttpRequestResolver {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final int STATE_NEW = 0;
	private static final int STATE_REQ_LINE = 1;
	private static final int STATE_HEADER = 2;
	private static final int STATE_BODY = 3;
	
	private StringBuilder context = new StringBuilder();
	
	AioHttpRequest req = null;
	
	private int state = STATE_NEW;
	private AsynchronousSocketChannel client;
	private HttpServer httpServer = null;
	
	public HttpRequestResolver(AioHttpServer httpServer,AsynchronousSocketChannel client) {
		this.httpServer = httpServer;
		this.client = client;
	}
	
	public void append(byte[] data) throws Exception{
		context.append(new String(data));
		if(state == STATE_NEW || state == STATE_REQ_LINE) {
			req = new AioHttpRequest();
			resolveRequestLine();
		}
		if(state == STATE_HEADER) {
			resolveHeader();
		}
		if(state == STATE_BODY) {
			resolveBody();
		}
		if(state == STATE_NEW) {
			logger.debug("receive a new request from {},uri={},req={},socket={}",client.getRemoteAddress(),req.uri,req.hashCode(),client.hashCode());
			HttpResponse resp = new AioHttpResponse(client);
			HttpServerState state = httpServer.getState();
			state.incrementAndGet(HttpServerState.FIELD_CONCURRENT);
			try {
				httpServer.getHttpRequestHandler().handle(req, resp);
				state.decrementAndGet(HttpServerState.FIELD_CONCURRENT);
			}catch(Exception e) {
				state.decrementAndGet(HttpServerState.FIELD_CONCURRENT);
				throw e;
			}
			logger.debug("handle finished the request from {},uri={},req={},socket={}",client.getRemoteAddress(),req.uri,req.hashCode(),client.hashCode());
		}
	}
	private void resolveRequestLine() {
		state = STATE_REQ_LINE;
		int x = context.indexOf("\r\n");
		if(x >= 0) {
			String firstLine = context.substring(0,x);
			context.delete(0, x+2);
			x = firstLine.indexOf(' ');
			//如果x<0代表请求不违法，会抛出异常
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
			state = STATE_HEADER;
		}
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
			if(AioHttpRequest.METHOD_POST.equals(req.method)) {
				state = STATE_BODY;
			}else {
				state = STATE_NEW;
			}
			return;
		}
	}
	private void resolveBody() {
		
	}
}
