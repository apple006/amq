package com.artlongs.amq.http;

import com.alibaba.fastjson.JSON;
import com.artlongs.amq.http.routes.Controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Func :
 *
 * @author: leeton on 2019/3/12.
 */
public class Render<T> implements HttpHandler,Controller {

    private byte[] data;
    private String templateUrl;
    private T params;
    private Fmt fmt;


    public Render(String templateUrl, T params) {
        this.templateUrl = templateUrl;
        this.params = params;
    }

    @Override
    public void handle(HttpRequest req, HttpResponse resp) {
        resp.setState(HttpStatus.OK);
        setHeadOfFmt(resp);
        resp.append(data);
        resp.end();
    }

    public static Render template(String url){
        return template(url, null);
    }

    public static <T> Render template(String url, T params) {
        Render result = new Render(url, params);
        result.data = read(url);
        result.fmt = Fmt.html;
//        System.err.println(new String(result.data));
        params = null;
        return result;
    }

    public static <T> Render json(T params) {
        Render result = new Render("", params);
        result.fmt = Fmt.json;
        result.data = JSON.toJSONBytes(params);
//        System.err.println(new String(result.data));
        params = null;
        return result;
    }


    /**
     * 读取模板文件
     * @param url 模板
     * @return
     */
    private static byte[] read(String url) {
        try {
            Path path = Paths.get(getHomePath().getPath() + url);
            InputStream is = Files.newInputStream(path);
            int length = is.available();
            byte[] b = new byte[length];//把所有的数据读取到这个字节当中
            is.read(b,0,length);
            is.close();
            return b;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];

    }

    /**
     * 取得项目的模板根目录的绝对路径
     * @return
     */
    private static File getHomePath() {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("views");
            if (null != url) return new File(url.getPath());
        } catch (Exception e) {
            e.printStackTrace();

        }
        return new File("");
    }

    private void setHeadOfFmt(HttpResponse resp){
        if (Fmt.json == this.fmt) {
            resp.setHeader("Content-Type","application/json; charset=utf-8");
        }
        if (Fmt.html == this.fmt) {
            resp.setHeader("Content-Type","text/html; charset=utf-8");
        }
    }

    public enum Fmt{
        html,json;
    }
}
