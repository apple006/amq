package com.artlongs.amq.core.store;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.serializer.GroupSerializer;

import java.util.List;

/**
 * Func :
 *
 * @author: leeton on 2019/3/13.
 */
public interface IStore {

    /**
     * 所有的数据备份
     */
    String mq_all_data = "mq_all_data.db";

    /**
     * 需要重发的数据备份
     */
    String mq_need_retry = "mq_need_retry.db";
    /**
     * 订阅数据备份
     */
    String mq_subscribe = "mq_subscribe.db";

    String mq_common_publish = "mq_common_publish_message.db";

    /**
     * 创建数据库
     * @param dbName
     * @return
     */
    DB markDb(String dbName);

    /**
     * 创建数据库对应的 MAP
     * @param dbName
     * @param seriaType
     * @return
     */
    BTreeMap markMap(String dbName, GroupSerializer seriaType);

    BTreeMap getMapBy(String dbName);

    <T> boolean save(String dbName,String key, T obj);

    <T> T get(String dbName,String key,Class<T> tClass);

    <T> List<T> getAll(String dbName,Class<T> tClass);

    void remove(String dbName,String key);

    /**
     * 查询信息
     * @param dbName 数据库名
     * @param topic  主题
     * @param <T>    存储的对象类型
     * @return
     */
    <T> List<T> find(String dbName, String topic,Class<T> tClass);


}
