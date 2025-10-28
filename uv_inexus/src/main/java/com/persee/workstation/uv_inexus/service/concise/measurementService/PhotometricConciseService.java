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
import com.persee.workstation.uv_inexus.dto.concise.common.FAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SaveDataDto;
import com.persee.workstation.uv_inexus.dto.concise.photometricDto.NewPhotometricMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FMethodParam;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPhotometricParameter;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FMethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSaveDataVO;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface PhotometricConciseService {
    /**
     * 创建光度测量方法
     */
    Result createMethodPort(FMethodParamRequestDto methodDto, String updateBy, FPhotometricParameter photometricParameter);

    /**
     * 新建草稿
     */
    Result<FMeasurementCurrentInfoVO> newDraft(SaveDataDto dto);

    /**
     * 检查方法参数
     */
    Result<FMethodChangeCheckVO> checkMethodParam(NewPhotometricMethodParamCheckDTO checkDTO);

    /**
     * 保存方法
     */
    Result<FMeasurementCurrentInfoVO> applyMethod(NewPhotometricMethodParamCheckDTO methodDTO);

    /**
     * 获取当前信息
     */
    Result<FMeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);

    /**
     * 新增或删除样本
     */
    Result addOrDeleteSample(FAddOrDeleteSampleDTO dto);
    /**
     * 删除单个样本
     */
    Result<Boolean> deleteSample(String taskId, String sampleId);
    /**
     * 撤销删除
     */
    Result revokeDel(String taskId, String sampleId);
    /**
     * 查询样品列表
     */
    Result<List<JSONObject>> newQuerySampleInfo(String taskId);

    /**
     * 创建样品
     */
    R<Boolean> createSample(String taskId, String updateBy);

    /**
     * 查询样品的表头
     */
    Result<List<FHeadVO>> querySampleHeads(String taskId);
    /**
     * 查询方法参数
     */
    Result<FMethodParam> queryMethodParam(String methodId);
    /**
     * 查询报告数据
     */
    R<FPhotometricReportDataVO> getReportCount(String taskId);
    /**
     *  查询表格头
     */
    Result<List<FHeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber);
    /**
     * 查询数据信息
     */
    Result<List<Map<String, Object>>> queryData(FQueryDataDTO queryDataDTO);

    /**
     * 重新分析数据
     */
    R<Boolean> reAnalyze(String taskId, String updateBy);


    /**
     * 计算公式
     */
    R<List<FPhotometricCountDataInfo>> formula(List<FPhotometricOriginDataInfo> vo, FMeasureSample sample, FMeasureTask task, FPhotometricParameter parameter, Integer digit);

    /**
     * 测量及分析后数据保存，如有记录，认为是重测，做覆盖操作
     */
    R<String> savePhotometricMeasureData(FPhotometricSampleAndDataVO vo);

    R<Boolean> saveCountData(FPhotometricSampleAndDataVO countData, String dataId);

    /**
     * 前端调用保存数据，用于保存相关内容，如采样时间，备注等
     */
    Result saveDataInfo(FSaveDataVO vo);

    /**
     * 校零
     */
    Result zero(String deviceSerialNumber, String methodId, String taskId,String updateBy);

    /**
     * 保存按钮
     */
    Result<Boolean> saveButton(String taskId, String updateBy);

    /**
     * 查询新方法任务信息
     */
    Result<FMeasurementCurrentInfoVO> getInfoByTaskId(String taskId);

    /**
     * 导出到word
     */
    R<File> downloadWord(String taskId, Boolean allDisplay, Integer digit);

    /**
     * 查询可绑定的方法参数
     */
    R<List<FInputParam>> queryInputParam(String methodId);

    /**
     * 查询绑定映射关系
     */
    R<List<FInputParamMapTableHead>> queryInputParamMapTableHead(String taskId);

    /**
     * 保存映射关系及表头信息
     */
    R<Boolean> saveInputParamMapTableHead(FTaskHeadSnapshotDTO taskHeadSnapshot);

    /**
     * 导出到word
     */
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, Integer digit);

}
