/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemLogSwitchVO {
    @ApiModelProperty("设备使用统计总开关")
    private Boolean statUsageTotalSwitch;

    @ApiModelProperty("仪器使用率统计开关")
    private Boolean statUsageEnabled;

    @ApiModelProperty("用户使用率统计开关")
    private Boolean statUserEnabled;

    @ApiModelProperty("检测类型分项统计开关")
    private Boolean statTypeEnabled;

    @ApiModelProperty("检测项目分项统计开关")
    private Boolean statItemEnabled;

    @ApiModelProperty("检测任务详细信息开关")
    private Boolean statDetailEnabled;

    @ApiModelProperty("仪器使用记录开关")
    private Boolean statUsageRecordEnabled;
}
