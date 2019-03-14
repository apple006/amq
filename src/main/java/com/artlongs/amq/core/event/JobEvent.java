package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.disruptor.EventFactory;

/**
 * Func : 服务端收到的数据等同于一个 JOB 事件
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvent {
    private Message message;  // 普通消息
    private Subscribe subscribe;  //订阅
    private boolean storeAllMsg; // 保存所有的消息


    public Message getMessage() {
        return message;
    }

    public JobEvent setMessage(Message message) {
        this.message = message;
        return this;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public JobEvent setSubscribe(Subscribe subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    public boolean isStoreAllMsg() {
        return storeAllMsg;
    }

    public JobEvent setStoreAllMsg(boolean storeAllMsg) {
        this.storeAllMsg = storeAllMsg;
        return this;
    }

    public static final EventFactory<JobEvent> EVENT_FACTORY = new EventFactory<JobEvent>()
    {
        public JobEvent newInstance()
        {
            return new JobEvent();
        }
    };

    public static void translate(JobEvent jobEvent, long sequence, Message msg) {
        jobEvent.setMessage(msg);
    }
    public static void translate(JobEvent jobEvent, long sequence, Subscribe msg) {
        jobEvent.setSubscribe(msg);
    }

    public static void translate(JobEvent jobEvent, long sequence, Message msg,boolean storeAllMsg) {
        jobEvent.setMessage(msg);
        jobEvent.setStoreAllMsg(storeAllMsg);
    }
}
