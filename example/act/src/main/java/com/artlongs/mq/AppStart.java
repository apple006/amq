package com.artlongs.mq;

import act.Act;

/**
 * Func :
 *
 * @author: leeton on 2019/4/1.
 */
public class AppStart {


    /**
     * 启动项目前.请先运行MQ 服务器: {@link com.artlongs.amq.core.AioMqServer}
     * @param args
     * @throws Exception
     */
      public static void main(String[] args) throws Exception {
        Act.start("AMQ","com.artlongs.mq");
    }
}
