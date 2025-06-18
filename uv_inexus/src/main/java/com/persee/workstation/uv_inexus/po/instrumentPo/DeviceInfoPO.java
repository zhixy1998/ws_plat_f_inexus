package com.persee.workstation.uv_inexus.po.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("t_ins_info_temp")
public class DeviceInfoPO extends BasePO {

    @ApiModelProperty("机器ip")
    @TableField("device_ip")
    private String ip;

    @ApiModelProperty("连接仪器的ip")
    @TableField("machine_ip")
    private String machineIp;

    @ApiModelProperty("初始化状态, 0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    @TableField("is_inited")
    private Integer isInited;

    @ApiModelProperty("是否开机")
    @TableField("is_dev_opened")
    private Integer isDevOpened;

    @ApiModelProperty("联机状态,0-未连接，1-联机中，2-联机成功，3-联机失败")
    @TableField("connect_status")
    private Integer connectStatus;

    @ApiModelProperty("序列号")
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("端口号")
    @TableField("device_port")
    private Integer devicePort;
}
