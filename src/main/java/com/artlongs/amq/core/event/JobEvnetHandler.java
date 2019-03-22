package com.artlongs.amq.core.event;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.core.ProcessorImpl;
import com.artlongs.amq.core.aio.AioPipe;
import com.artlongs.amq.disruptor.EventHandler;
import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.tools.io.Buffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.runtime.Bytes;

import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/2/13.
 */
public class JobEvnetHandler implements EventHandler<JobEvent> {
    private static Logger logger = LoggerFactory.getLogger(JobEvnetHandler.class);
    ISerializer serializer = ISerializer.Serializer.INST.of();

    @Override
    public void onEvent(JobEvent event, long sequence, boolean endOfBatch) throws Exception {
        logger.debug("[S]执行消息任务的分派 ......");
//        ProcessorImpl.INST.publishJobToWorker(event.getMessage());
        Message message = decode(event.getByteBuffer());
        if (null != message) {
            logger.debug("[M] "+message);
            ProcessorImpl.INST.onMessage(event.getPipe(), message);
        }
    }

    private synchronized Message decode(ByteBuffer buffer) {
        try {
            return serializer.getObj(buffer,Message.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
