package com.artlongs.amq.core;

import com.artlongs.amq.disruptor.BatchEventProcessor;
import com.artlongs.amq.disruptor.RingBuffer;
import com.artlongs.amq.disruptor.SequenceBarrier;
import com.artlongs.amq.disruptor.YieldingWaitStrategy;
import com.artlongs.amq.disruptor.util.DaemonThreadFactory;
import com.artlongs.amq.server.mq.NewHandler;
import com.artlongs.amq.tools.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Func :
 *
 * @author: leeton on 2019/1/15.
 */
public class NioServer implements MqServer {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private RingBuffer<MqAcceptEvent> accept_ringbuffer;
    private Set<SelectionKey> selectionKeys;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(DaemonThreadFactory.INSTANCE);
    private static final int ACCEPT_BUFFER_SIZE = 1024 * 64;
    private MqConfig config;

    public NioServer(MqConfig config) throws IOException {
        this.config = config;
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(config.server_ip, config.port));
        accept();
    }

    @Override
    public void regClient(NetworkChannel channel) {
        clientSocketMap.putIfAbsent(IOUtils.getRemoteAddress(channel).toString(), channel);
    }

    @Override
    public void start() {
        this.accept_ringbuffer = buildAcceptRingBuffer();
        final SequenceBarrier sequenceBarrier = accept_ringbuffer.newBarrier();
        final MqEventHandler handler = new MqEventHandler();
        final BatchEventProcessor<MqAcceptEvent> batchEventProcessor = new BatchEventProcessor<MqAcceptEvent>(accept_ringbuffer, sequenceBarrier, handler);
        accept_ringbuffer.addGatingSequences(batchEventProcessor.getSequence());

        executor.submit(batchEventProcessor);

        while (true) {
            try {
                int readyChannels = selector.select();
                if(readyChannels == 0) continue;
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    long sequence = accept_ringbuffer.next();
                    accept_ringbuffer.get(sequence).setVal(selectionKey);
                    accept_ringbuffer.publish(sequence);
                }
                long expectedCount = batchEventProcessor.getSequence().get() + selectionKeys.size();
//                waitForEventProcessorSequence(batchEventProcessor,expectedCount);
                batchEventProcessor.halt();
                selectionKeys.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private void waitForEventProcessorSequence(BatchEventProcessor<MqAcceptEvent> batchEventProcessor,long expectedCount) throws InterruptedException
    {
        while (batchEventProcessor.getSequence().get() != expectedCount)
        {
            Thread.sleep(1);
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void daemon(Runnable runnable) {

    }

    @Override
    public void run() {

    }

    private RingBuffer<MqAcceptEvent> buildAcceptRingBuffer() {
        RingBuffer ringBuffer = RingBuffer.createSingleProducer(MqAcceptEvent.EVENT_FACTORY, ACCEPT_BUFFER_SIZE, new YieldingWaitStrategy());
        return ringBuffer;

    }

    @Override
    public void accept() {
        try {
            SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            selectionKey.attach(new Acceptor());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    // 创建一个新的处理类
                    new NewHandler(socketChannel, selector).run();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public NetworkChannel getServerChannel() {
        return serverSocketChannel;
    }

    @Override
    public MqConfig getConfig() {
        return config;
    }

    @Override
    public void assignJob() {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }

    public static void main(String[] args) throws IOException {
        new NioServer(new MqConfig()).start();
    }


}
