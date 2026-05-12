/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.messageVo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageReport {


    @ApiModelProperty("消息类型")
    private Integer messageType;
    @ApiModelProperty("消息类型")
    private String messageTypeName;

    @ApiModelProperty("消息来源")
    private String messageSource;

    @ApiModelProperty("接收时间")
    private LocalDateTime receiveTime;

    @ApiModelProperty("消息摘要")
    private String messageSummary;
    @ApiModelProperty("消息内容")
    @TableField(value = "message_content", typeHandler = JacksonTypeHandler.class)
    private String messageContent;
}
