/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("仪器管理配置")
public class DeviceConfigVO {
    @ApiModelProperty("通讯端口")
    private Integer messagePort;

    @ApiModelProperty(value = "是否定时开机",example = "0-否，1-是")
    private Integer isTimeStartUp;

    @ApiModelProperty("定时开机时间")
    private String startUpTime;

}
