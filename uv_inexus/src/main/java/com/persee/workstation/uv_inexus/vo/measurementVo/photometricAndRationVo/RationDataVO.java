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
