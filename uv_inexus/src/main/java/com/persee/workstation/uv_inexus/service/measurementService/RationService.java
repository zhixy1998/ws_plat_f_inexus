package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricAndRationTableHeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodParam;

/**
 * 定量测定
 */
public interface RationService {
    Result updateMethodParameter(MethodParamsDto methodDto);

    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, RationParameter rationParameter);

    Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);

    Result querySampleInfo(String taskId);

    /**
     * 新增或删除样本
     * @param dto
     * @return
     */
    Result addOrDeleteSample(AddOrDeleteSampleDTO dto);

    Result<Boolean> deleteSample(String taskId, String sampleId);

    Result revokeDel(String taskId, String sampleId);

    /**
     * 查询方法参数
     * @param methodId
     * @return
     */
    Result<MethodParam> queryMethodParam(String methodId);

    /**
     *  查询表格头
     * @param taskId
     * @param methodId
     * @return
     */
    Result<PhotometricAndRationTableHeadVO> getTableHead(String taskId, String methodId);

}
