/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
