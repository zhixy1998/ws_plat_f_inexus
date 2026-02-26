/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.systemUserPermissionPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户表PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("用户表")
@TableName("t_sys_user")
public class SysUserPO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("用户ID")
    private String id;

    @TableField("username")
    @ApiModelProperty("用户名")
    private String username;

    @TableField("token")
    @ApiModelProperty("token")
    private String token;

    @TableField("password")
    @ApiModelProperty("密码（加密存储）")
    private String password;

    @TableField("position")
    @ApiModelProperty("职务")
    private String position;

    @TableField("remarks")
    @ApiModelProperty("备注")
    private String remarks;

    @TableField("real_name")
    @ApiModelProperty("真实姓名")
    private String realName;

    @TableField("phone")
    @ApiModelProperty("手机号")
    private String phone;

    @TableField("email")
    @ApiModelProperty("邮箱")
    private String email;

    @TableField("status")
    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;

    @TableField("role_id")
    @ApiModelProperty("角色id")
    private Integer roleId;

    @TableField("last_login_time")
    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;

    @TableField("quick_navigation")
    @ApiModelProperty("快捷导航")
    private String quickNavigation;
}