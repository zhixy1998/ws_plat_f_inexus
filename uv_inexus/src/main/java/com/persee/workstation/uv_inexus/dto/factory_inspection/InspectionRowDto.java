package com.persee.workstation.uv_inexus.dto.factory_inspection;

import lombok.Data;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
public class InspectionRowDto {
    private FieldItem wavelength_mpe;   // 波长最大允许误差值
    private FieldItem wl_rep;           // 波长重复性
    private FieldItem bsl_stab;         // 基线稳定性
    private FieldItem stab;             // 稳定性
    private FieldItem noise;            // 噪声
    private FieldItem stray_lt;         // 杂散光
    private FieldItem photo_acc;        // 光度准确度

    @Data
    public static class FieldItem {
        private String label;           // 验收项目（表头：验收项目）
        private String standardValue;   // 指标验收标准值（表头：指标验收标准值）
        private Object measuredValue;   // 验收实测值（单个值为 String，多个值为 List<String>）
    }
}
