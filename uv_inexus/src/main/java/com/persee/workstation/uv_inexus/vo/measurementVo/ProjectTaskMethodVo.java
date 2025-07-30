package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import lombok.Data;

@Data
public class ProjectTaskMethodVo {
    private String taskId;
    private String taskName;
    private String methodId;
}
