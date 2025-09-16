package com.persee.workstation.uv_inexus.dto.concise.timesweep;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import lombok.Data;

@Data
public class TimeUseMethodDto extends FTimeSweepParamter {
    // 方法id
    private String methodId;
    // 用户名
    private String updateBy;
    // 测量类型
    private String measureType;
    // 测量方式 (0. 向导数据库,1. 简洁数据库)
    private String type;
    // 状态
    private String state;
}
