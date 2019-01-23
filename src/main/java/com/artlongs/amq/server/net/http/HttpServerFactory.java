package com.artlongs.amq.server.net.http;

import com.artlongs.amq.server.net.http.aio.AioHttpServer;
import com.artlongs.amq.server.net.http.routes.Controller;
import com.artlongs.amq.server.net.http.routes.Get;
import com.artlongs.amq.server.net.http.routes.Url;
import com.artlongs.amq.server.net.http.routes.Post;

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
//                this.server = new NioHttpServer(config);
                break;
        }

    }


    public static void main(String[] args) {

        @Url
        Controller controller= new Controller() {
            @Get("/")
            public HttpHandler index(String username) {
                return ((res,resp)->{
                    resp.setState(200);
                    resp.append(username);
                    resp.end();

                });
            }
        };

        Controller controller2= new Controller() {
            @Post("/upload")
            public HttpHandler upFile(String file,String ok) {
                return ((res,resp)->{
                    resp.setState(200);
                    resp.append(file);
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
