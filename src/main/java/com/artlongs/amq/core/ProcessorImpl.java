package com.artlongs.amq.core;

import com.artlongs.amq.disruptor.RingBuffer;
import com.artlongs.amq.disruptor.YieldingWaitStrategy;
import com.artlongs.amq.disruptor.dsl.Disruptor;
import com.artlongs.amq.disruptor.dsl.ProducerType;
import com.artlongs.amq.disruptor.util.DaemonThreadFactory;
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

    public static ISerializer json = ISerializer.Serializer.INST.of();

    // ringBuffer setting
    private static final int BUFFER_SIZE = 1024 * 64;
    private final JobEvnetHandler jobEvnetHandler = new JobEvnetHandler();
    private final RingBuffer<JobEvent> ringBuffer;

    ProcessorImpl() {
        //创建消息多线程 ringBuffer
        this.ringBuffer = createDisrupter().start();
    }

    /**
     * 创建消息 disruptor
     * @return
     */
    private Disruptor<JobEvent> createDisrupter(){
        Disruptor<JobEvent> disruptor =
                new Disruptor<JobEvent>(
                        JobEvent.EVENT_FACTORY,
                        BUFFER_SIZE,
                        DaemonThreadFactory.INSTANCE,
                        ProducerType.SINGLE,
                        new YieldingWaitStrategy());
        disruptor.handleEventsWith(jobEvnetHandler);
        return disruptor;
    }

    @Override
    public ByteBuffer getBuffer(String id) {
        return client_data.get(id);
    }

    @Override
    public ConcurrentMap<String, ByteBuffer> add(ByteBuffer buffer) {
        Message message = doParser(buffer);
        if(!addSubscribeIF(message)){// 不是纯订阅的消息,才加入消息缓存中,进一步处理
            client_data.putIfAbsent(message.getK().getId(), buffer);
        }
        // 发布 JOB 到 RINGBUFFER
        ringBuffer.publishEvent(JobEvent::translate,message);

        return client_data;

    }

    private boolean addSubscribeIF(Message message) {
        if (message.isSubscribe()) {
            String clientKey = message.getK().getSendNode();
            NetworkChannel channel = MqServer.clientSocketMap.get(clientKey);
            Subscribe listen = new Subscribe(clientKey, message.getK().getTopic(),channel, message.getLife(), false);
            subscribe.putIfAbsent(clientKey, listen);
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
