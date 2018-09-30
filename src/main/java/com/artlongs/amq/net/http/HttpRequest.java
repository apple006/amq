package com.artlongs.amq.net.http;

import java.util.Map;

/**
*@author song(mejeesong@qq.com)
*2018年2月6日
*
*/
public interface HttpRequest {
	
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";
	public static final String METHOD_DELETE = "DELETE";
	
	String uri();
	String method();
	String query();
	String bodyString();
	byte[] bodyBytes();
	String header(String name);
	Map<String,String> headers();
}
