package com.artlongs.amq.http;

import com.artlongs.amq.http.aio.handler.SocketWriteHandler;
import com.artlongs.amq.http.routes.Controller;

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

	void writer(SocketWriteHandler writer);
	SocketWriteHandler getWriter();

	void setDaemon(Runnable runnable);








}
