package com.artlongs.amq.demo;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.aio.AioClient;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.AioProcessor;
import com.artlongs.amq.core.aio.Protocol;
import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.tools.ID;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutionException;

/**
 * Func :
 *
 * @author: leeton on 2019/2/25.
 */
public class AioMqClient<T> extends AioClient<T> {
    private static ISerializer serializer = ISerializer.Serializer.INST.of();

    private AioPipe aioPipe;

    public AioMqClient(String host, int port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        super(host, port, protocol, messageProcessor);
    }

    @Override
    public AioPipe<T> start(AsynchronousChannelGroup asynchronousChannelGroup) throws IOException, ExecutionException, InterruptedException {
        aioPipe =  super.start(asynchronousChannelGroup);
        return aioPipe;
    }

    @Override
    public void run() {
        super.run();
    }

    public <M> Message buildMessage(String topic, M data, Message.SPREAD spread) {
        Message.Key mKey = new Message.Key(ID.ONLY.id(), topic, spread);
        String node = aioPipe.getLocalAddress().toString();
        mKey.setSendNode(node);
        return Message.ofDef(mKey, data);
    }

    public <M> Message buildSubscribe(String topic,M data) {
        Message.Key mKey = new Message.Key(ID.ONLY.id(), topic, Message.SPREAD.TOPIC);
        String node = aioPipe.getLocalAddress().toString();
        mKey.setSendNode(node);
        Message message = Message.ofDef(mKey, data);
        message.setSubscribe(true);
        return message;
    }

    public Message buildAcked(String msgId) {
        Message message = Message.ofAcked(msgId);
        return message;
    }

}
