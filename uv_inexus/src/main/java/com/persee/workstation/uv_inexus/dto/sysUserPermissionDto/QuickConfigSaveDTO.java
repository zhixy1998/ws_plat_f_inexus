/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 快捷配置新增/修改DTO
 */
@Data
@ApiModel(description = "快捷配置新增/修改DTO")
public class QuickConfigSaveDTO {

    @ApiModelProperty(value = "配置ID（修改时必传，新增时不传）")
    private Long id;

    @ApiModelProperty(value = "配置名称", required = true)
    @NotBlank(message = "快捷配置名称不能为空")
    private String name;

    @ApiModelProperty(value = "绑定权限名称", required = true)
    private String permIds;

    @ApiModelProperty(value = "父级ID（0为一级配置）", required = true)
    @NotNull(message = "父级ID不能为空")
    private Long parentId;

    @ApiModelProperty(value = "状态：0-普通用户，0-GMP", required = true)
    private Integer quickConfigType;
}