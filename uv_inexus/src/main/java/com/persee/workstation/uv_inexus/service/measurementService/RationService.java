package com.persee.workstation.uv_inexus.service.measurementService;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;

import java.util.List;
import java.util.Map;

/**
 * 定量测定
 */
public interface RationService {
    Result updateMethodParameter(MethodParamsDto methodDto);

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, RationParameter rationParameter);
    /**
     * 获取当前信息
     */
    Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);
    /**
     * 查询样品列表
     */
    Result<List<SampleVO>> querySampleInfo(String taskId, String sampleKind);
    Result<List<JSONObject>> querySampleList(String taskId, String sampleKind);
    /**
     * 查询样品的表头
     */
    Result<List<HeadVO>> querySampleHeads(String taskId, String sampleKind);
    /**
     * 新增或删除样本
     */
    Result addOrDeleteSample(AddOrDeleteSampleDTO dto);
    /**
     * 删除单个样本
     */
    Result<Boolean> deleteSample(String taskId, String sampleId);
    /**
     * 撤销删除
     */
    Result revokeDel(String taskId, String sampleId);
    /**
     * 查询方法参数
     */
    Result<RationMethodParam> queryMethodParam(String methodId);
    /**
     *  查询表格头
     */
    Result<List<HeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay);
    /**
     * 查询未知样品的表头
     */
    Result<List<HeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay);

    Result saveStandSampleData(RationDataVO dataInfo);

    Result saveUnknowSampleData(RationDataVO dataInfo);

    R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, String methodId);

    Result<List<Map<String, String>>> queryStandSampleData(String taskId, Boolean allDisplay);

    Result<List<Map<String, String>>> queryUnknowSampleData(String taskId, Boolean allDisplay);

    void fittingCurve(List<RationCountDataInfo> originData, Boolean empty, String taskId);

    void queryCurveInfo(String taskId, String methodId);

}
