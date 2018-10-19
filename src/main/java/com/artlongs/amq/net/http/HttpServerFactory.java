package com.artlongs.amq.net.http;

import com.artlongs.amq.net.http.aio.AioHttpServer;
import com.artlongs.amq.net.http.nio.NioHttpServer;
import com.artlongs.amq.net.http.routes.Controller;
import com.artlongs.amq.net.http.routes.Get;

/**
 * Created by ${leeton} on 2018/10/16.
 */
public class HttpServerFactory {
    private HttpServer server;

    public HttpServerFactory(HttpServerConfig config) {
        switch (config.io) {
            case aio:
                this.server = new AioHttpServer(config);
                break;
            case nio:
                this.server = new NioHttpServer(config);
                break;
        }

    }


    public static void main(String[] args) {

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


        HttpServerFactory factory = new HttpServerFactory(new HttpServerConfig());
        factory.server.addController(controller);
        factory.server.run();

        while (true) {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }



    }


}
