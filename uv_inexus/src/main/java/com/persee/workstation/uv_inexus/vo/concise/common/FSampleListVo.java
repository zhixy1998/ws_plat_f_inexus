/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.common;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FSampleListVo {
    // 标题
    private String title;
    // 测量id
    private String analysisId;
    // 样品id
    private String sampleId;
    // 分析编号
    private Integer analysisNumber;
    // 样品位置
    private String sampleLocation;
    // 颜色
    private String color;
    // 数据
    private List<Map<String, Object>> data;
    // 原始数据
    private List<Map<String, Object>> originalData;
    // 类型(0.测量数据 1.分析数据)
    private String type;
    // deleteType
    private String deleteType;
    // 是否已测(0.未测,1.已测)
    private String isTested;
    // 峰谷差
    private String peakValleyDiff;
    // 峰间距
    private String peakInterval;
    // 是否选中(0.未选中,1.已选中)
    private String isSelected;
}