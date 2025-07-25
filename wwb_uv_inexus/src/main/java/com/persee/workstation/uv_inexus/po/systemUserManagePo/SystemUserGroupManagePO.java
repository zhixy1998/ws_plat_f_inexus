package com.persee.workstation.uv_inexus.po.systemUserManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("系统用户组管理")
@TableName("t_system_user_group_manage")
public class SystemUserGroupManagePO extends BasePO {

    @ApiModelProperty("组名称")
    @TableField(value = "group_name")
    private String groupName;

    @ApiModelProperty("组编号")
    @TableField(value = "group_no")
    private String groupNo;

    @ApiModelProperty("组描述")
    @TableField(value = "group_desc")
    private String groupDesc;

    @ApiModelProperty("系统身份")
    @TableField(value = "system_identity_id")
    private Integer systemIdentityId;

    @ApiModelProperty("用户组是否有效")
    @TableField(value = "is_valid")
    private Integer isValid;

    @ApiModelProperty("操作原因")
    @TableField(value = "reason")
    private String reason;

    @ApiModelProperty("系统权限列表")
    @TableField(value = "group_permission_list")
    private String groupPermissionList;
}
