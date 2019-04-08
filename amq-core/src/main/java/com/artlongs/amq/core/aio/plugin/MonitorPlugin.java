package com.artlongs.amq.core.aio.plugin;

import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.core.aio.State;
import com.artlongs.amq.tools.QuickTimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器运行状态监控插件
 *
 * @author 三刀
 * @version V1.0 , 2018/8/19
 */
public final class MonitorPlugin<T> extends QuickTimerTask implements Monitor<T>,Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MonitorPlugin.class);

    public static Dashboard dashboard = new Dashboard(); // 仪表盘,只为显示用
    private Dashboard curBashboard = null;
    //
    public MonitorPlugin() {
        this.curBashboard = new Dashboard();
    }

    @Override
    protected long getDelay() {
        return getPeriod();
    }

    @Override
    protected long getPeriod() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public boolean preProcess(AioPipe pipe, Object o) {
        curBashboard.processMsgNum.incrementAndGet();
        curBashboard.totalProcessMsgNum.incrementAndGet();
        return true;
    }

    @Override
    public void stateEvent(State State, AioPipe pipe, Throwable throwable) {
        switch (State) {
            case PROCESS_EXCEPTION:
                curBashboard.processFailNum.incrementAndGet();
                break;
            case NEW_PIPE:
                curBashboard.newConnect.incrementAndGet();
                break;
            case PIPE_CLOSED:
                curBashboard.disConnect.incrementAndGet();
                break;
        }
    }

    @Override
    public void read(AioPipe<T> pipe, int readSize) {
        //出现result为0,说明代码存在问题
        if (readSize == 0) {
            logger.error("readSize is 0");
        }
        curBashboard.inFlow.addAndGet(readSize);
    }

    @Override
    public void write(AioPipe<T> session, int writeSize) {
        curBashboard.outFlow.addAndGet(writeSize);
    }

    @Override
    public void run() {
        long curInFlow = curBashboard.inFlow.getAndSet(0);
        long curOutFlow = curBashboard.outFlow.getAndSet(0);
        long curDiscardNum = curBashboard.processFailNum.getAndSet(0);
        long curProcessMsgNum = curBashboard.processMsgNum.getAndSet(0);
        int connectCount = curBashboard.newConnect.getAndSet(0);
        int disConnectCount = curBashboard.disConnect.getAndSet(0);
        logger.debug("\r\n-----这一分钟发生了什么----"
                + "\r\n流入流量:\t" + curInFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n流出流量:\t" + curOutFlow * 1.0 / (1024 * 1024) + "(MB)"
                + "\r\n处理失败消息数:\t" + curDiscardNum
                + "\r\n已处理消息量:\t" + curProcessMsgNum
                + "\r\n已处理消息总量:\t" + curBashboard.totalProcessMsgNum.get()
                + "\r\n新建连接数:\t" + connectCount
                + "\r\n断开连接数:\t" + disConnectCount
                + "\r\n在线连接数:\t" + curBashboard.onlineCount.addAndGet(connectCount - disConnectCount)
                + "\r\n总连接次数:\t" + curBashboard.totalConnect.addAndGet(connectCount));

        dashboard = this.curBashboard;
    }


    /**
     * 监控仪表盘
     */
    public static class Dashboard{
        /** 当前周期内消息 流量监控*/
        private AtomicLong inFlow = new AtomicLong(0);

        /**当前周期内消息 流量监控*/
        private AtomicLong outFlow = new AtomicLong(0);

        /**当前周期内处理失败消息数*/
        private AtomicLong processFailNum = new AtomicLong(0);

        /**当前周期内处理消息数*/
        private AtomicLong processMsgNum = new AtomicLong(0);

        private AtomicLong totalProcessMsgNum = new AtomicLong(0);

        /**新建连接数*/
        private AtomicInteger newConnect = new AtomicInteger(0);

        /**断链数*/
        private AtomicInteger disConnect = new AtomicInteger(0);

        /**在线连接数*/
        private AtomicInteger onlineCount = new AtomicInteger(0);

        private AtomicInteger totalConnect = new AtomicInteger(0);

        //=========================================================
        public AtomicLong getInFlow() {
            return inFlow;
        }

        public Dashboard setInFlow(AtomicLong inFlow) {
            this.inFlow = inFlow;
            return this;
        }

        public AtomicLong getOutFlow() {
            return outFlow;
        }

        public Dashboard setOutFlow(AtomicLong outFlow) {
            this.outFlow = outFlow;
            return this;
        }

        public AtomicLong getProcessFailNum() {
            return processFailNum;
        }

        public Dashboard setProcessFailNum(AtomicLong processFailNum) {
            this.processFailNum = processFailNum;
            return this;
        }

        public AtomicLong getProcessMsgNum() {
            return processMsgNum;
        }

        public Dashboard setProcessMsgNum(AtomicLong processMsgNum) {
            this.processMsgNum = processMsgNum;
            return this;
        }

        public AtomicLong getTotalProcessMsgNum() {
            return totalProcessMsgNum;
        }

        public Dashboard setTotalProcessMsgNum(AtomicLong totalProcessMsgNum) {
            this.totalProcessMsgNum = totalProcessMsgNum;
            return this;
        }

        public AtomicInteger getNewConnect() {
            return newConnect;
        }

        public Dashboard setNewConnect(AtomicInteger newConnect) {
            this.newConnect = newConnect;
            return this;
        }

        public AtomicInteger getDisConnect() {
            return disConnect;
        }

        public Dashboard setDisConnect(AtomicInteger disConnect) {
            this.disConnect = disConnect;
            return this;
        }

        public AtomicInteger getOnlineCount() {
            return onlineCount;
        }

        public Dashboard setOnlineCount(AtomicInteger onlineCount) {
            this.onlineCount = onlineCount;
            return this;
        }

        public AtomicInteger getTotalConnect() {
            return totalConnect;
        }

        public Dashboard setTotalConnect(AtomicInteger totalConnect) {
            this.totalConnect = totalConnect;
            return this;
        }
    }

}
