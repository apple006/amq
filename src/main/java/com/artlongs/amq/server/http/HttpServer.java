package com.artlongs.amq.server.http;

import com.artlongs.amq.server.Write;
import com.artlongs.amq.server.http.routes.Controller;

/**
*@author leeton
 *2018年2月6日
 *
 */
public interface HttpServer extends Runnable {
	void start();

	void shutdown();

	void stop();

	void handler(HttpHandler httpHandler);

	HttpHandler getHandler();

	HttpServerState getState();

	HttpServerConfig getConfig();

	HttpServer addController(Controller... controller);

	void writer(Write writer);
	Write getWriter();

	void setDaemon(Runnable runnable);








}
