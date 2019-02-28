package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.tools.RingBufferQueue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

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
    private Listen listen;
    private int idx; // 在队列里的 index

    public Subscribe(String id, String topic, AioPipe pipe, Life life,Listen listen) {
        this.id = id;
        this.topic = topic;
        this.pipe = pipe;
        this.life = life;
        this.listen = listen;
    }

    /**
     * 订阅的生命周期
     */
    public enum Life{ ALL_COMFIRM, SPARK;}

    /**
     * 监听消息的模式
     */
    public enum Listen{
        PINGPONG, // 使用 Future 读取一次F
        CALLBACK; // 回调的模式
    }

    public void remove(Collection<Subscribe> subscribeList, Subscribe target) {
        Iterator<Subscribe> iterable = subscribeList.iterator();
        while (iterable.hasNext()) {
            Subscribe item = iterable.next();
            if (item.equals(item)) {
                subscribeList.remove(target);
            }
        }

    }

    public void remove(RingBufferQueue queue, Subscribe target) {
        queue.remove(target.idx);
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

    public int getIdx() {
        return idx;
    }

    public Subscribe setIdx(int idx) {
        this.idx = idx;
        return this;
    }


    public Listen getListen() {
        return listen;
    }

    public Subscribe setListen(Listen listen) {
        this.listen = listen;
        return this;
    }


}
