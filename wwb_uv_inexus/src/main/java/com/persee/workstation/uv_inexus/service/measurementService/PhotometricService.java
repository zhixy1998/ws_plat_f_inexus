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

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter);

    Result updateMethodParameter(PhotometricMethodParameterDto methodDto);

    Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);

    /**
     * 新增或删除样本
     * @param dto
     * @return
     */
    Result addOrDeleteSample(AddOrDeleteSampleDTO dto);

    Result<Boolean> deleteSample(String taskId, String sampleId);

    Result revokeDel(String taskId, String sampleId);

    Result querySampleInfo(String taskId);

    /**
     * 查询方法参数
     * @param methodId
     * @return
     */
    Result<MethodParam> queryMethodParam(String methodId);

    /**
     * 查询标准信息//暂时不需要
     * @param standardName
     * @return
     */
    @Deprecated
    Result queryStandardInfo(String standardName);

    R<PhotometricReportDataVO> getReportCount(String taskId);

    /**
     *  查询表格头
     * @param taskId
     * @param methodId
     * @return
     */

    Result<List<HeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay);

    /**
     * 查询数据信息
     * @param taskId
     * @return
     */
    Result<List<Map<String, String>>> queryData(String taskId, Boolean allDisplay);

    //暂时不用
    Result<List<PhotometricSampleAndDataVO>> queryDataInfo(String taskId);

    R<List<PhotometricCountDataInfo>> formula(List<PhotometricOriginDataInfo> vo, String sampleId, String methodId);

    /**
     * 测量及分析后数据保存，如有记录，认为是重测，做覆盖操作
     * @param vo
     * @return
     */
    Result savePhotometricMeasureData(PhotometricSampleAndDataVO vo);

    /**
     * 前端调用保存数据，用于保存相关内容，如采样时间，备注等
     * @param vo
     * @return
     */
    Result saveDataInfo(RemarkChangeSaveVO vo);

    void test(String taskId, String methodId);

    /**
     * 校零
     * @param serialNumber
     */
    Result zero(String serialNumber, String methodId);

    /**
     * 保存按钮
     */
    Result<Boolean> saveButton(String taskId, String updateBy);

    Result<MeasurementCurrentInfoVO> getInfoByTaskId(String taskId);

    R<File> downloadWord(String taskId);
}
