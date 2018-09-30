package com.artlongs.amq.net.http;

import java.io.IOException;

/**
*@author song(mejeesong@qq.com)
*2018年2月6日
*
*/
public interface HttpServer {
	void start() throws IOException;
	void shutdown();
	void stop();
	void setHttpRequestHandler(HttpRequestHandler handler);
	HttpRequestHandler getHttpRequestHandler();
	HttpServerState getState();
	HttpServerConfig getConfig();
	
}
