/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.systemUserManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AddSystemPermissionDTO {

    @ApiModelProperty("权限名称")
    private String permissionName;

    @ApiModelProperty("权限类型")
    private String permissionType;

    @ApiModelProperty("权限对应的接口名称")
    private String interfaceName;

    @ApiModelProperty("权限备注")
    private String remark;

    @ApiModelProperty("当前用户")
    private String updateBy;
}
