package com.persee.workstation.uv_inexus.dto.factory_inspection;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import lombok.Data;

/**
 * 单元格差异数据结构
 */
 @Data
public class CellDifference {
    private String sheetName;
    private int row;
    private int column;
    private Object value1;
    private Object value2;

    public CellDifference(String sheetName, int row, int column, Object value1, Object value2) {
        this.sheetName = sheetName;
        this.row = row;
        this.column = column;
        this.value1 = value1;
        this.value2 = value2;
    }


    @Override
    public String toString() {
        return String.format("Sheet: %s, Row: %d, Col: %d, Value1: %s, Value2: %s",
                sheetName, row, column, value1, value2);
    }
}

