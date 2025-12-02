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
public class SystemPanelConfVO {

    @ApiModelProperty("方法列表面板开关")
    private Boolean methodListSwitch;

    @ApiModelProperty("项目统计面板开关")
    private Boolean projectStatisticsSwitch;

    @ApiModelProperty("任务列表面板开关")
    private Boolean taskListSwitch;

    @ApiModelProperty("启用快捷登录")
    private Boolean quickLoginSwitch;

    @ApiModelProperty("默认选中快捷登录，true-选中")
    private Boolean defaultQuickLogin;

    @ApiModelProperty("自动开机")
    private Boolean isAutoBoot;

    @ApiModelProperty("自动进入实验测试")
    private Boolean isAutoEnterExperiment;
}
