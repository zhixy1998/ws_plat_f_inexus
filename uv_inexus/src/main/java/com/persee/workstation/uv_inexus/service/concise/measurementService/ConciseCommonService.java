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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;

/**
 * 简洁模式: 通用服务
 */
public interface ConciseCommonService {

    // 获取默认参数
    Result queryDefaultParameters(String measureType);

    // 查找方法按钮
    Result<FMethodListWithPageVO> searchMethodButton(FQueryMethodWithPageDto dto);
    // 查询保存状态
    Result querySaveState(String taskId);

    Result<Integer> queryDeviceStatus(String deviceSerialNumber);

    // 保存草稿按钮
    Result saveDraftButton(String taskId);

    Result<Page<FMeasureMethodParam>> queryHistoryMethod(QueryMethodDto queryMethodDto);

    // 查询方法参数
    Result queryMethodParameter(String methodId);

    Result<Page<FMeasureTaskDTO>> queryHistoryTask(SelectTaskDto selectTaskDto);

    Result<String> updateMethod(UpdateMethodDto updateMethodDto);

    // 修改任务名称
    Result updateTask(UpdateTaskDto updateTaskDto);

    /**
     * 光度和定量方法里输入参数校验
     */
    Result<Boolean> checkSampleInputParam(FCheckSampleInputParamDto checkSampleInputParamDto);
    /*
    光度和定量方法里公式校验
     */
    Result<Boolean> checkFormula(FCheckFormulaDto checkFormulaDto);

    // 获取报告参数
    Result obtainReportParameters(String taskId);


    // 获取报告所有参数
    Result queryReportAllParameters(String taskId, String sampleId, String serialNumber);

    // 新的报表数据查询
    Result newReportTableDataQuery(String taskId, String sampleId,String analysisId);

    // 是否添加样品
    Result isAddSample(String taskId,String deviceSerialNumber);

    Result<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber);
    //删除方法
    Result<String> delMethod(String methodId);

    Result delTask(String taskId);
    //另存为方法
    Result<String> copy(CopyParam copyParam);
}
