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

/**
 * 角色-权限关联表PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("权限关联表")
@TableName("t_sys_group_permission")
public class SysGroupPermissionPO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("关联ID")
    private String id;

    @TableField("group_id")
    @ApiModelProperty("角色ID，关联角色表")
    private String groupId;

    @TableField("perm_id")
    @ApiModelProperty("权限ID，关联权限表")
    private String permId;
}
