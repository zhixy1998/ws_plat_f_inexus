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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 快捷配置授权给小组的参数DTO
 */
@Data
@ApiModel("快捷配置授权给小组参数")
public class QuickConfigGrantPermDTO {

    @NotBlank(message = "小组ID不能为空")
    @ApiModelProperty(value = "小组ID", required = true, example = "1001")
    private String groupId;

    @NotEmpty(message = "快捷配置ID列表不能为空")
    @ApiModelProperty(value = "快捷配置ID列表", required = true, example = "[1,2,3]")
    private List<Long> quickConfigIds;
}