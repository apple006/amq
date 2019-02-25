package com.artlongs.amq.core.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Semaphore;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class AioPipe<T> {
    private static final Logger logger = LoggerFactory.getLogger(AioPipe.class);
    Semaphore writeSemaphore = new Semaphore(1);
    Semaphore readSemaphore = new Semaphore(1);

    /**
     * Session状态:已关闭
     */
    protected static final byte CLOSED = 1;
    /**
     * Session状态:关闭中
     */
    protected static final byte CLOSING = 2;
    /**
     * Session状态:正常
     */
    protected static final byte ENABLED = 3;
    private static final int MAX_WRITE_SIZE = 256 * 1024;

    /**
     * 底层通信channel对象
     */
    protected AsynchronousSocketChannel channel;
    /**
     * 读缓冲。
     * <p>大小取决于AioQuickClient/AioQuickServer设置的setReadBufferSize</p>
     */
    protected ByteBuffer readBuffer;
    /**
     * 写缓冲
     */
    protected ByteBuffer writeBuffer;

    protected byte status = ENABLED;
    /**
     * 附件对象
     */
    private Object attachment;
    /**
     * 是否流控,客户端写流控，服务端读流控
     */
    private boolean flowControl;
    /**
     * 响应消息缓存队列。
     */
    private FastBlockingQueue writeCacheQueue;
    private Reader<T> reader;
    private Writer<T> writer;
    private AioServerConfig<T> ioServerConfig;

    AioPipe(AsynchronousSocketChannel channel, AioServerConfig config, Reader<T> reader, Writer<T> writer) {
        this.channel = channel;
        this.reader = reader;
        this.writer = writer;
        this.writeCacheQueue = config.getQueueSize() > 0 ? new FastBlockingQueue(config.getQueueSize()) : null;
        this.ioServerConfig = config;
        //触发状态机
        config.getProcessor().stateEvent(this, State.NEW_PIPE, null);
        this.readBuffer = DirectBufferUtil.getTemporaryDirectBuffer(config.getDirctBufferSize());
    }

    /**
     * 初始化AioSession
     */
    void initSession() {
        readSemaphore.tryAcquire();
        continueRead();
    }

    /**
     * 内部方法：触发通道的读操作
     *
     * @param buffer
     */
    protected final void readFromChannel0(ByteBuffer buffer) {
        channel.read(buffer, this, reader);
    }

    /**
     * 内部方法：触发通道的写操作
     */
    protected final void writeToChannel0(ByteBuffer buffer) {
        channel.write(buffer, this, writer);
    }

    /**
     * 输出消息。
     * <p>必须实现{@link Protocol#encode(Object)}</p>方法
     *
     * @param t 待输出消息必须为当前服务指定的泛型
     * @throws IOException
     */
    public final boolean write(T t){
        try {
            write(ioServerConfig.getProtocol().encode(t));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 将数据buffer输出至网络对端。
     * <p>
     * 若当前无待输出的数据，则立即输出buffer.
     * </p>
     * <p>
     * 若当前存在待数据数据，且无可用缓冲队列(writeCacheQueue)，则阻塞。
     * </p>
     * <p>
     * 若当前存在待输出数据，且缓冲队列存在可用空间，则将buffer存入writeCacheQueue。
     * </p>
     *
     * @param buffer
     * @throws IOException
     */
    public final void write(final ByteBuffer buffer) throws IOException {
        if (isInvalid()) {
            throw new IOException("pipe is " + (status == CLOSED ? "closed" : "invalid"));
        }
        if (!buffer.hasRemaining()) {
            throw new InvalidObjectException("buffer has no remaining");
        }
        if (ioServerConfig.getQueueSize() <= 0) {
            try {
                writeSemaphore.acquire();
                writeBuffer = buffer;
                continueWrite();
            } catch (InterruptedException e) {
                logger.error("acquire fail", e);
                Thread.currentThread().interrupt();
                throw new IOException(e.getMessage());
            }
            return;
        } else if ((writeSemaphore.tryAcquire())) {
            writeBuffer = buffer;
            continueWrite();
            return;
        }
        try {
            writeSemaphore.release();
            //正常读取
            int size = writeCacheQueue.put(buffer);
            if (size >= ioServerConfig.getFlowLimitLine() && ioServerConfig.isServer()) {
                flowControl = true;
                ioServerConfig.getProcessor().stateEvent(this, State.FLOW_LIMIT, null);
            }
        } catch (InterruptedException e) {
            logger.error("put buffer into cache fail", e);
            Thread.currentThread().interrupt();
        }
        if (writeSemaphore.tryAcquire()) {
            writeToChannel();
        }
    }


    /**
     * 触发AIO的写操作,
     * <p>需要调用控制同步</p>
     */
    void writeToChannel() {
        if (writeBuffer != null && writeBuffer.hasRemaining()) {
            continueWrite();
            return;
        }

        if (writeCacheQueue == null || writeCacheQueue.size() == 0) {
            if (writeBuffer != null && writeBuffer.isDirect()) {
                DirectBufferUtil.offerFirstTemporaryDirectBuffer(writeBuffer);
            }
            writeBuffer = null;
            writeSemaphore.release();
            //此时可能是Closing或Closed状态
            if (isInvalid()) {
                close();
            }
            //也许此时有新的消息通过write方法添加到writeCacheQueue中
            else if (writeCacheQueue != null && writeCacheQueue.size() > 0 && writeSemaphore.tryAcquire()) {
                writeToChannel();
            }
            return;
        }
        int totalSize = writeCacheQueue.expectRemaining(MAX_WRITE_SIZE);
        ByteBuffer headBuffer = writeCacheQueue.poll();
        if (headBuffer.remaining() == totalSize) {
            writeBuffer = headBuffer;
        } else {
            if (writeBuffer == null || totalSize > writeBuffer.capacity()) {
                if (writeBuffer != null && writeBuffer.isDirect()) {
                    DirectBufferUtil.offerFirstTemporaryDirectBuffer(writeBuffer);
                }
                writeBuffer = DirectBufferUtil.getTemporaryDirectBuffer(totalSize);
            } else {
                writeBuffer.clear().limit(totalSize);
            }
            writeBuffer.put(headBuffer);
            writeCacheQueue.pollInto(writeBuffer);
            writeBuffer.flip();
        }

        //如果存在流控并符合释放条件，则触发读操作
        //一定要放在continueWrite之前
        if (flowControl && writeCacheQueue.size() < ioServerConfig.getReleaseLine()) {
            ioServerConfig.getProcessor().stateEvent(this, State.RELEASE_FLOW_LIMIT, null);
            flowControl = false;
            readFromChannel(false);
        }
        continueWrite();

    }

    /**
     * 触发通道的读操作，当发现存在严重消息积压时,会触发流控
     */
    void readFromChannel(boolean eof) {
        //处于流控状态
        if (flowControl || !readSemaphore.tryAcquire()) {
            return;
        }
        readBuffer.flip();


        while (readBuffer.hasRemaining()) {
            T dataEntry = null;
            try {
                dataEntry = ioServerConfig.getProtocol().decode(readBuffer);
            } catch (Exception e) {
                ioServerConfig.getProcessor().stateEvent(this, State.DECODE_EXCEPTION, e);
                throw e;
            }
            if (dataEntry == null) {
                break;
            }

            //处理消息
            try {
                ioServerConfig.getProcessor().process(this, dataEntry);
            } catch (Exception e) {
                ioServerConfig.getProcessor().stateEvent(this, State.PROCESS_EXCEPTION, e);
            }
        }

        if (eof || status == CLOSING) {
            close(false);
            ioServerConfig.getProcessor().stateEvent(this, State.INPUT_SHUTDOWN, null);
            return;
        }
        if (status == CLOSED) {
            return;
        }

        //数据读取完毕
        if (readBuffer.remaining() == 0) {
            readBuffer.clear();
        } else if (readBuffer.position() > 0) {
            // 仅当发生数据读取时调用compact,减少内存拷贝
            readBuffer.compact();
        } else {
            readBuffer.position(readBuffer.limit());
            readBuffer.limit(readBuffer.capacity());
        }
        continueRead();
    }


    protected void continueRead() {
        readFromChannel0(readBuffer);
    }

    protected void continueWrite() {
        writeToChannel0(writeBuffer);
    }


    /**
     * 强制关闭当前AIOSession。
     * <p>若此时还存留待输出的数据，则会导致该部分数据丢失</p>
     */
    public final void close() {
        close(true);
    }

    /**
     * 是否立即关闭会话
     *
     * @param immediate true:立即关闭,false:响应消息发送完后关闭
     */
    public synchronized void close(boolean immediate) {
        //status == SESSION_STATUS_CLOSED说明close方法被重复调用
        if (status == CLOSED) {
            logger.warn("ignore, pipe:{} is closed:", getSessionID());
            writeSemaphore.release();
            return;
        }
        status = immediate ? CLOSED : CLOSING;
        if (immediate) {
            try {
                channel.shutdownInput();
            } catch (IOException e) {
                logger.debug(e.getMessage(), e);
            }
            try {
                channel.shutdownOutput();
            } catch (IOException e) {
                logger.debug(e.getMessage(), e);
            }
            try {
                channel.close();
            } catch (IOException e) {
                logger.debug("close pipe exception", e);
            }
            try {
                ioServerConfig.getProcessor().stateEvent(this, State.PIPE_CLOSED, null);
            } finally {
                writeSemaphore.release();
            }
            DirectBufferUtil.offerFirstTemporaryDirectBuffer(readBuffer);
            if (writeBuffer != null && writeBuffer.isDirect()) {
                DirectBufferUtil.offerFirstTemporaryDirectBuffer(writeBuffer);
            }
        } else if ((writeBuffer == null || !writeBuffer.hasRemaining()) && (writeCacheQueue == null || writeCacheQueue.size() == 0) && writeSemaphore.tryAcquire()) {
            close(true);
        } else {
            ioServerConfig.getProcessor().stateEvent(this, State.PIPE_CLOSING, null);
        }
    }

    /**
     * 获取当前Session的唯一标识
     */
    public final String getSessionID() {
        return "aioSession-" + hashCode();
    }

    /**
     * 当前会话是否已失效
     */
    public final boolean isInvalid() {
        return status != ENABLED;
    }


    /**
     * 获取附件对象
     *
     * @return
     */
    public final <T> T getAttachment() {
        return (T) attachment;
    }

    /**
     * 存放附件，支持任意类型
     */
    public final <T> void setAttachment(T attachment) {
        this.attachment = attachment;
    }

    /**
     * @see AsynchronousSocketChannel#getLocalAddress()
     */
    public final InetSocketAddress getLocalAddress() {
        try {
            assertChannel();
            return (InetSocketAddress) channel.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @see AsynchronousSocketChannel#getRemoteAddress()
     */
    public final InetSocketAddress getRemoteAddress(){
        try {
            assertChannel();
            return (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void assertChannel() throws IOException {
        if (status == CLOSED || channel == null) {
            throw new IOException("pipe is closed");
        }
    }

    AioServerConfig<T> getServerConfig() {
        return this.ioServerConfig;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }
}
