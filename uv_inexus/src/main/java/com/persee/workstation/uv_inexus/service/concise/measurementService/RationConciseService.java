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

package com.persee.workstation.uv_inexus.service.concise.measurementService;


import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SaveDataDto;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FRationAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.NewRationMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 定量测定
 */
public interface RationConciseService {
    Result updateMethodParameter(MethodParamsDto methodDto);

    Result createMethodPort(FMethodParamRequestDto methodDto, String updateBy, FRationParameter rationParameter);

    /**
     * 新建草稿
     */
    Result<FMeasurementCurrentInfoVO> newDraft(SaveDataDto dto);

    /**
     * 检查方法参数
     */
    Result<FMethodChangeCheckVO> checkMethodParam(NewRationMethodParamCheckDTO checkDTO);

    /**
     * 保存方法
     */
    Result<FMeasurementCurrentInfoVO> applyMethod(NewRationMethodParamCheckDTO methodDTO);

    /**
     * 获取当前信息
     */
    Result<FMeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);
    /**
     * 查询样品列表
     */
    Result<List<JSONObject>> querySampleList(String taskId, String sampleKind);
    /**
     * 查询样品的表头
     */
    Result<List<FHeadVO>> querySampleHeads(String taskId, String sampleKind);
    /**
     * 新增或删除样本
     */
    Result addOrDeleteSample(FRationAddOrDeleteSampleDTO dto);
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
    Result<FRationMethodParam> queryMethodParam(String methodId);
    /**
     * 保存按钮
     */
    Result<Boolean> saveButton(String taskId, String updateBy);
    /**
     *  查询表格头
     */
    Result<List<FHeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber);
    /**
     * 查询未知样品的表头
     */
    Result<List<FHeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber);

    R<Boolean> checkAllSampleTest(String taskId);

    R<Boolean> createSample(String taskId, String sampleKind, Double normalConcentration, String updateBy, String deviceSerialNumber);

    R<String> saveRationSampleMeasureData(FRationDataVO dataInfo);
    R<FRationSaveCountDataInfo> saveSampleMeasureData(FRationDataVO dataInfo);

    R<Boolean> saveAnalyzeData(FRationSaveCountDataInfo countData);

    void saveEmptySampleData(List<FRationSaveCountDataInfo> emptySampleList);

    R<FRationCountDataInfo> countAbsValue(List<FRationWavelengthDataInfo> originData, FRationParameter parameter, Integer digit);
    //暂时不用
    R<FRationCountDataInfo> countAbsValue(List<FRationWavelengthDataInfo> originData, FRationParameter parameter);

    Result<FRationSampleDataVO> querySampleData(FQueryDataDTO queryDataDTO);

    //暂时不用
    Result<List<Map<String, Object>>> queryStandSampleData(String taskId, Boolean allDisplay);
    Result<List<Map<String, Object>>> queryUnknowSampleData(String taskId, Boolean allDisplay);

    /**
     * 拟合曲线
     * @param originData 原始数据
     * @param empty 是否有空白样品
     */
    R<FittingReceiveInfo> fittingCurve(List<FRationCountDataInfo> originData, Boolean empty, String methodId, List<String> inputParam);

    /**
     * 获取曲线信息
     */
    Result<FRationFitCurveVO> queryCurveInfo(String taskId, String methodId);

    /**
     * 生成曲线数据
     */
    FRationFitCurveVO curveData(FittingReceiveInfo info);

    /**
     * 值计算
     */
    String valueCalculate(FittingReceiveInfo info, Double value, String equationType, Integer digit);

    //暂不用
    String valueCalculate(FittingReceiveInfo info, Double value, String equationType);

    /**
     * 公式修约
     */
    List<FRationCountDataInfo> dataRounding(List<FRationCountDataInfo> dataVO, FRationParameter parameter);
    /**
     * 拟合浓度修约
     */
    String fitDataRounding(String fitConcentration, FRationParameter parameter);


    R<List<FRationCountDataInfo>> formula(String analysisDataId, List<FRationWavelengthDataInfo> vo, FMeasureSample sample, FMeasureTask task, FRationParameter parameter, Integer digit);

    R<Boolean> changeStandSampleDataDigit(String taskId, Integer digit);
    R<Boolean> changeUnknowSampleDigit(String taskId, Integer digit);

    Result zero(String deviceSerialNumber, String methodId, String taskId, String updateBy);

    Result zero2(String deviceSerialNumber, String methodId, String taskId, String updateBy);

    /**
     * 查询可绑定的方法参数
     */
    R<List<FInputParam>> queryInputParam(String methodId);

    /**
     * 查询绑定映射关系
     */
    R<List<FInputParamMapTableHead>> queryInputParamMapTableHead(String taskId, String sampleKind);

    /**
     * 保存映射关系及表头信息
     */
    R<Boolean> saveInputParamMapTableHead(FTaskHeadSnapshotDTO taskHeadSnapshot);

    /**
     * 导出到word
     */
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, Integer digit);

    /**
     * 导出文件（集成children和option数据）
     */
    R<File> downloadFileWithChildren(String taskId, Boolean allDisplay, String fileType, Integer digit, List<String> option);
}
