package com.artlongs.amq.server.core;

import com.artlongs.amq.serializer.FastJsonSerializer;
import com.artlongs.amq.serializer.ISerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * Func : 消息处理中心
 *
 * @author: leeton on 2019/1/22.
 */
public enum ProcessorImpl implements Processor {
    INST;
    private static Logger logger = LoggerFactory.getLogger(ProcessorImpl.class);

    /**
     * 从客户端读取到的数据缓存 map(key,ByteBuffer)
     */
    public static ConcurrentMap<String, ByteBuffer> client_data = new ConcurrentHashMap<>();

    /**
     * 客户端订阅的主题
     */
    public static ConcurrentMap<String, Subscribe> subscribe = new ConcurrentHashMap<>();

    public static ISerializer json = new FastJsonSerializer();

    @Override
    public ByteBuffer getBuffer(String id) {
        return client_data.get(id);
    }

    @Override
    public ConcurrentMap<String, ByteBuffer> addBuffer(ByteBuffer buffer) {
        Message message = doParser(buffer);
        if(!addToSubscribeIF(message)){// 不是纯订阅的消息,才加入消息缓存中,进一步处理
            client_data.putIfAbsent(message.getK().getId(), buffer);
        }
        return client_data;
    }

    private boolean addToSubscribeIF(Message message) {
        if (message.isSubscribe()) {
            String clientKey = message.getK().getSendNode();
            NetworkChannel channel = MqServer.clientSocketMap.get(clientKey);
            Subscribe listen = new Subscribe(clientKey, message.getK().getTopic(),channel, message.getLife(), false);
            subscribe.putIfAbsent(message.getK().getId(), listen);
            return true;
        }
        return false;
    }

    @Override
    public Message doParser(ByteBuffer buffer) {
        return json.getObj(buffer);
    }

    @Override
    public void doMatch(Message message) {

    }

    @Override
    public Future toStore(Message message) {
        return null;
    }

    @Override
    public void buildJob(Message message) {

    }

    @Override
    public void sendJobToRingBuffer(Message message) {

    }

    @Override
    public Future ackJob(Message message) {
        return null;
    }
}
