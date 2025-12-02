/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 定量测量参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FRationParameter extends FMeasurePublicParameters {
    // 波长值
    @ApiModelProperty("波长值")
    private List<String> waveLength;
    // 波长参数
    @ApiModelProperty("波长参数")
    private List<String> waveLengthParams;
    // 波长公式
    @ApiModelProperty("波长公式")
    private String waveLengthFormula;
    // 微分方式
    @ApiModelProperty("微分方式")
    private String differentialMode;

    @ApiModelProperty("微分方式名称")
    private String differentialModeName;
    // 曲线类型
    @ApiModelProperty("曲线类型")
    private String curveType;

    @ApiModelProperty("曲线类型名称")
    private String curveTypeName;
    // 方程类型
    @ApiModelProperty("方程类型")
    private String equationType;

    @ApiModelProperty("方程类型名称")
    private String equationTypeName;
    // 曲线公式
    @ApiModelProperty("曲线公式")
    private String curveFormula;
    // 系数方式(0.直接系数法 1.标准曲线法)
    @ApiModelProperty("系数方式,0.直接系数法 1.标准曲线法")
    private String coefficientMode;

    @ApiModelProperty("系数方式名称")
    private String coefficientModeName;
    // 系数参数(系数参数是系数方式延伸下来的参数,因为一个字段无法做到只能使用两个字段进行操作)
    @ApiModelProperty("系数参数")
    private List<String> coefficientParams;
    // 零点方式
    @ApiModelProperty("零点方式")
    private List<String> zeroMethod;

    @ApiModelProperty("零点方式名称")
    private String zeroMethodName;
    // 重复模式
    @ApiModelProperty("重复模式")
    private String repeatMode;
    // 重复次数
    @ApiModelProperty("重复次数")
    private String repeatCount;
    // 时间间隔
    @ApiModelProperty("时间间隔")
    private String timeInterval;
    // 统计内容:[平均值,SD,RSD]
    @ApiModelProperty("统计内容,[平均值,SD,RSD]")
    private List<String> statisticalContent;
    // 样品池方式(0: 单池重复后再移动 1: 依次移动并重复)
    @ApiModelProperty("样品池方式(0: 单池重复后再移动 1: 依次移动并重复)")
    private String samplePoolWay;
    //推荐浓度
    @ApiModelProperty("推荐浓度")
    private List<String> curveCoefficient;
    //单位
    @ApiModelProperty("浓度单位")
    private String unit;
    // 样品输入参数
    @ApiModelProperty("样品输入参数")
    private List<FInputParam> sampleInputParams;
    // 待计算结果
    @ApiModelProperty("待计算结果")
    private List<FPreCountResultParam> resultParams;
    // 修约设置
    @ApiModelProperty("修约设置")
    private List<FRoundingVO> approxParams;
    // 质控设置
    @ApiModelProperty("质控设置")
    private List<FQualityConfVO> qcParams;

}
