/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceInitInfo {

    @ApiModelProperty("自检任务名")
    private String initProject;

    @ApiModelProperty("初始化条目序号")
    private Integer initTaskNum;

    @ApiModelProperty(value = "自检任务状态",example = "0-进行中，1-成功，2-失败")
    private Integer initStatus;
}
