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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_int_injection_tray_param")
@ApiModel("进样盘参数设置")
public class InjectionTrayParamPO extends BasePO {

    @TableField(value = "device_serial_number")
    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("盘区设置, L,M,R")
    @TableField(value = "panel")
    private String panel;

    @ApiModelProperty("选中盘架与否，0-否，1-是")
    @TableField(value = "is_selected")
    private Integer isSelected;

    @ApiModelProperty("盘架设定")
    @TableField(value = "plate_rail")
    private Integer plateRail;

    @ApiModelProperty("编号信息")
    @TableField(value = "num_info")
    private String numInfo;


}
