package com.persee.workstation.uv_inexus.po.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_ins_calibration_temp")

public class CalibrationTempPO {

    @TableField("id")
    private Integer id;

    @TableField("calibration_type")
    private Integer calibrationType;

    @TableField("param")
    private String param;

    @TableField("param_value")
    private String paramValue;
}
