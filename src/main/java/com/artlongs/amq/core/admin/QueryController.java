package com.artlongs.amq.core.admin;

import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.core.store.IStore;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.http.Render;
import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Get;
import org.osgl.util.C;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Func :
 *
 * @author: leeton on 2019/3/15.
 */
public class QueryController extends BaseController {

    @Override
    protected void addController() {
        set(topic);
        set(topic_qurey);
    }

    Controller topic = new Controller() {
        @Get("/topic")
        public Render topicIndex() {
            return Render.template("/topic.html");
        }
    };

    Controller topic_qurey = new Controller() {
        @Get("/topic/q")
        public Render topicQurey(String topic, Long begin, Long end, int pageNumber, int pageSize) {
            Map<String, Subscribe> subscribeMap = C.newMap();
            Collection<Subscribe> subscribeList = Store.INST.<Subscribe>getAll(IStore.mq_subscribe, Subscribe.class);
            List<Subscribe> filterList = subscribeList.stream()
                    .filter(s -> s.getTopic().startsWith(topic))
                    .filter(s -> s.getCtime() >= begin && s.getCtime() <= end)
                    .collect(Collectors.toList());

            return Render.json(C.Map("subscribe", filterList));
        }
    };


}
