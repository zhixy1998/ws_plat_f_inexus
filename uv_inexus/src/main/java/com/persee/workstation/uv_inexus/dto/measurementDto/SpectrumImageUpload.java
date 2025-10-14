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

package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class SpectrumImageUpload {
    // 处理数据的id
    private String id;
    // 类型 (0.重叠 1.平铺 2.3D)
    private String type;
    // 图片的base64编码
    private String image;
    // 图片类型(0.测量/分析 1.任务图片)
    private String imageType;
}
