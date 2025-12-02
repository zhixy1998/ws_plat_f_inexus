/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
