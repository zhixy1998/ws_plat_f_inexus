package com.persee.workstation.uv_inexus.dto.factory_inspection;

import lombok.Data;

import java.util.List;

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
    private String inspectionItem;      // 验收项目（如：波长最大允许误差）
    private String standardValue;       // 指标验收标准值（如：±1 nm(T500)、±2 nm(T400)）
    private List<MeasurementSubItem> subItems; // 子项目（如 656.1nm 和 486.0nm 两行实测数据）
    @Data
    public static class MeasurementSubItem {
        private List<String> measuredValues; // 实测值（通常是 3 个）
    }
}
