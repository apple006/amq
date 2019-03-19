package com.artlongs.amq.core.store;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.serializer.FastJsonSerializer;
import com.artlongs.amq.tools.ID;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Func :
 *
 * @author: leeton on 2019/2/18.
 */
public enum Store implements IStore {
    INST;
    private FastJsonSerializer serializer = new FastJsonSerializer();

    private HashMap<String, DB> db = new HashMap<>();
    // MQ 收到的所有数据
    private BTreeMap<String, byte[]> all_data = markMap(IStore.mq_all_data, Serializer.BYTE_ARRAY);
    // 需要重发的 MQ 数据
    private BTreeMap<String, byte[]> need_retry = markMap(IStore.mq_need_retry, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_subscribe = markMap(IStore.mq_subscribe, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_common_publish = markMap(IStore.mq_common_publish, Serializer.BYTE_ARRAY);

    private final static String DEF_TREEMAP_NAME = "ampdata";

    @Override
    public DB markDb(String dbName) {
        DB _db = DBMaker.fileDB(MqConfig.mapdb_file_path + dbName)
                .fileMmapEnableIfSupported()
                .fileMmapPreclearDisable()
                .allocateIncrement(1024)
                .cleanerHackEnable()
                .closeOnJvmShutdown()
                .transactionEnable()
                .concurrencyScale(128)
                .make();

        db.put(dbName, _db);
        return _db;
    }

    @Override
    public BTreeMap markMap(String dbName, GroupSerializer seriaType) {
        BTreeMap<String, byte[]> myMap = markDb(dbName).treeMap(DEF_TREEMAP_NAME)
                .keySerializer(Serializer.STRING)
                .valueSerializer(seriaType)
                .valuesOutsideNodesEnable()
                .createOrOpen();

        return myMap;
    }

    @Override
    public BTreeMap getMapBy(String dbName) {
        BTreeMap map = getDB(dbName).treeMap(DEF_TREEMAP_NAME).open();
        return map;
    }

    private DB getDB(String dbName){
        return db.get(dbName);
    }


    @Override
    public <T> boolean save(String dbName, String key, T obj) {
        if(S.empty(dbName)) return false;
        if(S.empty(key)) return false;
        getMapBy(dbName).putIfAbsent(key, serializer.toByte(obj));
        getDB(dbName).commit();
        return true;
    }

    @Override
    public <T> T get(String dbName, String key,Class<?> tClass) {
        byte[] bytes = (byte[]) getMapBy(dbName).get(key);
        if (bytes != null) {
            T obj = serializer.getObj(bytes,tClass);
            return obj;
        }
        return null;
    }

    @Override
    public <T> List<T> getAll(String dbName,Class<?> tClass) {
        List<T> list = C.newList();
        for (Object o : getMapBy(dbName).values()) {
            list.add(serializer.getObj((byte[]) o,tClass));
        }
        return list;
    }

    public <T> List<T> list(String dbName,int pageNumber,int pageSize,Class<?> tClass) {
        List<T> result = C.newList();
        BTreeMap map = getMapBy(dbName);
        Collection list = map.values();
        int total = map.getSize();
        int first = getFirst(pageNumber, pageSize);
        int limit = limit(total, pageNumber, pageSize);
        Iterator iter = list.iterator();
        for (int i = first; i < limit; i++) {
            result.add((T)((List) list).get(i));
        }
        return result;
    }

    private int getFirst(int pageNumber,int pageSize) {
        pageNumber = pageNumber < 0 ? 1 : pageNumber;
        return (pageNumber - 1) * pageSize;
    }

    private long getTotalPages(int total,int pageSize) {
        if (total < 0) return 0;

        long count = total / pageSize;
        if (total % pageSize > 0) {
            count++;
        }
        return count;
    }

    private int limit(int total, int pageNumber, int pageSize) {
        if(pageNumber * pageSize > total){
            return (total % pageSize);
        }
        return pageSize;
    }

    @Override
    public void remove(String dbName, String key) {
        getMapBy(dbName).remove(key);
        getDB(dbName).commit();
    }

    @Override
    public <T> List<T> find(String dbName, String topic,Class<?> tClass) {
        BTreeMap map = getMapBy(dbName);
        List<T> list = C.newList();
        for (Object o : map.values()) {
            T obj = serializer.getObj((byte[])o,tClass);
            if (obj instanceof Message) {
                if(((Message)obj).getK().getTopic().startsWith(topic)) list.add(obj);
            }
            if (obj instanceof Subscribe) {
                if(((Subscribe)obj).getTopic().startsWith(topic)) list.add(obj);
            }
        }
        return list;
    }

    public static void main(String[] args) {
        Message msg = Message.ofDef(new Message.Key(ID.ONLY.id(), "hello"), "hello,world!");
        Store.INST.save(IStore.mq_all_data,msg.getK().getId(), msg);
        System.err.println(Store.INST.<Message>get(IStore.mq_all_data,msg.getK().getId(), Message.class));
        System.err.println(Store.INST.<Message>getAll(IStore.mq_all_data, Message.class));
        System.err.println(Store.INST.<Message>find(IStore.mq_all_data,msg.getK().getTopic(),Message.class));

    }


}
