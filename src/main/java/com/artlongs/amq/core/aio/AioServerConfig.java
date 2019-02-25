package com.artlongs.amq.core.aio;

import java.net.SocketOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public class AioServerConfig<T> {
    private boolean isServer;
    public static final String BANNER = "\n" +
            "                               _                           _             _   \n" +
            "                              ( )_                        ( )           ( )_ \n" +
            "  ___   ___ ___     _ _  _ __ | ,_)     ___    _      ___ | |/')    __  | ,_)\n" +
            "/',__)/' _ ` _ `\\ /'_` )( '__)| |     /',__) /'_`\\  /'___)| , <   /'__`\\| |  \n" +
            "\\__, \\| ( ) ( ) |( (_| || |   | |_    \\__, \\( (_) )( (___ | |\\`\\ (  ___/| |_ \n" +
            "(____/(_) (_) (_)`\\__,_)(_)   `\\__)   (____/`\\___/'`\\____)(_) (_)`\\____)`\\__)";

    public static final String VERSION = "v0.0.1";

    private List<SocketOption> socketOptions = new ArrayList<>();

    public String host ;
    public int port;

    public static List<Plugin> plugins = Plugin.plugins;
    private AioProcessor<T> processor;
    private Protocol<T> protocol;
    private Monitor monitor;


    /**
     * 消息队列缓存大小
     */
    private int queueSize = 512;

    /**
     * 消息体缓存大小,字节
     */
    private int dirctBufferSize = 512;

    /**
     * 服务器处理线程数
     */
    private int threadNum = Runtime.getRuntime().availableProcessors() + 1;
    private float limitRate = 0.9f;
    private float releaseRate = 0.6f;
    /**
     * 流控指标线
     */
    private int flowLimitLine = (int) (queueSize * limitRate);
    /**
     * 释放流控指标线
     */
    private int releaseLine = (int) (queueSize * releaseRate);
    /**
     * 是否启用控制台banner
     */
    private boolean bannerEnabled = true;

    public AioServerConfig(boolean isServer) {
        this.isServer = isServer;
    }

    public void setWriteQueueSize(int queueSize) {
        this.queueSize = queueSize;
        flowLimitLine = (int) (queueSize * limitRate);
        releaseLine = (int) (queueSize * releaseRate);
    }


    public void setProtocol(Protocol<T> protocol) {
        this.protocol = protocol;
    }


    public final void setProcessor(AioProcessor<T> processor) {
        this.processor = processor;
        if (processor instanceof Monitor) {
            this.monitor = (Monitor<T>) processor;
        }
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getDirctBufferSize() {
        return dirctBufferSize;
    }

    public AioProcessor<T> getProcessor() {
        return processor;
    }

    public Protocol<T> getProtocol() {
        return protocol;
    }

    public int getFlowLimitLine() {
        return flowLimitLine;
    }

    public int getReleaseLine() {
        return releaseLine;
    }

    public boolean isServer() {
        return isServer;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public String getHost() {
        return host;
    }

    public AioServerConfig<T> setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public AioServerConfig<T> setPort(int port) {
        this.port = port;
        return this;
    }
    public boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public List<SocketOption> getSocketOptions() {
        return socketOptions;
    }

    public AioServerConfig<T> setThreadNum(int threadNum) {
        this.threadNum = threadNum;
        return this;
    }

    public AioServerConfig<T> setDirctBufferSize(int dirctBufferSize) {
        this.dirctBufferSize = dirctBufferSize;
        return this;
    }

    public AioServerConfig<T> setBannerEnabled(boolean bannerEnabled) {
        this.bannerEnabled = bannerEnabled;
        return this;
    }

    public void setSocketOptions(SocketOption option) {
        this.socketOptions.add(option);
    }


}
