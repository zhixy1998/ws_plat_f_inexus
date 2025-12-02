/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义操作日志注解：标记需要记录操作日志的方法
 */
@Target({ElementType.METHOD})  // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME)  // 运行时生效（AOP可反射获取）
@Documented  // 生成JavaDoc时包含该注解
public @interface OperateLog {

    /**
     * 操作模块（如：用户管理、角色配置、权限分配）
     */
    String module() default "";

    /**
     * 操作描述（如：新增用户、删除角色、更新小组权限）
     */
    String description() default "";

    /**
     * 是否记录请求参数（默认记录）
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果（默认不记录，避免敏感数据）
     */
    boolean recordResult() default false;
}