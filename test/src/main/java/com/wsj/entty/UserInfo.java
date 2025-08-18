package com.wsj.entty;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息表实体类
 */
@Data
@TableName("t_user_info")
public class UserInfo {

    /** 用户ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 备注信息 */
    private String remarks;
}