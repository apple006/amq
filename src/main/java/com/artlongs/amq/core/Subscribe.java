package com.artlongs.amq.core;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.tools.RingBufferQueue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * Func :订阅(消息中收内部使用), Message 的简化版本
 *
 * @author: leeton on 2019/1/23.
 */
public class Subscribe implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String topic;
    private AioPipe pipe;
    private Message.Life life;
    private Message.Listen listen;
    private int idx; // 在队列里的 index,记录下来,以加速remove

    public Subscribe(String id, String topic, AioPipe pipe, Message.Life life,Message.Listen listen) {
        this.id = id;
        this.topic = topic;
        this.pipe = pipe;
        this.life = life;
        this.listen = listen;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscribe subscribe = (Subscribe) o;
        return Objects.equals(topic, subscribe.topic) &&
                Objects.equals(pipe, subscribe.pipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, pipe);
    }

//================================ 我的貂婵在那里 ================================================


    public String getId() {
        return id;
    }

    public Subscribe setId(String id) {
        this.id = id;
        return this;
    }
    public int getIdx() {
        return idx;
    }

    public Subscribe setIdx(int idx) {
        this.idx = idx;
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

    public Message.Life getLife() {
        return life;
    }

    public Subscribe setLife(Message.Life life) {
        this.life = life;
        return this;
    }

    public Message.Listen getListen() {
        return listen;
    }

    public Subscribe setListen(Message.Listen listen) {
        this.listen = listen;
        return this;
    }
}
