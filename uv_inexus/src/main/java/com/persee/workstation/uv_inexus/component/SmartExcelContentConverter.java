package com.persee.workstation.uv_inexus.component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

/**
 * 增强版Excel内容转换器 - 支持合并单元格处理（行和列）
 */
@Component
public class SmartExcelContentConverter {

    private static final int DEFAULT_DECIMAL_PLACES = 4;

    @Autowired
    private ExcelCellLocator excelCellLocator;

// ... existing code ...

    private List<Map<String, Object>> processSimpleSheet(Sheet sheet, FormulaEvaluator evaluator) {

        Map<String, Object> worksheet = new LinkedHashMap<>();

        // 确定是否为表格结构
        boolean isTableStructure = analyzeIfTableStructure(sheet);

        String sheetName = sheet.getSheetName();
        if (sheetName.contains("透射比示值误差与重复性校验")) {
            ArrayList<Map<String, Object>> list = new ArrayList<>();

            Map<String, Object> worksheet1 = new LinkedHashMap<>();

            if (isTableStructure) {
                // 提取表头和数据
                List<String> columns = extractHeaders(sheet, evaluator);
                HashMap<String, Object> hashMap = extractDataRowsWithMergedCells(sheet, evaluator, columns.size());
                List<List<String>> rows = (List<List<String>>) MapUtils.getObject(hashMap, "rows");
                HashSet<String> firstColumn = (HashSet<String>) MapUtils.getObject(hashMap, "firstColumn");
                if (firstColumn.contains("紫外分光光度计标准物质GBW(E)130066")) {
                    worksheet.put("name", sheet.getSheetName() + "紫外区");

                    HashMap<String, Object> objectHashMap = new HashMap<>();

                    // 构建合格线结构 - 每个项目对应其合格标准和相关数据行
                    HashMap<String, Object> qualifiedMap = new HashMap<>();
                    if (CollectionUtils.isNotEmpty(firstColumn)) {
                        for (String s : firstColumn) {
                            int i = 0;
                            List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, WAVELENGTH_ACCURACY.getDescription(), s);
                            List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, WAVELENGTH_REPEATABILITY.getDescription(), s);

                            if (CollectionUtils.isEmpty(positions1) || CollectionUtils.isEmpty(positions)) {
                                positions = excelCellLocator.findCellPositions(sheet, TRANSMITTANCE_ACCURACY.getDescription(), s);
                                positions1 = excelCellLocator.findCellPositions(sheet, TRANSMITTANCE_REPEATABILITY.getDescription(), s);
                                i++;
                            }
                            if (CollectionUtils.isEmpty(positions1) || CollectionUtils.isEmpty(positions)) {
                                positions = excelCellLocator.findCellPositions(sheet, INDICATOR_REQUIREMENT.getDescription(), s);
                                i++;
                            }
                            if (CollectionUtils.isEmpty(positions1) && CollectionUtils.isEmpty(positions)) {
                                positions = excelCellLocator.findCellPositions(sheet, ERROR_RANGE.getDescription(), s);
                                i++;
                            }
                            if (CollectionUtils.isEmpty(positions1) && CollectionUtils.isEmpty(positions)) {
                                positions = excelCellLocator.findCellPositions(sheet, VERTICAL_DISTANCE_RANGE.getDescription(), s);
                                i++;
                            }

                            String qualifiedLine = "";
                            String value = "";
                            String value1 = "";
                            if (CollectionUtils.isNotEmpty(positions) || CollectionUtils.isNotEmpty(positions1)) {
                                if (CollectionUtils.isNotEmpty(positions)) {
                                    value = formatNumericValue(positions.get(0).getValue());
                                }
                                if (CollectionUtils.isNotEmpty(positions1)) {

                                    value1 = formatNumericValue(positions1.get(0).getValue());
                                }
                                if (i == 0) {
                                    qualifiedLine = "波长示值误差" + "±" + value +"nm；" + "波长重复性" + "≤" + value1+"nm；";
                                } else if (i == 1) {
                                    qualifiedLine = "透射比示值误差" + "±" + value + "nm；" + "透射比重复性" + "≤" + value1+"nm；";
                                } else if (i == 2) {
                                    qualifiedLine = "指标要求" + "：" + value+"nm；";
                                } else if (i == 3) {
                                    qualifiedLine = "误差范围（%）" + "±" + value+"nm；";
                                } else {
                                    qualifiedLine = "垂直距离范围（mm）" + "±" + value+"nm；";
                                }
                                // 构建嵌套结构
                                Map<String, Object> itemData = new HashMap<>();
                                itemData.put("qualified_standard", qualifiedLine);

                                // 筛选与此项目相关的数据行
                                List<List<String>> relatedRows = filterRelatedRows(rows, s);
                                for (List<String> row : relatedRows) {
                                    String s1 = row.get(0);
                                    List<List<String>> lists = new ArrayList<>();
                                    if (s1.equals("紫外分光光度计标准物质GBW(E)130066")){
                                        lists.add(row);
                                        itemData.put("related_rows", lists);
                                        break;
                                    }
                                }

                                qualifiedMap.put(s, itemData);
                            }
                        }
                    }

                    // 构建table_data - 只保留columns
//            HashMap<String, Object> table_data = new HashMap<>();
//            table_data.put("columns", columns);
                    // 注意：这里移除了rows

                    // 合格线
//            objectHashMap.put("qualifiedLine", qualifiedMap);
//            objectHashMap.put("table_data", table_data);
                    qualifiedMap.remove("10%中性滤光片");
                    qualifiedMap.remove("20%中性滤光片");
                    qualifiedMap.remove("30%中性滤光片");
                    worksheet.put("qualifiedLine", qualifiedMap);
                    worksheet.put("head_data", columns);
                    list.add(worksheet);

                }
                worksheet.put("hasMergedCells", sheet.getNumMergedRegions() > 0);


                worksheet1.put("name", sheet.getSheetName() + "可见区");


                // 构建合格线结构 - 每个项目对应其合格标准和相关数据行
                HashMap<String, Object> qualifiedMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(firstColumn)) {
                    for (String s : firstColumn) {
                        int i = 0;
                        List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, WAVELENGTH_ACCURACY.getDescription(), s);
                        List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, WAVELENGTH_REPEATABILITY.getDescription(), s);

