/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 权限查询DTO
 */
@Data
@ApiModel("权限查询参数")
public class PermissionQueryDTO {

    @ApiModelProperty("标题名称")
    private String title;

    @ApiModelProperty("权限名称")
    private String permName;

    @ApiModelProperty("权限编码")
    private String permCode;

    @ApiModelProperty("权限类型：1-菜单路由权限，2-操作权限，3-数据权限")
    private Integer permType;

    @ApiModelProperty("父权限ID")
    private Integer parentId;

    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;

    @ApiModelProperty("是否隐藏：1-是，0-否")
    private Integer hidden;

    @ApiModelProperty("是否无需权限：1-是，0-否")
    private Integer noAuth;

    @ApiModelProperty("快捷配置类型：0-普通用户，1-GMP")
    private Integer quickConfigType;
}
