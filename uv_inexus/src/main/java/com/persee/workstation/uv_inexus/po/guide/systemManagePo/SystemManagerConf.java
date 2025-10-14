/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.persee.workstation.uv_inexus.vo.systemManageVo.ConfigVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.LongDistanceConfigVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemManagerConf {
    @ApiModelProperty("语言")
    private String language;
    @ApiModelProperty("认证物联代理配置信息")
    private ConfigVO config;
    @ApiModelProperty("远程访问配置")
    private LongDistanceConfigVO longDistanceConfigVO;
    @ApiModelProperty("经典模式或者向导模式开关  1：经典模式 2：向导模式")
    private Integer modeType;

}
