/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PhotometricSampleAndDataVO {

    private String taskId;

    private TMeasureTask task;

    private TMeasureSample sample;

    private String sampleId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("采集时间")
    private String sampleTime;

    @ApiModelProperty("仪器原始数据，保存使用，不展示给前端")
    @JsonIgnore
    private List<ChangeParam> machineData;

    @ApiModelProperty("原始计算数据")
    private List<PhotometricOriginDataInfo> wavelengthData;

    @ApiModelProperty("经过公式计算后的数据")
    private List<PhotometricCountDataInfo> countData;
}

