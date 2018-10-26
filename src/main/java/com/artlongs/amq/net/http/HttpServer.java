package com.artlongs.amq.net.http;

import com.artlongs.amq.net.http.routes.Controller;

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

	void writer(Writer writer);
	Writer getWriter();

	@Override
	default void run() {
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		this.start();
	}

}
