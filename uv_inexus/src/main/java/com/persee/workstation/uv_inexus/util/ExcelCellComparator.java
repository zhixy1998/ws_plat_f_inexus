package com.persee.workstation.uv_inexus.util;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import com.persee.workstation.uv_inexus.dto.factory_inspection.CellDifference;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Excel工作簿单元格值比较工具
 */
@Component
public class ExcelCellComparator {

    /**
     * 比较两个Workbook的单元格值差异
     * @param workbook1 第一个工作簿
     * @param workbook2 第二个工作簿
     * @return 差异列表
     */
    public List<CellDifference> compareWorkbooks(Workbook workbook1, Workbook workbook2) {
        List<CellDifference> differences = new ArrayList<>();

        // 获取最大sheet数量
        int maxSheets = Math.max(workbook1.getNumberOfSheets(), workbook2.getNumberOfSheets());

        for (int i = 0; i < maxSheets; i++) {
            Sheet sheet1 = i < workbook1.getNumberOfSheets() ? workbook1.getSheetAt(i) : null;
            Sheet sheet2 = i < workbook2.getNumberOfSheets() ? workbook2.getSheetAt(i) : null;

            String sheetName = getSheetName(sheet1, sheet2);
            compareSheets(sheet1, sheet2, sheetName, differences);
        }

        return differences;
    }

    /**
     * 获取有效的sheet名称
     */
    private String getSheetName(Sheet sheet1, Sheet sheet2) {
        if (sheet1 != null) return sheet1.getSheetName();
        if (sheet2 != null) return sheet2.getSheetName();
        return "Unknown";
    }

    /**
     * 比较两个Sheet的单元格值
     */
    private void compareSheets(Sheet sheet1, Sheet sheet2, String sheetName, List<CellDifference> differences) {
        if (sheet1 == null && sheet2 == null) return;

        int maxRows = getMaxRowCount(sheet1, sheet2);

        for (int rowNum = 0; rowNum < maxRows; rowNum++) {
            Row row1 = getRow(sheet1, rowNum);
            Row row2 = getRow(sheet2, rowNum);

            int maxCells = getMaxCellCount(row1, row2);

            for (int cellNum = 0; cellNum < maxCells; cellNum++) {
                Cell cell1 = getCell(row1, cellNum);
                Cell cell2 = getCell(row2, cellNum);

                Object value1 = getCellValue(cell1);
                Object value2 = getCellValue(cell2);

                if (!Objects.equals(value1, value2)) {
                    differences.add(new CellDifference(sheetName, rowNum, cellNum, value1, value2));
                }
            }
        }
    }

    /**
     * 获取最大行数
     */
    private int getMaxRowCount(Sheet sheet1, Sheet sheet2) {
        int rowCount1 = sheet1 != null ? sheet1.getLastRowNum() + 1 : 0;
        int rowCount2 = sheet2 != null ? sheet2.getLastRowNum() + 1 : 0;
        return Math.max(rowCount1, rowCount2);
    }

    /**
     * 获取指定行
     */
    private Row getRow(Sheet sheet, int rowNum) {
        if (sheet == null || rowNum > sheet.getLastRowNum()) return null;
        return sheet.getRow(rowNum);
    }

    /**
     * 获取最大列数
     */
    private int getMaxCellCount(Row row1, Row row2) {
        int cellCount1 = row1 != null ? row1.getLastCellNum() : 0;
        int cellCount2 = row2 != null ? row2.getLastCellNum() : 0;
        return Math.max(cellCount1, cellCount2);
    }

    /**
     * 获取指定单元格
     */
    private Cell getCell(Row row, int cellNum) {
        if (row == null || cellNum >= row.getLastCellNum()) return null;
        return row.getCell(cellNum);
    }

    /**
     * 获取单元格的值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return null;
        }
    }
}

