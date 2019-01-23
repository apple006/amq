package com.artlongs.amq.server.net.http.routes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Delete
 *
 * @author leeton
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Delete {
    public String value();

    public String[] patterns() default {};

    public int[] indexes() default {};
}
