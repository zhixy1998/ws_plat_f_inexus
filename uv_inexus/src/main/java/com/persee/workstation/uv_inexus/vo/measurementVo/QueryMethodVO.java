package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import lombok.Data;

@Data
public class QueryMethodVO extends TMeasureMethod {
    // 统计任务数
    private Long taskCount;
    // 最近任务时间
    private String lastTaskTime;
}
