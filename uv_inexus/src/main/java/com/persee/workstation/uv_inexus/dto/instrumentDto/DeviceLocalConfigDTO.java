/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("仪器基础配置类")
public class DeviceLocalConfigDTO {

    @ApiModelProperty("通讯端口")
    private Integer messagePort;

    @ApiModelProperty("附件设置")
    private Integer annexSetting;

    @ApiModelProperty("光谱带宽设置")
    private Double bandWidth;

    @ApiModelProperty("是否恢复出厂设置")
    private Boolean restoreFactorySetting;

    private String updateBy;
}
