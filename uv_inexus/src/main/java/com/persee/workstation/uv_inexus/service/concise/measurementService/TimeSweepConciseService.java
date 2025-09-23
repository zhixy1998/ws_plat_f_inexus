package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.timesweep.TimeUseMethodDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;

import java.io.File;
import java.util.List;

/**
 * 简洁模式: 时间扫描服务接口
 */
public interface TimeSweepConciseService {

    // 查询最近方法数据
    Result queryNearestMethodData(String updateBy, String measureType);

    // 查找方法按钮
    Result searchMethodButton(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName);

    // 使用方法接口
    Result use(TimeUseMethodDto paramDto);

    // 是否保存数据接口
    Result whetherSaveData(String taskId, Boolean isSave);

    // 创建草稿接口
    Result createDraftButton(String methodId, String updateBy);

    // 查询样品列表按钮
    Result querySampleListButton(String taskId);

    // 查询样本输入表头接口
    Result<List<FHeadVO>> querySampleHeads(String taskId);

    // 添加或删除样本接口
    Result addOrDeleteSamplePort(FAddOrDeleteSampleDTO paramDto);

    // 检测任务状态接口
    Result detectionTaskStatus(String taskId);

    // 保存草稿按钮接口
    Result saveDraftButton(String taskId, String taskName);

    // 查询样品数据列表
    Result querySampleDataList(String taskId, String type, Integer digit);

    // 删除样品列表接口
    Result deleteSampleListPort(String taskId, String sampleId, String analysisId);

    // 查询变换列表接口
    Result queryAnalysisDataList(String taskId, String type, Integer digit);

    // 删除变换列表接口
    Result deleteChangeListPort(String analysisId);

    // 恢复已删除数据接口
    Result restoreDeletedListData(String taskId, String sampleId, String type, String analysisId);

    // 更新数据颜色接口
    Result updateDataColor(FUpdateDataColorDto paramDto);

    // 获取标记点接口
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

    // 光度模式转换接口
    Result luminanceModeConversionPort(FLuminosityConversionDto paramDto);

    /**
     * 导出到word
     */
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds);
}
