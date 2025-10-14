/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationFitCurveVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationMethodParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationSampleDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationSaveCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationWavelengthDataInfo;

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
    Result<List<HeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay);
    /**
     * 查询未知样品的表头
     */
    Result<List<HeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay);

    R<String> saveRationSampleMeasureData(RationDataVO dataInfo);
    R<RationSaveCountDataInfo> saveSampleMeasureData(RationDataVO dataInfo);

    R<Boolean> saveAnalyzeData(RationSaveCountDataInfo countData);

    void saveEmptySampleData(List<RationSaveCountDataInfo> emptySampleList);

    R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter, Integer digit);
    R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter);

    Result<List<Map<String, Object>>> queryStandSampleData(String taskId, Boolean allDisplay);
    Result<RationSampleDataVO> querySampleData(QueryDataDTO queryDataDTO);


    Result<List<Map<String, Object>>> queryUnknowSampleData(String taskId, Boolean allDisplay);

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

    R<List<RationCountDataInfo>> formula(List<RationWavelengthDataInfo> vo, TMeasureSample sample, TMeasureTask task, RationParameter parameter, Integer digit);

    R<Boolean> changeStandSampleDataDigit(String taskId, Integer digit);
    R<Boolean> changeUnknowSampleDigit(String taskId, Integer digit);

    Result zero(String deviceSerialNumber, String methodId, String taskId, String updateBy);

    Result zero2(String deviceSerialNumber, String methodId, String taskId, String updateBy);

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


}
