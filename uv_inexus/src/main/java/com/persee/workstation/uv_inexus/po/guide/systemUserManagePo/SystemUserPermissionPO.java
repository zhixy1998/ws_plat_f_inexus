package com.persee.workstation.uv_inexus.po.guide.systemUserManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("系统权限表")
@TableName("t_system_user_permission")
public class SystemUserPermissionPO extends BasePO {

    @ApiModelProperty("权限序号")
    @TableField(value = "sid")
    private Integer sid;

    @ApiModelProperty("权限名称")
    @TableField(value = "permission_name")
    private String permissionName;

    @ApiModelProperty("权限类型")
    @TableField(value = "permission_type")
    private String permissionType;

    @ApiModelProperty("权限对应的接口名称")
    @TableField(value = "interface_name")
    private String interfaceName;

    @ApiModelProperty("权限备注")
    @TableField(value = "remark")
    private String remark;
}
