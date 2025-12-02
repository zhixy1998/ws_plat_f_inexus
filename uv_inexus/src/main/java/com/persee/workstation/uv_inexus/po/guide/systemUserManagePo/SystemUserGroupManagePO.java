/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
