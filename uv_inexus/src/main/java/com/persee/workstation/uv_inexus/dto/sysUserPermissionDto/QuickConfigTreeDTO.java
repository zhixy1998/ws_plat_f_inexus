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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 快捷配置树形结构DTO（匹配目标返回格式）
 */
@Data
@ApiModel(description = "快捷配置树形DTO")
public class QuickConfigTreeDTO {
    @ApiModelProperty("主键ID")
    private String id;

    @ApiModelProperty("创建人")
    private String createBy;

    @ApiModelProperty("更新人")
    private String updateBy;

    @ApiModelProperty("创建时间")
    private LocalDateTime created;

    @ApiModelProperty("更新时间")
    private LocalDateTime updated;

    @ApiModelProperty("配置名称")
    private String name;

    @ApiModelProperty("绑定权限名称")
    private String permIds;

    @ApiModelProperty("绑定权限名称")
    private Integer quickConfigType;

    @ApiModelProperty("父级ID")
    private Long parentId;

    @ApiModelProperty("子节点列表")
    private List<QuickConfigTreeDTO> children;
}