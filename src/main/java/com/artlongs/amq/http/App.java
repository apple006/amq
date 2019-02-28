package com.artlongs.amq.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class App extends Thread {
	private static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
      /*  //
		HttpServer httpServer = new AioHttpServer(new HttpServerConfig());
        //
        Controller controller= new Controller() {
            @Get("/user/{username}")
            public HttpHandler index(String username) {
                return ((res,resp)->{
                    resp.setState(200);
                    resp.append(username);
                    resp.end();

                });
            }
        };
        //
        httpServer.addController(controller);
        httpServer.run();

        while (true) {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

    }



}
