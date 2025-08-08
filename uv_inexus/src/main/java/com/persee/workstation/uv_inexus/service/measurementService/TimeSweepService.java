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

    // 查询方法参数接口
    Result queryMethodParameter(String methodId);

    // 修改方法参数接口
    Result updateMethodParameter(TimeSweepMethodParameterDto params);

    // 查询样品列表接口
    Result sampleListPort(String taskId, String type,Integer  digit);

    // 删除样品列表接口
    Result deleteSampleListPort(String sampleId, String taskId);

    // 获取变换列表接口
    Result changeTheListPort(String taskId, String type,Integer  digit);

    // 删除变换列表接口
    Result deleteChangeListPort(String analysisId);

    // 恢复已删除数据接口
    Result restoreDeletedListData(String taskId, String paramId, String type);

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

    // 获取峰谷数据列表
    Result queryPeakValleyDataList(String dataId, String type, Integer digit);

    // 查询峰谷接口
    Result seekingPeaksAndValleysPort(SeekingPeaksValleysDto paramDto);

    // 添加峰谷备注接口
    Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto);

    // 查询标记点数据列表
    Result queryGaugePointDataList(String dataId, String type, Integer digit);

    // 添加标记点接口
    Result addGaugePoint(AddGaugePointDto paramDto);

    // 添加标记点备注
    Result addGaugePointRemarks(PeakValleyMarkingDto paramDto);

    // 导出Word接口
    void exportToWord(String taskId, HttpServletResponse response) throws Exception;

    // 查询运算源数据接口
    Result queryArithmeticSourceData(String taskId);

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
