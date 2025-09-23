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
    /**
     * 查询测量数据
     * @param dataVo 查询测量数据的参数对象
     * @return 返回查询结果
     */
    Result queryMeasuredData(QueryMeasuredDataVo dataVo);

    /**
     * 查询项目任务方法
     * @param updateBy 更新人
     * @param measureType 测量类型
     * @return 返回查询结果
     */
    Result queryProjectTaskMethod(String updateBy,String measureType);

    /**
     * 查询项目所有方法
     * @param projectId 项目ID
     * @param measureForm 测量形式
     * @return 返回查询结果
     */
    Result queryProjectAllMethods(String projectId,  String measureForm);

    /**
     * 查询方法参数
     * @param methodId 方法ID
     * @return 返回查询结果
     */
    Result queryMethodParameter(String methodId);

    /**
     * 更新方法参数
     * @param methodDto 方法参数数据传输对象
     * @return 返回操作结果
     */
    Result updateMethodParameter(SpectralMethodParameterDto methodDto);

    /**
     * 测试Python协议
     */
    void testPythonProt();

    /**
     * 创建方法端口
     * @param methodDto 方法参数请求对象
     * @param updateBy 更新人
     * @param spectrumParameter 光谱参数
     * @return 返回操作结果
     */
    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter);

    /**
     * 查询测量参数
     * @param taskId 任务ID
     * @return 返回查询结果
     */
    Result queryMeasurementParameters(String taskId);

    /**
     * 四则运算端口
     * @param paramDto 四则运算参数对象
     * @return 返回运算结果
     */
    Result fourOperationsPort(FourOperationsParamDto paramDto);

    /**
     * 高级运算端口
     * @param paramDto 高级运算参数对象
     * @return 返回运算结果
     */
    Result advancedOperationsPort(AdvancedOperationsDto paramDto);

    /**
     * 亮度模式转换端口
     * @param paramDto 亮度转换参数对象
     * @return 返回转换结果
     */
    Result luminanceModeConversionPort(LuminosityConversionDto paramDto);

    // 获取样本列表
    Result sampleListPort(String taskId,String type,Integer digit);

    // 获取变换列表
    Result changeTheListPort(String taskId,String type,Integer digit);

    // 平滑度接口
    Result smoothnessPort(SmoothnessDto paramDto);

    // 获取差分列表
    Result differentialPort(DifferentialDto paramDto);

    // 获取测量点列表
    Result queryMeasureGaugePoint(String measureId);

    // 获取峰谷列表
    Result peakSearchingPort(SeekingPeaksValleysDto paramDto);

    // 获取谷列表
    Result valleySearchingPort(String measureDataId);

    // 获取转换谷列表
    Result conversionValleySearching(String measureDataId);

    // 新增测量点
    Result addMeasureDataGaugePointPort(MeasureDataGaugePointDto paramDto);

    Result queryAnalysisGaugePoint(String analysisId);

    Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto);

    Result addGaugePoint(AddGaugePointDto paramDto);

    Result setUpParametersPort(String methodId,String deviceSerialNumber);

    void testScan();

    void testStart();

    // 获取样本列表
    Result querySampleList(String taskId);

    // 新增或删除样本
    Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto);

    // 删除样本
    Result deleteSamplePort(String sampleId, String taskId);

    Result uploadImage(SpectrumImageUpload upload);

/**
 * 根据任务ID和类型查询任务图片
 * @param id 任务ID，用于唯一标识一个任务
 * @param type 图片类型，用于区分不同类型的图片
 * @return 返回查询结果，包含图片信息或错误信息
 */
    Result queryTheTaskImage(String id, String type);

    Result deleteChangeListPort(String analysisId);

    // 删除变换列表
    Result deleteSampleListPort(String taskId, String sampleId,String analysisId);

    Result saveData(String taskId);

    Result queryPreprocessingOperation(String methodId);

    Result obtainTheBaseline(String taskId,String methodId,String deviceSerialNumber);

    void exportToWord(String taskId, HttpServletResponse response) throws Exception;

    // 恢复删除列表数据
    Result restoreDeletedListData(String taskId,String sampleId, String type,String analysisId);

    // 查询峰谷接口
    Result queryPeakAndValleysPort(String dataId, Integer digit);

    // 查询标记点接口
    Result queryGaugePointPort(String dataId, Integer digit);

    // 添加峰谷备注接口
    Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto);

    // 添加标记点备注接口
    Result addGaugePointRemarks(PeakValleyMarkingDto paramDto);

    // 修改数据颜色接口
    Result updateDataColor(UpdateDataColorDto paramDto);

    // 查询样本头信息
    Result<List<HeadVO>> querySampleHeads(String taskId);

    // 测试对比接口
    Result testCompare(String taskId, SpectrumParameter spectrumParameter);

    /**
     * 导出到word
     */
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds);

    // 查询表格头
    Result<List<HeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay);

    // 查询表格数据
    List<Map<String, Object>> queryTableData(String taskId, String sampleId);
}
