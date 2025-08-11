package com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep;

import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import lombok.Data;

@Data
public class QueryTimeMethodParamterVo extends TimeSweepParamter {
    private String methodId;
    private String methodName;
}
