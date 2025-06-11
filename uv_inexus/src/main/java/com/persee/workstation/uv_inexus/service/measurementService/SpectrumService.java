package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.ExperimentMethodDto;

/**
 * 光谱扫描服务
 */
public interface SpectrumService {
    boolean newMethod(ExperimentMethodDto methodDto);
//    public boolean newMethod(ExperimentMethodDto methodDto) throws JsonProcessingException;
}
