package com.persee.workstation.uv_inexus.dto.parseReport;

import java.util.List;
import java.util.Map;

import com.persee.workstation.uv_inexus.po.parseReport.Detection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("检测详情DTO")
@Data
public class DetectionDetailDTO {
    @ApiModelProperty("主数据信息")
    private DetectionListDTO mainData;

    @ApiModelProperty("原始数据列表")
    private List<Detection> originalDataList;

    @ApiModelProperty("标准标识、检测类型及id数组")
    private List<Map<String, Object>> identityTypeList; // 包含id
}