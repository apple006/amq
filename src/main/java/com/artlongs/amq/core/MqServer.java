package com.artlongs.amq.core;

import java.nio.channels.Channel;
import java.nio.channels.NetworkChannel;

/**
 * Func :
 * Created by leeton on 2018/12/14.
 */
public interface MqServer extends Channel,Runnable {

    void start();
    void shutdown();
    void daemon(Runnable runnable);
    void accept();
    void assignJob();
    //
    MqConfig getConfig();
    NetworkChannel getServerChannel();


}
