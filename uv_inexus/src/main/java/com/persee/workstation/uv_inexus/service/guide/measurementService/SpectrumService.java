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
    // 查询测量数据接口
    Result queryMeasuredData(QueryMeasuredDataVo dataVo);

    // 查询项目任务方法
    Result queryProjectTaskMethod(String updateBy,String measureType);

    // 查询项目下所有方法
    Result queryProjectAllMethods(String projectId,  String measureForm);

    // 查询方法参数
    Result queryMethodParameter(String methodId);

    // 更新方法参数
    Result updateMethodParameter(SpectralMethodParameterDto methodDto);

    // 创建方法接口
    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter);

    // 查询测量参数接口
    Result queryMeasurementParameters(String taskId);

    // 设置参数接口
    Result setUpParametersPort(String methodId,String deviceSerialNumber);

    // 保存数据接口
    Result saveData(String taskId);

    // 查询样本头信息
    Result<List<HeadVO>> querySampleHeads(String taskId);

    // 获取样本列表
    Result querySampleList(String taskId);

    // 新增或删除样本
    Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto);

    // 删除样本
    Result deleteSamplePort(String sampleId, String taskId);

    // 获取样本列表
    Result sampleListPort(String taskId,String type,Integer digit);

    // 删除样本列表
    Result deleteSampleListPort(String taskId, String sampleId,String analysisId);

    // 获取变换列表
    Result changeTheListPort(String taskId,String type,Integer digit);

    // 删除变换列表
    Result deleteChangeListPort(String analysisId);

    // 恢复删除列表数据
    Result restoreDeletedListData(String taskId,String sampleId, String type,String analysisId);

    // 修改数据颜色接口
    Result updateDataColor(UpdateDataColorDto paramDto);

    // 获取分析标记点接口
    Result queryAnalysisGaugePoint(String analysisId);

    // 添加分析标记点接口
    Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto);

    // 查询峰谷接口
    Result queryPeakAndValleysPort(String dataId, Integer digit);

    // 寻峰寻谷接口
    Result peakSearchingPort(SeekingPeaksValleysDto paramDto);

    // 添加峰谷备注接口
    Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto);

    // 查询标记点接口
    Result queryGaugePointPort(String dataId, Integer digit);

    // 添加标记点接口
    Result addGaugePoint(AddGaugePointDto paramDto);

    // 添加标记点备注接口
    Result addGaugePointRemarks(PeakValleyMarkingDto paramDto);

    // 查询算术源数据
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

    // 测试扫描接口
    void testScan();

    // 测试开始接口
    void testStart();

    // 测试python接口
    void testPythonProt();

    // 获取预处理操作接口
    Result queryPreprocessingOperation(String methodId);

    // 获取基线接口
    Result obtainTheBaseline(String taskId,String methodId,String deviceSerialNumber);

    // 导出word接口
    void exportToWord(String taskId, HttpServletResponse response) throws Exception;

    // 测试对比接口
    Result testCompare(String taskId, SpectrumParameter spectrumParameter);

    /**
     * 导出到word
     */
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds);

    // 查询表格头
    Result<List<HeadVO>> tableHead(String taskId, String methodId, List<String> sampleIds);

    // 查询表格数据
    List<Map<String, Object>> queryTableData(String taskId, List<String> sampleIds);


}
