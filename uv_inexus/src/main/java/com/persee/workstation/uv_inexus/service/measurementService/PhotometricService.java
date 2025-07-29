package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;


import java.io.File;
import java.util.List;
import java.util.Map;

public interface PhotometricService {
    /**
     * 光度测量
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
    Result newQuerySampleInfo(String taskId);

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
    Result<List<Map<String, String>>> queryData(String taskId, Boolean allDisplay);

    /**
     * 计算公式
     */
    R<List<PhotometricCountDataInfo>> formula(List<PhotometricOriginDataInfo> vo, String sampleId, String methodId);

    /**
     * 测量及分析后数据保存，如有记录，认为是重测，做覆盖操作
     */
    Result savePhotometricMeasureData(PhotometricSampleAndDataVO vo);

    /**
     * 前端调用保存数据，用于保存相关内容，如采样时间，备注等
     */
    Result saveDataInfo(RemarkChangeSaveVO vo);

    /**
     * 校零
     */
    Result zero(String serialNumber, String methodId);

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
    R<File> downloadWord(String taskId);

}
