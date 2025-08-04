package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;

import javax.servlet.http.HttpServletResponse;

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
    Result deleteSampleListPort(String sampleId, String taskId);

    // 获取变换列表接口
    Result changeTheListPort(String taskId, String type);

    // 删除变换列表接口
    Result deleteChangeListPort(String analysisId);

    // 设置方法参数接口
    Result setMethodParameters(String methodId, String deviceSerialNumber);

    // 校零接口
    Result zero(String taskId,String serialNumber, String methodId);

    // 获取样本按钮列表接口
    Result querySampleList(String taskId);

    // 添加或删除样本接口
    Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto);

    // 图片上传
    Result uploadImage(SpectrumImageUpload upload);

    // 查询峰谷接口
    Result seekingPeaksAndValleysPort(SeekingPeaksValleysDto paramDto);

    // 添加标记点接口
    Result addGaugePoint(AddGaugePointDto paramDto);

    // 导出Word接口
    void exportToWord(String taskId, HttpServletResponse response) throws Exception;

    // 四则运算接口
    Result fourOperationsPort(FourOperationsParamDto paramDto);

    // 高级运算接口
    Result advancedOperationsPort(AdvancedOperationsDto paramDto);

    // 平滑接口
    Result smoothnessPort(SmoothnessDto paramDto);

    // 微分接口
    Result differentialPort(DifferentialDto paramDto);

    // 光度模式转换接口
    Result luminanceModeConversionPort(LuminosityConversionDto paramDto);

    // 保存数据接口
    Result saveData(String taskId);
}
