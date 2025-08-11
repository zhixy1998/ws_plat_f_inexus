package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;

import javax.servlet.http.HttpServletResponse;

/**
 * 光谱扫描服务
 */
public interface SpectrumService {
    Result queryMeasuredData(QueryMeasuredDataVo dataVo);

    Result queryProjectTaskMethod(String updateBy,String measureType);

    Result queryProjectAllMethods(String projectId,  String measureForm);

    Result queryMethodParameter(String methodId);

    Result updateMethodParameter(SpectralMethodParameterDto methodDto);

    Result obtainReportParameters(String taskId);

    void testPythonProt();

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter);

    Result queryMeasurementParameters(String taskId);

    Result fourOperationsPort(FourOperationsParamDto paramDto);

    Result advancedOperationsPort(AdvancedOperationsDto paramDto);

    Result luminanceModeConversionPort(LuminosityConversionDto paramDto);

    // 获取样本列表
    Result sampleListPort(String taskId,String type,Integer digit);

    // 获取变换列表
    Result changeTheListPort(String taskId,String type,Integer digit);

    Result smoothnessPort(SmoothnessDto paramDto);

    Result differentialPort(DifferentialDto paramDto);

    Result queryMeasureGaugePoint(String measureId);

    // 获取峰谷列表
    Result peakSearchingPort(SeekingPeaksValleysDto paramDto);


    Result valleySearchingPort(String measureDataId);

    Result conversionValleySearching(String measureDataId);

    Result addMeasureDataGaugePointPort(MeasureDataGaugePointDto paramDto);

    Result queryAnalysisGaugePoint(String analysisId);

    Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto);

    Result addGaugePoint(AddGaugePointDto paramDto);

    Result setUpParametersPort(String methodId,String deviceSerialNumber);

    void testScan();

    void testStart();

    Result querySampleList(String taskId);

    Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto);

    Result deleteSamplePort(String sampleId, String taskId);

    Result uploadImage(SpectrumImageUpload upload);

    Result queryTheTaskImage(String id, String type);

    Result deleteChangeListPort(String analysisId);

    Result deleteSampleListPort(String taskId, String sampleId);

    Result saveData(String taskId);

    Result queryPreprocessingOperation(String methodId);

    Result obtainTheBaseline(String taskId,String methodId,String deviceSerialNumber);

    void exportToWord(String taskId, HttpServletResponse response) throws Exception;

    Result restoreDeletedListData(String taskId,String paramId, String type);

    // 查询峰谷接口
    Result queryPeakAndValleysPort(String dataId, String type, Integer digit);

    // 查询标记点接口
    Result queryGaugePointPort(String dataId, String type, Integer digit);

    // 添加峰谷备注接口
    Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto);

    // 添加标记点备注接口
    Result addGaugePointRemarks(PeakValleyMarkingDto paramDto);
}
