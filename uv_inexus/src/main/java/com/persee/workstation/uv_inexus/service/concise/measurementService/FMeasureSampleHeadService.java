package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.measurementDto.SampleHeadTemplateDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSampleHead;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleTemplateVO;

import java.util.List;

/**
 * (测量样本头信息表)表服务接口
 */
public interface FMeasureSampleHeadService extends IService<FMeasureSampleHead> {

    R<Boolean> createTemplate(SampleHeadTemplateDTO templateDto);

    R<List<SampleTemplateVO>> queryTemplate(String measureType, String search);

    R<Boolean> deleteTemplate(String templateId, String updateBy);
}
