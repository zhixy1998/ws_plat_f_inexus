package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodParam;

import java.time.LocalDateTime;
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



    /**
     * 测量及分析后数据保存，如有记录，认为是重测，做覆盖操作
     * @param vo
     * @return
     */
    Result savePhotometricMeasureData(LocalDateTime startTime, LocalDateTime endTime, List<PhotometricSampleAndDataVO> vo);

    /**
     * 前端调用保存数据，用于保存相关内容，如采样时间，备注等
     * @param vo
     * @return
     */
    Result saveDataInfo(PhotometricSampleAndDataVO vo);

    /**
     * 光度测量
     * 从websocket建立连接-->获取样品-->测量-->保存数据-->关闭连接
     * socket负责连接建立，推数据，关连接，
     * service里，负责调测量，然后存数据？
     */


    void test(String taskId, String methodId);

}
