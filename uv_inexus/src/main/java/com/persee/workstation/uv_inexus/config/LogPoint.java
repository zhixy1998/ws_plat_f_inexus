package com.persee.workstation.uv_inexus.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogPoint {
    /**
     * 操作名称
     */
    String value() default "";
    
    /**
     * 模块名称
     */
    String module() default "";
    
    /**
     * 是否记录方法参数
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean recordResult() default true;
    
    /**
     * 是否记录用户信息
     */
    boolean recordUser() default true;
    
    /**
     * 是否记录客户端IP
     */
    boolean recordClientIP() default true;
    
    /**
     * 是否记录方法执行时间
     */
    boolean recordDuration() default true;
}