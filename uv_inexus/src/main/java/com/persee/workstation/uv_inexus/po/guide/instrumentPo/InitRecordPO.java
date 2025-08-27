package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("t_ins_init_temp")
public class InitRecordPO extends BasePO {

    @ApiModelProperty("仪器信息id")
    @TableField("device_info_id")
    private String deviceInfoId;

    @ApiModelProperty("序列号")
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("初始化条目")
    @TableField("init_task_num")
    private Integer initTaskNum;

    @ApiModelProperty("初始化状态,0-进行中，1-成功，2-失败")
    @TableField("init_status")
    private Integer initStatus;
}
