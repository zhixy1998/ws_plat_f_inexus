package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SpectrumImageUpload {
    // 处理数据的id
    private String id;
    // 类型 (0.测量数据 1.分析数据)
    private String type;
    // 图片的base64编码
    private MultipartFile file;
}
