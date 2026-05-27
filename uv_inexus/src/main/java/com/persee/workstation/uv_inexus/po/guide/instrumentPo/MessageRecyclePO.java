package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message_recycle")
public class MessageRecyclePO {

    @TableId("id")
    @ApiModelProperty("原消息ID")
    private String id;

    @TableField("message_type")
    @ApiModelProperty("消息类型")
    private Integer messageType;

    @TableField("message_source")
    @ApiModelProperty("消息来源")
    private String messageSource;

    @TableField("message_content")
    @ApiModelProperty("消息内容")
    private String messageContent;

    @TableField("message_status")
    @ApiModelProperty("消息状态")
    private Integer messageStatus;

    @TableField("receive_time")
    @ApiModelProperty("接收时间")
    private LocalDateTime receiveTime;

    @TableField("message_summary")
    @ApiModelProperty("消息摘要")
    private String messageSummary;

    @TableField("original_create_by")
    @ApiModelProperty("原始创建者")
    private String originalCreateBy;

    @TableField("original_update_by")
    @ApiModelProperty("原始修改者")
    private String originalUpdateBy;

    @TableField("original_create_time")
    @ApiModelProperty("原始创建时间")
    private LocalDateTime originalCreateTime;

    @TableField("deleted_time")
    @ApiModelProperty("删除时间（放入回收站的时间）")
    private LocalDateTime deletedTime;
}
