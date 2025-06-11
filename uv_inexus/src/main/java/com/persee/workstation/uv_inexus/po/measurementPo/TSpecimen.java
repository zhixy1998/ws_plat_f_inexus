package com.persee.workstation.uv_inexus.po.measurementPo;

import lombok.Data;

@Data
public class TSpecimen {
    /**
     * 样品id，类型为char，长度36，不为null，是主键
     */
    private String id;
    /**
     * 任务id，类型为varchar，长度255
     */
    private String taskId;
    /**
     * 样品名称，类型为varchar，长度255
     */
    private String specimenName;
    /**
     * 样品位置，类型为varchar，长度255；注意原字段名拼写可能有误，可根据实际情况确认是否为 specimen_location
     */
    private String specimeLocation;

}