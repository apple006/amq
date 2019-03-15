package com.artlongs.amq.core.admin;

import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.core.store.IStore;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.http.Render;
import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Get;
import org.osgl.util.C;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Func :
 *
 * @author: leeton on 2019/3/15.
 */
public class QueryController {

    public Controller[] getControllers(){
        Controller[] controllers = {topic, topic_qurey};
        return controllers;
    }

    /**
     * 列出所有主题
     */
    Controller topic = new Controller() {
        @Get("/topic")
        public Render allTopic() {
            C.Map params = C.newMap();
            Collection<Subscribe> subscribeList = Store.INST.<Subscribe>getAll(IStore.mq_subscribe, Subscribe.class);
            for (Subscribe subscribe : subscribeList) {
                params.put(subscribe.getId(), subscribe);
            }
            return Render.json(params);
        }
    };

    Controller topic_qurey = new Controller() {
        @Get("/topic/q")
        public Render topicQurey(String topic, Long begin, Long end) {
            Map<String, Subscribe> params = C.newMap();
            Collection<Subscribe> subscribeList = Store.INST.<Subscribe>getAll(IStore.mq_subscribe, Subscribe.class);
            params = subscribeList.stream()
                    .filter(s -> s.getTopic().startsWith(topic))
                    .filter(s -> s.getCtime() >= begin && s.getCtime() <= end)
            .collect(Collectors.toMap(s->s.getId(),s->s));

            return Render.json(params);
        }
    };


}
