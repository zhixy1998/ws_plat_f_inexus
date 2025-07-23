package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.ExperimentMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryMeasuredDataVo;
import com.persee.workstation.uv_inexus.model.Result;

/**
 * 光谱扫描服务
 */
public interface SpectrumService {
    Result queryMeasuredData(QueryMeasuredDataVo dataVo);

    Result newMethod(ExperimentMethodDto methodDto);

    Result queryProjectTaskMethod(String updateBy);

    Result queryProjectAllMethods(String projectId,  String measureForm);

    Result queryAllProject(String updateBy);

    Result queryMethodParameter(String methodId);

    Result saveMethodParameter(ExperimentMethodDto methodDto);

    Result measurement();

    void testPythonProt();

    Result queryMethodProt(String projectId);

    Result queryTaskProt(String projectId, String methodId,Integer pageNum,Integer pageSize);
//    public boolean newMethod(ExperimentMethodDto methodDto) throws JsonProcessingException;
}
