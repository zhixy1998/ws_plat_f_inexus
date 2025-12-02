/*
 * *****************************************************************************************************
 * Module name:    t800_uv_inexus
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:    加密配置实体类
 *
 * *****************************************************************************************************
 */
package com.persee.workstation.uv_inexus.po.guide.softwareActivation;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@ApiModel("远程访问配置实体表")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "encrypted_config")
public class EncryptedConfigPO extends BasePO {

    /**
     * 主键ID（手动写入的UUID）
     */
    @TableId(value = "id", type = IdType.INPUT)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /**
     * 加密后的开关状态
     */
    @TableField(value = "is_enabled")
    @ApiModelProperty(value = "加密后的开关状态")
    private String isEnabled;

    /**
     * 加密后的IP列表（二进制存储）
     */
    @TableField(value = "encrypted_ips")
    @ApiModelProperty(value = "加密后的IP列表")
    private byte[] encryptedIps;

    /**
     * 加密后的最大数量
     */
    @TableField(value = "encrypted_maxcount")
    @ApiModelProperty(value = "加密后的最大数量")
    private byte[] encryptedMaxcount;
}

