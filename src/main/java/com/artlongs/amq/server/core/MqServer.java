package com.artlongs.amq.server.core;

import java.nio.channels.Channel;
import java.nio.channels.NetworkChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Func :
 * Created by leeton on 2018/12/14.
 */
public interface MqServer extends Channel,Runnable {

    /**
     * 客户端socket map
     *  key: (ip+":"+port)
     */
    Map<String, NetworkChannel> clientSocketMap = new HashMap<>();
    void regClient(NetworkChannel channel);
    //
    void start();
    void shutdown();
    void daemon(Runnable runnable);
    void accept();
    void assignJob();
    //
    MqConfig getConfig();
    NetworkChannel getServerChannel();


}
