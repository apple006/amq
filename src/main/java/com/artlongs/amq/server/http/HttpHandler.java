package com.artlongs.amq.server.http;

/**
*@author leeton
*2018年2月6日
*
*/
public interface HttpHandler {
	void handle(HttpRequest req, HttpResponse res) ;
}
