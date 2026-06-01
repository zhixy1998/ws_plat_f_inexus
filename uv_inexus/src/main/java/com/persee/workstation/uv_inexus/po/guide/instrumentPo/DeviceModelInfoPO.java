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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel("设备型号信息")
@Data
@TableName(value = "t_device_init_entry", autoResultMap = true)
public class DeviceModelInfoPO {

    @ApiModelProperty("主键id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("设备种类id")
    @TableField(value = "dev_type_id")
    private Integer devTypeId;

    @ApiModelProperty("设备型号")
    @TableField(value = "dev_model")
    private String devModel;

    @ApiModelProperty("设备型号名称")
    @TableField(value = "dev_model_name")
    private String devModelName;

    @ApiModelProperty("设备名称")
    @TableField(value = "dev_name")
    private String devName;

    @ApiModelProperty("狭缝宽度")
    @TableField(value = "spacing_width", typeHandler = JacksonTypeHandler.class)
    private List<String> spacingWidth;


    @ApiModelProperty("初始化项")
    @TableField(value = "init_entry", typeHandler = JacksonTypeHandler.class)
    private List<DeviceInitEntryInfo> initEntry;

    @ApiModelProperty("滤色片波长点数组")
    @TableField(value = "filter_wavelengths", typeHandler = JacksonTypeHandler.class)
    private List<Integer>  filterWavelength;
}
