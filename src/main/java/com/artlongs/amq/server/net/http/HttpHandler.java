package com.artlongs.amq.server.net.http;

/**
*@author leeton
*2018年2月6日
*
*/
public interface HttpHandler {
	void handle(HttpRequest req, HttpResponse res) ;
}
