package com.persee.workstation.uv_inexus.po.systemctlConfigPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_sysconfig_user")
public class UserConfigPO extends BasePO {

    @TableField("user_name")
    private String userName;

    @TableField("password")
    private String password;

    @TableField("user_real_name")
    private String userRealName;

    @TableField("user_position")
    private String userPosition;

    @TableField("remark")
    private String remark;

    @TableField("group_id")
    private String groupId;

    @TableField("system_identity_id")
    private String systemIdentityId;

    @TableField("logo")
    private String logo;

    @TableField("digital_signature")
    private String digitalSignature;

    @TableField("digital_signature_path")
    private String digitalSignaturePath;

    @TableField("is_valid")
    private Boolean isValid;

    @TableField("is_login")
    private Boolean isLogin;

    @TableField("login_fail_count")
    private Integer loginFailCount;

    @TableField("change_password_time")
    private LocalDateTime changePasswordTime;

    @TableField("reason")
    private String reason;


}
