package com.artlongs.amq.net.http;

import com.artlongs.amq.net.http.routes.Controller;

/**
*@author leeton
*2018年2月6日
*
*/
public interface HttpHandler extends Controller{
	void handle(HttpRequest req, HttpResponse res) throws Exception;
}
