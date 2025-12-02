/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RationDataVO {

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("样品id")
    private String sampleId;

    LocalDateTime startTime;
    LocalDateTime endTime;

    private String sampleTime;

    @ApiModelProperty("机器rsd数据")
    private List<ChangeParam> machineData;

    @ApiModelProperty("原始计算数据")
    private List<RationWavelengthDataInfo> wavelengthData;

    @ApiModelProperty("计算数据")
    private List<RationCountDataInfo> countData;

}
