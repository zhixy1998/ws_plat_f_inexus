package com.persee.workstation.uv_inexus.service.parseReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dao.parseReport.CalculationFormulaMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.DetectionMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.QualityControlMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.RoundingRulesMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.StandardPretreatmentMapper;
import com.persee.workstation.uv_inexus.excel.CalculationFormulaExcelModel;
import com.persee.workstation.uv_inexus.excel.ExcelImageImportUtils;
import com.persee.workstation.uv_inexus.excel.QualityControlExcelModel;
import com.persee.workstation.uv_inexus.excel.RoundingRulesExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionUVVISCurveExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionUVVISPhotometryExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardPretreatmentExcelModel;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules;
import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
   
@Service
public class ImportService {
    // 哈希算法：使用SHA-256（识别所有字段变化）
    private static final String HASH_ALGORITHM = "SHA-256";

    @Autowired
    private DetectionMapper detectionMapper;
    @Autowired
    private StandardPretreatmentMapper standardPretreatmentMapper;
    @Autowired
    private RoundingRulesMapper roundingRulesMapper;
    @Autowired
    private QualityControlMapper qualityControlMapper;
    @Autowired
    private CalculationFormulaMapper calculationFormulaMapper;


    // --------------------------- 主表：标准信息表导入 ---------------------------
    public void importStandardDetection(MultipartFile file) {
        try {
            List<StandardDetectionExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(StandardDetectionExcelModel.class)
                    .sheet("标准信息表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                System.out.println("标准信息表数据为空，请检查Excel文件是否正确");
                return;
            }

            // 读取UV-VIS参数表
            List<StandardDetectionUVVISCurveExcelModel> standardCurveList = null;
            try {
                standardCurveList = EasyExcel.read(file.getInputStream())
                        .head(StandardDetectionUVVISCurveExcelModel.class)
                        .sheet("UV-VIS-标曲法-检测参数表")
                        .doReadSync();
            } catch (Exception e) {
                System.out.println("未找到UV-VIS-标曲法-检测参数表，忽略该部分数据");
            }

            List<StandardDetectionUVVISPhotometryExcelModel> photometryList = null;
            try {
                photometryList = EasyExcel.read(file.getInputStream())
                        .head(StandardDetectionUVVISPhotometryExcelModel.class)
                        .sheet("UV-VIS-光度法-检测参数表")
                        .doReadSync();
            } catch (Exception e) {
                System.out.println("未找到UV-VIS-光度法-检测参数表，忽略该部分数据");
            }

            // 合并检测参数
            Map<String, JSONObject> detectionParametersMap = new HashMap<>();
            if (standardCurveList != null) {
                for (StandardDetectionUVVISCurveExcelModel model : standardCurveList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("波长1", model.getWavelength1());
                    jsonObject.put("波长2", model.getWavelength2());
                    jsonObject.put("波长3", model.getWavelength3());
                    jsonObject.put("光谱带宽", model.getSpectralBandwidth());
                    jsonObject.put("拟合公式", model.getFittingFormula());
                    jsonObject.put("推荐标样浓度", model.getRecommendedStandardConcentration());
                    jsonObject.put("推荐单位", model.getRecommendedUnits());
                    detectionParametersMap.put(model.getDetectionId(), jsonObject);
                }
            }

            if (photometryList != null) {
                for (StandardDetectionUVVISPhotometryExcelModel model : photometryList) {
                    if (detectionParametersMap.containsKey(model.getDetectionId())) {
                        JSONObject jsonObject = detectionParametersMap.get(model.getDetectionId());
                        jsonObject.put("波长1", model.getWavelength1());
                        jsonObject.put("波长2", model.getWavelength2());
                        jsonObject.put("波长3", model.getWavelength3());
                    } else {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("波长1", model.getWavelength1());
                        jsonObject.put("波长2", model.getWavelength2());
                        jsonObject.put("波长3", model.getWavelength3());
                        detectionParametersMap.put(model.getDetectionId(), jsonObject);
                    }
                }
            }

            // 处理主表数据
            for (int i = 0; i < list.size(); i++) {
                StandardDetectionExcelModel model = list.get(i);
                int currentRow = i + 2;

                // 校验必填字段
                if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：标准标识不能为空");
                }
                if (model.getIndustry() == null || model.getIndustry().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：行业不能为空");
                }

                // 构建Detection对象
                Detection detection = new Detection();
                detection.setStandardIdentity(model.getStandardIdentity());
                detection.setIndustry(model.getIndustry());
                detection.setDetectionType(model.getDetectionType());
                detection.setDetectionInstrument(model.getDetectionInstrument());
                detection.setStandardNumber(model.getStandardNumber());
                detection.setStandardName(model.getStandardName());
                detection.setDetectionItem(model.getDetectionItem());
                detection.setMethodNumber(model.getMethodNumber());
                detection.setRemark(model.getRemark());
                detection.setReleaseDate(model.getReleaseDate());
                detection.setExpirationDate(model.getExpirationDate());
                detection.setEffectiveDate(model.getEffectiveDate());
                detection.setReleaseStatus(model.getReleaseStatus());
                detection.setIndustryType(model.getIndustryType());

                // 设置检测参数
                if (detectionParametersMap.containsKey(model.getStandardIdentity())) {
                    detection.setDetectionParameters(detectionParametersMap.get(model.getStandardIdentity()).toJSONString());
                } else {
                    JSONObject defaultParams = new JSONObject();
                    if (model.getDetectionParameters() != null && !model.getDetectionParameters().isEmpty()) {
                        try {
                            defaultParams = JSONObject.parseObject(model.getDetectionParameters());
                        } catch (Exception e) {
                            System.out.println("第 " + currentRow + " 行数据错误：解析检测参数失败，使用空参数: " + e.getMessage());
                        }
                    }
                    detection.setDetectionParameters(defaultParams.toJSONString());
                }

                // 主表新增/更新逻辑（基于所有字段哈希）
                String currentHash = generateDetectionHash(detection);
                Detection dbDetection = detectionMapper.selectByStandardIdentity(model.getStandardIdentity());
                if (dbDetection != null) {
                    String dbHash = generateDetectionHash(dbDetection);
                    if (!currentHash.equals(dbHash)) {
                        // 哈希不同：更新
                        detectionMapper.updateByStandardIdentity(detection);
                        System.out.println("第 " + currentRow + " 行: 更新标准信息（哈希变化），标准标识 " + model.getStandardIdentity());
                    } else {
                        // 哈希相同：不操作
                        System.out.println("第 " + currentRow + " 行: 标准信息未变更，标准标识 " + model.getStandardIdentity());
                    }
                } else {
                    // 不存在：插入
                    detectionMapper.insert(detection);
                    System.out.println("第 " + currentRow + " 行: 插入标准信息，标准标识 " + model.getStandardIdentity());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("导入标准信息表时发生异常: " + e.getMessage(), e);
        }
    }

    // 主表哈希（包含所有字段）
    private String generateDetectionHash(Detection detection) {
        String combined = 
            detection.getStandardIdentity() + "|" +
            detection.getIndustry() + "|" +
            (detection.getDetectionType() != null ? detection.getDetectionType() : "") + "|" +
            (detection.getDetectionInstrument() != null ? detection.getDetectionInstrument() : "") + "|" +
            (detection.getStandardNumber() != null ? detection.getStandardNumber() : "") + "|" +
            (detection.getStandardName() != null ? detection.getStandardName() : "") + "|" +
            (detection.getDetectionItem() != null ? detection.getDetectionItem() : "") + "|" +
            (detection.getMethodNumber() != null ? detection.getMethodNumber() : "") + "|" +
            (detection.getRemark() != null ? detection.getRemark() : "") + "|" +
            (detection.getReleaseDate() != null ? detection.getReleaseDate().toString() : "") + "|" +
            (detection.getExpirationDate() != null ? detection.getExpirationDate().toString() : "") + "|" +
            (detection.getEffectiveDate() != null ? detection.getEffectiveDate().toString() : "") + "|" +
            (detection.getReleaseStatus() != null ? detection.getReleaseStatus() : "") + "|" +
            (detection.getIndustryType() != null ? detection.getIndustryType() : "") + "|" +
            (detection.getDetectionParameters() != null ? detection.getDetectionParameters() : "");
        return generateHash(combined);
    }


    // --------------------------- 子表1：前处理信息表导入 ---------------------------
    public void importStandardPretreatment(MultipartFile file) throws IOException {
        try {
            List<StandardPretreatmentExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(StandardPretreatmentExcelModel.class)
                    .sheet("前处理信息表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return;
            }
            
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            int currentRow = 2;

            for (StandardPretreatmentExcelModel model : list) {
                try {
                    // 校验必填字段
                    if (model.getDetectionId() == null || model.getDetectionId().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getPretreatmentDescription() == null || model.getPretreatmentDescription().isEmpty()) {
                        throw new IllegalArgumentException("前处理描述不能为空");
                    }
                    
                    String identityName = model.getDetectionId();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("标准标识在关联表中不存在: " + identityName);
                    }

                    // 构建前处理对象
                    StandardPretreatment pretreatment = new StandardPretreatment();
                    pretreatment.setDetectionId(detectionId.toString()); // 主表ID
                    pretreatment.setPretreatmentDescription(model.getPretreatmentDescription());
                    // 若有其他字段，此处需从Excel获取

                    // 生成当前记录哈希（包含所有字段）
                    String currentHash = generatePretreatmentHash(pretreatment);

                    // 查询数据库中该主表ID下的所有记录
                    List<StandardPretreatment> dbRecords = standardPretreatmentMapper.selectByDetectionId(detectionId.toString());
                    Map<String, StandardPretreatment> hashToRecordMap = new HashMap<>();
                    for (StandardPretreatment record : dbRecords) {
                        hashToRecordMap.put(generatePretreatmentHash(record), record);
                    }

                    // 对比哈希
                    if (hashToRecordMap.containsKey(currentHash)) {
                        System.out.println("第 " + currentRow + " 行: 前处理记录未变更，标准标识ID: " + detectionId);
                    } else {
                        // 检查同描述记录（用于更新）
                        Optional<StandardPretreatment> sameDescRecord = dbRecords.stream()
                                .filter(r -> Objects.equals(r.getPretreatmentDescription(), pretreatment.getPretreatmentDescription()))
                                .findFirst();

                        if (sameDescRecord.isPresent()) {
                            pretreatment.setId(sameDescRecord.get().getId()); // 复用ID
                            standardPretreatmentMapper.updateById(pretreatment);
                            System.out.println("第 " + currentRow + " 行: 更新前处理记录（哈希变化），标准标识ID: " + detectionId);
                        } else {
                            standardPretreatmentMapper.insert(pretreatment);
                            System.out.println("第 " + currentRow + " 行: 插入前处理记录，标准标识ID: " + detectionId);
                        }
                    }

                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("导入前处理信息表时发生异常: " + e.getMessage(), e);
        }
    }

    // 前处理表哈希（包含所有字段，不依赖pretreatmentOrder）
    private String generatePretreatmentHash(StandardPretreatment pretreatment) {
        String combined = 
            (pretreatment.getId() != null ? pretreatment.getId().toString() : "") + "|" +  // 包含ID
            pretreatment.getDetectionId() + "|" +  // 主表ID
            pretreatment.getPretreatmentDescription();  // 描述
        return generateHash(combined);
    }


    // --------------------------- 子表2：计算公式表导入 ---------------------------
    public void importCalculationFormula(MultipartFile file) {
        try {
            Map<String, List<ExcelImageImportUtils.ImageInfo>> allImages = 
                ExcelImageImportUtils.extractAllImages(file);
            List<ExcelImageImportUtils.ImageInfo> formulaSheetImages = 
                allImages.getOrDefault("计算公式表", new ArrayList<>());

            List<CalculationFormulaExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(CalculationFormulaExcelModel.class)
                    .sheet("计算公式表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return;
            }

            System.out.println("读取到 " + list.size() + " 条计算公式数据");
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            int currentRow = 2;

            for (CalculationFormulaExcelModel model : list) {
                try {
                    // 校验必填字段
                    if (model.getDetectionId() == null || model.getDetectionId().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getResultIdentity() == null || model.getResultIdentity().isEmpty()) {
                        throw new IllegalArgumentException("结果标识不能为空");
                    }
                    if (model.getFormulaText() == null || model.getFormulaText().isEmpty()) {
                        throw new IllegalArgumentException("公式文本不能为空");
                    }

                    // 获取主表ID
                    String identityName = model.getDetectionId();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
                    }

                    // 提取图片
                    String base64Image = ExcelImageImportUtils.findImageByRow(formulaSheetImages, currentRow - 1);
                    if (base64Image == null && model.getFormulaImage() != null && model.getFormulaImage().startsWith("data:image/")) {
                        base64Image = model.getFormulaImage();
                    }

                    // 构建计算公式对象
                    CalculationFormula formula = new CalculationFormula();
                    formula.setDetectionId(detectionId.toString()); // 主表ID
                    formula.setResultIdentity(model.getResultIdentity());
                    formula.setResultName(model.getResultName());
                    formula.setUnit(model.getUnit());
                    formula.setInputParam1Name(model.getInputParam1Name());
                    formula.setInputParam1Identity(model.getInputParam1Identity());
                    formula.setInputParam2Name(model.getInputParam2Name());
                    formula.setInputParam2Identity(model.getInputParam2Identity());
                    formula.setInputParam3Name(model.getInputParam3Name());
                    formula.setInputParam3Identity(model.getInputParam3Identity());
                    formula.setInputParam4Name(model.getInputParam4Name());
                    formula.setInputParam4Identity(model.getInputParam4Identity());
                    formula.setInputParam5Name(model.getInputParam5Name());
                    formula.setInputParam5Identity(model.getInputParam5Identity());
                    formula.setInputParam6Name(model.getInputParam6Name());
                    formula.setInputParam6Identity(model.getInputParam6Identity());
                    formula.setInputParam7Name(model.getInputParam7Name());
                    formula.setInputParam7Identity(model.getInputParam7Identity());
                    formula.setInputParam8Name(model.getInputParam8Name());
                    formula.setInputParam8Identity(model.getInputParam8Identity());
                    formula.setInputParam9Name(model.getInputParam9Name());
                    formula.setInputParam9Identity(model.getInputParam9Identity());
                    formula.setInputParam10Name(model.getInputParam10Name());
                    formula.setInputParam10Identity(model.getInputParam10Identity());
                    formula.setFormulaText(model.getFormulaText());
                    formula.setFormulaImage(base64Image);
                    formula.setDetectionLimit(model.getDetectionLimit());

                    // 生成当前哈希（包含所有字段）
                    String currentHash = generateFormulaHash(formula);

                    // 查询数据库中该主表ID下的所有公式
                    List<CalculationFormula> dbFormulas = calculationFormulaMapper.selectByDetectionId(detectionId.toString());
                    Map<String, CalculationFormula> hashToFormulaMap = new HashMap<>();
                    for (CalculationFormula dbFormula : dbFormulas) {
                        hashToFormulaMap.put(generateFormulaHash(dbFormula), dbFormula);
                    }

                    // 对比哈希
                    if (hashToFormulaMap.containsKey(currentHash)) {
                        System.out.println("第 " + currentRow + " 行: 公式记录未变更，标准标识ID: " + detectionId);
                    } else {
                        // 检查同结果标识记录
                        Optional<CalculationFormula> sameResultRecord = dbFormulas.stream()
                                .filter(f -> Objects.equals(f.getResultIdentity(), formula.getResultIdentity()))
                                .findFirst();

                        if (sameResultRecord.isPresent()) {
                            formula.setId(sameResultRecord.get().getId());
                            calculationFormulaMapper.updateById(formula);
                            System.out.println("第 " + currentRow + " 行: 更新公式记录（哈希变化），标准标识ID: " + detectionId);
                        } else {
                            calculationFormulaMapper.insert(formula);
                            System.out.println("第 " + currentRow + " 行: 插入公式记录，标准标识ID: " + detectionId);
                        }
                    }

                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("导入计算公式表时发生异常: " + e.getMessage(), e);
        }
    }

    // 计算公式表哈希（包含所有字段）
    private String generateFormulaHash(CalculationFormula formula) {
        String combined = 
            (formula.getId() != null ? formula.getId().toString() : "") + "|" +  // ID
            formula.getDetectionId() + "|" +  // 主表ID
            formula.getResultIdentity() + "|" +
            (formula.getResultName() != null ? formula.getResultName() : "") + "|" +
            (formula.getUnit() != null ? formula.getUnit() : "") + "|" +
            (formula.getInputParam1Name() != null ? formula.getInputParam1Name() : "") + "|" +
            (formula.getInputParam1Identity() != null ? formula.getInputParam1Identity() : "") + "|" +
            (formula.getInputParam2Name() != null ? formula.getInputParam2Name() : "") + "|" +
            (formula.getInputParam2Identity() != null ? formula.getInputParam2Identity() : "") + "|" +
            (formula.getInputParam3Name() != null ? formula.getInputParam3Name() : "") + "|" +
            (formula.getInputParam3Identity() != null ? formula.getInputParam3Identity() : "") + "|" +
            (formula.getInputParam4Name() != null ? formula.getInputParam4Name() : "") + "|" +
            (formula.getInputParam4Identity() != null ? formula.getInputParam4Identity() : "") + "|" +
            (formula.getInputParam5Name() != null ? formula.getInputParam5Name() : "") + "|" +
            (formula.getInputParam5Identity() != null ? formula.getInputParam5Identity() : "") + "|" +
            (formula.getInputParam6Name() != null ? formula.getInputParam6Name() : "") + "|" +
            (formula.getInputParam6Identity() != null ? formula.getInputParam6Identity() : "") + "|" +
            (formula.getInputParam7Name() != null ? formula.getInputParam7Name() : "") + "|" +
            (formula.getInputParam7Identity() != null ? formula.getInputParam7Identity() : "") + "|" +
            (formula.getInputParam8Name() != null ? formula.getInputParam8Name() : "") + "|" +
            (formula.getInputParam8Identity() != null ? formula.getInputParam8Identity() : "") + "|" +
            (formula.getInputParam9Name() != null ? formula.getInputParam9Name() : "") + "|" +
            (formula.getInputParam9Identity() != null ? formula.getInputParam9Identity() : "") + "|" +
            (formula.getInputParam10Name() != null ? formula.getInputParam10Name() : "") + "|" +
            (formula.getInputParam10Identity() != null ? formula.getInputParam10Identity() : "") + "|" +
            formula.getFormulaText() + "|" +
            (formula.getFormulaImage() != null ? formula.getFormulaImage() : "") + "|" +
            (formula.getDetectionLimit() != null ? formula.getDetectionLimit() : "");
        return generateHash(combined);
    }


    // --------------------------- 子表3：质量控制表导入 ---------------------------
    public void importQualityControl(MultipartFile file) throws IOException {
        try {
            List<QualityControlExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(QualityControlExcelModel.class)
                    .sheet("质量控制表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return;
            }

            int currentRow = 2;
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();

            for (QualityControlExcelModel model : list) {
                try {
                    if (model.getDetectionId() == null || model.getDetectionId().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getControlType() == null || model.getControlType().isEmpty()) {
                        throw new IllegalArgumentException("控制类型不能为空");
                    }

                    // 获取主表ID
                    String identityName = model.getDetectionId();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
                    }

                    // 构建质量控制对象
                    QualityControl control = new QualityControl();
                    control.setDetectionId(detectionId.toString()); // 主表ID
                    control.setControlType(model.getControlType());
                    control.setControlDescription(model.getControlDescription());
                    control.setControlFormula(model.getControlFormula());

                    // 生成当前哈希（包含所有字段）
                    String currentHash = generateControlHash(control);

                    // 查询数据库中该主表ID下的所有记录
                    List<QualityControl> dbControls = qualityControlMapper.selectByDetectionId(detectionId.toString());
                    Map<String, QualityControl> hashToControlMap = new HashMap<>();
                    for (QualityControl dbControl : dbControls) {
                        hashToControlMap.put(generateControlHash(dbControl), dbControl);
                    }

                    // 对比哈希
                    if (hashToControlMap.containsKey(currentHash)) {
                        System.out.println("第 " + currentRow + " 行: 质量控制记录未变更，标准标识ID: " + detectionId);
                    } else {
                        // 检查同类型记录
                        Optional<QualityControl> sameTypeRecord = dbControls.stream()
                                .filter(c -> Objects.equals(c.getControlType(), control.getControlType()))
                                .findFirst();

                        if (sameTypeRecord.isPresent()) {
                            control.setId(sameTypeRecord.get().getId());
                            qualityControlMapper.updateById(control);
                            System.out.println("第 " + currentRow + " 行: 更新质量控制记录（哈希变化），标准标识ID: " + detectionId);
                        } else {
                            qualityControlMapper.insert(control);
                            System.out.println("第 " + currentRow + " 行: 插入质量控制记录，标准标识ID: " + detectionId);
                        }
                    }

                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }
            System.out.println("质量控制表导入完成。");
        } catch (Exception e) {
            throw new IllegalArgumentException("导入质量控制表时发生异常: " + e.getMessage(), e);
        }
    }

    // 质量控制表哈希（包含所有字段）
    private String generateControlHash(QualityControl control) {
        String combined = 
            (control.getId() != null ? control.getId().toString() : "") + "|" +  // ID
            control.getDetectionId() + "|" +  // 主表ID
            control.getControlType() + "|" +
            (control.getControlDescription() != null ? control.getControlDescription() : "") + "|" +
            (control.getControlFormula() != null ? control.getControlFormula() : "");
        return generateHash(combined);
    }


    // --------------------------- 子表4：修约表导入 ---------------------------
    public void importRoundingRules(MultipartFile file) {
        try {
            List<RoundingRulesExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(RoundingRulesExcelModel.class)
                    .sheet("修约表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return;
            }

            int currentRow = 2;
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();

            for (RoundingRulesExcelModel model : list) {
                try {
                    // 校验必填字段
                    if (model.getDetectionId() == null || model.getDetectionId().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getResultIdentity() == null || model.getResultIdentity().isEmpty()) {
                        throw new IllegalArgumentException("结果标识不能为空");
                    }
                    if (model.getRoundingType() == null || model.getRoundingType().isEmpty()) {
                        throw new IllegalArgumentException("修约类型不能为空");
                    }
                    if (model.getDecimalPlaces() == null) {
                        throw new IllegalArgumentException("位数不能为空");
                    }

                    // 获取主表ID
                    String identityName = model.getDetectionId();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
                    }

                    // 构建修约规则对象
                    RoundingRules rules = new RoundingRules();
                    rules.setDetectionId(detectionId.toString()); // 主表ID
                    rules.setResultIdentity(model.getResultIdentity());
                    rules.setRoundingType(model.getRoundingType());
                    rules.setRoundingCondition(model.getRoundingCondition());
                    rules.setDecimalPlaces(model.getDecimalPlaces());

                    // 生成当前哈希（包含所有字段）
                    String currentHash = generateRoundingHash(rules);

                    // 查询数据库中该主表ID下的所有记录
                    List<RoundingRules> dbRules = roundingRulesMapper.selectByDetectionId(detectionId.toString());
                    Map<String, RoundingRules> hashToRulesMap = new HashMap<>();
                    for (RoundingRules dbRule : dbRules) {
                        hashToRulesMap.put(generateRoundingHash(dbRule), dbRule);
                    }

                    // 对比哈希
                    if (hashToRulesMap.containsKey(currentHash)) {
                        System.out.println("第 " + currentRow + " 行: 修约规则未变更，标准标识ID: " + detectionId);
                    } else {
                        // 检查同结果标识记录
                        Optional<RoundingRules> sameResultRecord = dbRules.stream()
                                .filter(r -> Objects.equals(r.getResultIdentity(), rules.getResultIdentity()))
                                .findFirst();

                        if (sameResultRecord.isPresent()) {
                            rules.setId(sameResultRecord.get().getId());
                            roundingRulesMapper.updateById(rules);
                            System.out.println("第 " + currentRow + " 行: 更新修约规则（哈希变化），标准标识ID: " + detectionId);
                        } else {
                            roundingRulesMapper.insert(rules);
                            System.out.println("第 " + currentRow + " 行: 插入修约规则，标准标识ID: " + detectionId);
                        }
                    }

                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("导入修约表时发生异常: " + e.getMessage(), e);
        }
    }

    // 修约表哈希（包含所有字段）
    private String generateRoundingHash(RoundingRules rules) {
        String combined = 
            (rules.getId() != null ? rules.getId().toString() : "") + "|" +  // ID
            rules.getDetectionId() + "|" +  // 主表ID
            rules.getResultIdentity() + "|" +
            rules.getRoundingType() + "|" +
            (rules.getRoundingCondition() != null ? rules.getRoundingCondition() : "") + "|" +
            rules.getDecimalPlaces().toString();
        return generateHash(combined);
    }


    // --------------------------- 公共工具方法 ---------------------------
    // 通用哈希生成方法（SHA-256）
    private String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes); // 转为Base64便于存储和对比
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("哈希算法初始化失败", e);
        }
    }

    // 获取主表ID（缓存逻辑）
    private Integer getDetectionId(String identityName, Map<String, Integer> cache) {
        if (cache.containsKey(identityName)) {
            return cache.get(identityName);
        }
        Integer detectionId = detectionMapper.findIdByStandardIdentityName(identityName);
        if (detectionId != null) {
            cache.put(identityName, detectionId);
        }
        return detectionId;
    }

    // 导入所有表
    public void importExcel(MultipartFile file) throws IOException {
        importStandardDetection(file);
        importStandardPretreatment(file);
        importRoundingRules(file);
        importQualityControl(file);
        importCalculationFormula(file);
    }
}