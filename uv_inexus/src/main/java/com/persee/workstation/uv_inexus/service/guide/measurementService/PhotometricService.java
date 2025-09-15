package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface PhotometricService {
    /**
     * 创建光度测量方法
     */
    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter);

    /**
     * 方法参数修改接口
     */
    Result updateMethodParameter(PhotometricMethodParameterDto methodDto);

    /**
     * 获取当前信息
     */
    Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);

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
     * 查询样品列表
     */
    Result<List<JSONObject>> newQuerySampleInfo(String taskId);

    /**
     * 查询样品的表头
     */
    Result<List<HeadVO>> querySampleHeads(String taskId);
    /**
     * 查询方法参数
     */
    Result<MethodParam> queryMethodParam(String methodId);
    /**
     * 查询报告数据
     */
    R<PhotometricReportDataVO> getReportCount(String taskId);
    /**
     *  查询表格头
     */
    Result<List<HeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay);
    /**
     * 查询数据信息
     */
    Result<List<Map<String, Object>>> queryData(QueryDataDTO queryDataDTO);

    /**
     * 计算公式
     */
    R<List<PhotometricCountDataInfo>> formula(List<PhotometricOriginDataInfo> vo, TMeasureSample sample, TMeasureTask task, PhotometricParameter parameter, Integer digit);

    /**
     * 测量及分析后数据保存，如有记录，认为是重测，做覆盖操作
     */
    R<String> savePhotometricMeasureData(PhotometricSampleAndDataVO vo);

    R<Boolean> saveCountData(PhotometricSampleAndDataVO countData, String dataId);

    /**
     * 前端调用保存数据，用于保存相关内容，如采样时间，备注等
     */
    Result saveDataInfo(SaveDataVO vo);

    /**
     * 校零
     */
    Result zero(String deviceSerialNumber, String methodId, String taskId,String updateBy);

    /**
     * 保存按钮
     */
    Result<Boolean> saveButton(String taskId, String updateBy);

    /**
     * 查询新方法任务信息
     */
    Result<MeasurementCurrentInfoVO> getInfoByTaskId(String taskId);

    /**
     * 导出到word
     */
    R<File> downloadWord(String taskId, Boolean allDisplay);

    /**
     * 查询可绑定的方法参数
     */
    R<List<InputParam>> queryInputParam(String methodId);

    /**
     * 查询绑定映射关系
     */
    R<List<InputParamMapTableHead>> queryInputParamMapTableHead(String taskId);

    /**
     * 保存映射关系及表头信息
     */
    R<Boolean> saveInputParamMapTableHead(TaskHeadSnapshotDTO taskHeadSnapshot);

}
