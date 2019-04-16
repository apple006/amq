package com.artlongs.amq.core.aio;

import com.artlongs.amq.disruptor.RingBuffer;
import com.artlongs.amq.tools.FastBlockingQueue;
import com.artlongs.amq.tools.RingBufferQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * Func : 对 Aio channel 的包装
 * 不喜欢 session 这个单词,按 nProcess 项目把 channel 叫 pipe(通道),感觉更好理解
 * @author: leeton on 2019/2/22.
 */
public class AioPipe<T> implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AioPipe.class);

    private Integer id;
    Semaphore writeSemaphore = new Semaphore(1);

    /**
     * Session状态:正常
     */
    protected static final byte ENABLED = 1;
    /**
     * Session状态:已关闭
     */
    protected static final byte CLOSED = 0;
    /**
     * Session状态:关闭中
     */
    protected static final byte CLOSING = -1;

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
     * 是否流控,客户端写流控，服务端读流控
     */
    private boolean flowControl;
    /**
     * 响应消息缓存队列。
     */
    private RingBufferQueue<ByteBuffer> writeCacheQueue;
    private Reader<T> reader;
    private Writer<T> writer;
    private AioServerConfig<T> ioServerConfig;

    /**
     * 附件对象(通常传输文件才用得到)
     */
    private Object attachment;
    

    public AioPipe() {
    }

    public AioPipe(AsynchronousSocketChannel channel, AioServerConfig config, Reader<T> reader, Writer<T> writer) {
        this.channel = channel;
        this.reader = reader;
        this.writer = writer;
        this.ioServerConfig = config;
        this.writeCacheQueue = config.getQueueSize() > 0 ? new RingBufferQueue(config.getQueueSize()) : null;
        //初始化状态机
        config.getProcessor().stateEvent(this, State.NEW_PIPE, null);
        this.readBuffer = DirectBufferUtil.allocateDirectBuffer(config.getDirctBufferSize());
        this.id = hashCode();
    }

    /**
     * 初始化AioSession
     */
    void initSession() {
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

    private void clear(ByteBuffer buffer) {
        buffer.clear();
    }


    /**
     * 输出数据。
     * <p>必须实现{@link Protocol#encode(Object)}</p>方法
     *
     * @param t 待输出数据必须为当前服务指定的泛型
     * @throws IOException
     */
    public final boolean write(T t) {
        return writeBuffer(ioServerConfig.getProtocol().encode(t));
    }

    public final boolean writeBuffer(ByteBuffer buffer) {
        try {
            if (writeSemaphore.tryAcquire()) {
                boolean succ = writeToCacheQueue(buffer);
                if (succ) {
                    writeToChannel();
                }
                return succ;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 将数据写入缓冲队列。
     *
     * @param buffer
     */
    private final boolean writeToCacheQueue(final ByteBuffer buffer) {
        if (isInvalid()) {
            logger.error(" pipe({}) is " + (status == CLOSED ? "closed" : "invalid"), getId());
            return false;
        }
        if (!buffer.hasRemaining()) {
            logger.error("buffer has no remaining");
            return false;
        }

        // buffer 写入到队列缓存
        int size = writeCacheQueue.put(buffer);
        if (size >= ioServerConfig.getFlowLimitLine() && ioServerConfig.isServer()) {
            flowControl = true;
            ioServerConfig.getProcessor().stateEvent(this, State.FLOW_LIMIT, null);
        }
        return size>=0;
    }


    /**
     * 触发AIO的写操作,将数据 buffer 输出至网络对端。
     *
     * <p>需要调用控制同步</p>
     * <p>若当前 writeBuffer 存在数据，则立即输出buffer.</p>
     * <p>若缓冲队列(writeCacheQueue)为空,说明数据已传输完毕,则清空 writeBuffer 并解锁</p>
     * <p>如果存在流控并符合释放条件，则触发读操作</p>
     */
    void writeToChannel() {
        if (writeBuffer != null && writeBuffer.hasRemaining()) {
            continueWrite();
            return;
        }

        if (writeCacheQueue == null || writeCacheQueue.size() == 0) {
            clearWriteBufferAndUnLock();
            //此时可能是Closing或Closed状态
            if (isInvalid()) {
                close();
            }
            return;
        }

        //如果存在流控并符合释放条件，则触发读操作
        //一定要放在continueWrite之前
        if (flowControl && writeCacheQueue.size() < ioServerConfig.getReleaseLine()) {
            ioServerConfig.getProcessor().stateEvent(this, State.RELEASE_FLOW_LIMIT, null);
            flowControl = false;
            readFromChannel(false);
        }

        // 从队列读取 buffer
        if (writeCacheQueue.size()>0) {
            if (writeBuffer == null) {
                writeBuffer = DirectBufferUtil.allocateDirectBuffer(ioServerConfig.getDirctBufferSize());
            }else {
                writeBuffer.clear();
            }
            ByteBuffer headBuffer = writeCacheQueue.pop();
            writeBuffer.put(headBuffer);
            writeBuffer.flip();
        }
        continueWrite();
    }


    /**
     * 触发通道的读操作，当发现存在严重消息积压时,会触发流控
     */
    void readFromChannel(boolean eof) {
        //处于流控状态
        if (flowControl ) {
            return;
        }
/*        if (readCacheQueue.isEmpty()) {
            return;
        }
        readBuffer= readCacheQueue.pop();*/
        readBuffer.flip();
        while (readBuffer.hasRemaining()) {
            T dataEntry = null;
            try {
                dataEntry = ioServerConfig.getProtocol().decode(readBuffer);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
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
                logger.error(e.getMessage(), e);
                ioServerConfig.getProcessor().stateEvent(this, State.PROCESS_EXCEPTION, e);
            }
        }

        if (eof || status == CLOSING) {
            close(false);
//            ioServerConfig.getProcessor().stateEvent(this, State.INPUT_SHUTDOWN, null);
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
     * 强制关闭当前AioPipe。
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
            logger.warn("ignore, pipe:{} is closed:", getId());
            writeSemaphore.release();
            return;
        }
        status = immediate ? CLOSED : CLOSING;
        if (immediate) {
            try {
                channel.shutdownInput();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            try {
                channel.shutdownOutput();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("close pipe exception", e);
            }
            try {
                ioServerConfig.getProcessor().stateEvent(this, State.PIPE_CLOSED, null);
            } finally {
                writeSemaphore.release();
            }
            DirectBufferUtil.freeFirstBuffer(readBuffer);
            if (writeBuffer != null && writeBuffer.isDirect()) {
                DirectBufferUtil.freeFirstBuffer(writeBuffer);
            }
        } else if ((writeBuffer == null || !writeBuffer.hasRemaining()) && (writeCacheQueue == null || writeCacheQueue.size() == 0) && writeSemaphore.tryAcquire()) {
            close(true);
        } else {
            ioServerConfig.getProcessor().stateEvent(this, State.PIPE_CLOSING, null);
        }
    }

    /**
     * 获取当前 pipe 的唯一标识
     */
    public final Integer getId() {
        return id;
    }

    /**
     * 当前会话是否已失效
     */
    public final boolean isInvalid() {
        return status != ENABLED;
    }

    /**
     * @see AsynchronousSocketChannel#getLocalAddress()
     */
    public final InetSocketAddress getLocalAddress() {
        try {
            return (InetSocketAddress) channel.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @see AsynchronousSocketChannel#getRemoteAddress()
     */
    public final InetSocketAddress getRemoteAddress() {
        try {
            return (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
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

    public Object getAttachment() {
        return attachment;
    }

    public AioPipe<T> setAttachment(Object attachment) {
        this.attachment = attachment;
        return this;
    }

    /**
     * 清空 WriteBuffer 并解锁
     */
    public void clearWriteBufferAndUnLock() {
        if (this.writeBuffer != null && this.writeBuffer.isDirect()) {
            DirectBufferUtil.freeFirstBuffer(writeBuffer);
        }
        this.writeBuffer.clear();
        this.writeBuffer = null;
        this.writeSemaphore.release();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AioPipe{");
        sb.append("id=").append(id);
        sb.append('}');
        return sb.toString();
    }
}
