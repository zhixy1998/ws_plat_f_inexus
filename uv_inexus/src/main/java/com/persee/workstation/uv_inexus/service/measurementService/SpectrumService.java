package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 光谱扫描服务
 */
public interface SpectrumService {
    Result queryMeasuredData(QueryMeasuredDataVo dataVo);

    Result queryProjectTaskMethod(String updateBy);

    Result queryProjectAllMethods(String projectId,  String measureForm);

    Result queryMethodParameter(String methodId);

    Result updateMethodParameter(SpectralMethodParameterDto methodDto);

    Result obtainReportParameters(String methodId, String updateBy);

    void testPythonProt();

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter);

    Result queryMeasurementParameters(String taskId);

    Result fourOperationsPort(FourOperationsParamDto paramDto);

    Result advancedOperationsPort(AdvancedOperationsDto paramDto);

    Result luminanceModeConversionPort(LuminosityConversionDto paramDto);

    Result sampleListPort(String taskId,String type);

    Result changeTheListPort(String taskId,String type);

    Result smoothnessPort(SmoothnessDto paramDto);

    Result differentialPort(DifferentialDto paramDto);

    Result queryMeasureGaugePoint(String measureId);

    Result peakSearchingPort(String measureDataId);

    Result conversionPeakSearching(String dataId);

    Result valleySearchingPort(String measureDataId);

    Result conversionValleySearching(String measureDataId);

    Result addMeasureDataGaugePointPort(MeasureDataGaugePointDto paramDto);

    Result queryAnalysisGaugePoint(String analysisId);

    Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto);

    Result addGaugePoint(AddGaugePointDto paramDto);

    void testSetUpParameters();

    void testZeroCalibration();

    void testScan();

    void testStart();

    Result querySampleList(String taskId);

    Result addSamplePort(AddSampleDto paramDto);

    Result deleteSamplePort(String sampleId, String taskId);
}
