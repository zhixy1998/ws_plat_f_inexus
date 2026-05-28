package com.persee.workstation.uv_inexus.dto.factory_inspection;

import lombok.Data;

import java.util.List;
import java.util.Map;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
public class SheetDto {
    private String sheetName;
    private Integer firstRowNum;
    private Integer lastRowNum;
    private Map<String, Map<String, Object>> rows; // 行列映射
    private List<MergedRegionDto> mergedRegions;   // 合并区域列表
    private List<String> yellowColumns;            // 黄色高亮列名列表
}

