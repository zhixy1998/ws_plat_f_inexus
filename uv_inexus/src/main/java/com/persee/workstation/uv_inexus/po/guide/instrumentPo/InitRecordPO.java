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
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_ins_init_temp", autoResultMap = true)
public class InitRecordPO extends BasePO {

    @ApiModelProperty("仪器信息id")
    @TableField("device_info_id")
    private String deviceInfoId;

    @ApiModelProperty("序列号")
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("初始化序号")
    @TableField("init_task_num")
    private Integer initTaskNum;

    @ApiModelProperty("初始化状态,0-进行中，1-成功，2-失败, 3-待自检")
    @TableField("init_status")
    private Integer initStatus;

    @ApiModelProperty("初始化指令id")
    @TableField(value = "init_cmd_id", typeHandler = JacksonTypeHandler.class)
    private List<String> initCmdIdList;

    @ApiModelProperty("初始化指令名称")
    @TableField("init_cmd_name")
    private String initCmdName;

    @ApiModelProperty("初始化指令值")
    @TableField("init_cmd_value")
    private String initCmdValue;
}
