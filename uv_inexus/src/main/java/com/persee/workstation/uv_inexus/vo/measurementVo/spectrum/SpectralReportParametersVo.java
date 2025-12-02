/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo.spectrum;

import lombok.Data;

// 光谱报告参数Vo类
@Data
public class SpectralReportParametersVo {
    // 重叠图片
    private byte[] overlapImage;
    // 平铺图片
    private byte[] mosaicImage;
    // 3D 图片
    private byte[] threeDImage;
    // 光谱数据
    private SpectralReport data;
}
