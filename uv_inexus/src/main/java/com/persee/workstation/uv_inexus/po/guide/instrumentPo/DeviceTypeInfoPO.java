/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.WorkTimeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel("设备类型信息")
@Data
@TableName(value = "t_device_type_info", autoResultMap = true)
public class DeviceTypeInfoPO {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键id")
    private Integer id;

    @TableField(value = "dev_type")
    @ApiModelProperty("设备类型， T600/T700")
    private String devType;

    @TableField(value = "luminosity", typeHandler = JacksonTypeHandler.class)
    @ApiModelProperty("光度模式")
    private List<Integer> luminosity;

    @TableField(value = "lamp_type", typeHandler = JacksonTypeHandler.class)
    @ApiModelProperty("灯类型，钨灯，氘灯")
    private List<Integer> lampType;

    @TableField(value = "connect_mode")
    @ApiModelProperty("连接方式，0-串口，1-网口")
    private Integer connectMode;

    @ApiModelProperty("没有波长列表时的系数")
    @TableField(value = "coefficient")
    private Double coefficient;

    @ApiModelProperty("增益")
    @TableField(value = "gain", typeHandler = JacksonTypeHandler.class)
    private List<Integer> gain;

    @ApiModelProperty("响应时间")
    @TableField(value = "response_time", typeHandler = JacksonTypeHandler.class)
    private List<String> responseTime;

    @ApiModelProperty("扫描速度")
    @TableField(value = "scan_speed", typeHandler = JacksonTypeHandler.class)
    private JSONObject scanSpeed;

    @ApiModelProperty("最小波长")
    @TableField(value = "min_wavelength")
    private String minWavelength;

    @ApiModelProperty("最小换灯波长")
    @TableField(value = "min_lamp_wavelength")
    private String minLampWavelength;

    @ApiModelProperty("最大波长")
    @TableField(value = "max_wavelength")
    private String maxWavelength;

    @ApiModelProperty("最大换灯波长")
    @TableField(value = "max_lamp_wavelength")
    private String maxLampWavelength;

    @ApiModelProperty("扫描间隔")
    @TableField(value = "scan_interval", typeHandler = JacksonTypeHandler.class)
    private List<String> scanInterval;

    @ApiModelProperty("校正方式")
    @TableField(value = "calibration_type", typeHandler = JacksonTypeHandler.class)
    private List<Integer> calibrationType;

    @ApiModelProperty("使用时长统计指令")
    @TableField(value = "work_time_count", typeHandler = JacksonTypeHandler.class)
    private List<WorkTimeInfo>  workTimeCount;
}
