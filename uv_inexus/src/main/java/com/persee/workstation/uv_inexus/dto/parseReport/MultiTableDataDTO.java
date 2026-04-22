/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.parseReport;

import java.util.List;
import java.util.Map;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingVO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 多子表多条数据查询DTO
 */
@ApiModel("多子表多条数据查询DTO")
@Data
public class MultiTableDataDTO {

    @ApiModelProperty("波长列表（波长1、波长2、波长3的值）")
    private List<String> detectionParameters; // 原detectionParameters改造为波长列表

    @ApiModelProperty("波长参数列表（波长参数1、波长参数2的值）")
    private List<String> wavelengthParamList; // 新增字段

    @ApiModelProperty("曲线参数（推荐单位、推荐标样浓度、拟合公式）")
    private Map<String, String> curveParameter; // 新增字段

    @ApiModelProperty("待计算结果列表（计算公式表）")
    private List<CalculationResultDTO> calculationResults;

    @ApiModelProperty("修约设置列表（修约表）")
    private List<FRoundingVO> roundingSettings;
//    private List<RoundingSettingDTO> roundingSettings;

    @ApiModelProperty("输入参数列表（计算公式表）")
    private List<InputParamDTO> inputParams;

    @ApiModelProperty("前处理描述列表（前处理表）")
    private List<PretreatmentDTO> pretreatments;

    @ApiModelProperty("质量控制列表（质量控制表）")
    private List<QualityControlDTO> qualityControls;
}