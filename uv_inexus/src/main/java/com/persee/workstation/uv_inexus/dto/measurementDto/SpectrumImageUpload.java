package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

@Data
public class SpectrumImageUpload {
    // 处理数据的id
    private String id;
    // 类型 (0.测量数据 1.分析数据///0.重叠 1.平铺 2.3D)
    private String type;
    // 图片的base64编码
    private String image;
    // 图片类型(0.测量/分析 1.任务图片)
    private String imageType;
}
