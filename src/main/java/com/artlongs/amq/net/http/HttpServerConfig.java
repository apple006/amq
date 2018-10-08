package com.artlongs.amq.net.http;

/**
*@author leeton
*2018年2月6日
*
*/
public class HttpServerConfig {
	public int port = 8080;
	public String address = "0.0.0.0";
	public int maxConnection = 3000;
	public int maxConcurrent = 1000;
	public int readWait = 3;
	public int requestWait = 300;
	public int threadPoolSize=20;
	public final Object connectionLock = new Object();
	public final Object concurrentLock = new Object();
}