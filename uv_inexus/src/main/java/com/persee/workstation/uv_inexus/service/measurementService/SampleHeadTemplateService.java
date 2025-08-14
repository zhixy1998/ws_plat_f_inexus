package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.SampleHeadTemplateDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.measurementPo.SampleHeadTemplatePO;

import java.util.List;

public interface SampleHeadTemplateService {

    R<Boolean> createTemplate(SampleHeadTemplateDTO templateDto);

    R<List<SampleHeadTemplatePO>> queryTemplate(String measureType, String search);

    R<Boolean> deleteTemplate(String templateId, String updateBy);
}
