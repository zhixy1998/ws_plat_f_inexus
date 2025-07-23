package com.persee.workstation.uv_inexus.service.parseReport.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.parseReport.CalculationFormulaMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.DetectionDao; // 注意：此处使用你的DetectionDao（对应原DetectionMapper）
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
    private DetectionDao detectionDao; // 对应你的DetectionDao（原DetectionMapper）
    @Autowired
    private StandardPretreatmentMapper standardPretreatmentMapper;

    // 全量导出（支持筛选参数，保持原有功能）
    @Override
    public void exportAllTables(OutputStream outputStream, Map<String, Object> queryParams) throws IOException {
        // 构建查询条件
        QueryWrapper<Detection> queryWrapper = buildQueryWrapper(queryParams);
        // 查询所有符合条件的原始数据（不去重）
        List<Detection> allDetections = detectionDao.selectList(queryWrapper);
        log.info("全量导出（带筛选），共获取{}条原始Detection记录", allDetections.size());
        // 执行导出
        exportData(outputStream, allDetections);
    }

    // 按条件导出（核心修改：保留原始数据，不去重）
    @Override
    public void exportByCondition(OutputStream outputStream, Integer isAll, String exportIds, Map<String, Object> queryParams) throws IOException {
        List<Detection> targetDetections;

        if (isAll == 1) {
            // 全量导出 + 筛选参数
            QueryWrapper<Detection> queryWrapper = buildQueryWrapper(queryParams);
            targetDetections = detectionDao.selectList(queryWrapper);
            log.info("全量导出（带筛选），共获取{}条原始Detection记录", targetDetections.size());
        } else {
            // 按ID筛选导出（保留原始数据，不去重）
            if (exportIds == null || exportIds.isEmpty()) {
                throw new IllegalArgumentException("当isAll=0时，必须提供exportIds参数");
            }
            
            // 1. 解析exportIds为ID列表
            List<String> exportIdList = Arrays.stream(exportIds.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());
            
            if (exportIdList.isEmpty()) {
                throw new IllegalArgumentException("exportIds参数格式错误，应为逗号分隔的ID列表");
            }

            // 2. 构建查询条件（包含筛选参数+ID列表）
            QueryWrapper<Detection> queryWrapper = buildQueryWrapper(queryParams);
            queryWrapper.in("id", exportIdList); // 直接按ID筛选，不进行去重

            // 3. 查询原始Detection记录（保留所有匹配的记录，不去重）
            List<Detection> baseDetections = detectionDao.selectList(queryWrapper);
            if (baseDetections.isEmpty()) {
                log.warn("exportIds对应的Detection记录为空");
                targetDetections = new ArrayList<>();
            } else {
                // 直接使用原始查询结果（不去重）
                targetDetections = baseDetections;
                log.info("按ID筛选后共获取{}条原始Detection记录（未去重）", targetDetections.size());
            }
        }

        // 执行导出（关联子表数据）
        exportData(outputStream, targetDetections);
    }

    // 构建查询条件（支持筛选参数）
    private QueryWrapper<Detection> buildQueryWrapper(Map<String, Object> queryParams) {
        QueryWrapper<Detection> queryWrapper = new QueryWrapper<>();
        if (queryParams == null || queryParams.isEmpty()) {
            return queryWrapper;
        }

        // 处理standard_name（模糊查询）
        if (queryParams.containsKey("standard_name")) {
            String standardName = (String) queryParams.get("standard_name");
            if (standardName != null && !standardName.isEmpty()) {
                queryWrapper.like("standard_name", standardName);
            }
        }

        // 处理industry（精确匹配）
        if (queryParams.containsKey("industry")) {
            String industry = (String) queryParams.get("industry");
            if (industry != null && !industry.isEmpty()) {
                queryWrapper.eq("industry", industry);
            }
        }

        // 处理release_date（精确匹配）
        if (queryParams.containsKey("release_date")) {
            String releaseDate = (String) queryParams.get("release_date");
            if (releaseDate != null && !releaseDate.isEmpty()) {
                queryWrapper.eq("release_date", releaseDate);
            }
        }

        // 处理日期范围（startDate <= release_date <= endDate）
        if (queryParams.containsKey("startDate")) {
            String startDate = (String) queryParams.get("startDate");
            if (startDate != null && !startDate.isEmpty()) {
                queryWrapper.ge("release_date", startDate);
            }
        }
        if (queryParams.containsKey("endDate")) {
            String endDate = (String) queryParams.get("endDate");
            if (endDate != null && !endDate.isEmpty()) {
                queryWrapper.le("release_date", endDate);
            }
        }

        return queryWrapper;
    }

    // 核心导出逻辑（保持不变，关联所有子表数据）
    private void exportData(OutputStream outputStream, List<Detection> targetDetections) throws IOException {
        // 1. 提取所有原始Detection的id（用于关联子表）
        Set<String> detectionIds = targetDetections.stream()
                .map(detection -> String.valueOf(detection.getId())) // 假设id是Integer类型，转为String
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("关联子表的Detection ID共{}个", detectionIds.size());

        // 2. 查询所有关联子表数据（基于原始id）
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

        // 3. 创建Excel并写入数据
        ExcelWriter excelWriter = EasyExcel.write(outputStream)
                .registerWriteHandler(new FormulaImageHandler()) // 保留图片处理
                .build();

        // 4. 写入标准信息表（主表）
        WriteSheet sheet1 = EasyExcel.writerSheet(0, "标准信息表")
                .head(StandardDetectionExcelModel.class)
                .build();
        excelWriter.write(convertToStandardModels(targetDetections), sheet1);

        // 5. 写入前处理信息表
        WriteSheet sheet2 = EasyExcel.writerSheet(1, "前处理信息表")
                .head(StandardPretreatmentExcelModel.class)
                .build();
        excelWriter.write(convertToPretreatmentModels(standardPretreatments), sheet2);

        // 6. 写入计算公式表（带图片）
        WriteSheet sheet3 = EasyExcel.writerSheet(2, "计算公式表")
                .head(CalculationFormulaExcelModel.class)
                .build();
        excelWriter.write(convertToFormulaModels(calculationFormulas), sheet3);

        // 7. 写入质量控制表
        WriteSheet sheet4 = EasyExcel.writerSheet(3, "质量控制表")
                .head(QualityControlExcelModel.class)
                .build();
        excelWriter.write(convertToQualityModels(qualityControls), sheet4);

        // 8. 写入修约表
        WriteSheet sheet5 = EasyExcel.writerSheet(4, "修约表")
                .head(RoundingRulesExcelModel.class)
                .build();
        excelWriter.write(convertToRoundingModels(roundingRules), sheet5);

        // 9. 完成导出
        excelWriter.finish();
    }

    // 模型转换方法（保持不变）
    private List<StandardDetectionExcelModel> convertToStandardModels(List<Detection> detections) {
        return detections.stream().map(detection -> {
            StandardDetectionExcelModel model = new StandardDetectionExcelModel();
            model.setStandardIdentity(detection.getStandardIdentity());
            model.setIndustry(detection.getIndustry());
            model.setDetectionType(detection.getDetectionType());
            model.setDetectionInstrument(detection.getDetectionInstrument());
            model.setStandardNumber(detection.getStandardNumber());
            model.setStandardName(detection.getStandardName());
            model.setDetectionItem(detection.getDetectionItem());
            model.setMethodNumber(detection.getMethodNumber());
            model.setRemark(detection.getRemark());
            model.setDetectionParameters(detection.getDetectionParameters());
            model.setReleaseDate(detection.getReleaseDate());
            model.setExpirationDate(detection.getExpirationDate());
            model.setEffectiveDate(detection.getEffectiveDate());
            model.setReleaseStatus(detection.getReleaseStatus());
            model.setIndustryType(detection.getIndustryType());
            return model;
        }).collect(Collectors.toList());
    }

    private List<StandardPretreatmentExcelModel> convertToPretreatmentModels(List<StandardPretreatment> pretreatments) {
        return pretreatments.stream().map(pretreatment -> {
            StandardPretreatmentExcelModel model = new StandardPretreatmentExcelModel();
            model.setDetectionId(pretreatment.getDetectionId());
            model.setPretreatmentDescription(pretreatment.getPretreatmentDescription());
            return model;
        }).collect(Collectors.toList());
    }

    private List<CalculationFormulaExcelModel> convertToFormulaModels(List<CalculationFormula> formulas) {
        return formulas.stream().map(formula -> {
            CalculationFormulaExcelModel model = new CalculationFormulaExcelModel();
            model.setDetectionId(formula.getDetectionId());
            model.setResultIdentity(formula.getResultIdentity());
            model.setResultName(formula.getResultName());
            model.setUnit(formula.getUnit());
            model.setInputParam1Name(formula.getInputParam1Name());
            model.setInputParam1Identity(formula.getInputParam1Identity());
            model.setInputParam2Name(formula.getInputParam2Name());
            model.setInputParam2Identity(formula.getInputParam2Identity());
            model.setInputParam3Name(formula.getInputParam3Name());
            model.setInputParam3Identity(formula.getInputParam3Identity());
            model.setInputParam4Name(formula.getInputParam4Name());
            model.setInputParam4Identity(formula.getInputParam4Identity());
            model.setInputParam5Name(formula.getInputParam5Name());
            model.setInputParam5Identity(formula.getInputParam5Identity());
            model.setFormulaText(formula.getFormulaText());
            model.setFormulaImage(formula.getFormulaImage());
            model.setDetectionLimit(formula.getDetectionLimit());
            return model;
        }).collect(Collectors.toList());
    }

    private List<QualityControlExcelModel> convertToQualityModels(List<QualityControl> controls) {
        return controls.stream().map(control -> {
            QualityControlExcelModel model = new QualityControlExcelModel();
            model.setDetectionId(control.getDetectionId());
            model.setControlType(control.getControlType());
            model.setControlDescription(control.getControlDescription());
            model.setControlFormula(control.getControlFormula());
            return model;
        }).collect(Collectors.toList());
    }

    private List<RoundingRulesExcelModel> convertToRoundingModels(List<RoundingRules> rules) {
        return rules.stream().map(rule -> {
            RoundingRulesExcelModel model = new RoundingRulesExcelModel();
            model.setDetectionId(rule.getDetectionId());
            model.setResultIdentity(rule.getResultIdentity());
            model.setRoundingType(rule.getRoundingType());
            model.setRoundingCondition(rule.getRoundingCondition());
            model.setDecimalPlaces(rule.getDecimalPlaces());
            return model;
        }).collect(Collectors.toList());
    }

    // 图片处理器（修复方法参数类型，保持功能不变）
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

        // 修复其他方法的参数类型（Boolean -> boolean）
        // @Override
        // public void beforeRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Integer rowIndex, Integer relativeRowIndex, boolean isHead) {}

        // @Override
        // public void afterRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, boolean isHead) {}

        // @Override
        // public void beforeRowWrite(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, boolean isHead) {}
    }
}