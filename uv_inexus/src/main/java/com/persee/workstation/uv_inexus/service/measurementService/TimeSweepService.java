package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.model.Result;

public interface TimeSweepService {
    // 查询项目任务方法接口
    Result queryProjectTaskMethod(String updateBy, String measureType);

    // 创建方法
    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, TimeSweepParamter timeSweepParamter);

    // 查询项目下的所有方法
    Result queryProjectAllMethods(String projectId, String measureForm);

    // 查询样品列表接口
    Result sampleListPort(String taskId, String type);

    // 删除样品列表接口
    Result deleteSampleListPort(String measureId);

    // 获取变换列表接口
    Result changeTheListPort(String taskId, String type);

    // 删除变换列表接口
    Result deleteChangeListPort(String analysisId);

    // 设置方法参数接口
    Result setMethodParameters(String methodId, String deviceSerialNumber);

    // 校零接口
    Result zero(String serialNumber, String methodId);
}
