/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DeviceUsageCountDTO {

    @ApiModelProperty("起始时间")
    private LocalDate startTime;

    @ApiModelProperty("终止时间")
    private LocalDate endTime;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("用户密码")
    private String password;

    @ApiModelProperty("需要统计的仪器序列号列表")
    private List<String> deviceSerialNumber;

    @ApiModelProperty("统计类型，需要获取哪些统计信息")
    private List<Integer> countType;

    @ApiModelProperty("查询字段")
    private String search;
}
