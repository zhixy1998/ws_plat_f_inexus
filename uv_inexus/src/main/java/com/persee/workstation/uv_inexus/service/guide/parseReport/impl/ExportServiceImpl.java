package com.persee.workstation.uv_inexus.service.guide.parseReport.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.persee.workstation.uv_inexus.dao.guide.parseReport.CalculationFormulaMapper;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.DetectionDao;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.QualityControlMapper;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.RoundingRulesMapper;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.StandardPretreatmentMapper;
import com.persee.workstation.uv_inexus.excel.CalculationFormulaExcelModel;
import com.persee.workstation.uv_inexus.excel.ExcelImageExportUtils;
import com.persee.workstation.uv_inexus.excel.QualityControlExcelModel;
import com.persee.workstation.uv_inexus.excel.RoundingRulesExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardPretreatmentExcelModel;
import com.persee.workstation.uv_inexus.po.guide.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.guide.parseReport.Detection;
import com.persee.workstation.uv_inexus.po.guide.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.po.guide.parseReport.RoundingRules;
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.service.guide.parseReport.ExportService;

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
    private DetectionDao detectionDao;
    @Autowired
    private StandardPretreatmentMapper standardPretreatmentMapper;

    // 核心优化1：缓存图片数据（key=Excel行索引，value=Base64字符串），不写入单元格
    private Map<Integer, String> formulaImageCache = new HashMap<>();

    @Override
    public void exportAllTables(OutputStream outputStream, Map<String, Object> queryParams) throws IOException {
        try {
            formulaImageCache.clear(); // 清理缓存
            QueryWrapper<Detection> queryWrapper = buildQueryWrapper(queryParams);
            List<Detection> allDetections = detectionDao.selectList(queryWrapper);
            log.info("全量导出（带筛选），共获取{}条原始Detection记录", allDetections.size());
            exportData(outputStream, allDetections);
        } finally {
            formulaImageCache.clear(); // 释放资源
        }
    }

    @Override
    public void exportByCondition(OutputStream outputStream, Integer isAll, String exportIds, Map<String, Object> queryParams) throws IOException {
        try {
            formulaImageCache.clear(); // 清理缓存
            List<Detection> targetDetections;

            if (isAll == 1) {
                QueryWrapper<Detection> queryWrapper = buildQueryWrapper(queryParams);
                targetDetections = detectionDao.selectList(queryWrapper);
                log.info("全量导出（带筛选），共获取{}条原始Detection记录", targetDetections.size());
            } else {
                if (exportIds == null || exportIds.isEmpty()) {
                    throw new IllegalArgumentException("当isAll=0时，必须提供exportIds参数");
                }

                List<String> exportIdList = Arrays.stream(exportIds.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .collect(Collectors.toList());

                if (exportIdList.isEmpty()) {
                    throw new IllegalArgumentException("exportIds参数格式错误，应为逗号分隔的ID列表");
                }

                QueryWrapper<Detection> queryWrapper = buildQueryWrapper(queryParams);
                queryWrapper.in("id", exportIdList);

                List<Detection> baseDetections = detectionDao.selectList(queryWrapper);
                if (baseDetections.isEmpty()) {
                    log.warn("exportIds对应的Detection记录为空");
                    targetDetections = new ArrayList<>();
                } else {
                    targetDetections = baseDetections;
                    log.info("按ID筛选后共获取{}条原始Detection记录（未去重）", targetDetections.size());
                }
            }

            exportData(outputStream, targetDetections);
        } finally {
            formulaImageCache.clear(); // 释放资源
        }
    }

    // 在ExportServiceImpl中检查buildQueryWrapper方法
    private QueryWrapper<Detection> buildQueryWrapper(Map<String, Object> queryParams) {
        QueryWrapper<Detection> queryWrapper = new QueryWrapper<>();
        if (queryParams == null || queryParams.isEmpty()) {
            log.info("无筛选参数，查询所有数据");
            return queryWrapper;
        }

        // 1. 标准名称（模糊查询）：参数非空才添加条件
        Object standardNameObj = queryParams.get("standard_name");
        if (standardNameObj != null) {
            String standardName = standardNameObj.toString().trim();
            // 仅当参数非空时才添加筛选条件
            if (!standardName.isEmpty()) {
                queryWrapper.like("standard_name", standardName);
                log.info("添加筛选条件：standard_name LIKE %{}%", standardName);
            } else {
                log.info("standard_name为空，不筛选该条件");
            }
        }

        // 2. 行业（精确匹配）：参数非空才添加条件
        Object industryObj = queryParams.get("industry");
        if (industryObj != null) {
            String industry = industryObj.toString().trim();
            // 仅当参数非空时才添加筛选条件
            if (!industry.isEmpty()) {
                queryWrapper.eq("industry", industry);
                log.info("添加筛选条件：industry = {}", industry);
            } else {
                log.info("industry为空，不筛选该条件");
            }
        }

        // 3. 发布日期（精确匹配）：参数为空则不筛选该条件（任何时间都匹配）
        Object releaseDateObj = queryParams.get("release_date");
        if (releaseDateObj != null) {
            String releaseDate = releaseDateObj.toString().trim();
            // 仅当参数非空且格式有效时，才添加条件
            if (!releaseDate.isEmpty() && isValidDate(releaseDate)) {
                queryWrapper.eq("release_date", releaseDate);
                log.info("添加筛选条件：release_date = {}", releaseDate);
            } else {
                log.info("release_date为空或格式无效，不筛选该时间条件");
            }
        }

        // 4. 日期范围：开始日期（>=）为空则不筛选，非空才添加
        Object startDateObj = queryParams.get("startDate");
        if (startDateObj != null) {
            String startDate = startDateObj.toString().trim();
            // 仅当参数非空且格式有效时，才添加条件
            if (!startDate.isEmpty() && isValidDate(startDate)) {
                queryWrapper.ge("release_date", startDate);
                log.info("添加筛选条件：release_date >= {}", startDate);
            } else {
                log.info("startDate为空或格式无效，不筛选开始时间条件");
            }
        }

        // 5. 日期范围：结束日期（<=）为空则不筛选，非空才添加
        Object endDateObj = queryParams.get("endDate");
        if (endDateObj != null) {
            String endDate = endDateObj.toString().trim();
            // 仅当参数非空且格式有效时，才添加条件
            if (!endDate.isEmpty() && isValidDate(endDate)) {
                queryWrapper.le("release_date", endDate);
                log.info("添加筛选条件：release_date <= {}", endDate);
            } else {
                log.info("endDate为空或格式无效，不筛选结束时间条件");
            }
        }

        log.info("构建的筛选条件：{}", queryWrapper.getCustomSqlSegment());
        return queryWrapper;
    }

    // 日期格式校验（支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
    private boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return false;
        }
        // 正则匹配常见日期格式
        String datePattern = "^\\d{4}-\\d{2}-\\d{2}(\\s\\d{2}:\\d{2}:\\d{2})?$";
        return dateStr.matches(datePattern);
    }

    private void exportData(OutputStream outputStream, List<Detection> targetDetections) throws IOException {
        Set<String> detectionIds = targetDetections.stream()
                .map(detection -> String.valueOf(detection.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("关联子表的Detection ID共{}个", detectionIds.size());

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

        // 核心优化2：提前缓存图片数据（不写入Excel单元格）
        cacheFormulaImages(calculationFormulas);

        ExcelWriter excelWriter = EasyExcel.write(outputStream)
                .registerWriteHandler(new FormulaImageHandler()) // 注册图片处理器
                .build();

        WriteSheet sheet1 = EasyExcel.writerSheet(0, "标准信息表")
                .head(StandardDetectionExcelModel.class)
                .build();
        excelWriter.write(convertToStandardModels(targetDetections), sheet1);

        WriteSheet sheet2 = EasyExcel.writerSheet(1, "前处理信息表")
                .head(StandardPretreatmentExcelModel.class)
                .build();
        excelWriter.write(convertToPretreatmentModels(standardPretreatments), sheet2);

        WriteSheet sheet3 = EasyExcel.writerSheet(2, "计算公式表")
                .head(CalculationFormulaExcelModel.class)
                .build();
        // 核心优化3：写入的模型中formulaImage已为空，避免Base64写入单元格
        excelWriter.write(convertToFormulaModels(calculationFormulas), sheet3);

        WriteSheet sheet4 = EasyExcel.writerSheet(3, "质量控制表")
                .head(QualityControlExcelModel.class)
                .build();
        excelWriter.write(convertToQualityModels(qualityControls), sheet4);

        WriteSheet sheet5 = EasyExcel.writerSheet(4, "修约表")
                .head(RoundingRulesExcelModel.class)
                .build();
        excelWriter.write(convertToRoundingModels(roundingRules), sheet5);

        excelWriter.finish();
    }

    // 核心优化4：缓存图片数据（计算Excel行索引与Base64的对应关系）
    private void cacheFormulaImages(List<CalculationFormula> formulas) {
        // Excel表头占1行（索引0），数据从第1行（索引1）开始
        int dataStartRowIndex = 1;
        for (int i = 0; i < formulas.size(); i++) {
            CalculationFormula formula = formulas.get(i);
            if (formula.getFormulaImage() != null && !formula.getFormulaImage().isEmpty()) {
                // 计算当前数据在Excel中的行索引（表头行 + 数据索引）
                int excelRowIndex = dataStartRowIndex + i;
                formulaImageCache.put(excelRowIndex, formula.getFormulaImage());
                log.info("缓存计算公式表第{}行图片（Base64长度：{}）", excelRowIndex, formula.getFormulaImage().length());
            }
        }
    }

    // 模型转换方法（核心优化5：清空formulaImage，避免Base64写入单元格）
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
            // 关键：不设置Base64到单元格，避免超长
            model.setFormulaImage(""); 
            model.setDetectionLimit(formula.getDetectionLimit());
            return model;
        }).collect(Collectors.toList());
    }

    // 其他模型转换方法（保持不变）
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

    // 图片处理器（核心优化6：从缓存读取Base64，直接转换为图片插入，不读取单元格）
    private class FormulaImageHandler implements RowWriteHandler {
        // 图片所在列索引（必须与CalculationFormulaExcelModel中formulaImage的@ExcelProperty(index)一致）
        private static final int IMAGE_COLUMN_INDEX = 25;

        @Override
        public void afterRowDispose(WriteSheetHolder writeSheetHolder,
                                    WriteTableHolder writeTableHolder,
                                    Row row, Integer relativeRowIndex,
                                    Boolean isHead) { // 修复参数类型：Boolean -> boolean
            // 只处理"计算公式表"的非表头行
            if (isHead || row == null || !"计算公式表".equals(writeSheetHolder.getSheetName())) {
                return;
            }

            try {
                // 获取当前行在Excel中的索引
                int currentRowIndex = row.getRowNum();
                // 从缓存获取Base64（不读取单元格内容）
                String base64Image = formulaImageCache.get(currentRowIndex);
                if (base64Image == null || base64Image.isEmpty()) {
                    log.debug("计算公式表第{}行无图片，跳过", currentRowIndex);
                    return;
                }

                // 调用工具类插入图片（直接从缓存Base64转换）
                Sheet sheet = writeSheetHolder.getSheet();
                String imgRef = ExcelImageExportUtils.insertImage(
                        base64Image,
                        "formula-" + currentRowIndex,
                        sheet,
                        currentRowIndex,
                        IMAGE_COLUMN_INDEX
                );

                if (imgRef != null) {
                    log.info("插入图片到第{}行第{}列，引用: {}", currentRowIndex, IMAGE_COLUMN_INDEX, imgRef);
                } else {
                    log.warn("第{}行图片插入失败（工具类返回空）", currentRowIndex);
                }

            } catch (Exception e) {
                log.error("处理第{}行图片时出错: {}", row.getRowNum(), e.getMessage(), e);
            }
        }

        // 接口其他方法（空实现，避免编译错误）
        // @Override
        // public void beforeRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
        //                            Integer rowIndex, Integer relativeRowIndex, boolean isHead) {}

        // @Override
        // public void afterRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
        //                           Row row, Integer relativeRowIndex, boolean isHead) {}

        // @Override
        // public void beforeRowWrite(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
        //                           Row row, Integer relativeRowIndex, boolean isHead) {}
    }
}