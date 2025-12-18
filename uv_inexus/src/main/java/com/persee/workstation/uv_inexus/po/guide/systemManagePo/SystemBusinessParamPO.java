/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_system_business_param")
@ApiModel("系统业务参数")
public class SystemBusinessParamPO extends BasePO {

    @ApiModelProperty("标准曲线配置, 1-显示R,2-显示R2")
    @TableField("standard_curve_conf")
    private String standardCurveConf;

    @ApiModelProperty("显示已删除")
    @TableField("show_deleted")
    private Boolean showDeleted;

    @ApiModelProperty("显示样品测量时间")
    @TableField("show_measure_time")
    private Boolean showMeasureTime;

    @ApiModelProperty("自动保存开关")
    @TableField("auto_save_switch")
    private Boolean autoSaveSwitch;

    @ApiModelProperty("自动保存间隔")
    @TableField("auto_save_time")
    private String autoSaveTime;

    @ApiModelProperty("自动登出开关")
    @TableField("auto_leave_switch")
    private Boolean autoLeaveSwitch;

    @ApiModelProperty("自动登出时间")
    @TableField("auto_leave_time")
    private String autoLeaveTime;

    @TableField("exe_file_path")
    @ApiModelProperty("exe文件路径")
    private String filePath;

    @ApiModelProperty("Abs显示位数")
    @TableField("abs_digits")
    private Integer absDigits;

    @ApiModelProperty("T%、R%显示位数")
    @TableField("t_digits")
    private Integer tDigits;

    @ApiModelProperty("E显示位数")
    @TableField("e_digits")
    private Integer eDigits;

    @ApiModelProperty("浓度显示位数")
    @TableField("concentration_digits")
    private Integer concentrationDigits;

    @ApiModelProperty("SD和RSD显示位数")
    @TableField("sd_digits")
    private Integer sdDigits;

    @ApiModelProperty("K显示位数")
    @TableField("k_digits")
    private Integer kDigits;

    @ApiModelProperty("R/R平方显示位数")
    @TableField("r_square_digits")
    private Integer rSquareDigits;

    @ApiModelProperty("浓度负值是否显示未检出")
    @TableField("is_negative_show")
    private Boolean isNegativeShow;




}
