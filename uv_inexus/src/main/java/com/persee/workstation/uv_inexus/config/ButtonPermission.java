package com.persee.workstation.uv_inexus.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按钮权限控制注解
 * 用于标记需要进行权限控制的按钮操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ButtonPermission {
    /**
     * 权限标识（与数据库权限表中的perm_code字段一致）
     */
    String value();

    /**
     * 操作描述
     */
    String description() default "";
}
