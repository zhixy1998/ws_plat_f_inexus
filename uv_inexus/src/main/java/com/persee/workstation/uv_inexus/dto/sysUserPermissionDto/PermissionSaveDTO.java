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
 * 权限新增DTO
 */
@Data
@ApiModel("权限新增参数")
public class PermissionSaveDTO {

    @ApiModelProperty("标题名称")
    private String title;

    @ApiModelProperty("权限名称")
    private String permName;

    @ApiModelProperty("权限编码（如：user:add）")
    private String permCode;

    @ApiModelProperty("权限类型：1-菜单路由权限，2-操作权限，3-数据权限")
    private Integer permType;

    @ApiModelProperty("父权限ID，用于构建权限层级，为空默认0")
    private Integer parentId;

    @ApiModelProperty("路由路径，仅菜单权限需要")
    private String path;

    @ApiModelProperty("重定向")
    private String redirect;

    @ApiModelProperty("前端组件路径，仅菜单权限需要")
    private String component;

    @ApiModelProperty("菜单图标，仅菜单权限需要")
    private String icon;

    @ApiModelProperty("排序号")
    private Integer sort;

    @ApiModelProperty("状态：1-启用，0-禁用")
    private Integer status;

    @ApiModelProperty("是否隐藏：1-是，0-否")
    private Integer hidden;

    @ApiModelProperty("是否无需权限：1-是，0-否")
    private Integer noAuth;
}
