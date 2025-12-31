package com.persee.workstation.uv_inexus.component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExcelDynamicMerger {

    /**
     * 动态插入行并处理合并、公式、边框
     */
    public void insertDynamicMergedRow(Workbook workbook, String sheetName, int rowIndex,
                                       Map<Integer, List<Object>> columnData) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) return;
// 在方法开始处添加标志位
        boolean hasMerged = false;
        // 1. 确定最大行数（各列数据长度的最大值）
        int maxRows = columnData.values().stream().mapToInt(List::size).max().orElse(0);

        // 2. 查找模板行（最近的有公式的行，作为公式继承的基准）
        Row templateRow = findTemplateRow(sheet, rowIndex);
        CellStyle templateStyle = templateRow != null ? templateRow.getRowStyle() : createDefaultCellStyle(workbook);

        // 3. 逐行处理新数据
        for (int i = 0; i < maxRows; i++) {
            Row newRow = sheet.createRow(rowIndex + i);
            newRow.setRowStyle(templateStyle); // 继承模板行样式
            boolean a=true;
            // 逐列处理
            for (Map.Entry<Integer, List<Object>> entry : columnData.entrySet()) {
                int colIndex = entry.getKey();
                List<Object> values = entry.getValue();
                Cell newCell = newRow.createCell(colIndex);

//                // 处理合并单元格（仅当列数据长度为1时，合并当前列的所有行）
//                if (values.size() == 1) {
//                    mergeColumnCells(sheet, rowIndex, maxRows, colIndex);
//                }
//
//                // 设置单元格值 + 继承模板行公式（并调整行号）
//                setCellValueWithFormula(newCell, values, i, templateRow, rowIndex + i, colIndex);
                // 处理合并单元格（仅当列数据长度为1时，合并当前列的所有行）
                if (values.size() == 1) {
                    // 在循环中判断是否已合并
                    if (!hasMerged) {
                        mergeColumnCells(sheet, rowIndex, maxRows, colIndex);
                        hasMerged = true;
                    }
//                        mergeColumnCells(sheet, rowIndex, maxRows, colIndex); //  修正：合并 maxRows 行

                    // 只在第一行设置值
                    if (i == 0) { // 第一行
                        setCellValueWithFormula(newCell, values, 0, templateRow, rowIndex, colIndex);
                    }

                } else {
                    // 非合并列，正常设置值
                    setCellValueWithFormula(newCell, values, i, templateRow, rowIndex + i, colIndex);
                }
            }
        }

        // 4. 调整已有合并区域（插入新行后，修复跨越新行的合并区域）
