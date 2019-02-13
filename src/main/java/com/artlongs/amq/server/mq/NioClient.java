package com.artlongs.amq.server.mq;

import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.disruptor.util.DaemonThreadFactory;
import com.artlongs.amq.tools.FastList;
import com.artlongs.amq.tools.IOUtils;
import com.artlongs.amq.tools.ID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioClient implements Runnable {
    // The host:port combination to connect to
    private String serverIp;
    private int port;

    //this channel
    private SocketChannel channel;

    // The selector we'll be monitoring
    private Selector selector;

    // The buffer into which we'll read data when it's available
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    // A list of PendingChange instances
    private FastList<ChangeRequest> pendingChanges = new FastList(ChangeRequest.class,MqConfig.clinet_send_max);

    // Maps a SocketChannel to a list of ByteBuffer instances
    private Map<SocketChannel,FastList> pendingData = new HashMap();

    // Maps a SocketChannel to a RspHandler
    private Map rspHandlers = Collections.synchronizedMap(new HashMap());

    private static ISerializer serializer = ISerializer.Serializer.INST.of();

    public NioClient(String serverIp, int port) throws IOException {
        this.serverIp = serverIp;
        this.port = port;
        // Start a new connection
        channel = this.initiateConnection();
        this.selector = this.initSelector();
        //为该通道注册SelectionKey.OP_CONNECT事件
        channel.register(selector, SelectionKey.OP_CONNECT);
    }


    private SocketChannel initiateConnection() throws IOException {
        // Create a non-blocking socket channel
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        // Kick off connection establishment
        socketChannel.connect(new InetSocketAddress(this.serverIp, this.port));

        // Queue a channel registration since the caller is not the
        // selecting thread. As part of the registration we'll register
        // an interest in connection events. These are raised when a channel
        // is ready to complete connection establishment.
        synchronized (pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }

        return socketChannel;
    }

    private Selector initSelector() throws IOException {
        // Create a new selector
        return SelectorProvider.provider().openSelector();
    }

    public void run() {
        while (true) {
            try {
                // Process any pending changes
                synchronized (this.pendingChanges) {
                    Iterator changes = this.pendingChanges.iterator();
                    while (changes.hasNext()) {
                        ChangeRequest change = (ChangeRequest) changes.next();
                        switch (change.type) {
                            case ChangeRequest.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(this.selector);
                                key.interestOps(change.ops);
                                break;
                            case ChangeRequest.REGISTER:
                                change.socket.register(this.selector, change.ops);
                                break;
                        }
                    }
                    this.pendingChanges.clear();
                }

                // Wait for an event one of the registered channels
                this.selector.select();

                // Iterate over the set of keys for which events are available
                Iterator selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // Check what event is available and deal with it
                    if (key.isConnectable()) {
                        this.finishConnection(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            FastList queue = this.pendingData.get(socketChannel);

            // Write until there's not more data ...
            while (null != queue && !queue.isEmpty()) {
                ByteBuffer buf = (ByteBuffer) queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break;
                }
                queue.remove(0);
                //
                buf.clear();
            }

            if (null != queue && queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested
                // in writing on this socket. Switch back to waiting for
                // data.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data
        this.readBuffer.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            key.cancel();
            socketChannel.close();
            return;
        }

        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            key.channel().close();
            key.cancel();
            return;
        }

        // Handle the response
        this.handleResponse(socketChannel, this.readBuffer.array(), numRead);
    }


    public void send(byte[] data, RspHandler handler) throws IOException {
        // Register the response handler
        this.rspHandlers.put(channel, handler);

        // And queue the data we want written
        synchronized (this.pendingData) {
            FastList queue = this.pendingData.get(channel);
            if (queue == null) {
                queue = new FastList(ByteBuffer.class,MqConfig.clinet_send_max);
                this.pendingData.put(channel, queue);
            }
            ByteBuffer buffer = MqConfig.mq_buffer_pool.allocate().getResource();// 分配外部 direct buffer
            buffer.put(data);
            buffer.position(0);
            buffer.limit(data.length);
            queue.add(buffer);
        }

        // Finally, wake up our selecting thread so it can make the required changes
        this.selector.wakeup();
    }

    private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
        // Make a correctly sized copy of the data before handing it
        // to the client
        byte[] rspData = new byte[numRead];
        System.arraycopy(data, 0, rspData, 0, numRead);

        // Look up the handler for this channel
        RspHandler handler = (RspHandler) this.rspHandlers.get(socketChannel);

        // And pass the response to it
        if (handler.handleResponse(rspData)) {
            // The handler has seen enough, close the connection
            socketChannel.close();
            socketChannel.keyFor(this.selector).cancel();
        }
    }


    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Finish the connection. If the connection operation failed
        // this will raise an IOException.
        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            // Cancel the channel's registration with our selector
            System.out.println(e);
            key.cancel();
            return;
        }

        // Register an interest in writing on this channel
        key.interestOps(SelectionKey.OP_WRITE);
    }


    public <T> byte[] buildMessage(T data) {
        Message.Key mKey = new Message.Key(ID.ONLY.id(), "hello", Message.SPREAD.TOPIC);
        String node = IOUtils.getLocalAddress(channel);
        mKey.setSendNode(node);
        Message message = Message.ofDef(mKey, data);
        return serializer.toByte(message);
    }

    public static class ChangeRequest {
        public static final int REGISTER = 1;
        public static final int CHANGEOPS = 2;

        public SocketChannel socket;
        public int type;
        public int ops;

        public ChangeRequest(SocketChannel socket, int type, int ops) {
            this.socket = socket;
            this.type = type;
            this.ops = ops;
        }
    }

    public static class RspHandler {
        private byte[] rsp = null;

        public synchronized boolean handleResponse(byte[] rsp) {
            this.rsp = rsp;
            this.notify();
            return true;
        }

        public synchronized void waitForResponse() {
            while (this.rsp == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }

            System.out.println(new String(this.rsp));
        }
    }

    public static void main(String[] args) {
        try {
//            NioClient client = new NioClient(InetAddress.getByName("www.google.com"), 80);
//            client.send("GET / HTTP/1.0\r\n\r\n".getBytes(), handler);
            ExecutorService pool = Executors.newFixedThreadPool(MqConfig.connect_thread_pool_size);

            NioClient client = new NioClient(MqConfig.server_ip, MqConfig.port);
            Thread t = new Thread(client);
            t.setDaemon(true);
            pool.submit(t);

            RspHandler handler = new RspHandler();
            client.send(client.buildMessage("hello"), handler);
            handler.waitForResponse();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}