/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_sys_logs")
public class SysLogsPO {
    /**
     * 主键ID（自增），由数据库自动生成，不需代码赋值
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 日志时间（对应表字段 time）
     */
    @TableField("time")
    private LocalDateTime time;

    /**
     * 操作类型（对应表字段 type），通常来自返回值
     */
    @TableField("type")
    private String type;

    /**
     * 消息来源/业务模块（对应表字段 source），通常来自注解的 module
     */
    @TableField("source")
    private String source;

    /**
     * 用户ID（对应表字段 user_id）
     * 暂未启用，后期可视情况变为其他字段
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名（对应表字段 user_name）
     */
    @TableField("user_name")
    private String userName;

    /**
     * 任务ID（对应表字段 task_id），从入参按约定字段提取
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 任务名称（对应表字段 task_name），从入参按约定字段提取
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 方法ID（对应表字段 method_id），可使用方法全名或其哈希
     */
    @TableField("method_id")
    private String methodId;

    /**
     * 方法名称（对应表字段 method_name），建议使用类名.方法名
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 消息（对应表字段 message），通常来自注解的 action
     */
    @TableField("message")
    private String message;

    /**
     * 仪器ID（对应表字段 instrument_id），从入参按约定字段提取
     */
    @TableField("instrument_id")
    private String instrumentId;

    /**
     * 仪器名称（对应表字段 instrument_name），从返回值里获取
     */
    @TableField("instrument_name")
    private String instrumentName;

    @TableField("project_id")
    private String projectId;

    @TableField("project_name")
    private String projectName;

    /**
     * PC 名称（对应表字段 pc_name），来自环境变量或默认值
     */
    @TableField("pc_name")
    private String pcName;
}
