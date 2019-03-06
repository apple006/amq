package com.artlongs.amq.core;

import com.artlongs.amq.tools.io.Pool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class MqConfig {
	//========================== IO ITME =====================================
	/**
	 * 创建一个全局的 ByteBuffer Pool
	 */
	public static final Pool<ByteBuffer> mq_buffer_pool= Pool.MEDIUM_DIRECT;
	public static Charset utf_8 = StandardCharsets.UTF_8;
	public static String host = "127.0.0.1";
	public static int port = 8888;
	//========================== MQ ITME =====================================
	//读取数据的行等侍时长(秒)
	public static int read_wait_timeout = 3;
	// 客户端的连接线程池大小
	public static int client_connect_thread_pool_size =2000;
	// 服务端的连接线程池大小
	public static int server_connect_thread_pool_size =2000;
	// 工作线程池大小
	public static int worker_thread_pool_size =2000;
	// 工作线程最大时长(秒)
	public static long worker_keepalive_second =30* 60;
	// socket 连接超时(秒)
	public static int connect_timeout = 3;
	public static int clinet_send_max = 30;
	// 间隔x秒,重发消息如果消息没有确认收到
	public static int msg_not_acked_resend_period = 10;
	// 消息未确认的消息重发的最大次数
	public static int msg_not_acked_resend_max_times = 0;
	// 间隔x秒,发送失败的消息重发间隔
	public static int msg_falt_message_resend_period = 60;
	//发送失败的消息重发的最大次数
	public static int msg_falt_message_resend_max_times = 0;

	// cache map size
	public static int mq_cache_map_sizes = 1024;

	// 自动确认收到消息
	public static boolean mq_auto_acked = true;

	// 保存所有的消息(持久化)
	public static boolean store_all_message_to_db = false;

	//========================== DB ITME =====================================
	// MAPDB 数据库文件
	public static final String mapdb_file_path = "/volumes/work/mapdb/";

}