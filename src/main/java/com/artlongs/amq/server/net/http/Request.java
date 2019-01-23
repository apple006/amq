package com.artlongs.amq.server.net.http;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
*@author leeton
*2018年2月6日
*
*/
public class Request implements HttpRequest {

	public String method = METHOD_GET;
	public String uri;
	public String query;
	public byte[] bodyBytes;
	public final Map<String,String> headers = new HashMap<>();
	public final Map<String,Object> params = new HashMap<>();

	@Override
	public String uri() {
		return uri;
	}
	@Override
	public String method() {
		return method;
	}
	@Override
	public String query() {
		return query;
	}
	@Override
	public byte[] bodyBytes() {
		return bodyBytes;
	}
	@Override
	public String header(String name) {
		return headers.get(name);
	}
	@Override
	public Map<String, String> headers() {
		return headers;
	}

	@Override
	public Map<String, Object> params() {
		return params;
	}

	@Override
	public String bodyString() {
		return String.valueOf(bodyBytes);
	}

	@Override
	public String toString() {
		return "Request [method=" + method + ", uri=" + uri + ", query=" + query + ", bodyBytes="
				+ Arrays.toString(bodyBytes) + ", headers=" + headers + "]";
	}
	
}