package com.artlongs.amq.core;

import com.artlongs.amq.tools.io.Pool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class MqConfig {
	/**
	 * 创建一个全局的 ByteBuffer Pool
	 */
	public static final Pool<ByteBuffer> mq_buffer_pool= Pool.MEDIUM_DIRECT;
	public enum IO{
		nio,aio;
	}
	public static Charset utf_8 = StandardCharsets.UTF_8;

	public static IO io = IO.aio;
	public static int port = 8888;
	public static String server_ip = "127.0.0.1";

	//读取数据的行等侍时长(秒)
	public static int read_wait_timeout = 3;
	//最大连接数
	public static int max_connection=2000;
	// 连接线程池大小
	public static int connect_thread_pool_size =10;
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
	public static int msg_not_acked_resend_max_times = 3;
	// 间隔x秒,发送失败的消息重发间隔
	public static int msg_falt_message_resend_period = 60;
	//发送失败的消息重发的最大次数
	public static int msg_falt_message_resend_max_times = 10;

	// cache map size
	public static int mq_cache_map_sizes = 512;

	// MAPDB 数据库文件
	public static final String mapdb_file_path = "/volumes/work/mapdb/";

}