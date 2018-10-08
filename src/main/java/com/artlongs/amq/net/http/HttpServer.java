package com.artlongs.amq.net.http;

import com.artlongs.amq.net.http.routes.Controller;

/**
*@author leeton
*2018年2月6日
*
*/
public interface HttpServer extends Runnable{
	void start();
	void shutdown();
	void stop();
	void setHttpRequestHandler(HttpHandler handler);
	HttpHandler getHttpRequestHandler();
	HttpServerState getState();
	HttpServerConfig getConfig();

	public HttpServer accept(Controller controller);
	
}
