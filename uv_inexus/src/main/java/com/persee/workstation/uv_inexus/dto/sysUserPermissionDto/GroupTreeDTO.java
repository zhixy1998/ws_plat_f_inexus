/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 组-用户树形结构DTO
 */
@Data
@ApiModel("小组树形结构")
public class GroupTreeDTO {
    @ApiModelProperty("小组ID")
    private String id;

    @ApiModelProperty("小组名称")
    private String groupName;

    @ApiModelProperty("小组描述")
    private String description;

    @ApiModelProperty("小组状态（1-启用，0-禁用）")
    private Integer status;

    @ApiModelProperty("该小组下的用户列表")
    private List<UserTreeDTO> userList;  // 同级包含用户列表
}