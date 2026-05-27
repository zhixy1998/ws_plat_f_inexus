/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.messageDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xudian
 */
@Data
public class MessageInfoQueryDTO {

    @ApiModelProperty(value = "当前页码")
    private Integer pageNum;
    @ApiModelProperty(value = "每页显示数量")
    private Integer pageSize;
    @ApiModelProperty(value = "消息类型")
    private Integer messageType;
    @ApiModelProperty(value = "消息状态")
    private Integer messageStatus;
    @ApiModelProperty(value = "消息来源")
    private String messageSource;
    @ApiModelProperty(value = "用户名（开启用户隔离后生效）")
    private String userName;

    @ApiModelProperty("查询条件")
    private String query;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;


}
