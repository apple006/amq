package com.artlongs.amq.net.http.aio;


import com.artlongs.amq.net.http.HttpRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
*@author leeton
*2018年2月6日
*
*/
public class AioHttpRequest implements HttpRequest {

	public String method = METHOD_GET;
	public String uri;
	public String query;
	private byte[] bodyBytes;
	public final Map<String,String> headers = new HashMap<>();

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
	public String bodyString() {
		return String.valueOf(bodyBytes);
	}

	@Override
	public String toString() {
		return "AioHttpRequest [method=" + method + ", uri=" + uri + ", query=" + query + ", bodyBytes="
				+ Arrays.toString(bodyBytes) + ", headers=" + headers + "]";
	}
	
}