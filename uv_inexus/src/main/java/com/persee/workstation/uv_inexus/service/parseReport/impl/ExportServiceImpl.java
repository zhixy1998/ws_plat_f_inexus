package com.persee.workstation.uv_inexus.service.parseReport.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.persee.workstation.uv_inexus.dao.parseReport.CalculationFormulaMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.DetectionMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.QualityControlMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.RoundingRulesMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.StandardPretreatmentMapper;
import com.persee.workstation.uv_inexus.excel.CalculationFormulaExcelModel;
import com.persee.workstation.uv_inexus.excel.ExcelImageExportUtils;
import com.persee.workstation.uv_inexus.excel.QualityControlExcelModel;
import com.persee.workstation.uv_inexus.excel.RoundingRulesExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardPretreatmentExcelModel;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules;
import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.service.parseReport.ExportService;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private CalculationFormulaMapper calculationFormulaMapper;
    @Autowired
    private QualityControlMapper qualityControlMapper;
    @Autowired
    private RoundingRulesMapper roundingRulesMapper;
    @Autowired
    private DetectionMapper standardDetectionMapper;
    @Autowired
    private StandardPretreatmentMapper standardPretreatmentMapper;

    @Override
    public void exportAllTables(OutputStream outputStream) throws IOException {
        // 查询所有表的数据
        List<Detection> standardDetections = standardDetectionMapper.selectAll();
        List<CalculationFormula> calculationFormulas = calculationFormulaMapper.selectAllForExport();
        List<QualityControl> qualityControls = qualityControlMapper.selectAllForExport();
        List<RoundingRules> roundingRules = roundingRulesMapper.selectAllForExport();
        List<StandardPretreatment> standardPretreatments = standardPretreatmentMapper.selectAllForExport();

        // 创建Excel写入器，注册图片处理器
        ExcelWriter excelWriter = EasyExcel.write(outputStream)
                .registerWriteHandler(new FormulaImageHandler())
                .build();

        // 1. 标准信息表
        WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "标准信息表")
                .head(StandardDetectionExcelModel.class)
                .build();
        excelWriter.write(standardDetections, writeSheet1);

        // 2. 前处理信息表
        WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "前处理信息表")
                .head(StandardPretreatmentExcelModel.class)
                .build();
        excelWriter.write(standardPretreatments, writeSheet2);

        // 3. 计算公式表（带图片）
        WriteSheet writeSheet3 = EasyExcel.writerSheet(2, "计算公式表")
                .head(CalculationFormulaExcelModel.class)
                .build();
        excelWriter.write(calculationFormulas, writeSheet3);

        // 4. 质量控制表
        WriteSheet writeSheet4 = EasyExcel.writerSheet(3, "质量控制表")
                .head(QualityControlExcelModel.class)
                .build();
        excelWriter.write(qualityControls, writeSheet4);

        // 5. 修约表
        WriteSheet writeSheet5 = EasyExcel.writerSheet(4, "修约表")
                .head(RoundingRulesExcelModel.class)
                .build();
        excelWriter.write(roundingRules, writeSheet5);

        // 关闭Excel写入器
        excelWriter.finish();
    }

    /**
     * 计算公式表图片处理器（使用新版RowWriteHandler接口）
     */
    private static class FormulaImageHandler implements RowWriteHandler {
        @Override
        public void afterRowDispose(WriteSheetHolder writeSheetHolder, 
                                WriteTableHolder writeTableHolder,
                                Row row, Integer relativeRowIndex, 
                                Boolean isHead) {
                // 跳过表头和非计算公式表
                if (isHead || row == null || !"计算公式表".equals(writeSheetHolder.getSheetName())) {
                return;
                }

                try {
                        // 获取第25列的单元格（索引25）
                        Cell cell = row.getCell(25);
                        
                        if (cell != null && cell.getCellType() == CellType.STRING) {
                                String cellValue = cell.getStringCellValue();
                                if (cellValue != null && !cellValue.isEmpty()) {
                                // 调用ExcelImageExportUtils插入图片
                                Sheet sheet = writeSheetHolder.getSheet();
                                String imgRef = ExcelImageExportUtils.insertImage(
                                        cellValue, 
                                        "formula-" + row.getRowNum(), 
                                        sheet, 
                                        row.getRowNum(), 
                                        25
                                );
                                
                                if (imgRef != null) {
                                        // 插入成功，清除单元格内容
                                        cell.setCellValue("");
                                        System.out.println("成功插入图片到第25列，引用: " + imgRef);
                                }
                                }
                        }
                        } catch (Exception e) {
                        System.err.println("处理行 " + (row != null ? row.getRowNum() : "null") + " 时出错: " + e.getMessage());
                        e.printStackTrace();
                }
        }
        }
}