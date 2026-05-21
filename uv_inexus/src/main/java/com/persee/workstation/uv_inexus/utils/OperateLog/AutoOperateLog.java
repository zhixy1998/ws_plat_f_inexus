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
 * 自动操作日志注解：用于自动记录详细的业务操作日志
 * 无需手动调用 LogManualUtil，切面自动查询数据库并记录日志
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoOperateLog {

    /**
     * 操作模块（如：用户管理-用户权限管理）
     */
    String module() default "";

    /**
     * 操作类型：add(新增)、edit(修改)、delete(删除)
     */
    String operateType() default "";

    /**
     * 实体类型：sysUser(用户)、sysGroup(小组)、device(设备)等
     */
    String entityType() default "";

    /**
     * 实体ID字段名（默认id）
     */
    String idField() default "id";

    /**
     * 实体名称字段名（用于日志描述，默认username或name）
     */
    String nameField() default "";
}