                        if (CollectionUtils.isEmpty(positions1) || CollectionUtils.isEmpty(positions)) {
                            positions = excelCellLocator.findCellPositions(sheet, TRANSMITTANCE_ACCURACY.getDescription(), s);
                            positions1 = excelCellLocator.findCellPositions(sheet, TRANSMITTANCE_REPEATABILITY.getDescription(), s);
                            i++;
                        }
                        if (CollectionUtils.isEmpty(positions1) || CollectionUtils.isEmpty(positions)) {
                            positions = excelCellLocator.findCellPositions(sheet, INDICATOR_REQUIREMENT.getDescription(), s);
                            i++;
                        }
                        if (CollectionUtils.isEmpty(positions1) && CollectionUtils.isEmpty(positions)) {
                            positions = excelCellLocator.findCellPositions(sheet, ERROR_RANGE.getDescription(), s);
                            i++;
                        }
                        if (CollectionUtils.isEmpty(positions1) && CollectionUtils.isEmpty(positions)) {
                            positions = excelCellLocator.findCellPositions(sheet, VERTICAL_DISTANCE_RANGE.getDescription(), s);
                            i++;
                        }

                        String qualifiedLine = "";
                        String value = "";
                        String value1 = "";
                        if (CollectionUtils.isNotEmpty(positions) || CollectionUtils.isNotEmpty(positions1)) {
                            if (CollectionUtils.isNotEmpty(positions)) {
                                value = formatNumericValue(positions.get(0).getValue());
                            }
                            if (CollectionUtils.isNotEmpty(positions1)) {

                                value1 = formatNumericValue(positions1.get(0).getValue());
                            }
                            if (i == 0) {
                                qualifiedLine = "波长示值误差" + "±" + value + "nm；" + "波长重复性" + "≤" + value1+"nm；";
                            } else if (i == 1) {
                                qualifiedLine = "透射比示值误差" + "±" + value + "nm；" + "透射比重复性" + "≤" + value1+"nm；";
                            } else if (i == 2) {
                                qualifiedLine = "指标要求" + "≤" + value+"nm；";
                            } else if (i == 3) {
                                qualifiedLine = "误差范围（%）" + "≤" + value+"nm；";
                            } else {
                                qualifiedLine = "垂直距离范围（mm）" + "≤" + value+"nm；";
                            }
                            // 构建嵌套结构
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("qualified_standard", qualifiedLine);

                            // 筛选与此项目相关的数据行
                            List<List<String>> relatedRows = filterRelatedRows(rows, s);
                            for (List<String> row : relatedRows) {
                                String s1 = row.get(0);
                                if (s1.contains("紫外分光光度计标准物质GBW(E)130066")){
                                    relatedRows.remove(row);
                                    break;
                                }
                            }
                            itemData.put("related_rows", relatedRows);

                            qualifiedMap.put(s, itemData);
                        }
                    }
                }

                // 构建table_data - 只保留columns
