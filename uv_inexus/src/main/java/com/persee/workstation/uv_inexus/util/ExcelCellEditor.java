package com.persee.workstation.uv_inexus.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ExcelCellEditor {

    /**
     * 获取Excel中特定单元格的值
     * @param sheet 工作表对象
     * @param rowNum 行号(从0开始)
     * @param cellNum 列号(从0开始)
     * @return 单元格值
     */
    public String getCellValue(Sheet sheet, int rowNum, int cellNum) {
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            return null;
        }

        Cell cell = row.getCell(cellNum);
        return getCellValueAsString(cell);
    }

    /**
     * 设置Excel中特定单元格的值
     * @param sheet 工作表对象
     * @param rowNum 行号(从0开始)
     * @param cellNum 列号(从0开始)
     * @param value 要设置的值
     */
    public void setCellValue(Sheet sheet, int rowNum, int cellNum, String value) {
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            row = sheet.createRow(rowNum);
        }

        Cell cell = row.getCell(cellNum);
        if (cell == null) {
            cell = row.createCell(cellNum);
        }

        cell.setCellValue(Double.valueOf(value.trim()));
    }

    /**
     * 安全获取单元格值为字符串
     * @param cell 单元格对象
     * @return 单元格字符串值
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
     * 根据内容查找单元格位置
     * @param sheet 工作表对象
     * @param content 要查找的内容
     * @return 单元格位置映射(行号->列号列表)
     */
    public Map<Integer, Integer> findCellPosition(Sheet sheet, String content) {
        Map<Integer, Integer> positions = new HashMap<>();
        
        for (Row row : sheet) {
            for (Cell cell : row) {
                String cellValue = getCellValueAsString(cell);
                if (content.equals(cellValue)) {
                    positions.put(row.getRowNum(), cell.getColumnIndex());
                }
            }
        }
        
        return positions;
    }
}