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
 * 小组表PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("小组表")
@TableName("t_sys_group")
public class SysGroupPO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("小组ID")
    private String id;

    @TableField("group_name")
    @ApiModelProperty("小组名称")
    private String groupName;

    @TableField("role_id")
    @ApiModelProperty("角色ID，关联角色表")
    private String roleId;

    @TableField("description")
    @ApiModelProperty("小组描述")
    private String description;

    @TableField("status")
    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;
}