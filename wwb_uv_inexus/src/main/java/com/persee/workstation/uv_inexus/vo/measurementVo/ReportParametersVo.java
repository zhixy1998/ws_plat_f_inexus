package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import lombok.Data;

@Data
public class ReportParametersVo extends SpectrumParameter {
    private String testAPerson;

}
