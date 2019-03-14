package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.core.store.IStore;
import com.artlongs.amq.core.store.Store;
import com.artlongs.amq.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Func : 存储事件 Handler
 *
 * @author: leeton on 2019/2/15.
 */
public class StoreEventHandler implements WorkHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(StoreEventHandler.class);
    @Override
    public void onEvent(JobEvent event) throws Exception {
        logger.debug("[S]执行消息保存到硬盘 ......");
        if(event.isStoreAllMsg()){
            Message message = event.getMessage();
            Store.INST.save(IStore.mq_all_data, message.getK().getId(), message);
        }else {
            Message message = event.getMessage();
            if (message != null) {
                Store.INST.save(getDbNameByMsgType(message),message.getK().getId(), message);
            }
            Subscribe subscribe = event.getSubscribe();
            if (subscribe != null) {
                Store.INST.save(IStore.mq_subscribe,subscribe.getId(), subscribe);
            }
        }

    }

    private String getDbNameByMsgType(Message msg) {
        String dbName = "";
        switch (msg.getType()){
            case ACK:
                break;
            case SUBSCRIBE:
                return IStore.mq_subscribe;
            case PUBLISH:
                return IStore.mq_common_publish;
            case PUBLISH_JOB:
                return IStore.mq_common_publish;
            case ACCEPT_JOB:
                return IStore.mq_subscribe;

        }
        return dbName;
    }
}
