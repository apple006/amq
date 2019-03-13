package com.artlongs.amq.http;

import com.artlongs.amq.tools.io.Pool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
*@author leeton
*2018年2月6日
*
*/
public class HttpServerConfig {
	/**
	 * 创建一个全局的 ByteBuffer Pool
	 */
	public static final Pool<ByteBuffer> bufferPool= Pool.MEDIUM_DIRECT;

	public static Charset charsets = StandardCharsets.UTF_8;
	public static int port = 8080;
	public static String ip = "0.0.0.0";
	public static int maxConnection = 1000;
	public static int maxConcurrent = 1000;
	public static int readWait = 3;
	public static int requestWait = 300;
	public static int threadPoolSize=20;
	public static int connectTimeout = 3;

}