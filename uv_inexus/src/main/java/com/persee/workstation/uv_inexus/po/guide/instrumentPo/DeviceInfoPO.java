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

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_ins_info_temp")
public class DeviceInfoPO extends BasePO {

    @ApiModelProperty("机器ip")
    @TableField("device_ip")
    private String ip;

    @ApiModelProperty("联机方式")
    @TableField("connect_type")
    private Integer connectType;

    @ApiModelProperty("唯一标识")
    @TableField("device_id")
    private String deviceId;

    @ApiModelProperty("仪器名称")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty("仪器型号")
    @TableField("device_model")
    private String deviceModel;

    @ApiModelProperty("仪器型号名称")
    @TableField("device_model_name")
    private String deviceModelName;

    @ApiModelProperty("连接仪器的ip")
    @TableField("machine_ip")
    private String machineIp;

    @ApiModelProperty("初始化状态, 0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    @TableField("is_inited")
    private Integer isInited;

    @ApiModelProperty("是否开机,0-未开机,1-已开机,2-软关机")
    @TableField("is_dev_opened")
    private Integer isDevOpened;

    @ApiModelProperty("联机状态,0-未连接，1-联机中，2-联机成功，3-联机失败")
    @TableField("connect_status")
    private Integer connectStatus;

    @ApiModelProperty("激活状态，0-未激活，1-激活")
    @TableField("is_activated")
    private Integer isActivated;

    @ApiModelProperty("仪器当前状态")
    @TableField("device_current_status")
    private Integer deviceCurrentStatus;

    @ApiModelProperty("序列号")
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("端口号")
    @TableField("device_port")
    private Integer devicePort;

    @ApiModelProperty("附件类型")
    @TableField("attachment_type")
    private Integer attachmentType;

    @ApiModelProperty("附件名称")
    @TableField("attachment_name")
    private String attachmentName;

    @ApiModelProperty("计算公式")
    @TableField("linear_segment")
    private String linearSegment;
}
