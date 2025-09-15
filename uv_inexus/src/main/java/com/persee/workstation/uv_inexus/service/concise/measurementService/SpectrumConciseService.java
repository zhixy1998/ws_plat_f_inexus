package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 简洁模式: 光谱扫描服务业务层
 */
public interface SpectrumConciseService {

    // 查询最近方法数据
    Result queryNearestMethodData(String updateBy, String measureType);

    // 查找方法按钮
    Result searchMethodButton(Integer pageNum,Integer pageSize, String updateBy, String measureType, String methodName);

    // 应用接口
    Result use(FUseMethodDto paramDto);

    // 查询样品数据列表
    Result querySampleDataList(String taskId,String type,Integer digit);

    // 删除样品列表接口
    Result deleteSampleListPort(String taskId, String sampleId, String analysisId);

    // 查询分析数据列表
    Result queryAnalysisDataList(String taskId,String type,Integer digit);

    // 删除分析列表接口
    Result deleteChangeListPort(String analysisId);

    // 恢复已删除数据接口
    Result restoreDeletedListData(String taskId, String sampleId, String type, String analysisId);

    // 更新数据颜色接口
    Result updateDataColor(FUpdateDataColorDto paramDto);

    // 获取分析标记点接口
    Result queryGaugePoint(String dataId, Integer digit);

    // 添加标记点接口
    Result addGaugePointPort(FAddGaugePointDto paramDto);

    // 添加标记点备注接口
    Result addGaugePointRemarks(FPeakValleyMarkingDto paramDto);

    // 获取峰谷接口
    Result queryPeakAndValleysPort(String dataId, Integer digit);

    // 寻峰寻谷接口
    Result peakSearchingPort(FSeekingPeaksValleysDto paramDto);

    // 添加峰谷备注接口
    Result addPeakValleyRemarks(FPeakValleyMarkingDto paramDto);

    // 设置参数接口
    Result setUpParametersPort(String methodId, String deviceSerialNumber);

    // 查询历史方法接口
    Result queryHistoryMethod(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName);

    // 查询历史任务接口
    Result queryHistoryTask(Integer pageNum, Integer pageSize, String updateBy, String measureType, String taskName);

    // 查询算术源数据接口
    Result queryArithmeticSourceData(String taskId);

    // 四则运算接口
    Result fourOperationsPort(FFourOperationsParamDto paramDto);

    // 高级运算接口
    Result advancedOperationsPort(FAdvancedOperationsDto paramDto);

    // 平滑接口
    Result smoothnessPort(FSmoothnessDto paramDto);

    // 微分接口
    Result differentialPort(FDifferentialDto paramDto);

    // 光度模式转换
    Result luminanceModeConversionPort(FLuminosityConversionDto paramDto);
}
