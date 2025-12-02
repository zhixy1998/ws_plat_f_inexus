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
