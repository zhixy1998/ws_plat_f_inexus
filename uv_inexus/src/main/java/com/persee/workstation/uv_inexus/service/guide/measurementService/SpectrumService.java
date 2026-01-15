/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 光谱扫描服务接口
 * 该接口定义了光谱扫描相关的各种操作方法，包括数据查询、方法管理、参数处理等功能
 */
public interface SpectrumService {

    // 查询项目任务方法接口
    Result queryProjectTaskMethod(String updateBy,String measureType);

    // 查询项目下的所有方法
    Result queryProjectAllMethods(String projectId,  String measureForm);

    // 查询方法参数接口
    Result queryMethodParameter(String methodId);

    // 方法参数修改接口
    Result updateMethodParameter(SpectralMethodParameterDto methodDto);

    // 查询测量数据列表接口
    Result queryMeasuredData(QueryMeasuredDataVo dataVo);

    // 查询测量参数接口
    Result queryMeasurementParameters(String taskId);

    // 设置参数接口
    Result setUpParametersPort(String methodId,String deviceSerialNumber);

    // 保存数据接口
    Result saveData(String taskId);

    // 获取样本输入表头接口
    Result<List<HeadVO>> querySampleHeads(String taskId);

    // 获取样本按钮列表接口
    Result querySampleList(String taskId);

    // 添加或删除样本接口
    Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto);

    // 删除样本接口
    Result deleteSamplePort(String sampleId, String taskId);

    // 获取样本列表接口
    Result sampleListPort(String taskId,String type,Integer digit);

    // 删除样品列表接口
    Result deleteSampleListPort(String taskId, String sampleId,String analysisId);

    // 获取变换列表接口
    Result changeTheListPort(String taskId,String type,Integer digit);

    // 删除变换列表接口
    Result deleteChangeListPort(String analysisId);

    // 恢复已删除数据接口
    Result restoreDeletedListData(String taskId,String sampleId, String type,String analysisId);

    // 更新数据颜色接口
    Result updateDataColor(UpdateDataColorDto paramDto);

    // 获取分析标记点接口
    Result queryAnalysisGaugePoint(String analysisId);

    // 添加分析标记点接口
    Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto);

    // 获取峰谷接口
    Result queryPeakAndValleysPort(String dataId, Integer digit);

    // 寻峰寻谷接口
    Result peakSearchingPort(SeekingPeaksValleysDto paramDto);

    // 添加峰谷备注接口
    Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto);

    // 获取标记点接口
    Result queryGaugePointPort(String dataId, Integer digit);

    // 添加标记点接口
    Result addGaugePoint(AddGaugePointDto paramDto);

    // 添加标记点备注接口
    Result addGaugePointRemarks(PeakValleyMarkingDto paramDto);

    // 查询运算源数据接口
    Result queryArithmeticSourceData(String taskId);

    // 四则运算接口
    Result fourOperationsPort(FourOperationsParamDto paramDto);

    // 高级运算接口
    Result advancedOperationsPort(AdvancedOperationsDto paramDto);

    // 平滑度接口
    Result smoothnessPort(SmoothnessDto paramDto);

    // 微分接口
    Result differentialPort(DifferentialDto paramDto);

    // 光度模式转换
    Result luminanceModeConversionPort(LuminosityConversionDto paramDto);

    // 图片上传
    Result uploadImage(SpectrumImageUpload upload);

    // 获取任务图片接口
    Result queryTheTaskImage(String id, String type);

    // 查询预处理操作接口
    Result queryPreprocessingOperation(String methodId);

    // 获取基线接口
    Result obtainTheBaseline(String taskId,String methodId,String deviceSerialNumber);

    // 导出word接口
    void exportToWord(String taskId, HttpServletResponse response) throws Exception;

    // 测试对比接口
    Result testCompare(String taskId, SpectrumParameter spectrumParameter);

    // 多格式文件下载接口（支持word/excel/txt）
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds);

    // (实验测试)创建方法接口
    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter);

    // (实验测试)查询表格头
    Result<List<HeadVO>> tableHead(String taskId, String methodId, List<String> sampleIds);

    // (实验测试)查询表格数据
    List<Map<String, Object>> queryTableData(String taskId, List<String> sampleIds);

    // 仪器设置参数
    boolean setInstrumentParameters(String deviceSerialNumber, SetParam setParam);
}
