package com.artlongs.amq.net.http;

import com.artlongs.amq.io.Pool;

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
	public enum IO{
		nio,aio;
	}
	public static Charset charsets = StandardCharsets.UTF_8;
	public static IO io = IO.aio;
	public static int port = 8080;
	public String address = "0.0.0.0";
	public int maxConnection = 300000;
	public int maxConcurrent = 1000;
	public int readWait = 3;
	public int requestWait = 300;
	public int threadPoolSize=20;

}