package com.persee.workstation.uv_inexus.po.measurementPo;

import lombok.Data;

@Data
public class TTask {
    /**
     * 任务id，类型为char，长度36，不为null，是主键
     */
    private String id;
    /**
     * 项目id，类型为varchar，长度255
     */
    private String projectId;
    /**
     * 任务名称，类型为varchar，长度255
     */
    private String taskName;
    /**
     * 测量类型（原字段 discharge_pattern 对应注释为测量类型 ），类型为varchar，长度255
     */
    private String dischargePattern;
}