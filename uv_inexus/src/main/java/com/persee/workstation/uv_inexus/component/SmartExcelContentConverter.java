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
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 完整版智能Excel内容转换器 - 完美处理合并单元格和公式计算
 */
public class SmartExcelContentConverter {

    /**
     * 主转换方法 - 返回智能处理后的纯内容Map
     */
    public static Map<String, Object> convertToSmartContent(Workbook workbook) {
        FormulaEvaluator evaluator = createFormulaEvaluator(workbook);
        Map<String, Object> result = new LinkedHashMap<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            Object contentData = extractSheetContentIntelligently(sheet, evaluator);
            result.put(sheet.getSheetName(), contentData);
        }

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
     * 智能提取Sheet内容 - 主入口
     */
    private static Object extractSheetContentIntelligently(Sheet sheet, FormulaEvaluator evaluator) {
        // 分析Sheet结构
        SheetStructure structure = analyzeSheetStructure(sheet);

        if (structure.isTableStructure) {
            // 表格型数据
            List<Map<String, String>> tableData = parseAsSmartTable(sheet, evaluator, structure);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "table");
            result.put("headers", tableData.get(0).keySet().stream().collect(Collectors.toList()));            result.put("rows", tableData.subList(1, tableData.size()));
            result.put("totalRecords", tableData.size() - 1);
            result.put("hasFormulas", structure.hasFormulas);
            result.put("mergedCellsCount", structure.mergedCells.size());
            return result;
        } else {
            // 结构化数据
            List<Map<String, Object>> structuredData = parseAsSmartSections(sheet, evaluator, structure);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "structured");
            result.put("sections", structuredData);
            result.put("hasFormulas", structure.hasFormulas);
            result.put("mergedCellsCount", structure.mergedCells.size());
            return result;
        }
    }

    /**
     * 分析Sheet结构
     */
    private static SheetStructure analyzeSheetStructure(Sheet sheet) {
        SheetStructure structure = new SheetStructure();
        structure.mergedCells = parseMergedCells(sheet);
        structure.hasFormulas = hasFormulas(sheet);

        // 分析是否为表格结构
        int potentialHeaderRows = 0;
        int dataRows = 0;
        boolean consistentColumns = true;
        int lastColCount = -1;

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            int colCount = getNonEmptyCellCount(row);
            if (lastColCount != -1 && Math.abs(colCount - lastColCount) > 2) {
                consistentColumns = false;
            }
            lastColCount = colCount;

            if (i <= 3 && colCount >= 2) { // 前几行且有多列，可能是表头
                potentialHeaderRows++;
            }

            if (i > 0 && colCount >= 2) { // 数据行
                dataRows++;
            }
        }

        structure.isTableStructure = potentialHeaderRows >= 1 && dataRows >= 1 && consistentColumns;
        return structure;
    }

    /**
     * 智能表格解析 - 完美处理合并单元格
     */
    private static List<Map<String, String>> parseAsSmartTable(Sheet sheet, FormulaEvaluator evaluator,
                                                               SheetStructure structure) {
        List<Map<String, String>> table = new ArrayList<>();

        // 找到表头行
        int headerRowIndex = findBestHeaderRow(sheet, structure.mergedCells);
        if (headerRowIndex == -1) return table;

        // 构建智能表头映射
        HeaderMapping headerMapping = buildHeaderMapping(sheet, headerRowIndex, structure.mergedCells);

        // 提取数据行
        List<Map<String, String>> dataRows = new ArrayList<>();
        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row dataRow = sheet.getRow(i);
            if (dataRow == null || isEmptyRow(dataRow)) continue;

            Map<String, String> rowData = extractRowWithHeaderMapping(dataRow, headerMapping, evaluator);
            if (!rowData.isEmpty()) {
                dataRows.add(rowData);
            }
        }

        // 构建完整表格（表头 + 数据）
        // 表头行
        Map<String, String> headerRow = new LinkedHashMap<>();
        for (String headerKey : headerMapping.getHeaders()) {
            headerRow.put(headerKey, headerKey);
        }
        table.add(headerRow);

        // 数据行
        table.addAll(dataRows);

        return table;
    }

    /**
     * 构建智能表头映射
     */
    private static HeaderMapping buildHeaderMapping(Sheet sheet, int headerRowIndex,
                                                    Map<String, MergedCellInfo> mergedCells) {
        HeaderMapping mapping = new HeaderMapping();
        Row headerRow = sheet.getRow(headerRowIndex);

        // 先收集所有单元格的值和位置
        Map<String, String> cellValues = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell != null) {
                String value = getCellValueAsString(cell, null);
                if (!value.trim().isEmpty()) {
                    cellValues.put(cell.getColumnIndex() + "_" + headerRowIndex, value);
                }
            }
        }

        // 处理合并单元格，建立列名映射
        Set<Integer> processedCols = new HashSet<>();

        // 按列顺序处理
        int maxCol = getMaxColumnIndex(headerRow);
        for (int col = 0; col <= maxCol; col++) {
            if (processedCols.contains(col)) continue;

            String headerKey = null;

            // 检查这个位置是否在合并单元格中
            MergedCellInfo mergeInfo = findMergedCellContaining(col, headerRowIndex, mergedCells);
            if (mergeInfo != null) {
                // 这是合并单元格，使用主单元格的值
                String masterKey = mergeInfo.firstCol + "_" + mergeInfo.firstRow;
                headerKey = cellValues.get(masterKey);
                if (headerKey == null) headerKey = "Column_" + (mapping.getHeaders().size() + 1);

                // 标记这个合并区域的所有列为已处理
                for (int c = mergeInfo.firstCol; c <= mergeInfo.lastCol; c++) {
                    processedCols.add(c);
                }

                // 为合并区域的每一列都映射到同一个表头
                for (int c = mergeInfo.firstCol; c <= mergeInfo.lastCol; c++) {
                    mapping.addColumnMapping(c, headerKey);
                }
            } else {
                // 普通单元格
                String cellKey = col + "_" + headerRowIndex;
                headerKey = cellValues.get(cellKey);
                if (headerKey == null || headerKey.trim().isEmpty()) {
                    headerKey = "Column_" + (mapping.getHeaders().size() + 1);
                } else {
                    processedCols.add(col);
                }
                mapping.addColumnMapping(col, headerKey);
            }
        }

        return mapping;
    }

    /**
     * 根据表头映射提取行数据
     */
    private static Map<String, String> extractRowWithHeaderMapping(Row row, HeaderMapping headerMapping,
                                                                   FormulaEvaluator evaluator) {
        Map<String, String> rowData = new LinkedHashMap<>();

        for (int col = 0; col <= getMaxColumnIndex(row); col++) {
            String headerKey = headerMapping.getHeaderForColumn(col);
            if (headerKey == null) continue;

            Cell cell = row.getCell(col);
            String value = cell != null ? getCellValueAsString(cell, evaluator) : "";

            // 如果已经有值，追加（处理合并单元格的延续行）
            String existingValue = rowData.get(headerKey);
            if (existingValue != null && !existingValue.isEmpty()) {
                if (!value.isEmpty()) {
                    rowData.put(headerKey, existingValue + " " + value);
                }
            } else {
                rowData.put(headerKey, value);
            }
        }

        return rowData;
    }

    /**
     * 智能分块解析
     */
    private static List<Map<String, Object>> parseAsSmartSections(Sheet sheet, FormulaEvaluator evaluator,
                                                                  SheetStructure structure) {
        List<Map<String, Object>> sections = new ArrayList<>();

        // 按空行分块
        List<List<Row>> rowBlocks = splitIntoRowBlocks(sheet);

        for (List<Row> block : rowBlocks) {
            if (block.isEmpty()) continue;

            Map<String, Object> section = new LinkedHashMap<>();
            section.put("blockType", determineBlockType(block));

            if (isSingleTitleBlock(block)) {
                // 标题块
                section.put("title", extractTitleFromBlock(block));
                section.put("content", new ArrayList<>());
            } else {
                // 数据块
                List<Map<String, String>> blockData = new ArrayList<>();
                for (Row row : block) {
                    Map<String, String> rowData = extractRowSimply(row, evaluator);
                    if (!rowData.isEmpty()) {
                        blockData.add(rowData);
                    }
                }
                section.put("content", blockData);
                section.put("rowCount", blockData.size());
            }

            sections.add(section);
        }

        return sections;
    }

    // ==================== 工具方法 ====================

    private static int findBestHeaderRow(Sheet sheet, Map<String, MergedCellInfo> mergedCells) {
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row != null && getNonEmptyCellCount(row) >= 2) {
                return i;
            }
        }
        return -1;
    }

    private static MergedCellInfo findMergedCellContaining(int col, int row,
                                                           Map<String, MergedCellInfo> mergedCells) {
        for (MergedCellInfo info : mergedCells.values()) {
            if (info.firstRow <= row && info.lastRow >= row &&
                    info.firstCol <= col && info.lastCol >= col) {
                return info;
            }
        }
        return null;
    }

    private static int getMaxColumnIndex(Row row) {
        if (row == null) return 0;
        int maxCol = 0;
        for (Cell cell : row) {
            if (cell != null && cell.getColumnIndex() > maxCol) {
                maxCol = cell.getColumnIndex();
            }
        }
        return Math.max(maxCol, 10); // 最小10列
    }

    private static int getNonEmptyCellCount(Row row) {
        if (row == null) return 0;
        int count = 0;
        for (Cell cell : row) {
            if (cell != null && getCellValueAsString(cell, null).trim().length() > 0) {
                count++;
            }
        }
        return count;
    }

    private static List<List<Row>> splitIntoRowBlocks(Sheet sheet) {
        List<List<Row>> blocks = new ArrayList<>();
        List<Row> currentBlock = new ArrayList<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && !isEmptyRow(row)) {
                currentBlock.add(row);
            } else {
                if (!currentBlock.isEmpty()) {
                    blocks.add(new ArrayList<>(currentBlock));
                    currentBlock.clear();
                }
            }
        }

        if (!currentBlock.isEmpty()) {
            blocks.add(currentBlock);
        }

        return blocks;
    }

    private static boolean isSingleTitleBlock(List<Row> block) {
        if (block.size() != 1) return false;
        Row row = block.get(0);
        int nonEmptyCount = getNonEmptyCellCount(row);
        String firstValue = getFirstNonEmptyValue(row);
        return nonEmptyCount <= 3 && firstValue != null && firstValue.length() > 5;
    }

    private static String getFirstNonEmptyValue(Row row) {
        for (Cell cell : row) {
            if (cell != null) {
                String value = getCellValueAsString(cell, null);
                if (!value.trim().isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String extractTitleFromBlock(List<Row> block) {
        Row row = block.get(0);
        StringBuilder title = new StringBuilder();
        for (Cell cell : row) {
            if (cell != null) {
                String value = getCellValueAsString(cell, null);
                if (!value.trim().isEmpty()) {
                    if (title.length() > 0) title.append(" ");
                    title.append(value);
                }
            }
        }
        return title.toString();
    }

    private static String determineBlockType(List<Row> block) {
        return isSingleTitleBlock(block) ? "title" : "data";
    }

    private static Map<String, String> extractRowSimply(Row row, FormulaEvaluator evaluator) {
        Map<String, String> rowData = new LinkedHashMap<>();
        if (row == null) return rowData;

        for (Cell cell : row) {
            if (cell != null) {
                String colName = CellReference.convertNumToColString(cell.getColumnIndex());
                String value = getCellValueAsString(cell, evaluator);
                if (!value.trim().isEmpty()) {
                    rowData.put(colName, value);
                }
            }
        }
        return rowData;
    }

    private static boolean hasFormulas(Sheet sheet) {
        for (Row row : sheet) {
            if (row != null) {
                for (Cell cell : row) {
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && getCellValueAsString(cell, null).trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, MergedCellInfo> parseMergedCells(Sheet sheet) {
        Map<String, MergedCellInfo> mergedCells = new HashMap<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            MergedCellInfo info = new MergedCellInfo();
            info.firstRow = region.getFirstRow();
            info.lastRow = region.getLastRow();
            info.firstCol = region.getFirstColumn();
            info.lastCol = region.getLastColumn();
            mergedCells.put(generateMergeKey(info.firstRow, info.firstCol), info);
        }
        return mergedCells;
    }

    private static String generateMergeKey(int row, int col) {
        return row + "_" + col;
    }

    private static String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";

        if (cell.getCellType() == CellType.FORMULA) {
            try {
                CellValue cellValue = evaluator.evaluate(cell);
                return convertCellValueToString(cellValue);
            } catch (Exception e) {
                return "=" + cell.getCellFormula();
            }
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return DateUtil.isCellDateFormatted(cell) ?
                        cell.getDateCellValue().toString() : formatNumber(cell.getNumericCellValue());
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
                return DateUtil.isValidExcelDate(cellValue.getNumberValue()) ?
                        new Date((long) cellValue.getNumberValue()).toString() : formatNumber(cellValue.getNumberValue());
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



    // ==================== 数据模型 ====================

    private static class SheetStructure {
        public boolean isTableStructure = false;
        public boolean hasFormulas = false;
        public Map<String, MergedCellInfo> mergedCells = new HashMap<>();
    }

    private static class MergedCellInfo {
        public int firstRow, lastRow, firstCol, lastCol;
    }

    private static class HeaderMapping {
        private final Map<Integer, String> columnMappings = new HashMap<>();
        private final Map<String, Integer> headerUsage = new HashMap<>();

        public void addColumnMapping(int columnIndex, String header) {
            columnMappings.put(columnIndex, header);
            headerUsage.put(header, headerUsage.getOrDefault(header, 0) + 1);
        }

        public String getHeaderForColumn(int columnIndex) {
            return columnMappings.get(columnIndex);
        }

        public List<String> getHeaders() {
            return new ArrayList<>(new LinkedHashSet<>(columnMappings.values()));
        }
    }


}

