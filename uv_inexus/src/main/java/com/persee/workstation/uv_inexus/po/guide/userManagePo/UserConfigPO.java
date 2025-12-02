/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.userManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_sysconfig_user")
public class UserConfigPO extends BasePO {

    @TableField("user_name")
    @ApiModelProperty("用户名")
    private String userName;

    @TableField("password")
    @ApiModelProperty("用户密码")
    private String password;

    @TableField("token")
    @ApiModelProperty("用户当前token")
    private String token;

    @TableField("user_real_name")
    @ApiModelProperty("用户真实姓名")
    private String userRealName;

    @TableField("user_position")
    @ApiModelProperty("用户职位")
    private String userPosition;

    @TableField("remark")
    @ApiModelProperty("用户备注")
    private String remark;

    @ApiModelProperty("组名")
    @TableField(value = "group_name")
    private String groupName;

    @TableField("group_id")
    @ApiModelProperty("用户组id")
    private String groupId;

    @TableField("system_identity_id")
    @ApiModelProperty("系统标识id")
    private String systemIdentityId;

    @TableField("logo")
    private String logo;

    @TableField("digital_signature")
    @ApiModelProperty("数字签名")
    private String digitalSignature;

    @TableField("digital_signature_path")
    @ApiModelProperty("数字签名路径")
    private String digitalSignaturePath;

    @TableField("is_valid")
    @ApiModelProperty("用户是否有效")
    private Boolean isValid;

    @TableField("is_login")
    @ApiModelProperty("用户是否登录")
    private Boolean isLogin;

    @TableField("login_fail_count")
    @ApiModelProperty("用户登录失败次数")
    private Integer loginFailCount;

    @TableField("change_password_time")
    @ApiModelProperty("用户修改密码时间")
    private LocalDateTime changePasswordTime;

    @TableField("reason")
    @ApiModelProperty("修改原因")
    private String reason;


}
