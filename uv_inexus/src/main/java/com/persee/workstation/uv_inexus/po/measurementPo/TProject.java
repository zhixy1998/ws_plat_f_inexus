package com.persee.workstation.uv_inexus.po.measurementPo;

import lombok.Data;

@Data
public class TProject {
    /**
     * 项目id，类型为char，长度36，不为null，是主键
     */
    private String id;
    /**
     * 项目名称，类型为varchar，长度255
     */
    private String projectName;
    /**
     * 备注信息，类型为varchar，长度255
     */
    private String remark;
}