package com.persee.workstation.uv_inexus.excel;

import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.persee.workstation.uv_inexus.util.I18nExcelHelper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

/**
 * EasyExcel 表头翻译拦截器
 * 在写入表头行时自动将中文翻译为当前 locale 对应的语言
 */
public class I18nHeadWriteHandler implements RowWriteHandler {

    private final I18nExcelHelper i18nExcelHelper;

    public I18nHeadWriteHandler(I18nExcelHelper i18nExcelHelper) {
        this.i18nExcelHelper = i18nExcelHelper;
    }

    @Override
    public void beforeRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                Integer rowIndex, Integer relativeRowIndex, Boolean isHead) {
    }

    @Override
    public void afterRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                               Row row, Integer relativeRowIndex, Boolean isHead) {
    }

    /**
     * 在表头行写入完成后，遍历所有单元格进行翻译
     */
    @Override
    public void afterRowDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                Row row, Integer relativeRowIndex, Boolean isHead) {
        if (!Boolean.TRUE.equals(isHead)) {
            return;
        }
        if (i18nExcelHelper.isChinese()) {
            return;
        }
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.STRING) {
                continue;
            }
            String value = cell.getStringCellValue();
            if (value != null && !value.isEmpty()) {
                String translated = i18nExcelHelper.t(value);
                if (!translated.equals(value)) {
                    cell.setCellValue(translated);
                }
            }
        }
    }
}
