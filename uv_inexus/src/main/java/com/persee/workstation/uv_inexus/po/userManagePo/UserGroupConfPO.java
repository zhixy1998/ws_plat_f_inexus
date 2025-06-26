package com.persee.workstation.uv_inexus.po.userManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ApiModel("用户组配置")
@Data
@TableName("t_system_user_group")
public class UserGroupConfPO extends BasePO {

    @ApiModelProperty("组名称")
    @TableField(value = "user_type")
    private String userType;

    @ApiModelProperty("密码时效性，不设置为0，以天为单位")
    @TableField(value = "password_valid")
    private Integer passwordValid;

    @ApiModelProperty("密码复杂度")
    @TableField(value = "password_rule")
    private String passwordRule;

    @ApiModelProperty("登录错误次数")
    @TableField(value = "login_error_time")
    private Integer loginErrorTime;

    @ApiModelProperty("登录错误处理")
    @TableField(value = "login_error_done")
    private String loginErrorDone;

    @ApiModelProperty("用户组是否有效")
    @TableField(value = "is_valid")
    private Integer isValid;

    @ApiModelProperty("用户组备注")
    @TableField(value = "remark")
    private String remark;

    @ApiModelProperty("操作原因")
    @TableField(value = "reason")
    private String reason;

    @ApiModelProperty("系统权限列表")
    @TableField(value = "system_permission_list")
    private String systemPermissionList;

    @ApiModelProperty("系统权限列表")
    @TableField(value = "system_permission_list", exist = false)
    private List<String> permissionList;

}
