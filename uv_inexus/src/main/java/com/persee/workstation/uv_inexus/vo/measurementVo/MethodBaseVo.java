package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import lombok.Data;

@Data
public class MethodBaseVo extends TMeasureMethod {
    // 最近使用时间
    private String recentUsageTime;
    // 创建时间
    private String createTime;
}
