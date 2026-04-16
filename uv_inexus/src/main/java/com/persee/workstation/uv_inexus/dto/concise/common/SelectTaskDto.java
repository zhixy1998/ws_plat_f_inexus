/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
public class SelectTaskDto {
    @ApiModelProperty("模糊查询任务名")
    private String taskName;
    @ApiModelProperty("模糊查询方法名")
    private String methodName;
    @ApiModelProperty("页数")
    private Integer pageNum;
    @ApiModelProperty("每页展示数")
    private Integer pageSize;
    @ApiModelProperty("方法类型")
    private String methodType;
    @ApiModelProperty("操作类型")
    private String operateType;

    @ApiModelProperty("用户名")
    private String updateBy;

}
