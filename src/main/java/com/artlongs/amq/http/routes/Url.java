package com.artlongs.amq.http.routes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ${leeton} on 2018/11/2.
 */
@Target({ElementType.TYPE,ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Url {
    public String value() default "";
}
