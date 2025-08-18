package com.persee.workstation.uv_inexus.po.softwareActivation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.persee.workstation.uv_inexus.model.BasePO;
import com.persee.workstation.uv_inexus.utils.serializer.NullToDashSerializer;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 软件激活相关实体类，对应包含设备序列号、设备IP等字段的表
 */
@EqualsAndHashCode(callSuper = true)
@Data
// 请根据实际数据库表名修改此处
@TableName("t_software_activation") 
public class SoftwareActivationPO extends BasePO {

    @TableId(type = IdType.AUTO) // 自增主键
    @ApiModelProperty("ID")
    private String id;
 
    // 设备序列号
    @TableField("device_number") 
    private String deviceNumber;

    // 设备IP
    @TableField("device_ip") 
    private String deviceIp;

    // 类型
    @JsonSerialize(using = NullToDashSerializer.class)
    @TableField("type") 
    private String type;

    // 激活状态
    @TableField("activation_status") 
    private int activationStatus;

    // 激活时间，假设数据库中是datetime类型，这里使用Date类型，也可根据实际情况调整为LocalDateTime
    @JsonSerialize(using = NullToDashSerializer.class)
    @TableField("activation_time") 
    private String activationTime;

    // 修改时间，假设数据库中是datetime类型，这里使用Date类型，也可根据实际情况调整为LocalDateTime
    @JsonSerialize(using = NullToDashSerializer.class)
    @TableField("modification_time") 
    private String modificationTime;

    // 过期时间
    @JsonSerialize(using = NullToDashSerializer.class)
    @TableField("expiration_date") 
    private String expirationDate;

    // 附加功能
    @JsonSerialize(using = NullToDashSerializer.class)
    @TableField("additional_functions") 
    private String additionalFunctions;

    @JsonSerialize(using = NullToDashSerializer.class)
    @TableField("is_add_status")
    private int isAddStatus;
}