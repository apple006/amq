package com.artlongs.amq.net.http;

import com.artlongs.amq.net.http.aio.AioHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App {
	private static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		HttpServer httpServer = new AioHttpServer(new HttpServerConfig());
		httpServer.setHttpRequestHandler((res,resp)->{

		});
		try {
			httpServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(true) {
			logger.debug(httpServer.getState().getInfo());
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
