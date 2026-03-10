/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 兼容版按钮权限注解
 * 支持两种写法：
 * 1. 老写法：@ButtonPermission("quantityCreateTask")（仅基础权限）
 * 2. 新写法：@ButtonPermission(permissionMapping = "...", methodMapping = "...")（自定义映射）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ButtonPermission {
    /**
     * 老写法：基础权限标识（兼容原有逻辑，可选）
     * 新写法下如果配置了permissionMapping，该参数会被忽略
     */
    String value() default "";

    /**
     * 操作描述（可选）
     */
    String description() default "";

    /**
     * SpEL表达式：指定前端传的操作类型字段（默认：dto.operateType）
     * 仅新写法生效
     */
    String operateTypeField() default "dto.operateType";

    /**
     * 新写法：操作类型-权限标识映射（格式：操作类型=完整权限标识）
     * 优先级高于value+拼接，配置后忽略value
     */
    String permissionMapping() default "";

    /**
     * 新写法：操作类型-业务方法映射（格式：操作类型=bean名称.方法名）
     * 可选，不配置则执行接口原有代码
     */
    String methodMapping() default "";
}