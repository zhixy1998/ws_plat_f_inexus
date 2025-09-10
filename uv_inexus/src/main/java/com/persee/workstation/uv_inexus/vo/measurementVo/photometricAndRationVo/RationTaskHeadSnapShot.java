package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import lombok.Data;

@Data
public class RationTaskHeadSnapShot {

    TaskHeadSnapshot standHead;

    TaskHeadSnapshot unknownHead;

}
