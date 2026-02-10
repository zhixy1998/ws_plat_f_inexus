/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "t_ins_calibration_config", autoResultMap = true)
public class DeviceCalibrationConfigPO {

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    @ApiModelProperty("校正名称")
    @TableField(value = "calibration_name")
    private String calibrationName;

    @ApiModelProperty("校正类型")
    @TableField(value = "calibration_type")
    private String calibrationType;

    @ApiModelProperty("参数列表")
    @TableField(value = "param_list",  typeHandler = JacksonTypeHandler.class)
    private List<Integer> paramList;

}
