package com.artlongs.amq.core.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class AioServer<T> implements Runnable{
    private static final Logger LOGGER = LoggerFactory.getLogger(AioServer.class);
    /**
     * Server端服务配置。
     * <p>调用AioQuickServer的各setXX()方法，都是为了设置config的各配置项</p>
     */
    protected AioServerConfig<T> config = new AioServerConfig<>(true);
    /**
     * 读回调事件处理
     */
    protected Reader<T> reader = new Reader<>();
    /**
     * 写回调事件处理
     */
    protected Writer<T> writer = new Writer<>();
    private Function<AsynchronousSocketChannel, AioPipe<T>> aioPipeFunction;

    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousChannelGroup asynchronousChannelGroup;

    /**
     * 设置服务端启动必要参数配置
     *
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     */
    public AioServer(String host, int port, Protocol<T> protocol, AioProcessor<T> messageProcessor) {
        config.setHost(host);
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
    }

    @Override
    public void run() {
    }

    /**
     * 启动Server端的AIO服务
     *
     * @throws IOException
     */
    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            LOGGER.info(config.BANNER + "\r\n :: smart-socket ::\t(" + config.VERSION + ")");
        }
        start0(new Function<AsynchronousSocketChannel, AioPipe<T>>() {
            @Override
            public AioPipe<T> apply(AsynchronousSocketChannel channel) {
                return new AioPipe<T>(channel, config, reader, writer);
            }
        });
    }

    /**
     * 内部启动逻辑
     *
     * @throws IOException
     */
    protected final void start0(Function<AsynchronousSocketChannel, AioPipe<T>> aioPipeFunction) throws IOException {
        try {
            this.aioPipeFunction = aioPipeFunction;
            asynchronousChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(config.getThreadNum(), new ThreadFactory() {
                byte index = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "smart-socket:AIO-" + (++index));
                }
            });
            this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
            //set socket options
            if (config.getSocketOptions() != null) {
                for (SocketOption option :config.getSocketOptions()) {
                    this.serverSocketChannel.setOption(option,option.type());
                }
            }
            //bind host
            if (config.getHost() != null) {
                serverSocketChannel.bind(new InetSocketAddress(config.getHost(), config.getPort()), 1000);
            } else {
                serverSocketChannel.bind(new InetSocketAddress(config.getPort()), 1000);
            }

            serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
                @Override
                public void completed(final AsynchronousSocketChannel channel, AsynchronousServerSocketChannel serverSocketChannel) {
                    serverSocketChannel.accept(serverSocketChannel, this);
                    createSession(channel);
                }

                @Override
                public void failed(Throwable exc, AsynchronousServerSocketChannel serverSocketChannel) {
                    LOGGER.error("smart-socket server accept fail", exc);
                }
            });
        } catch (IOException e) {
            shutdown();
            throw e;
        }
        LOGGER.info("smart-socket server started on port {}", config.getPort());
        LOGGER.info("smart-socket server config is {}", config);
    }

    /**
     * 为每个新建立的连接创建AIOSession对象
     *
     * @param channel
     */
    private void createSession(AsynchronousSocketChannel channel) {
        //连接成功则构造AIOSession对象
        AioPipe<T> session = null;
        try {
            session = aioPipeFunction.apply(channel);
            session.initSession();
        } catch (Exception e1) {
            LOGGER.debug(e1.getMessage(), e1);
            if (session == null) {
                try {
                    channel.shutdownInput();
                } catch (IOException e) {
                    LOGGER.debug(e.getMessage(), e);
                }
                try {
                    channel.shutdownOutput();
                } catch (IOException e) {
                    LOGGER.debug(e.getMessage(), e);
                }
                try {
                    channel.close();
                } catch (IOException e) {
                    LOGGER.debug("close channel exception", e);
                }
            } else {
                session.close();
            }

        }
    }

    /**
     * 停止服务端
     */
    public final void shutdown() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        if (!asynchronousChannelGroup.isTerminated()) {
            try {
                asynchronousChannelGroup.shutdownNow();
            } catch (IOException e) {
                LOGGER.error("shutdown exception", e);
            }
        }
        try {
            asynchronousChannelGroup.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("shutdown exception", e);
        }
    }


    /**
     * 设置处理线程数量
     *
     * @param num 线程数
     */
    public final AioServer<T> setThreadNum(int num) {
        this.config.setThreadNum(num);
        return this;
    }


    /**
     * 设置输出队列缓冲区长度
     *
     * @param size 缓存队列长度
     */
    public final AioServer<T> setWriteQueueSize(int size) {
        this.config.setWriteQueueSize(size);
        return this;
    }

    /**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     */
    public final AioServer<T> setReadBufferSize(int size) {
        this.config.setDirctBufferSize(size);
        return this;
    }

    /**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     */
    public final AioServer<T> setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    /**
     * 设置Socket的TCP参数配置。
     * <p>
     * AIO客户端的有效可选范围为：<br/>
     * 2. StandardSocketOptions.SO_RCVBUF<br/>
     * 4. StandardSocketOptions.SO_REUSEADDR<br/>
     * </p>
     * @return
     */
    public final <V> AioServer<T> setOption(SocketOption options) {
        config.setSocketOptions(options);
        return this;
    }


}
