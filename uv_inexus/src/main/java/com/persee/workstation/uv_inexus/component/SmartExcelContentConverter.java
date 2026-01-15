package com.persee.workstation.uv_inexus.component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.*;

/**
 * 增强版Excel内容转换器 - 支持合并单元格处理（行和列）
 */
public class SmartExcelContentConverter {

    /**
     * 主转换方法 - 返回扁平化的内容Map
     */
    public static Map<String, Object> convertToSimpleStructure(Workbook workbook) {
        FormulaEvaluator evaluator = createFormulaEvaluator(workbook);
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> worksheets = new ArrayList<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            worksheets.add(processSimpleSheet(sheet, evaluator));
        }

        result.put("worksheets", worksheets);
        return result;
    }

    /**
     * 创建公式计算器
     */
    private static FormulaEvaluator createFormulaEvaluator(Workbook workbook) {
        if (workbook instanceof XSSFWorkbook) {
            return new XSSFFormulaEvaluator((XSSFWorkbook) workbook);
        } else if (workbook instanceof org.apache.poi.hssf.usermodel.HSSFWorkbook) {
            return new HSSFFormulaEvaluator((org.apache.poi.hssf.usermodel.HSSFWorkbook) workbook);
        } else {
            return workbook.getCreationHelper().createFormulaEvaluator();
        }
    }

    /**
     * 处理单个工作表为简化结构
     */
    private static Map<String, Object> processSimpleSheet(Sheet sheet, FormulaEvaluator evaluator) {
        Map<String, Object> worksheet = new LinkedHashMap<>();

        // 确定是否为表格结构
        boolean isTableStructure = analyzeIfTableStructure(sheet);

        if (isTableStructure) {
            // 提取表头和数据
            List<String> columns = extractHeaders(sheet, evaluator);
            List<List<String>> rows = extractDataRowsWithMergedCells(sheet, evaluator, columns.size());

            worksheet.put("name", sheet.getSheetName());
            worksheet.put("columns", columns);
            worksheet.put("rows", rows);
        } else {
            // 非表格结构，返回原始数据
            List<List<String>> rawRows = extractAllRows(sheet, evaluator);
            worksheet.put("name", sheet.getSheetName());
            worksheet.put("rows", rawRows);
        }

        worksheet.put("hasMergedCells", sheet.getNumMergedRegions() > 0);

        return worksheet;
    }

    /**
     * 分析是否为表格结构
     */
    private static boolean analyzeIfTableStructure(Sheet sheet) {
        int nonEmptyRowCount = 0;
        int lastColCount = -1;
        boolean consistentColumns = true;

        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            int colCount = getNonEmptyCellCount(row);
            if (lastColCount != -1 && Math.abs(colCount - lastColCount) > 2) {
                consistentColumns = false;
            }
            lastColCount = colCount;
            nonEmptyRowCount++;

            if (nonEmptyRowCount >= 2 && consistentColumns) {
                break;
            }
        }

        return nonEmptyRowCount >= 2 && consistentColumns;
    }

    /**
     * 提取表头
     */
    private static List<String> extractHeaders(Sheet sheet, FormulaEvaluator evaluator) {
        List<String> headers = new ArrayList<>();

        // 寻找最佳表头行（通常是第一行）
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row != null && getNonEmptyCellCount(row) >= 2) {
                String headerRowValue = "";
                for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        String value = getCellValueAsString(cell, evaluator);
                        if (value.isEmpty()) {
                            value = headerRowValue;
                        }
                        headers.add(value);
                        headerRowValue = value;
                    }
                }
                break; // 找到表头后退出
            }
        }

        return headers;
    }

    /**
     * 提取数据行（支持合并单元格）
     */
    private static List<List<String>> extractDataRowsWithMergedCells(Sheet sheet, FormulaEvaluator evaluator, int expectedCols) {
        List<List<String>> rows = new ArrayList<>();

        // 从表头行之后开始提取数据
        boolean foundHeader = false;

        // 存储合并单元格信息
        Map<String, String> mergedCellValues = new HashMap<>();

        // 首先收集所有合并单元格的值
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            int firstRow = region.getFirstRow();
            int firstCol = region.getFirstColumn();
            int lastRow = region.getLastRow();
            int lastCol = region.getLastColumn();

            // 获取主单元格的值
            Row row = sheet.getRow(firstRow);
            if (row != null) {
                Cell cell = row.getCell(firstCol);
                if (cell != null) {
                    String value = getCellValueAsString(cell, evaluator);
                    // 为合并区域内的每个单元格存储相同的值
                    for (int r = firstRow; r <= lastRow; r++) {
                        for (int c = firstCol; c <= lastCol; c++) {
                            mergedCellValues.put(r + "_" + c, value);
                        }
                    }
                }
            }
        }

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            if (!foundHeader && getNonEmptyCellCount(row) >= 2) {
                foundHeader = true; // 跳过表头行
                continue;
            }

            List<String> rowData = new ArrayList<>();
            for (int j = 0; j < expectedCols; j++) {
                String value = "";

                // 检查是否在合并单元格中
                String key = i + "_" + j;
                if (mergedCellValues.containsKey(key)) {
                    value = mergedCellValues.get(key);
                } else {
                    // 不是合并单元格，正常获取值
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        value = getCellValueAsString(cell, evaluator);
                    }
                }

                rowData.add(value);
            }
            rows.add(rowData);
        }

        return rows;
    }

    /**
     * 提取所有行（用于非表格结构）
     */
    private static List<List<String>> extractAllRows(Sheet sheet, FormulaEvaluator evaluator) {
        List<List<String>> rows = new ArrayList<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            List<String> rowData = new ArrayList<>();
            for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                String value = cell != null ? getCellValueAsString(cell, evaluator) : "";
                rowData.add(value);
            }
            if (!rowData.isEmpty()) {
                rows.add(rowData);
            }
        }

        return rows;
    }

    // ==================== 工具方法 ====================

    private static int getNonEmptyCellCount(Row row) {
        if (row == null) return 0;
        int count = 0;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell, null).trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell, null).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";

        if (cell.getCellType() == CellType.FORMULA) {
            try {
                CellValue cellValue = evaluator != null ? evaluator.evaluate(cell) : null;
                return cellValue != null ? convertCellValueToString(cellValue) : "=" + cell.getCellFormula();
            } catch (Exception e) {
                return "=" + cell.getCellFormula();
            }
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 强制作为数值处理，避免误判为日期
                double numericValue = cell.getNumericCellValue();
                return formatNumber(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    private static String convertCellValueToString(CellValue cellValue) {
        if (cellValue == null) return "";

        switch (cellValue.getCellType()) {
            case STRING:
                return cellValue.getStringValue();
            case NUMERIC:
//                if (DateUtil.isValidExcelDate(cellValue.getNumberValue())) {
//                    // 正确处理日期
//                    return new Date((long) (cellValue.getNumberValue() * 24 * 60 * 60 * 1000)).toString();
//                } else {
                return formatNumber(cellValue.getNumberValue());
//                }
            case BOOLEAN:
                return String.valueOf(cellValue.getBooleanValue());
            case ERROR:
                return "#ERROR";
            default:
                return "";
        }
    }

    private static String formatNumber(double number) {
        if (number == (long) number) {
            return String.valueOf((long) number);
        } else {
            return String.valueOf(number);
        }
    }
}
