package com.artlongs.amq.core;

import java.nio.channels.NetworkChannel;

/**
 * Func :订阅
 *
 * @author: leeton on 2019/1/23.
 */
public class Subscribe {
    private String id;
    private String topic;
    private NetworkChannel channel;
    private Life life;
    private boolean done;

    public Subscribe(String id, String topic, NetworkChannel channel, Life life ,boolean done) {
        this.id = id;
        this.topic = topic;
        this.channel = channel;
        this.life = life;
        this.done = done;
    }

    /**
     * 订阅的生命周期
     */
    public enum Life{
        LONG,
        SPARK;
    }

    //================


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

    public NetworkChannel getChannel() {
        return channel;
    }

    public Subscribe setChannel(NetworkChannel channel) {
        this.channel = channel;
        return this;
    }

    public boolean isDone() {
        return done;
    }

    public Subscribe setDone(boolean done) {
        this.done = done;
        return this;
    }

    public Life getLife() {
        return life;
    }

    public Subscribe setLife(Life life) {
        this.life = life;
        return this;
    }
}
