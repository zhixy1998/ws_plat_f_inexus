package com.persee.workstation.uv_inexus.service.parseReport.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private CalculationFormulaMapper calculationFormulaMapper;
    @Autowired
    private QualityControlMapper qualityControlMapper;
    @Autowired
    private RoundingRulesMapper roundingRulesMapper;
    @Autowired
    private DetectionMapper detectionMapper;
    @Autowired
    private StandardPretreatmentMapper standardPretreatmentMapper;

    // 全量导出（不变）
    @Override
    public void exportAllTables(OutputStream outputStream) throws IOException {
        List<Detection> allDetections = detectionMapper.selectAll();
        exportData(outputStream, allDetections);
    }

    // 按条件导出（新增）
    @Override
    public void exportByCondition(OutputStream outputStream, Integer isAll, String exportIds) throws IOException {
        List<Detection> targetDetections;

        if (isAll == 1) {
            // 全量导出
            targetDetections = detectionMapper.selectAll();
            log.info("全量导出，共获取{}条Detection记录", targetDetections.size());
        } else {
            // 按ID筛选导出
            if (exportIds == null || exportIds.isEmpty()) {
                throw new IllegalArgumentException("当isAll=0时，必须提供exportIds参数");
            }
            
            // 1. 解析exportIds为字符串列表
            List<String> exportIdList = Arrays.stream(exportIds.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());
            
            if (exportIdList.isEmpty()) {
                throw new IllegalArgumentException("exportIds参数格式错误，应为逗号分隔的ID列表");
            }

            // 2. 查询指定ID的Detection记录
            List<Detection> baseDetections = detectionMapper.selectByIds(exportIdList);
            if (baseDetections.isEmpty()) {
                log.warn("exportIds对应的Detection记录为空");
                targetDetections = new ArrayList<>();
            } else {
                // 3. 提取这些记录的standard_number（去重）
                Set<String> standardNumbers = baseDetections.stream()
                        .map(Detection::getStandardNumber)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                
                if (standardNumbers.isEmpty()) {
                    log.warn("未提取到有效的standard_number");
                    targetDetections = new ArrayList<>();
                } else {
                    // 4. 查询所有standard_number匹配的Detection记录
                    targetDetections = detectionMapper.selectByStandardNumbers(standardNumbers);
                    log.info("筛选后共获取{}条Detection记录", targetDetections.size());
                }
            }
        }

        // 执行导出
        exportData(outputStream, targetDetections);
    }

    // 核心导出逻辑（复用）
    private void exportData(OutputStream outputStream, List<Detection> targetDetections) throws IOException {
        // 1. 提取targetDetections的id（用于关联其他表）
        Set<String> detectionIds = targetDetections.stream()
                .map(Detection::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 查询其他4张表的数据（按detection_ids筛选）
        List<CalculationFormula> calculationFormulas = detectionIds.isEmpty() 
                ? new ArrayList<>() 
                : calculationFormulaMapper.selectByDetectionIds(detectionIds);
        
        List<QualityControl> qualityControls = detectionIds.isEmpty()
                ? new ArrayList<>()
                : qualityControlMapper.selectByDetectionIds(detectionIds);
        
        List<RoundingRules> roundingRules = detectionIds.isEmpty()
                ? new ArrayList<>()
                : roundingRulesMapper.selectByDetectionIds(detectionIds);
        
        List<StandardPretreatment> standardPretreatments = detectionIds.isEmpty()
                ? new ArrayList<>()
                : standardPretreatmentMapper.selectByDetectionIds(detectionIds);

        log.info("导出数据统计：" +
                "Detection={}, " +
                "CalculationFormula={}, " +
                "QualityControl={}, " +
                "RoundingRules={}, " +
                "StandardPretreatment={}",
                targetDetections.size(),
                calculationFormulas.size(),
                qualityControls.size(),
                roundingRules.size(),
                standardPretreatments.size());

        // 3. 创建Excel写入器，注册图片处理器
        ExcelWriter excelWriter = EasyExcel.write(outputStream)
                .registerWriteHandler(new FormulaImageHandler())
                .build();

        // 4. 写入各sheet
        // 4.1 标准信息表
        WriteSheet sheet1 = EasyExcel.writerSheet(0, "标准信息表")
                .head(StandardDetectionExcelModel.class)
                .build();
        excelWriter.write(targetDetections, sheet1);

        // 4.2 前处理信息表
        WriteSheet sheet2 = EasyExcel.writerSheet(1, "前处理信息表")
                .head(StandardPretreatmentExcelModel.class)
                .build();
        excelWriter.write(standardPretreatments, sheet2);

        // 4.3 计算公式表（带图片）
        WriteSheet sheet3 = EasyExcel.writerSheet(2, "计算公式表")
                .head(CalculationFormulaExcelModel.class)
                .build();
        excelWriter.write(calculationFormulas, sheet3);

        // 4.4 质量控制表
        WriteSheet sheet4 = EasyExcel.writerSheet(3, "质量控制表")
                .head(QualityControlExcelModel.class)
                .build();
        excelWriter.write(qualityControls, sheet4);

        // 4.5 修约表
        WriteSheet sheet5 = EasyExcel.writerSheet(4, "修约表")
                .head(RoundingRulesExcelModel.class)
                .build();
        excelWriter.write(roundingRules, sheet5);

        // 5. 关闭写入器
        excelWriter.finish();
    }

    // 图片处理器（不变）
    private static class FormulaImageHandler implements RowWriteHandler {
        @Override
        public void afterRowDispose(WriteSheetHolder writeSheetHolder,
                                    WriteTableHolder writeTableHolder,
                                    Row row, Integer relativeRowIndex,
                                    Boolean isHead) {
            if (isHead || row == null || !"计算公式表".equals(writeSheetHolder.getSheetName())) {
                return;
            }

            try {
                Cell cell = row.getCell(25); // 第25列（索引25）
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue();
                    if (cellValue != null && !cellValue.isEmpty()) {
                        Sheet sheet = writeSheetHolder.getSheet();
                        String imgRef = ExcelImageExportUtils.insertImage(
                                cellValue,
                                "formula-" + row.getRowNum(),
                                sheet,
                                row.getRowNum(),
                                25
                        );
                        if (imgRef != null) {
                            cell.setCellValue("");
                            log.info("插入图片到第{}行第25列，引用: {}", row.getRowNum(), imgRef);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("处理行{}图片时出错: {}", row.getRowNum(), e.getMessage());
            }
        }
    }
}