/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author xudian
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_message_info", autoResultMap = true)
public class MessageInfoPO extends BasePO {

    @ApiModelProperty("消息类型")
    @TableField("message_type")
    private Integer messageType;

    @ApiModelProperty("消息来源")
    @TableField("message_source")
    private String messageSource;

    @ApiModelProperty("消息内容")
    @TableField("message_content")
    private String messageContent;

    @ApiModelProperty("消息状态")
    @TableField("message_status")
    private Integer messageStatus;

    @ApiModelProperty("接收时间")
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    @ApiModelProperty("消息摘要")
    @TableField("message_summary")
    private String messageSummary;
}
