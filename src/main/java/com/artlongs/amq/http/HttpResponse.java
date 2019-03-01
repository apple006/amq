package com.artlongs.amq.http;

/**
*@author leeton
*2018年2月6日
*
*/
public interface HttpResponse {
	String getHeader(String name);
	void setHeader(String name, String value);
	int getState();
	void setState(int code);
	void append(String str);
	void flush() ;
	void write(byte[] data) ;
	void end();
}