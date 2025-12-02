/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.systemManageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemManageDTO {
    @ApiModelProperty("语言")
    private String language;

    @ApiModelProperty("远程控制")
    private Boolean remoteControl;

    @ApiModelProperty("远程控制数量上限")
    private Integer remoteControlNum;

    @ApiModelProperty("远程数据访问")
    private Boolean remoteDataAccess;

    @ApiModelProperty("远程数据访问用户上限")
    private Integer remoteDataAccessNum;

    @ApiModelProperty("物联代理IP配置")
    private String ipConfiguration;

    @ApiModelProperty("物联代理端口配置")
    private String portConfiguration;

    @ApiModelProperty("仪器使用模式")
    private String deviceUseMode;

    @ApiModelProperty("仪器资源自动释放时间")
    private Integer deviceResourceAutoReleaseTime;

    @ApiModelProperty("用户")
    private String updateBy;
}
