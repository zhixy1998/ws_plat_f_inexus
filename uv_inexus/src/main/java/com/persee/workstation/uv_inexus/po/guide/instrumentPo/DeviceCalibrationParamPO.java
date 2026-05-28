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
import com.persee.workstation.uv_inexus.vo.instrumentVo.CalibrationTableVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DataNameValue;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "t_ins_calibration_param", autoResultMap = true)
public class DeviceCalibrationParamPO {
    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    @ApiModelProperty("参数名称")
    @TableField(value = "param_name")
    private String paramName;

    @ApiModelProperty("参数--英文名")
    @TableField(value = "param")
    private String param;

    @ApiModelProperty("参数类型：输入框或下拉")
    @TableField(value = "param_type")
    private Integer paramType;

    @ApiModelProperty("默认值")
    @TableField(value = "default_value",  typeHandler = JacksonTypeHandler.class)
    private List<DataNameValue> defaultValue;

    @ApiModelProperty("表数据")
    @TableField(value = "table_data",  typeHandler = JacksonTypeHandler.class)
    private CalibrationTableVO tableData;
}