//            HashMap<String, Object> table_data = new HashMap<>();
//            table_data.put("columns", columns);
                // 注意：这里移除了rows

                // 合格线
//            objectHashMap.put("qualifiedLine", qualifiedMap);
//            objectHashMap.put("table_data", table_data);
                qualifiedMap.remove("紫外分光光度计标准物质GBW(E)130066");

                worksheet1.put("qualifiedLine", qualifiedMap);
                worksheet1.put("head_data", columns);
                list.add(worksheet1);
            }
            return list;
        }

        if (isTableStructure) {
            // 提取表头和数据
            List<String> columns = extractHeaders(sheet, evaluator);
            HashMap<String, Object> hashMap = extractDataRowsWithMergedCells(sheet, evaluator, columns.size());
            List<List<String>> rows = (List<List<String>>) MapUtils.getObject(hashMap, "rows");
            HashSet<String> firstColumn = (HashSet<String>) MapUtils.getObject(hashMap, "firstColumn");
            worksheet.put("name", sheet.getSheetName());

            HashMap<String, Object> objectHashMap = new HashMap<>();

            // 构建合格线结构 - 每个项目对应其合格标准和相关数据行
            HashMap<String, Object> qualifiedMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(firstColumn)) {
                for (String s : firstColumn) {
                    int i = 0;
                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, WAVELENGTH_ACCURACY.getDescription(), s);
                    List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, WAVELENGTH_REPEATABILITY.getDescription(), s);

                    if (CollectionUtils.isEmpty(positions1) || CollectionUtils.isEmpty(positions)) {
                        positions = excelCellLocator.findCellPositions(sheet, TRANSMITTANCE_ACCURACY.getDescription(), s);
                        positions1 = excelCellLocator.findCellPositions(sheet, TRANSMITTANCE_REPEATABILITY.getDescription(), s);
                        i++;
                    }
                    if (CollectionUtils.isEmpty(positions1) || CollectionUtils.isEmpty(positions)) {
                        positions = excelCellLocator.findCellPositions(sheet, INDICATOR_REQUIREMENT.getDescription(), s);
                        i++;
                    }
                    if (CollectionUtils.isEmpty(positions1) && CollectionUtils.isEmpty(positions)) {
                        positions = excelCellLocator.findCellPositions(sheet, ERROR_RANGE.getDescription(), s);
                        i++;
                    }
                    if (CollectionUtils.isEmpty(positions1) && CollectionUtils.isEmpty(positions)) {
                        positions = excelCellLocator.findCellPositions(sheet, VERTICAL_DISTANCE_RANGE.getDescription(), s);
                        i++;
                    }

                    String qualifiedLine = "";
                    String value = "";
                    String value1 = "";
                    if (CollectionUtils.isNotEmpty(positions) || CollectionUtils.isNotEmpty(positions1)) {
                        if (CollectionUtils.isNotEmpty(positions)) {
                            value = formatNumericValue(positions.get(0).getValue());
                        }
                        if (CollectionUtils.isNotEmpty(positions1)) {

                            value1 = formatNumericValue(positions1.get(0).getValue());
                        }
                        if (i == 0) {
                            qualifiedLine = "波长示值误差" + ":" + value + ":" + "波长重复性" + ":" + value1;
                        } else if (i == 1) {
                            qualifiedLine = "透射比示值误差" + ":" + value + ":" + "透射比重复性" + ":" + value1;
                        } else if (i == 2) {
                            qualifiedLine = "指标要求" + ":" + value;
                        } else if (i == 3) {
                            qualifiedLine = "误差范围（%）" + ":" + value;
                        } else {
                            qualifiedLine = "垂直距离范围（mm）" + ":" + value;
                        }
                        // 构建嵌套结构
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("qualified_standard", qualifiedLine);

                        // 筛选与此项目相关的数据行
                        List<List<String>> relatedRows = filterRelatedRows(rows, s);
                        itemData.put("related_rows", relatedRows);

                        qualifiedMap.put(s, itemData);
                    }
                }
            }

            // 构建table_data - 只保留columns
