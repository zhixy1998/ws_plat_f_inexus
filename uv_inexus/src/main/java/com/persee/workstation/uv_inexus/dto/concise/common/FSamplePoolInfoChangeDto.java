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

@Data
public class FSamplePoolInfoChangeDto {
    @ApiModelProperty("操作类型：lumne/quantity/spectralScan/timeScan")
    private String operateType;

    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池号")
    private Integer poolNum;

    @ApiModelProperty("是否自动切换样品池")
    private Boolean isAutoChangePool;

    @ApiModelProperty("样品池状态")
    private Boolean poolStatus;

    @ApiModelProperty("样品池用途")
    private Integer poolPurpose;

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("更新人")
    private String updateBy;

    @ApiModelProperty("更新数据库")
    private String updateDatabase;
}
