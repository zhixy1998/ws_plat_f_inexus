/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dto.guide.GuideCreateSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRationCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 定量测定
 */
public interface RationService {
    Result updateMethodParameter(MethodParamsDto methodDto);

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, RationParameter rationParameter);
    /**
     * 获取当前信息
     */
    Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);

    /**
     * 修改方法并保存
     */
    Result<MeasurementCurrentInfoVO> updateMethodParam(MethodParamsDto methodDto);

    /**
     * 检查方法参数
     */
    Result<MethodChangeCheckVO> checkMethodParam(MethodParamsDto checkDTO);

//    /**
//     * 使用方法时检查参数
//     */
//    Result<MethodChangeCheckVO> checkTwoMethodParam(String currenntMethodId, String newMethodId);

    /**
     * 查询样品列表
     */
    Result<List<JSONObject>> querySampleList(String taskId, String sampleKind);
    /**
     * 查询样品的表头
     */
    Result<List<HeadVO>> querySampleHeads(String taskId, String sampleKind);
    /**
     * 新增或删除样本
     */
    Result addOrDeleteSample(RationAddOrDeleteSampleDTO dto);
    /**
     * 删除单个样本
     */
    Result<Boolean> deleteSample(String taskId, String sampleId);
    /**
     * 撤销删除
     */
    Result revokeDel(String taskId, String sampleId);
    /**
     * 查询方法参数
     */
    Result<RationMethodParam> queryMethodParam(String methodId);
    /**
     *  查询表格头
     */
    Result<List<HeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber);
    /**
     * 查询未知样品的表头
     */
    Result<List<HeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber);

    R<String> saveRationSampleMeasureData(RationDataVO dataInfo);
    R<RationSaveCountDataInfo> saveSampleMeasureData(RationDataVO dataInfo);

    R<Boolean> saveAnalyzeData(RationSaveCountDataInfo countData);

    void saveEmptySampleData(List<RationSaveCountDataInfo> emptySampleList);

    R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter, Integer digit);
    R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter);

    Result<List<Map<String, Object>>> queryStandSampleData(String taskId, Boolean allDisplay);
    Result<RationSampleDataVO> querySampleData(QueryDataDTO queryDataDTO);


    Result<List<Map<String, Object>>> queryUnknowSampleData(String taskId, Boolean allDisplay);

    R<Boolean> checkStandAllSampleTest(String taskId);

    R<Boolean> createSample(GuideCreateSampleDTO dto);

    /**
     * 拟合曲线
     * @param originData 原始数据
     * @param empty 是否有空白样品
     */
    R<FittingReceiveInfo> fittingCurve(List<RationCountDataInfo> originData, Boolean empty, String methodId, List<String> inputParam);

    /**
     * 获取曲线信息
     */
    Result<RationFitCurveVO> queryCurveInfo(String taskId, String methodId);

    /**
     * 生成曲线数据
     */
    RationFitCurveVO curveData(FittingReceiveInfo info);

    /**
     * 值计算
     */
    String valueCalculate(FittingReceiveInfo info, Double value, String equationType, Integer digit);
    String valueCalculate(FittingReceiveInfo info, Double value, String equationType);

    /**
     * 公式修约
     */
    List<RationCountDataInfo> dataRounding(List<RationCountDataInfo> dataVO, RationParameter parameter);
    /**
     * 拟合浓度修约
     */
    String fitDataRounding(String fitConcentration, RationParameter parameter);


    R<List<RationCountDataInfo>> formula(List<RationWavelengthDataInfo> vo, TMeasureSample sample, TMeasureTask task, RationParameter parameter, Integer digit);

    R<Boolean> changeStandSampleDataDigit(String taskId, Integer digit);
    R<Boolean> changeUnknowSampleDigit(String taskId, Integer digit);

    Result zero(String deviceSerialNumber, String methodId, String taskId, String updateBy);

    Result zero2(String deviceSerialNumber, String methodId, String taskId, String updateBy);

    /**
     * 质控判断
     */
    List<QualityCheckResultVO> qualityControl(String taskId, String methodId);

    /**
     * 查询可绑定的方法参数
     */
    R<List<InputParam>> queryInputParam(String methodId);

    /**
     * 查询绑定映射关系
     */
    R<List<InputParamMapTableHead>> queryInputParamMapTableHead(String taskId, String sampleKind);

    /**
     * 保存映射关系及表头信息
     */
    R<Boolean> saveInputParamMapTableHead(TaskHeadSnapshotDTO taskHeadSnapshot);

    /**
     * 导出到word
     */
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType);

    void test(String a, String b);

}
