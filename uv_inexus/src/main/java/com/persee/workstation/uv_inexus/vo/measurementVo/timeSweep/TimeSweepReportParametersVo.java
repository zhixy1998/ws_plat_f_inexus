package com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep;

import lombok.Data;
// 时间报告参数Vo类
@Data
public class TimeSweepReportParametersVo {
    // 重叠图片
    private byte[] overlapImage;
    // 平铺图片
    private byte[] mosaicImage;
    // 3D 图片
    private byte[] threeDImage;
    // 光谱数据
    private TimeSweepReport data;
}
