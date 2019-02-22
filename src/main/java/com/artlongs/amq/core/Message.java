package com.artlongs.amq.core;

import com.alibaba.fastjson.JSON;
import com.artlongs.amq.tools.DateUtils;
import com.artlongs.amq.tools.ID;
import org.osgl.util.C;

import java.io.Serializable;
import java.util.Set;

/**
 * FUNC: Mq message
 * Created by leeton on 2018/12/13.
 */
public class Message<K extends Message.Key, V> implements KV<K, V> {
    private static final long serialVersionUID = 1L;

    protected Message() {
    }

    ////=============================================
    private Key k;
    private V v; // centent body
    private Stat stat;
    private Boolean subscribe;
    private Subscribe.Life life;
    private boolean acked;

    ////=============================================
    public static <V> Message ofDef(Key k, V v) {
        long now = DateUtils.now();
        Message m = new Message();
        m.k = k;
        m.v = v;
        m.subscribe = false;
        Stat stat = new Stat()
                .setCtime(now)
                .setMtime(now)
                .setDelay(0)
                .setRetry(0);

        m.stat = stat;
        return m;
    }

    public static Message empty() {
        Message m = new Message();
        return m;
    }

    public static <V> Message ofSubscribe(Key k, V v, boolean subscribe) {
        return ofDef(k, v).setSubscribe(subscribe);
    }

    public void upStatOfSended(String node) {
        Stat stat = getStat();
        if (null == stat.nodesDelivered) {
            stat.nodesDelivered = C.newSet();
        }
        stat.nodesDelivered.add(node);
        stat.setMtime(System.currentTimeMillis());
        stat.setOn(Message.ON.SENED);
    }

    public void upStatOfACK(String node) {
        Stat stat = getStat();
        if (null == stat.nodesConfirmed) {
            stat.nodesConfirmed = C.newSet();
        }
        stat.nodesConfirmed.add(node);
        stat.setMtime(System.currentTimeMillis());
        stat.setOn(Message.ON.SENED);
    }

    /**
     * 累加重发次数
     */
    public void incrRetry() {
        Stat stat = getStat();
        if (null != stat) {
            stat.setRetry(stat.getRetry() + 1);
        }
    }

    /**
     * 累加延迟次数
     */
    public void incrDelay() {
        Stat stat = getStat();
        if (null != stat) {
            stat.setDelay(stat.getDelay() + 1);
        }
    }

    public int ackedSize(){
        if(null == this.getStat()) return 0;
        if(null == this.getStat().getNodesConfirmed()) return 0;
        return this.getStat().getNodesConfirmed().size();
    }

    /**
     * 创建 ACK 消息
     *
     * @return
     */
    public Message changeToAck() {
        this.acked = true;
        this.k.spread = null;
        this.k.sendNode = null;
        this.k.recNode = null;
        this.k.topic = null;
        this.stat = null;
        this.life = null;
        this.v = null;
        return this;
    }

    public static Message ofAcked(String msgId) {
        Message m = new Message();
        m.setK(new Key());
        m.k.id = msgId;
        m.acked = true;
        m.k.spread = null;
        m.k.sendNode = null;
        m.k.recNode = null;
        m.k.topic = null;
        m.stat = null;
        m.life = null;
        m.v = null;
        return m;
    }

    ////=============================================

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

    public Key getK() {
        return k;
    }

    public Message<K, V> setK(Key k) {
        this.k = k;
        return this;
    }

    public V getV() {
        return v;
    }

    public Message<K, V> setV(V v) {
        this.v = v;
        return this;
    }

    public Stat getStat() {
        return stat;
    }

    public Message<K, V> setStat(Stat stat) {
        this.stat = stat;
        return this;
    }

    public Boolean isSubscribe() {
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

    public boolean isAcked() {
        return acked;
    }

    public Message<K, V> setAcked(boolean acked) {
        this.acked = acked;
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

        public boolean isSelf(String sendNode) {
            return this.getSendNode().equals(sendNode);
        }

        //==============================================================

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
        private int delay; //延迟发送(消息未ACKED)
        private int retry; //重试次数(发送失败之后再重发)
        private Set<String> nodesDelivered; // 已送达
        private Set<String> nodesConfirmed; // 已确认

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

        public int getDelay() {
            return delay;
        }

        public Stat setDelay(int delay) {
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

        public Set<String> getNodesDelivered() {
            return nodesDelivered;
        }

        public Stat setNodesDelivered(Set<String> nodesDelivered) {
            this.nodesDelivered = nodesDelivered;
            return this;
        }

        public Set<String> getNodesConfirmed() {
            return nodesConfirmed;
        }

        public Stat setNodesConfirmed(Set<String> nodesConfirmed) {
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
        QUENED, SENDING,SENDONFAIL, SENED, ACKED, DONE;
    }

    /**
     * SPREAD
     */
    public enum SPREAD {
        TOPIC, DIRECT;
    }


    public static void main(String[] args) {
        Message msg = new Message().ofDef(new Key(ID.ONLY.id(), "quene", SPREAD.TOPIC), "hello");
        System.err.println("msg=" + msg);
        System.err.println("ack=" + msg.changeToAck());
    }


}
