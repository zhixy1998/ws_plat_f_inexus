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
package com.persee.workstation.uv_inexus.dto.softwareActivation;
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

import java.util.List;

@ApiModel("远程访问配置")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncryptedConfigDto {

    /**
     * 主键ID（手动写入的UUID）
     */
    @ApiModelProperty(value = "主键ID")
    private String id;
    /**
     * 开关up/down
     */
    @ApiModelProperty(value = "开关")
    private String switchOn="1";

    /**
     * 加密后的IP列表（二进制存储）
     */
    @ApiModelProperty(value = "加密后的IP列表")
    private List<String> encryptedIps;
}

