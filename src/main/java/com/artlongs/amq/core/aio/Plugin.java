package com.artlongs.amq.core.aio;

import java.util.ArrayList;
import java.util.List;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public interface Plugin<T> {
    List<Plugin> plugins = new ArrayList<>();
    void regPlgin(Plugin plugin);
    /**
     * 对请求消息进行预处理，并决策是否进行后续的MessageProcessor处理。
     * 若返回false，则当前消息将被忽略。
     * 若返回true，该消息会正常秩序MessageProcessor.process.
     * @param pipe
     * @param t
     * @return
     */
    boolean preProcess(AioPipe<T> pipe, T t);

}
