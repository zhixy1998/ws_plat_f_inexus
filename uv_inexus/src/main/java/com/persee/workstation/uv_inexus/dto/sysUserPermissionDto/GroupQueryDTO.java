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
 * 小组分页查询数据传输对象
 */
@Data
@ApiModel(description = "小组查询DTO")
public class GroupQueryDTO {

    @ApiModelProperty(value = "页码（默认1）")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页条数（默认10）")
    private Integer pageSize = 10;

    @ApiModelProperty(value = "小组名称（模糊查询）")
    private String groupName;

    @ApiModelProperty(value = "角色ID（查询指定角色下的小组）")
    private String roleId;

    @ApiModelProperty(value = "状态（1-启用，0-禁用）")
    private Integer status;
}