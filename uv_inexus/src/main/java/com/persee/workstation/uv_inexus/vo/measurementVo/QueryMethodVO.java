package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QueryMethodVO extends TMeasureMethod {
    // 统计任务数
    private Long taskCount;
    // 最近任务时间
    private LocalDateTime lastTaskTime;
}
