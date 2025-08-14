package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateTaskParamDto {
    private String projectId;
    private String taskName;
    private String methodId;
    private String measurementType;
    private String deviceSerialNumber;
//    private List<SpectrumSpecimenDto> specimens;

    private List<Map<String, String>> specimens;

    private List<HeadVO> heads;

    private List<HeadVO> rationStandHeads;
    private List<HeadVO> rationUnknowHeads;

    private List<Map<String, String>> rationStandData;
    private List<Map<String, String>> rationUnknowData;
}
