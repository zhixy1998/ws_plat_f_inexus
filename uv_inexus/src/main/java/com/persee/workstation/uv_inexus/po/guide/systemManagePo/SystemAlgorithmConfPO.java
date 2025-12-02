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
@ApiModel("系统算法配置")
@TableName("t_system_algorithm_conf")
public class SystemAlgorithmConfPO extends BasePO {

    @ApiModelProperty("透过率门限系数k")
    @TableField("transmittance_k")
    private String transmittance;

    @ApiModelProperty("积分球门限系数k")
    @TableField("integrating_sphere_k")
    private String integratingSphere;

    @ApiModelProperty("Abs门限系数k")
    @TableField("abs_k")
    private String abs;

    @ApiModelProperty("峰间距")
    @TableField("peak_interval")
    private String peakInterval;

    @ApiModelProperty("峰谷差")
    @TableField("peak_valley_diff")
    private String peakValleyDiff;
}
