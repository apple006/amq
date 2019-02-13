package com.artlongs.amq.client;

import com.artlongs.amq.core.Message;

/**
 * Func :
 * Created by leeton on 2018/12/24.
 */
public interface Sender {
    Message.Stat send(Message message);
}
