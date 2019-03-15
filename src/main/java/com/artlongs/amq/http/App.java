package com.artlongs.amq.http;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.store.IStore;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.http.aio.AioHttpServer;
import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Get;
import org.osgl.util.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Hello world!
 *
 */
public class App extends Thread {
	private static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
        //
		HttpServer httpServer = new AioHttpServer(new HttpServerConfig());
/*
		Controller controller= new Controller() {
			@Get("/user")
			public HttpHandler index(String username) {
                return ((res,resp)->{
                    resp.setState(200);
                    resp.append(username);
                    resp.end();

                });
			}
		};*/

        // Template
        Controller controller1= new Controller() {
            @Get("/user")
            public Render index(String username) {
				return Render.template("/hello.html",C.newMap("username", username));
            }
        };

        Controller controller2= new Controller() {
            @Get("/user/{username}")
            public Render index(String username) {
                return Render.json(C.newMap("username", username));
            }
        };

        Controller topic= new Controller() {
            @Get("/amq/topic")
            public Render index() {
                C.Map params = C.Map().readOnly(false);
                Collection<Message> messageList = Store.INST.<Message>getAll(IStore.mq_all_data,Message.class);
    /*            while (iterator.hasNext()) {
                    Subscribe subscribe = iterator.next();
                    if (subscribe != null) {
                        params.put(subscribe.getId(), subscribe.getTopic());
                    }
                }*/
                for (Message message : messageList) {
                    params.put(message.getK().getId(), message);
                }

                return Render.json(params);
            }
        };

        //
        httpServer.addController(controller1,controller2,topic);
        httpServer.start();

        while (true) {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }



}
