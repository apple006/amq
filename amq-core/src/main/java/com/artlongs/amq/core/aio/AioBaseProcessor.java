package com.artlongs.amq.core.aio;

import com.artlongs.amq.core.aio.plugin.Monitor;
import com.artlongs.amq.core.aio.plugin.MonitorPlugin;
import com.artlongs.amq.core.aio.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

/**
 * Func :
 *
 * @author: leeton on 2019/3/7.
 */
public abstract class AioBaseProcessor<T> implements AioProcessor<T> {

    private Set<Plugin> plugins = new HashSet<>();
    private Monitor monitor ;

    @Override
    public void process(AioPipe<T> pipe, T msg) {
        boolean flag = true;
        for (Plugin<T> plugin : plugins) {
            if (!plugin.preProcess(pipe, msg)) {
                flag = false;
            }
        }
        if (flag) {
            process0(pipe, msg);
        }
    }

    public abstract void process0(AioPipe<T> pipe, T msg);

    @Override
    public final void stateEvent(AioPipe<T> pipe, State state, Throwable throwable) {
        for (Plugin<T> plugin : plugins) {
            plugin.stateEvent(state, pipe, throwable);
        }
        stateEvent0(pipe, state, throwable);
    }

    public abstract void stateEvent0(AioPipe<T> pipe, State state, Throwable throwable);


    @Override
    public void addPlugin(Plugin plugin) {
        plugins.add(plugin);
        if(plugin instanceof MonitorPlugin){
            this.monitor = (Monitor) plugin;
        }
    }

    public Set<Plugin> getPlugins() {
        return plugins;
    }

    public Monitor getMonitor(){
        return monitor;
    }
}
