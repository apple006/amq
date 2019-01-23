package com.artlongs.amq.server.core;

import com.alibaba.fastjson.JSON;
import com.artlongs.amq.tools.DateUtils;
import com.artlongs.amq.tools.ID;

import java.io.Serializable;
import java.util.List;

/**
 * FUNC: Mq message
 * Created by leeton on 2018/12/13.
 */
public class Message<K extends Message.Key, V> implements KV<K, V> {
    private static final long serialVersionUID = 1L;

    private Message() {
    }

    ////=============================================
    private Key k;
    private V v; // centent body
    private Stat stat;
    private boolean subscribe;
    private Subscribe.Life life;

    ////=============================================
    public static <V> Message ofDef(Key k, V v) {
        long now = DateUtils.now();
        Message m = new Message();
        m.k = k;
        m.v = v;
        Stat stat = new Stat()
                .setCtime(now)
                .setMtime(now)
                .setDelay(0)
                .setRetry(0);

        m.stat = stat;
        return m;
    }
    public static <V> Message ofSubscribe(Key k, V v,boolean subscribe) {
        return ofDef(k, v).setSubscribe(subscribe);
    }

    ////=============================================
    public Message<K, V> setK(Key k) {
        this.k = k;
        return this;
    }

    public Message<K, V> setV(V v) {
        this.v = v;
        return this;
    }

    public Key getK() {
        return k;
    }

    public V getV() {
        return v;
    }

    @Override
    public V get(K k) {
        if (this.k.equals(k)) return this.v;
        return null;
    }

    @Override
    public KV put(K k, V v) {
        this.k = k;
        this.v = v;
        return this;
    }

    public boolean isSubscribe() {
        return subscribe;
    }

    public Message<K, V> setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    public Subscribe.Life getLife() {
        return life;
    }

    public Message<K, V> setLife(Subscribe.Life life) {
        this.life = life;
        return this;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

//====================================    Message Key   ====================================

    /**
     * Message Key
     */
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        private Key() {
        }

        private String id;
        private String topic;
        private SPREAD spread;
        private String recNode;   //接收者 (ip+port)
        private String sendNode;  //发布者 (ip+port)

        public Key(String id, String topic, SPREAD spread) {
            this.id = id;
            this.topic = topic;
            this.spread = spread;
        }

        public Key(String id, String topic, SPREAD spread, String recNode, String sendNode) {
            this.id = id;
            this.topic = topic;
            this.spread = spread;
            this.recNode = recNode;
            this.sendNode = sendNode;
        }

        /**
         * 创建 MESSAGE ID 格式: xx(2位客户机编号,16进制)_yyyyMMddHHmmssSSS"(17) + (2位)原子顺序数累加
         *
         * @param clientId
         * @return
         */
        public String createAndSetId(String clientId) {
            String id = clientId + ID.ONLY.id();
            setId(id);
            return id;
        }

        public String getId() {
            return id;
        }

        public String getTopic() {
            return topic;
        }

        public SPREAD getSpread() {
            return spread;
        }

        public String getRecNode() {
            return recNode;
        }

        public String getSendNode() {
            return sendNode;
        }

        public Key setId(String id) {
            this.id = id;
            return this;
        }

        public Key setTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public Key setSpread(SPREAD spread) {
            this.spread = spread;
            return this;
        }

        public Key setRecNode(String recNode) {
            this.recNode = recNode;
            return this;
        }

        public Key setSendNode(String sendNode) {
            this.sendNode = sendNode;
            return this;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }

    //====================================    Message Stat   ====================================
    public static class Stat {
        private static final long serialVersionUID = 1L;
        private ON on;
        private Long ttl;   //Time To Live
        private Long ctime; //create time
        private Long mtime; //modify time
        private long delay; //延迟发送
        private int retry; //重试次数
        private List<String> nodesDelivered; // 已送达
        private List<String> nodesConfirmed; // 已确认

        public ON getOn() {
            return on;
        }

        public Stat setOn(ON on) {
            this.on = on;
            return this;
        }

        public Long getTtl() {
            return ttl;
        }

        public Stat setTtl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        public Long getCtime() {
            return ctime;
        }

        public Stat setCtime(long ctime) {
            this.ctime = ctime;
            return this;
        }

        public Long getMtime() {
            return mtime;
        }

        public Stat setMtime(long mtime) {
            this.mtime = mtime;
            return this;
        }

        public Long getDelay() {
            return delay;
        }

        public Stat setDelay(long delay) {
            this.delay = delay;
            return this;
        }

        public int getRetry() {
            return retry;
        }

        public Stat setRetry(int retry) {
            this.retry = retry;
            return this;
        }

        public List<String> getNodesDelivered() {
            return nodesDelivered;
        }

        public Stat setNodesDelivered(List<String> nodesDelivered) {
            this.nodesDelivered = nodesDelivered;
            return this;
        }

        public List<String> getNodesConfirmed() {
            return nodesConfirmed;
        }

        public Stat setNodesConfirmed(List<String> nodesConfirmed) {
            this.nodesConfirmed = nodesConfirmed;
            return this;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }

    /**
     * Message status
     */
    public enum ON {
        SENED, QUENED, ACKED, DONE;
    }

    /**
     * SPREAD
     */
    public enum SPREAD {
        TOPIC, FANOUT, DIRECT;
    }


    public static void main(String[] args) {
        Message msg = new Message().ofDef(new Key(ID.ONLY.id(), "quene", SPREAD.FANOUT), "hello");
        System.err.println("msg=" + msg);
    }


}
