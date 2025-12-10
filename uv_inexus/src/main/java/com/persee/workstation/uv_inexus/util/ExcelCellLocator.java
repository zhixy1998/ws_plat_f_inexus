package com.persee.workstation.uv_inexus.util;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelCellLocator {

    /**
     * 根据表头和第一列值定位单元格位置
     * @param sheet 工作表对象
     * @param columnHeader 表头名称
     * @param firstColumnValue 第一列的值
     * @return 单元格位置信息列表
     */
    public List<CellPosition> findCellPositions(Sheet sheet,
                                                String columnHeader,
                                                String firstColumnValue) {
        List<CellPosition> positions = new ArrayList<>();

        List<Integer> columnIndices = findColumnIndicesByHeader(sheet, columnHeader);
        if (columnIndices.isEmpty()) return positions;

        List<Integer> rowIndices = findRowIndicesByFirstColumn(sheet, firstColumnValue);
        if (rowIndices.isEmpty()) return positions;

        for (Integer rowIndex : rowIndices) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            for (Integer colIndex : columnIndices) {
                Cell cell = row.getCell(colIndex);
                if (cell != null) {
                    positions.add(getCellPositionInfo(sheet, cell));
                }
            }
        }

        return positions;
    }

    private List<Integer> findColumnIndicesByHeader(Sheet sheet, String columnHeader) {
        List<Integer> indices = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return indices;

        // 获取所有合并区域
        List<CellRangeAddress> mergedRegions = new ArrayList<>();
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            mergedRegions.add(sheet.getMergedRegion(i));
        }

        // 遍历每一列
        for (Cell cell : headerRow) {
            String cellValue = getCellValueAsString(cell);
            if (cellValue.contains(columnHeader)) {
                int colIndex = cell.getColumnIndex();

                // 查找是否属于某个合并区域
                for (CellRangeAddress mergedRegion : mergedRegions) {
                    if (mergedRegion.isInRange(0, colIndex)) { // 第0行，即表头行
                        // 添加合并区域内的所有列
                        for (int c = mergedRegion.getFirstColumn(); c <= mergedRegion.getLastColumn(); c++) {
                            if (!indices.contains(c)) {
                                indices.add(c);
                            }
                        }
                        break;
                    }
                }

                // 如果不是合并单元格，则只添加当前列
                if (indices.isEmpty()) {
                    indices.add(colIndex);
                }
            }
        }

        return indices;
    }


//    /**
//     * 根据表头名称找到列索引
//     */
//    private int findColumnIndexByHeader(Sheet sheet, String columnHeader) {
//        Row headerRow = sheet.getRow(0); // 假设第一行为表头
//        if (headerRow != null) {
//            for (Cell cell : headerRow) {
//                String cellValue = getCellValueAsString(cell);
//                if (StringUtils.isBlank(cellValue)) continue;
//                if (cellValue.startsWith(columnHeader)) {
//                    return cell.getColumnIndex();
//                }
//            }
//        }
//        return -1;
//    }

    /**
     * 根据第一列值找到行索引
     */
    private List<Integer> findRowIndicesByFirstColumn(Sheet sheet, String firstColumnValue) {
        List<Integer> rowIndices = new ArrayList<>();
        List<CellRangeAddress> mergedRegions = new ArrayList<>();

        // 获取所有合并区域
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            mergedRegions.add(sheet.getMergedRegion(i));
        }

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // 跳过表头行

            Cell firstCell = row.getCell(0);
            if (firstCell != null) {
                String cellValue = getCellValueAsString(firstCell);
                if (firstColumnValue.equals(cellValue)) {
                    rowIndices.add(row.getRowNum());
                }
            }
        }

        // 处理合并单元格情况
        for (CellRangeAddress mergedRegion : mergedRegions) {
            int firstRow = mergedRegion.getFirstRow();
            int lastRow = mergedRegion.getLastRow();
            int firstCol = mergedRegion.getFirstColumn();
            
            // 如果合并区域的第一列是目标值
            if (firstCol == 0) {
                Row firstRowObj = sheet.getRow(firstRow);
                if (firstRowObj != null) {
                    Cell firstCell = firstRowObj.getCell(0);
                    if (firstCell != null) {
                        String cellValue = getCellValueAsString(firstCell);
                        if (firstColumnValue.equals(cellValue)) {
                            // 添加合并区域内的所有行
                            for (int i = firstRow; i <= lastRow; i++) {
                                if (!rowIndices.contains(i)) {
                                    rowIndices.add(i);
                                }
                            }
                        }
                    }
                }
            }
        }

        return rowIndices;
    }

    /**
     * 获取单元格位置信息（包括合并区域信息）
     */
    private CellPosition getCellPositionInfo(Sheet sheet, Cell cell) {
        int rowNum = cell.getRowIndex();
        int colIndex = cell.getColumnIndex();
        int rowSpan = 1;
        int colSpan = 1;
        String value = getCellValueAsString(cell);

        // 检查是否在合并区域内
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
            if (mergedRegion.isInRange(rowNum, colIndex)) {
                rowSpan = mergedRegion.getLastRow() - mergedRegion.getFirstRow() + 1;
                colSpan = mergedRegion.getLastColumn() - mergedRegion.getFirstColumn() + 1;
                break;
            }
        }

        return new CellPosition(rowNum, colIndex, rowSpan, colSpan, value);
    }

    /**
     * 安全获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            return new DataFormatter().formatCellValue(cell);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 单元格位置信息类
     */
    public static class CellPosition {
        private int row;
        private int col;
        private int rowSpan;
        private int colSpan;
        private String value;

        public CellPosition(int row, int col, int rowSpan, int colSpan, String value) {
            this.row = row;
            this.col = col;
            this.rowSpan = rowSpan;
            this.colSpan = colSpan;
            this.value = value;
        }

        // Getter方法
        public int getRow() { return row; }
        public int getCol() { return col; }
        public int getRowSpan() { return rowSpan; }
        public int getColSpan() { return colSpan; }
        public String getValue() { return value; }

        @Override
        public String toString() {
            return String.format("Cell[row=%d, col=%d, rowSpan=%d, colSpan=%d, value='%s']",
                    row, col, rowSpan, colSpan, value);
        }
    }
}