//            HashMap<String, Object> table_data = new HashMap<>();
//            table_data.put("columns", columns);
            // 注意：这里移除了rows

            // 合格线
//            objectHashMap.put("qualifiedLine", qualifiedMap);
//            objectHashMap.put("table_data", table_data);
            worksheet.put("qualifiedLine", qualifiedMap);
            worksheet.put("head_data", columns);

        } else {
            // 非表格结构，返回原始数据
            List<List<String>> rawRows = extractAllRows(sheet, evaluator);
            worksheet.put("name", sheet.getSheetName());
            worksheet.put("rows", rawRows);
        }

        worksheet.put("hasMergedCells", sheet.getNumMergedRegions() > 0);


        ArrayList<Map<String, Object>> list = new ArrayList<>();
        list.add(worksheet);
        return list;
    }

    /**
     * 格式化数值字符串，避免科学计数法
     */
    private String formatNumericValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        try {
            // 尝试解析为数字
            double numValue = Double.parseDouble(value);

            // 检查是否是科学计数法或需要格式化的数值
            if (value.contains("E") || value.contains("e")) {
                return String.format("%." + DEFAULT_DECIMAL_PLACES + "f", numValue);
            }

            // 对于普通数值，如果小数位过多也进行格式化
            if (value.contains(".")) {
                String[] parts = value.split("\\.");
                if (parts.length == 2 && parts[1].length() > DEFAULT_DECIMAL_PLACES) {
                    return String.format("%." + DEFAULT_DECIMAL_PLACES + "f", numValue);
                }
            }

            return value;
        } catch (NumberFormatException e) {
            // 不是数值，直接返回原值
            return value;
        }
    }

