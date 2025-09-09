package com.persee.workstation.uv_inexus.po.OperateLog;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 操作日志实体类（对应数据库表：sys_operate_log）
 */
@Data
@TableName("t_sys_operate_log")  // 数据库表名，需与实际表名一致
public class OperateLogPO {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户ID（关联sys_user.id）
     */
    // private String userId;

    /**
     * 操作用户名（冗余存储，便于快速查询）
     */
    private String username;

    /**
     * 操作模块（如：用户管理）
     */
    private String module;

    /**
     * 操作描述（如：新增用户）
     */
    private String description;

    /**
     * 调用方法全路径（如：com.xxx.UserController.addUser）
     */
    private String method;

    /**
     * 请求参数（JSON格式，敏感数据建议脱敏）
     */
    private String params;

    /**
     * 返回结果（JSON格式，仅recordResult=true时记录）
     */
    private String result;

    /**
     * 操作IP地址
     */
    private String ip;

    /**
     * 操作状态（1-成功，0-失败）
     */
    private Integer status;

    /**
     * 错误信息（仅失败时记录）
     */
    private String errorMsg;

    /**
     * 操作耗时（毫秒）
     */
    private Long operateTime;

    /**
     * 日志记录时间
     */
    private Date createTime;
}