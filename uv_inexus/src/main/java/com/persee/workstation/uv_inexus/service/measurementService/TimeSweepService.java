package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
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

    // 获取样本按钮列表接口
    Result querySampleList(String taskId);

    // 添加或删除样本接口
    Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto);

    // 图片上传
    Result uploadImage(SpectrumImageUpload upload);

    // 查询峰谷接口
    Result seekingPeaksAndValleysPort(SeekingPeaksValleysDto paramDto);

    Result addGaugePoint(AddGaugePointDto paramDto);
}
