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

import java.util.List;

@Data
public class FQueryDataDTO {
    private String taskId;
    @ApiModelProperty("是否显示被删除的数据")
    private Boolean allDisplay;
    @ApiModelProperty("小数位数")
    private Integer digit;
    @ApiModelProperty("要显示的数据类型：平均值/sd/rsd等")
    private List<String> option;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;
 }
