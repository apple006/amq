package com.artlongs.amq.net.http;

/**
*@author song(mejeesong@qq.com)
*2018年2月6日
*
*/
public interface HttpRequestHandler {
	void handle(HttpRequest req, HttpResponse res) throws Exception;
}
