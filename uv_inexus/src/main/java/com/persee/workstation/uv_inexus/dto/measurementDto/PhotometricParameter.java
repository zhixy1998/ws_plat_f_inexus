/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.i18n.NoI18n;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.PreCountResultParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.QualityConfVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RoundingVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 光度测量参数
 */
@Data
public class PhotometricParameter extends MeasurePublicParameters {
    // 光度模式(1.Abs 2.T% 3.Es 4.ER)
    @ApiModelProperty("光度模式")
    private String luminosity;
    @ApiModelProperty("光度模式名称")
    private String luminosityName;
    // 波长值(波长值是多个值,不连续的)
    @ApiModelProperty("波长值")
    private List<String> waveLength;
    // 重复模式
    @ApiModelProperty("重复模式")
    private String repetitionPattern;
    // 重复次数
    @NoI18n
    @ApiModelProperty("重复次数")
    private String repetitionTime;
    // 时间间隔
    @ApiModelProperty("时间间隔")
    private String timeInterval;
    // 统计内容:[平均值,SD,RSD]
    @ApiModelProperty("统计内容")
    private List<String> statisticalContent;
    // 样品池方式(0: 单池重复后再移动 1: 依次移动并重复)
    @ApiModelProperty("样品池方式")
    private String samplePoolWay;

    @ApiModelProperty("样品池方式名称")
    private String samplePoolWayName;
    // 输入参数
    @ApiModelProperty("输入参数")
    private List<InputParam> sampleInputParams;
    // 待计算结果
    @ApiModelProperty("待计算结果")
    private List<PreCountResultParam> resultParams;
    // 修约设置
    @ApiModelProperty("修约设置")
    private List<RoundingVO> approxParams;
    // 质控设置
    @ApiModelProperty("质控设置")
    private List<QualityConfVO> qcParams;
//    // 校零参数
//    @ApiModelProperty("校零参数")
//    private List<ChangeParam> chanageParams;

}
