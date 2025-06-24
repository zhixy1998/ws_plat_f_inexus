package com.persee.workstation.uv_inexus.po.systemDefaultConfPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("t_system_role")
public class SystemRolePO extends BasePO {

    @ApiModelProperty("系统角色名称")
    @TableField(value = "system_role_name")
    private String systemRoleName;

    @ApiModelProperty("系统角色编号")
    @TableField(value = "system_role_num")
    private String systemRoleNum;

    @ApiModelProperty("系统权限列表")
    @TableField(value = "permission_list")
    private String permissionList;
}
