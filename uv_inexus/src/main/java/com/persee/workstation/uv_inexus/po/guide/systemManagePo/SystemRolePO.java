/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
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
