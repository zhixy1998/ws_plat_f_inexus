package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_reminder_mute")
public class UserReminderMutePO {

    @TableId("id")
    @ApiModelProperty("主键")
    private String id;

    @TableField("user_name")
    @ApiModelProperty("用户名")
    private String userName;

    @TableField("reminder_type")
    @ApiModelProperty("提醒类型：1-期间核查提醒，2-标准物到期提醒")
    private Integer reminderType;

    @TableField("muted")
    @ApiModelProperty("是否屏蔽弹窗：1-屏蔽，0-恢复")
    private Integer muted;

    @TableField("mute_scope")
    @ApiModelProperty("屏蔽范围：0-下次开机恢复，1-永久屏蔽需手动恢复")
    private Integer muteScope;

    @TableField("create_time")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
