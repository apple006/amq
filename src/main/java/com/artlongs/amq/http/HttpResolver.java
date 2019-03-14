package com.artlongs.amq.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

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

	private Request req = null;
	
	private ReadState state = ReadState.BEGIN;
	private HttpServer httpServer = null;
	
	public HttpResolver(HttpServer httpServer) {
		this.httpServer = httpServer;
	}

    public void excute(ByteBuffer data){
//		context.excute(write);
		data.flip();
		CharBuffer buffer = HttpServerConfig.charsets.decode(data);
		context.append(buffer);
		if(state == ReadState.BEGIN || state == ReadState.REQ_LINE) {
			req = new Request();
			resolveRequestLine();
		}
		if(state == ReadState.HEADERS) {
			resolveHeader();
		}
		if(state == ReadState.BODY) {
			resolveBody(context);
		}
		if(state == ReadState.END) {
//			logger.debug("receive a new request from {},uri={},req={},socket={}",client.getRemoteAddress(),req.uri,req.hashCode(),client.hashCode());
			HttpResponse resp = new Response(this.httpServer);
			HttpServerState.CONNECTION_NUMS.getAndIncrement();
			try {
				httpServer.getHandler().handle(req, resp);
			}catch(Exception e) {
				logger.error("Build http handle exception:", e);

			}finally {
				// free buffer
				HttpServerConfig.bufferPool.allocate().free();
				data.clear();
			}
//			logger.debug("handle finished the request from {},uri={},req={},socket={}",client.getRemoteAddress(),req.uri,req.hashCode(),client.hashCode());
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
				parserReqParams(req.query);
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
	private void resolveBody(StringBuilder data) {
		if (state != ReadState.BODY) return;
		req.bodyBytes = data.toString().getBytes();
		/*int idx = write.indexOf("; ");
		String paramChars = write.substring(idx+2);
		parserPostParams(paramChars);*/
		state = ReadState.END;
	}

	private void parserReqParams(String paramStr) {
		if (paramStr.length() <= 0) return;
		String[] paramArr = paramStr.split("&");
		for (String kvStr : paramArr) {
			String[] kv = kvStr.split("=");
			req.params.put(kv[0], kv[1]);
		}
	}

	private void parserPostParams(String paramStr) {
		if (paramStr.length() <=0) return;
		String[] paramArr = paramStr.split(";");
		for (String kvStr : paramArr) {
			String[] kv = kvStr.split("=");
			req.params.put(kv[0], kv[1]);
		}
	}

}
