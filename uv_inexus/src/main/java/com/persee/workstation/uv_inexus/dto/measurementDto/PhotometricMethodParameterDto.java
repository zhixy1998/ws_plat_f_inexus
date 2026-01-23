/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.measurementDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PhotometricMethodParameterDto extends PhotometricParameter{


    // 用户名
    private String updateBy;
    // 项目id
    private String projectId;
    // 方法id
    private String currentMethodId;
    // 方法名称
    private String methodName;
    // 任务id
    private String taskId;
    //任务名
    private String newTaskName;

    @ApiModelProperty("清除数据范围")
    private String clearDataRange;

    @ApiModelProperty("分析参数是否应用到当前")
    private Boolean isApplyToCurrent;

    private String deviceSerialNumber;

    @ApiModelProperty("前处理Key")
    private String preprocessKey;
}
