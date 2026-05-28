/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.pythonUtil;

import lombok.Data;

import java.util.List;

@Data
public class PeakReceiveInfo {
    /**
     * Code，状态码
     */
    private Integer code;
    /**
     * Differentiated Y，微分后的Y值
     */
    private List<Double> differentiatedY;
    /**
     * Message，消息
     */
    private String message;
    /**
     * Num Peaks，检测到的峰数量
     */
    private long numPeaks;
    /**
     * Peak Areas，峰面积
     */
    private List<Double> peakAreas;
    /**
     * Peak Indices，峰值在原始数组中的索引位置
     */
    private List<Long> peakIndices;
    /**
     * Peak Widths，半峰宽
     */
    private List<Double> peakWidths;
    /**
     * Peak X，峰值对应的 x 坐标
     */
    private List<Double> peakX;
    /**
     * Peak Y，峰值对应的 y 值
     */
    private List<Double> peakY;
    /**
     * Smoothed Y，平滑后的Y值
     */
    private List<Double> smoothedY;
}
