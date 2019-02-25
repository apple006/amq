package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * Func :订阅
 *
 * @author: leeton on 2019/1/23.
 */
public class Subscribe implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String topic;
    private AioPipe pipe;
    private Life life;
    private boolean done;

    public Subscribe(String id, String topic, AioPipe pipe, Life life ,boolean done) {
        this.id = id;
        this.topic = topic;
        this.pipe = pipe;
        this.life = life;
        this.done = done;
    }

    /**
     * 订阅的生命周期
     */
    public enum Life{ LONG, SPARK;}

    public void remove(List<Subscribe> subscribeList,Subscribe target) {
        Iterator<Subscribe> iterable = subscribeList.iterator();
        while (iterable.hasNext()) {
            Subscribe item = iterable.next();
            if (item.equals(item)) {
                subscribeList.remove(target);
            }
        }

    }

    //================================ 我的貂婵在那里 ================================================


    public String getId() {
        return id;
    }

    public Subscribe setId(String id) {
        this.id = id;
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public Subscribe setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public AioPipe getPipe() {
        return pipe;
    }

    public Subscribe setPipe(AioPipe pipe) {
        this.pipe = pipe;
        return this;
    }

    public Life getLife() {
        return life;
    }

    public Subscribe setLife(Life life) {
        this.life = life;
        return this;
    }

    public boolean isDone() {
        return done;
    }

    public Subscribe setDone(boolean done) {
        this.done = done;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Subscribe{");
        sb.append("id='").append(id).append('\'');
        sb.append(", topic='").append(topic).append('\'');
        sb.append(", pipe=").append(pipe);
        sb.append(", life=").append(life);
        sb.append(", done=").append(done);
        sb.append('}');
        return sb.toString();
    }
}
