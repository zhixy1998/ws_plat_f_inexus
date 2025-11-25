package com.persee.workstation.uv_inexus.config.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SysLog {
    /**
     * 操作动作（用于生成 message 与 type 字段）
     * 示例："查看日志"、"新增用户"、"删除角色"
     */
    String action() default "";

    /**
     * 业务模块（用于 source 字段），示例："系统管理"、"用户管理"
     */
    String module() default "";

    /**
     * 是否记录参数摘要（可能进行脱敏与长度截断），默认记录
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果摘要（可能进行脱敏与长度截断），默认记录
     */
    boolean recordResult() default true;
}