// ... existing code ...


    /**
     * 主转换方法 - 返回扁平化的内容Map
     */
    public Map<String, Object> convertToSimpleStructure(Workbook workbook) {
        FormulaEvaluator evaluator = createFormulaEvaluator(workbook);
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> worksheets = new ArrayList<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            worksheets.addAll(processSimpleSheet(sheet, evaluator));
        }

        result.put("worksheets", worksheets);
        return result;
    }

    /**
     * 创建公式计算器
     */
    private FormulaEvaluator createFormulaEvaluator(Workbook workbook) {
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
//    private  Map<String, Object> processSimpleSheet(Sheet sheet, FormulaEvaluator evaluator) {
//        Map<String, Object> worksheet = new LinkedHashMap<>();
//
//        // 确定是否为表格结构
//        boolean isTableStructure = analyzeIfTableStructure(sheet);
//
//        if (isTableStructure) {
//            // 提取表头和数据
//            List<String> columns = extractHeaders(sheet, evaluator);
//            HashMap<String, Object> hashMap = extractDataRowsWithMergedCells(sheet, evaluator, columns.size());
//            List<List<String>> rows  = (List<List<String>>) MapUtils.getObject(hashMap, "rows");
//            HashSet<String> firstColumn = (HashSet<String>) MapUtils.getObject(hashMap, "firstColumn");
//            worksheet.put("name", sheet.getSheetName());
//            HashMap<String, Object> objectHashMap = new HashMap<>();
//            HashMap<String, Object> table_data = new HashMap<>();
//            table_data.put("columns", columns);
//            table_data.put("rows", rows);
//            HashMap<String, String> map = new HashMap<>();
//            if (CollectionUtils.isNotEmpty(firstColumn)) {
//                for (String s : firstColumn) {
//
//                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "波长示值误差", s);
//                    List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, "波长重复性", s);
//                    if (CollectionUtils.isNotEmpty(positions) && CollectionUtils.isNotEmpty(positions1)) {
//                        String value = positions.get(0).getValue();
//                        String value1 = positions1.get(0).getValue();
//
//                        String qualifiedLine = "波长示值误差" + ":" + value + ":" + "波长重复性" + ":" + value1;
//                        map.put(s, qualifiedLine);
//                    }
//                }
//            }
//            //合格线
//            objectHashMap.put("qualifiedLine", map);
//            objectHashMap.put("table_data", table_data);
//            worksheet.put("sections", objectHashMap);
//
//        } else {
//            // 非表格结构，返回原始数据
//            List<List<String>> rawRows = extractAllRows(sheet, evaluator);
//            worksheet.put("name", sheet.getSheetName());
//            worksheet.put("rows", rawRows);
//        }
//
//        worksheet.put("hasMergedCells", sheet.getNumMergedRegions() > 0);
//
//        return worksheet;
//    }


    /**
     * 筛选与指定项目名称相关的数据行
     */
    private List<List<String>> filterRelatedRows(List<List<String>> allRows, String projectName) {
        List<List<String>> filteredRows = new ArrayList<>();
        for (List<String> row : allRows) {
            if (!row.isEmpty() && projectName.equals(row.get(0))) {
                filteredRows.add(row);
            }
        }
        return filteredRows;
    }

    /**
     * 分析是否为表格结构
     */
    private boolean analyzeIfTableStructure(Sheet sheet) {
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
    private List<String> extractHeaders(Sheet sheet, FormulaEvaluator evaluator) {
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
    private HashMap<String, Object> extractDataRowsWithMergedCells(Sheet sheet, FormulaEvaluator evaluator, int expectedCols) {
        HashMap<String, Object> hashMap = new HashMap<>();
        List<List<String>> rows = new ArrayList<>();
        HashSet<String> set = new HashSet<>();
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
                if (j == 0) {
                    set.add(value);
                }
                rowData.add(value);
            }
            rows.add(rowData);
        }
        hashMap.put("rows", rows);
        hashMap.put("firstColumn", set);

        return hashMap;
    }

    /**
     * 提取所有行（用于非表格结构）
     */
    private List<List<String>> extractAllRows(Sheet sheet, FormulaEvaluator evaluator) {
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

    private int getNonEmptyCellCount(Row row) {
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

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell, null).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
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

    private String convertCellValueToString(CellValue cellValue) {
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


    private String formatNumber(double number) {
        if (number == (long) number && Math.abs(number) < 1e15) {
            return String.valueOf((long) number);
        } else {
            return String.format("%." + DEFAULT_DECIMAL_PLACES + "f", number);
        }
    }
}



