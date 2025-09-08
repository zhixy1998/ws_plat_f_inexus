package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_ins_calibration_temp")
public class CalibrationTempPO  extends BasePO {

    @ApiModelProperty("仪器序列号")
    @TableField(value = "device_serial_number")
    private String deviceSerialNumber;

    @TableField("calibration_type")
    @ApiModelProperty("校准类型")
    private Integer calibrationType;

    @ApiModelProperty("参数名")
    @TableField("param")
    private String param;

    @ApiModelProperty("参数值")
    @TableField("param_value")
    private String paramValue;
}
