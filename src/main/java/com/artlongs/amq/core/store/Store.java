package com.artlongs.amq.core.store;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.serializer.FastJsonSerializer;
import com.artlongs.amq.tools.ID;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;

import java.util.Map;

/**
 * Func :
 *
 * @author: leeton on 2019/2/18.
 */
public enum Store {
    INST;

    // MQ 收到的所有数据
    private BTreeMap<String, byte[]> all_data = map("mq_all_data.db", Serializer.BYTE_ARRAY);
    // MQ 数据的创建时间
    private BTreeMap<String, Long> time = map("mq_time.db", Serializer.LONG);
    // 需要重发的 MQ 数据
    private BTreeMap<String, byte[]> need_retry = map("mq_need_retry.db", Serializer.BYTE_ARRAY);

    private FastJsonSerializer serializer = new FastJsonSerializer();

    private DB markDb(String dbName) {
        DB db = DBMaker.fileDB(MqConfig.mapdb_file_path + dbName)
                .fileMmapEnableIfSupported()
                .fileMmapPreclearDisable()
                .allocateIncrement(1024)
                .closeOnJvmShutdown()
                .transactionEnable()
                .cleanerHackEnable()
                .make();

        return db;
    }

    private BTreeMap map(String dbName, GroupSerializer seriaType) {
        BTreeMap<String, byte[]> myMap = markDb(dbName).treeMap("mqdata")
                .keySerializer(Serializer.STRING)
                .valueSerializer(seriaType)
                .createOrOpen();

        return myMap;
    }

    public boolean save(String key, Message message) {
        Store.INST.all_data.put(key, serializer.toByte(message));
        return true;
    }

    public Message get(String key) {
        return serializer.getObj(Store.INST.all_data.get(key));
    }

    public void remove(String key) {
        Store.INST.all_data.remove(key);
    }

    public boolean saveToRetryList(String key, Message message) {
        Store.INST.need_retry.put(key, serializer.toByte(message));
        return true;
    }

    public Message getOfRetryList(String key) {
        return serializer.getObj(Store.INST.need_retry.get(key));
    }

    public void removeOfRetryList(String key) {
        Store.INST.need_retry.remove(key);
    }

    public boolean uptime(String key, long time) {
        Store.INST.time.put(key, time);
        return true;
    }

    public long getUptime(String key) {
        return Store.INST.time.get(key);
    }

    public void removeOfUptime(String key) {
        Store.INST.time.remove(key);
    }

    public Map appendOfRetryMap(Map<String ,Message> targetMap) {
        for (byte[] bytes : need_retry.values()) {
            Message message = serializer.getObj(bytes);
            String key = message.getK().getId();
            if (null != targetMap.get(key)) {
                targetMap.put(key, message);
                need_retry.remove(key);
            }
        }
        return targetMap;
    }


    public static void main(String[] args) {
        Message msg = Message.ofDef(new Message.Key(ID.ONLY.id(), "quene", Message.SPREAD.TOPIC), "hello,world!");
        Store.INST.save("hello", msg);
        System.err.println(Store.INST.get("hello"));


    }


}
