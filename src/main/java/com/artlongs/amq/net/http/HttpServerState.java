package com.artlongs.amq.net.http;

import java.util.concurrent.atomic.AtomicInteger;

/**
*@author song(mejeesong@qq.com)
*2018年2月6日
*
*/
public class HttpServerState {

	public static final String STATE_RUNNING = "RUNNING";
	public static final String STATE_STOPED = "STOPED";
	
	public static int FIELD_CONNECTION = 0;
	public static int FIELD_CONCURRENT = 1;
	
	private AtomicInteger[] fields = new AtomicInteger[2];
	
	private String state = STATE_STOPED;
	
	public HttpServerState(HttpServer httpServer){
		fields[0] = new AtomicInteger(0);
		fields[1] = new AtomicInteger(0);
	}
	
	public int incrementAndGet(int field) {
		return fields[field].incrementAndGet();
	}
	public int decrementAndGet(int field) {
		return fields[field].decrementAndGet();
	}
	public int get(int field) {
		return fields[field].get();
	}
	
	
	public String getInfo() {
		StringBuilder info = new StringBuilder();
		info.append("state:").append(state).append(",(");
		for(AtomicInteger ai:fields) {
			info.append(ai.get()).append(",");
		}
		info.append(")");
		return info.toString();
	}
}