//        adjustExistingMergedRegions(sheet, rowIndex);
        // 5. 计算所有公式
        workbook.setForceFormulaRecalculation(true);
    }

    /**
     * 查找模板行：rowIndex 上方最近的有公式的行
     */
    private Row findTemplateRow(Sheet sheet, int rowIndex) {
        for (int r = rowIndex - 1; r >= 0; r--) {
            Row row = sheet.getRow(r);
            if (row != null) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        return row;
                    }
                }
            }
        }
        return null;
    }

    private void removeEmptyRowsAndColumns(Sheet sheet, int maxDataRow, int maxDataCol) {
        // 删除空行（从最后一行开始向前遍历）
        for (int i = sheet.getLastRowNum(); i > maxDataRow; i--) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                sheet.removeRow(row);
            }
        }

        // 删除空列（从最后一列开始向前遍历）
        for (int j = sheet.getRow(0).getLastCellNum() - 1; j > maxDataCol; j--) {
            if (isColumnEmpty(sheet, j)) {
                for (Row row : sheet) {
                    if (row != null) {
                        Cell cell = row.getCell(j);
                        if (cell != null) {
                            row.removeCell(cell);
                        }
                    }
                }
            }
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private boolean isColumnEmpty(Sheet sheet, int colIndex) {
        for (Row row : sheet) {
            if (row != null) {
                Cell cell = row.getCell(colIndex);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 合并列的所有行（生成 CellRangeAddress 并设置合并）
     */
    private void mergeColumnCells(Sheet sheet, int startRow, int rowCount, int colIndex) {
        if (rowCount <= 1) return; // 只有一行不需要合并

        int endRow = startRow + rowCount - 1;

        // 检查是否已存在相同区域的合并
//        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
//            CellRangeAddress region = sheet.getMergedRegion(i);
//            if (region.getFirstRow() == startRow &&
//                    region.getLastRow() == endRow &&
//                    region.getFirstColumn() == colIndex &&
//                    region.getLastColumn() == colIndex) {
//                return; // 已存在，无需重复合并
//            }
//        }
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstColumn() == colIndex &&
                    region.getFirstRow() <= startRow &&
                    region.getLastRow() >= startRow + rowCount - 1) {
                sheet.removeMergedRegion(i);
            }
        }

        // 创建新的合并区域
        CellRangeAddress mergeRegion = new CellRangeAddress(
                startRow, endRow, colIndex, colIndex
        );
        sheet.addMergedRegion(mergeRegion);
        //把这个sheet页作为Excel文件导出

        // 设置合并区域的边框样式
        setMergedRegionBorderStyle(sheet, mergeRegion);
    }




    /**
     * 设置单元格值 + 继承模板行公式（自动调整行号）
     */
    private void setCellValueWithFormula(Cell cell, List<Object> values, int rowNum,
                                         Row templateRow, int currentRow, int colIndex) {
        Object value = rowNum < values.size() ? values.get(rowNum) : null;

        if (templateRow != null) {
            Cell templateCell = templateRow.getCell(colIndex);
            if (templateCell != null) {
                // 1. 继承样式
                cell.setCellStyle(templateCell.getCellStyle());
/*          // 1. 继承样式，但不覆盖边框
                CellStyle inheritedStyle = templateCell.getCellStyle();
                CellStyle finalStyle = cell.getCellStyle(); // 获取当前样式

                // 合并样式：保留现有边框，继承其他样式
                finalStyle.cloneStyleFrom(inheritedStyle);
                finalStyle.setBorderTop(BorderStyle.THIN);
                finalStyle.setBorderBottom(BorderStyle.THIN);
                finalStyle.setBorderLeft(BorderStyle.THIN);
                finalStyle.setBorderRight(BorderStyle.THIN);

                cell.setCellStyle(finalStyle);*/
                // 2. 继承公式并调整行号
                if (templateCell.getCellType() == CellType.FORMULA) {
                    String formula = templateCell.getCellFormula();
                    String adjustedFormula = adjustFormulaRow(formula, currentRow, templateRow.getRowNum());
                    cell.setCellFormula(adjustedFormula);
                    return; // 公式已设置，无需再设值
                }
            }
        }

        // 无公式/模板行无公式时，直接设值
        setCellValue(cell, value);
    }


    /**
     * 调整公式中的行引用（列引用不变，仅行号偏移）
     */
    private String adjustFormulaRow(String formula, int targetRow, int sourceRow) {
        Pattern pattern = Pattern.compile("([A-Za-z]+)(\\$?)(\\d+)(\\$?)");
        Matcher matcher = pattern.matcher(formula);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String col = matcher.group(1);
            String rowPrefix = matcher.group(2); // 列前的$
            String rowStr = matcher.group(3);
            String rowSuffix = matcher.group(4); // 行后的$

            int originalRow = Integer.parseInt(rowStr);
            int newRow = originalRow + (targetRow - sourceRow);

            // 重建单元格引用（保留$符号）
            matcher.appendReplacement(sb, col + rowPrefix + newRow + rowSuffix);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 设置合并区域的边框（上下左右细边框）
     */
    private void setMergedRegionBorderStyle(Sheet sheet, CellRangeAddress region) {
        CellStyle borderStyle = createBorderStyle(sheet.getWorkbook());
        for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
            Row row = sheet.getRow(r) != null ? sheet.getRow(r) : sheet.createRow(r);
            for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                Cell cell = row.getCell(c) != null ? row.getCell(c) : row.createCell(c);
                cell.setCellStyle(borderStyle);
            }
        }

    }

    /**
     * 调整已有合并区域（插入新行后，修复跨越新行的合并区域）
     */
    private void adjustExistingMergedRegions(Sheet sheet, int insertRowIndex) {
        List<CellRangeAddress> mergedRegions = new ArrayList<>(sheet.getMergedRegions());
        for (int i = mergedRegions.size() - 1; i >= 0; i--) {
            CellRangeAddress region = mergedRegions.get(i);
            int firstRow = region.getFirstRow();
            int lastRow = region.getLastRow();

            if (firstRow >= insertRowIndex) {
                // 合并区域在新行下方 → 行号+1
                sheet.removeMergedRegion(i);
                CellRangeAddress newRegion = new CellRangeAddress(
                        firstRow + 1, lastRow + 1, region.getFirstColumn(), region.getLastColumn()
                );
                sheet.addMergedRegion(newRegion);
            } else if (lastRow >= insertRowIndex) {
                // 合并区域跨越新行 → 扩展最后一行
                sheet.removeMergedRegion(i);
                CellRangeAddress newRegion = new CellRangeAddress(
                        firstRow, lastRow + 1, region.getFirstColumn(), region.getLastColumn()
                );
                sheet.addMergedRegion(newRegion);
            }
        }
    }

    // 辅助：创建默认单元格样式（含边框）
    private CellStyle createDefaultCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    // 辅助：创建仅边框的样式（合并区域用）
    private CellStyle createBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    // 辅助：设置单元格值（兼容多种类型）
    private void setCellValue(Cell cell, Object value) {
        if (value == null) return;
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        }
        // 可扩展其他类型（如Formula等）
    }



}
