package com.artlongs.amq.core.store;

import com.artlongs.amq.core.Message;
import com.artlongs.amq.core.MqConfig;
import com.artlongs.amq.core.Subscribe;
import com.artlongs.amq.serializer.ISerializer;
import com.artlongs.amq.tools.ID;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Func :
 *
 * @author: leeton on 2019/2/18.
 */
public enum Store implements IStore {
    INST;
    ISerializer serializer = ISerializer.Serializer.INST.of();

    private HashMap<String, DB> db = new HashMap<>();
    // MQ 收到的所有数据
    private BTreeMap<String, byte[]> all_data = markMap(IStore.mq_all_data, Serializer.BYTE_ARRAY);
    // 需要重发的 MQ 数据
    private BTreeMap<String, byte[]> need_retry = markMap(IStore.mq_need_retry, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_subscribe = markMap(IStore.mq_subscribe, Serializer.BYTE_ARRAY);
    private BTreeMap<String, byte[]> mq_common_publish = markMap(IStore.mq_common_publish, Serializer.BYTE_ARRAY);

    private final static String DEF_TREEMAP_NAME = "ampdata";
    private static Map<Integer, List> filterListCache = new ConcurrentHashMap<>();

    @Override
    public DB markDb(String dbName) {
        DB _db = DBMaker.fileDB(MqConfig.inst.mq_db_store_file_path + dbName)
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

    private DB getDB(String dbName) {
        return db.get(dbName);
    }


    @Override
    public <T> boolean save(String dbName, String key, T obj) {
        if (S.empty(dbName)) return false;
        if (S.empty(key)) return false;
        getMapBy(dbName).putIfAbsent(key, serializer.toByte(obj));
        getDB(dbName).commit();
        removeFilterListCache(dbName,getTopic(obj));
        return true;
    }

    @Override
    public <T> T get(String dbName, String key, Class<T> tClass) {
        byte[] bytes = (byte[]) getMapBy(dbName).get(key);
        if (bytes != null) {
            T obj = serializer.getObj(bytes, tClass);
            return obj;
        }
        return null;
    }

    @Override
    public <T> List<T> getAll(String dbName, Class<T> tClass) {
        List<T> list = C.newList();
        for (Object o : getMapBy(dbName).values()) {
            list.add(serializer.getObj((byte[]) o, tClass));
        }
        return list;
    }

    public <T> List<T> list(String dbName, int pageNumber, int pageSize, Class<T> tClass) {
        List<T> result = C.newList();
        List<T> allList = getAll(dbName, tClass);
        // filter of page
        int total = allList.size();
        int first = Page.first(pageNumber, pageSize);
        int limit = Page.limit(total, pageNumber, pageSize);
        Iterator<T> iter = allList.iterator();
        for (int i = first; i < limit; i++) {
            result.add(iter.next());
        }
        return result;
    }

    public <T> Page<T> getPage(String dbName, Condition<T> topicFilter, Condition<T> timeFilter, Page page, Class<T> tClass) {
        List<T> result = C.newList();
        List<T> filteredList = C.newList();
        // filter of condition
        filteredList = filterByCondition(dbName, topicFilter, timeFilter, tClass);
        // filter of page
        filterByPage(page, filteredList, result);
        page.setItems(result);
        return page;
    }

    private <T> void filterByPage(Page page, List<T> oldList, List<T> result) {
        int total = oldList.size();
        page.setTotal(total);
        int first = page.first();
        int limit = page.limit();
        Iterator<T> iter = oldList.iterator();
        for (int i = first; i < limit; i++) {
            result.add(iter.next());
        }
    }

    private <T> List<T> filterByCondition(String dbName,Condition<T> topicFilter, Condition<T> timeFilter, Class<T> tClass) {
        List<T> filteredList = C.newList();
        int filterKey = Condition.hashCondition(dbName, topicFilter, timeFilter);
        filteredList = filterListCache.get(filterKey); // 从缓存里找一下,看相同的条件之前是不是已经查询过.
        if (C.isEmpty(filteredList)) { //
            List<T> allList = getAll(dbName, tClass);
            // filter of condition ...
            if (timeFilter != null) {
                filteredList = allList.stream().filter(topicFilter.getPredicate()).filter(timeFilter.getPredicate()).collect(Collectors.toList());
            } else {
                filteredList = allList.stream().filter(topicFilter.getPredicate()).collect(Collectors.toList());
            }
            filterListCache.putIfAbsent(filterKey, filteredList);
        }
        return filteredList;
    }

    /**
     * 删除缓存的结果集
     * @param dbName
     * @param topic
     */
    private void removeFilterListCache(String dbName, String topic) {
        List<Integer> keyList = Condition.hashMapOfTopic.get(dbName + topic);
        if (C.notEmpty(keyList)) {
            for (Integer key : keyList) {
                filterListCache.remove(key);
            }
        }
    }




    @Override
    public void remove(String dbName, String key) {
        getMapBy(dbName).remove(key);
        getDB(dbName).commit();
    }

    @Override
    public <T> List<T> find(String dbName, String topic, Class<T> tClass) {
        BTreeMap map = getMapBy(dbName);
        List<T> list = C.newList();
        for (Object o : map.values()) {
            T obj = serializer.getObj((byte[]) o, tClass);
            if (obj instanceof Message) {
                if (((Message) obj).getK().getTopic().startsWith(topic)) list.add(obj);
            }
            if (obj instanceof Subscribe) {
                if (((Subscribe) obj).getTopic().startsWith(topic)) list.add(obj);
            }
        }
        return list;
    }

    private String getTopic(Object obj) {
        if (obj instanceof Message) {
            return ((Message) obj).getK().getTopic();
        }
        if (obj instanceof Subscribe) {
            return ((Subscribe) obj).getTopic();
        }
        return "";
    }



    public static void main(String[] args) {
        Message msg = Message.ofDef(new Message.Key(ID.ONLY.id(), "hello"), "hello,world!");
        Store.INST.save(IStore.mq_all_data, msg.getK().getId(), msg);
        System.err.println(Store.INST.<Message>get(IStore.mq_all_data, msg.getK().getId(), Message.class));
        System.err.println(Store.INST.<Message>getAll(IStore.mq_all_data, Message.class));
        System.err.println(Store.INST.<Message>find(IStore.mq_all_data, msg.getK().getTopic(), Message.class));

    }


}
