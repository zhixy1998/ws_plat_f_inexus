package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PhotometricSampleAndDataVO {

    private String taskId;

    private String sampleId;

    private TMeasureSample sample;

    LocalDateTime startTime;
    LocalDateTime endTime;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("采样时间")
    private String sampleTime;

    @ApiModelProperty("仪器原始数据，保存使用，不展示给前端")
    @JsonIgnore
    private List<ChangeParam> machineData;

    @ApiModelProperty("原始计算数据")
    private List<PhotometricOriginDataInfo> wavelengthData;

    @ApiModelProperty("经过公式计算后的数据")
    private List<PhotometricCountDataInfo> countData;
}

