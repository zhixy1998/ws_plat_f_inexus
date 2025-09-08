package com.persee.workstation.uv_inexus.service.guide.parseReport;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.CalculationFormulaMapper;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.DetectionMapper;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.QualityControlMapper;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.RoundingRulesMapper;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.StandardPretreatmentMapper;
import com.persee.workstation.uv_inexus.excel.CalculationFormulaExcelModel;
import com.persee.workstation.uv_inexus.excel.ExcelImageImportUtils;
import com.persee.workstation.uv_inexus.excel.QualityControlExcelModel;
import com.persee.workstation.uv_inexus.excel.RoundingRulesExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionUVVISCurveExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardDetectionUVVISPhotometryExcelModel;
import com.persee.workstation.uv_inexus.excel.StandardPretreatmentExcelModel;
import com.persee.workstation.uv_inexus.po.guide.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.guide.parseReport.Detection;
import com.persee.workstation.uv_inexus.po.guide.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.po.guide.parseReport.RoundingRules;
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;
   
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
    // 修改：返回当前表的错误信息列表（不再中途抛出异常）
    @Transactional
    public List<String> importStandardDetection(MultipartFile file) {
        List<String> errorMessages = new ArrayList<>();
        try {
            // 读取主表数据
            List<StandardDetectionExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(StandardDetectionExcelModel.class)
                    .sheet("标准信息表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                errorMessages.add("标准信息表数据为空，请检查Excel文件是否正确");
                return errorMessages; // 空表也算错误，加入列表后返回
            }

            // 读取UV-VIS参数表
            List<StandardDetectionUVVISCurveExcelModel> standardCurveList = null;
            try {
                standardCurveList = EasyExcel.read(file.getInputStream())
                        .head(StandardDetectionUVVISCurveExcelModel.class)
                        .sheet("UV-VIS-标曲法-检测参数表")
                        .doReadSync();
            } catch (Exception e) {
                errorMessages.add("标准信息表：未找到UV-VIS-标曲法-检测参数表，忽略该部分数据");
            }

            List<StandardDetectionUVVISPhotometryExcelModel> photometryList = null;
            try {
                photometryList = EasyExcel.read(file.getInputStream())
                        .head(StandardDetectionUVVISPhotometryExcelModel.class)
                        .sheet("UV-VIS-光度法-检测参数表")
                        .doReadSync();
            } catch (Exception e) {
                errorMessages.add("标准信息表：未找到UV-VIS-光度法-检测参数表，忽略该部分数据");
            }

            // 合并检测参数
            Map<String, JSONObject> detectionParametersMap = new HashMap<>();
            if (standardCurveList != null) {
                for (StandardDetectionUVVISCurveExcelModel model : standardCurveList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("波长1", model.getWavelength1());
                    jsonObject.put("波长2", model.getWavelength2());
                    jsonObject.put("波长3", model.getWavelength3());
                    jsonObject.put("系数1", model.getCoefficient1());
                    jsonObject.put("系数2", model.getCoefficient2());
                    jsonObject.put("系数3", model.getCoefficient3());
                    jsonObject.put("光谱带宽", model.getSpectralBandwidth());
                    jsonObject.put("拟合公式", model.getFittingFormula());
                    jsonObject.put("推荐标样浓度", model.getRecommendedStandardConcentration());
                    jsonObject.put("推荐单位", model.getRecommendedUnits());
                    jsonObject.put("曲线方程", model.getCurveEquation());
                    jsonObject.put("是否需要扣空白", model.getIsBlank());
                    jsonObject.put("加入零点", model.getJoinZeroPoint());
                    jsonObject.put("强制过零点", model.getForceZeroCrossing());
                    jsonObject.put("斜率", model.getSlope());
                    jsonObject.put("截距", model.getIntercept());
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
                        jsonObject.put("波长4", model.getWavelength4());
                    } else {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("波长1", model.getWavelength1());
                        jsonObject.put("波长2", model.getWavelength2());
                        jsonObject.put("波长3", model.getWavelength3());
                        jsonObject.put("波长4", model.getWavelength4());
                        detectionParametersMap.put(model.getDetectionId(), jsonObject);
                    }
                }
            }

            // 步骤1：全量校验并收集有效数据
            List<Detection> validDetections = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                StandardDetectionExcelModel model = list.get(i);
                int currentRow = i + 2;

                try {
                    // 校验必填字段
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getIndustry() == null || model.getIndustry().isEmpty()) {
                        throw new IllegalArgumentException("行业不能为空");
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
                                throw new IllegalArgumentException("解析检测参数失败: " + e.getMessage());
                            }
                        }
                        detection.setDetectionParameters(defaultParams.toJSONString());
                    }

                    // 校验通过，加入有效列表
                    validDetections.add(detection);
                } catch (Exception e) {
                    // 错误信息添加表名前缀，方便区分
                    errorMessages.add("标准信息表 - 第 " + currentRow + " 行数据错误：" + e.getMessage());
                }
            }

            // 步骤2：当前表无错误时才执行数据操作（有错误则不处理）
            if (errorMessages.isEmpty()) {
                // 步骤3：全量校验通过，批量保存
                for (Detection detection : validDetections) {
                    String currentHash = generateDetectionHash(detection);
                    Detection dbDetection = detectionMapper.selectByStandardIdentity(detection.getStandardIdentity());
                    
                    if (dbDetection != null) {
                        String dbHash = generateDetectionHash(dbDetection);
                        if (!currentHash.equals(dbHash)) {
                            detectionMapper.updateByStandardIdentity(detection);
                            System.out.println("更新标准信息（哈希变化），标准标识 " + detection.getStandardIdentity());
                        } else {
                            System.out.println("标准信息未变更，标准标识 " + detection.getStandardIdentity());
                        }
                    } else {
                        detectionMapper.insert(detection);
                        System.out.println("插入标准信息，标准标识 " + detection.getStandardIdentity());
                    }
                }
            }

        } catch (IOException e) {
            errorMessages.add("标准信息表：读取Excel文件时发生异常: " + e.getMessage());
        } catch (Exception e) {
            errorMessages.add("标准信息表：导入时发生异常: " + e.getMessage());
        }
        return errorMessages; // 返回当前表的所有错误
    }

    // 主表哈希（原有逻辑不变）
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
    // 修改：返回当前表的错误信息列表
    @Transactional
    public List<String> importStandardPretreatment(MultipartFile file) {
        List<String> errorMessages = new ArrayList<>();
        try {
            List<StandardPretreatmentExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(StandardPretreatmentExcelModel.class)
                    .sheet("前处理信息表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return errorMessages; // 空表无错误，返回空列表
            }
            
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            List<StandardPretreatment> validPretreatments = new ArrayList<>();
            int currentRow = 2;

            // 步骤1：全量校验
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

                    validPretreatments.add(pretreatment);
                } catch (IllegalArgumentException e) {
                    errorMessages.add("前处理信息表 - 第 " + currentRow + " 行数据错误：" + e.getMessage());
                } finally {
                    currentRow++;
                }
            }

            // 步骤2：当前表无错误时才执行数据操作
            if (errorMessages.isEmpty()) {
                // 步骤3：全量保存
                currentRow = 2;
                identityNameToDetectionIdMap.clear(); // 重置缓存
                for (StandardPretreatment pretreatment : validPretreatments) {
                    try {
                        String detectionId = pretreatment.getDetectionId();
                        String identityName = getIdentityNameById(detectionId); 
                        
                        // 生成当前记录哈希
                        String currentHash = generatePretreatmentHash(pretreatment);

                        // 查询数据库中该主表ID下的所有记录
                        List<StandardPretreatment> dbRecords = standardPretreatmentMapper.selectByDetectionId(detectionId);
                        Map<String, StandardPretreatment> hashToRecordMap = new HashMap<>();
                        for (StandardPretreatment record : dbRecords) {
                            hashToRecordMap.put(generatePretreatmentHash(record), record);
                        }

                        // 对比哈希
                        if (hashToRecordMap.containsKey(currentHash)) {
                            System.out.println("第 " + currentRow + " 行: 前处理记录未变更，标准标识ID: " + detectionId);
                        } else {
                            // 检查同描述记录
                            Optional<StandardPretreatment> sameDescRecord = dbRecords.stream()
                                    .filter(r -> Objects.equals(r.getPretreatmentDescription(), pretreatment.getPretreatmentDescription()))
                                    .findFirst();

                            if (sameDescRecord.isPresent()) {
                                pretreatment.setId(sameDescRecord.get().getId());
                                standardPretreatmentMapper.updateById(pretreatment);
                                System.out.println("第 " + currentRow + " 行: 更新前处理记录，标准标识ID: " + detectionId);
                            } else {
                                standardPretreatmentMapper.insert(pretreatment);
                                System.out.println("第 " + currentRow + " 行: 插入前处理记录，标准标识ID: " + detectionId);
                            }
                        }
                    } finally {
                        currentRow++;
                    }
                }
            }
        } catch (IOException e) {
            errorMessages.add("前处理信息表：读取Excel文件时发生异常: " + e.getMessage());
        } catch (Exception e) {
            errorMessages.add("前处理信息表：导入时发生异常: " + e.getMessage());
        }
        return errorMessages; // 返回当前表错误
    }

    // 前处理表哈希（原有逻辑不变）
    private String generatePretreatmentHash(StandardPretreatment pretreatment) {
        String combined = 
            (pretreatment.getId() != null ? pretreatment.getId().toString() : "") + "|" +
            pretreatment.getDetectionId() + "|" +
            pretreatment.getPretreatmentDescription();
        return generateHash(combined);
    }


    // --------------------------- 子表2：计算公式表导入 ---------------------------
    // 修改：返回当前表的错误信息列表
    @Transactional
    public List<String> importCalculationFormula(MultipartFile file) {
        List<String> errorMessages = new ArrayList<>();
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
                return errorMessages; // 空表无错误
            }

            System.out.println("读取到 " + list.size() + " 条计算公式数据");
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            List<CalculationFormula> validFormulas = new ArrayList<>();
            int currentRow = 2;

            // 步骤1：全量校验
            for (CalculationFormulaExcelModel model : list) {
                try {
                    // 校验必填字段
                    if (model.getDetectionId() == null || model.getDetectionId().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getResultIdentity() == null || model.getResultIdentity().isEmpty()) {
                        throw new IllegalArgumentException("结果标识不能为空");
                    }
                    if (model.getResultName() == null || model.getResultName().isEmpty()) {
                        throw new IllegalArgumentException("结果名称不能为空");
                    }
                    if (model.getInputParam1Name() == null || model.getInputParam1Name().isEmpty()) {
                        throw new IllegalArgumentException("输入参数名称1不能为空");
                    }
                    if (model.getInputParam1Identity() == null || model.getInputParam1Identity().isEmpty()) {
                        throw new IllegalArgumentException("输入参数标识1不能为空");
                    }
                    if (model.getUnit() == null || model.getUnit().isEmpty()) {
                        throw new IllegalArgumentException("单位不能为空");
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
                    formula.setDetectionId(detectionId.toString());
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

                    validFormulas.add(formula);
                } catch (IllegalArgumentException e) {
                    errorMessages.add("计算公式表 - 第 " + currentRow + " 行数据错误：" + e.getMessage());
                } finally {
                    currentRow++;
                }
            }

            // 步骤2：当前表无错误时才执行数据操作
            if (errorMessages.isEmpty()) {
                // 步骤3：全量保存
                currentRow = 2;
                identityNameToDetectionIdMap.clear();
                for (CalculationFormula formula : validFormulas) {
                    try {
                        String detectionId = formula.getDetectionId();
                        String currentHash = generateFormulaHash(formula);

                        List<CalculationFormula> dbFormulas = calculationFormulaMapper.selectByDetectionId(detectionId);
                        Map<String, CalculationFormula> hashToFormulaMap = new HashMap<>();
                        for (CalculationFormula dbFormula : dbFormulas) {
                            hashToFormulaMap.put(generateFormulaHash(dbFormula), dbFormula);
                        }

                        if (hashToFormulaMap.containsKey(currentHash)) {
                            System.out.println("第 " + currentRow + " 行: 公式记录未变更，标准标识ID: " + detectionId);
                        } else {
                            Optional<CalculationFormula> sameResultRecord = dbFormulas.stream()
                                    .filter(f -> Objects.equals(f.getResultIdentity(), formula.getResultIdentity()))
                                    .findFirst();

                            if (sameResultRecord.isPresent()) {
                                formula.setId(sameResultRecord.get().getId());
                                calculationFormulaMapper.updateById(formula);
                                System.out.println("第 " + currentRow + " 行: 更新公式记录，标准标识ID: " + detectionId);
                            } else {
                                calculationFormulaMapper.insert(formula);
                                System.out.println("第 " + currentRow + " 行: 插入公式记录，标准标识ID: " + detectionId);
                            }
                        }
                    } finally {
                        currentRow++;
                    }
                }
            }

        } catch (Exception e) {
            errorMessages.add("计算公式表：导入时发生异常: " + e.getMessage());
        }
        return errorMessages; // 返回当前表错误
    }

    // 校验计算公式
    // @Transactional
    // public List<String> importCalculationFormula(MultipartFile file) {
    //     List<String> errorMessages = new ArrayList<>();
    //     try {
    //         // 提取Excel中的图片（公式图片）
    //         Map<String, List<ExcelImageImportUtils.ImageInfo>> allImages = 
    //             ExcelImageImportUtils.extractAllImages(file);
    //         List<ExcelImageImportUtils.ImageInfo> formulaSheetImages = 
    //             allImages.getOrDefault("计算公式表", new ArrayList<>());

    //         // 读取计算公式表数据
    //         List<CalculationFormulaExcelModel> list = EasyExcel.read(file.getInputStream())
    //                 .head(CalculationFormulaExcelModel.class)
    //                 .sheet("计算公式表")
    //                 .doReadSync();

    //         if (list == null || list.isEmpty()) {
    //             return errorMessages; // 空表无错误
    //         }

    //         System.out.println("读取到 " + list.size() + " 条计算公式数据");
    //         Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
    //         List<CalculationFormula> validFormulas = new ArrayList<>();
    //         int currentRow = 2; // 行号从第2行开始（表头为第1行）
    //         LaTeXConverter formulaValidator = new LaTeXConverter(); // 初始化公式校验器

    //         // 步骤1：全量校验
    //         for (CalculationFormulaExcelModel model : list) {
    //             try {
    //                 // 1. 基础必填字段校验
    //                 if (model.getDetectionId() == null || model.getDetectionId().isEmpty()) {
    //                     throw new IllegalArgumentException("标准标识不能为空");
    //                 }
    //                 if (model.getResultIdentity() == null || model.getResultIdentity().isEmpty()) {
    //                     throw new IllegalArgumentException("结果标识不能为空");
    //                 }
    //                 if (model.getResultName() == null || model.getResultName().isEmpty()) {
    //                     throw new IllegalArgumentException("结果名称不能为空");
    //                 }
    //                 if (model.getInputParam1Name() == null || model.getInputParam1Name().isEmpty()) {
    //                     throw new IllegalArgumentException("输入参数名称1不能为空");
    //                 }
    //                 if (model.getInputParam1Identity() == null || model.getInputParam1Identity().isEmpty()) {
    //                     throw new IllegalArgumentException("输入参数标识1不能为空");
    //                 }
    //                 if (model.getUnit() == null || model.getUnit().isEmpty()) {
    //                     throw new IllegalArgumentException("单位不能为空");
    //                 }
    //                 if (model.getFormulaText() == null || model.getFormulaText().isEmpty()) {
    //                     throw new IllegalArgumentException("公式文本不能为空");
    //                 }

    //                 // 2. 提取所有输入参数标识（用于公式校验）
    //                 List<String> paramList = new ArrayList<>();
    //                 // 依次添加1-10号参数标识（非空才加入）
    //                 addParamIfValid(model.getInputParam1Identity(), paramList);
    //                 addParamIfValid(model.getInputParam2Identity(), paramList);
    //                 addParamIfValid(model.getInputParam3Identity(), paramList);
    //                 addParamIfValid(model.getInputParam4Identity(), paramList);
    //                 addParamIfValid(model.getInputParam5Identity(), paramList);
    //                 addParamIfValid(model.getInputParam6Identity(), paramList);
    //                 addParamIfValid(model.getInputParam7Identity(), paramList);
    //                 addParamIfValid(model.getInputParam8Identity(), paramList);
    //                 addParamIfValid(model.getInputParam9Identity(), paramList);
    //                 addParamIfValid(model.getInputParam10Identity(), paramList);

    //                 // 3. 调用公式校验方法
    //                 String verifyResult = formulaValidator.verifyAndConvert(paramList, model.getFormulaText());
    //                 System.out.println("paramList"+paramList);
    //                 System.out.println("model.getFormulaText()"+model.getFormulaText());
    //                 if (!"true".equals(verifyResult)) {
    //                     throw new IllegalArgumentException(verifyResult);
    //                 }

    //                 // 4. 校验主表关联
    //                 String identityName = model.getDetectionId();
    //                 Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
    //                 if (detectionId == null) {
    //                     throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
    //                 }

    //                 // 5. 处理公式图片
    //                 String base64Image = ExcelImageImportUtils.findImageByRow(formulaSheetImages, currentRow - 1);
    //                 if (base64Image == null && model.getFormulaImage() != null && model.getFormulaImage().startsWith("data:image/")) {
    //                     base64Image = model.getFormulaImage();
    //                 }

    //                 // 6. 构建计算公式对象
    //                 CalculationFormula formula = new CalculationFormula();
    //                 formula.setDetectionId(detectionId.toString());
    //                 formula.setResultIdentity(model.getResultIdentity());
    //                 formula.setResultName(model.getResultName());
    //                 formula.setUnit(model.getUnit());
    //                 formula.setInputParam1Name(model.getInputParam1Name());
    //                 formula.setInputParam1Identity(model.getInputParam1Identity());
    //                 formula.setInputParam2Name(model.getInputParam2Name());
    //                 formula.setInputParam2Identity(model.getInputParam2Identity());
    //                 formula.setInputParam3Name(model.getInputParam3Name());
    //                 formula.setInputParam3Identity(model.getInputParam3Identity());
    //                 formula.setInputParam4Name(model.getInputParam4Name());
    //                 formula.setInputParam4Identity(model.getInputParam4Identity());
    //                 formula.setInputParam5Name(model.getInputParam5Name());
    //                 formula.setInputParam5Identity(model.getInputParam5Identity());
    //                 formula.setInputParam6Name(model.getInputParam6Name());
    //                 formula.setInputParam6Identity(model.getInputParam6Identity());
    //                 formula.setInputParam7Name(model.getInputParam7Name());
    //                 formula.setInputParam7Identity(model.getInputParam7Identity());
    //                 formula.setInputParam8Name(model.getInputParam8Name());
    //                 formula.setInputParam8Identity(model.getInputParam8Identity());
    //                 formula.setInputParam9Name(model.getInputParam9Name());
    //                 formula.setInputParam9Identity(model.getInputParam9Identity());
    //                 formula.setInputParam10Name(model.getInputParam10Name());
    //                 formula.setInputParam10Identity(model.getInputParam10Identity());
    //                 formula.setFormulaText(model.getFormulaText());
    //                 formula.setFormulaImage(base64Image);
    //                 formula.setDetectionLimit(model.getDetectionLimit());

    //                 validFormulas.add(formula);
    //             } catch (IllegalArgumentException e) {
    //                 // 添加包含公式校验结果的错误信息
    //                 errorMessages.add("计算公式表 - 第 " + currentRow + " 行数据错误：" + e.getMessage());
    //             } finally {
    //                 currentRow++;
    //             }
    //         }

    //         // 步骤2：当前表无错误时才执行数据操作
    //         if (errorMessages.isEmpty()) {
    //             // 步骤3：全量保存
    //             currentRow = 2;
    //             identityNameToDetectionIdMap.clear();
    //             for (CalculationFormula formula : validFormulas) {
    //                 try {
    //                     String detectionId = formula.getDetectionId();
    //                     String currentHash = generateFormulaHash(formula);

    //                     List<CalculationFormula> dbFormulas = calculationFormulaMapper.selectByDetectionId(detectionId);
    //                     Map<String, CalculationFormula> hashToFormulaMap = new HashMap<>();
    //                     for (CalculationFormula dbFormula : dbFormulas) {
    //                         hashToFormulaMap.put(generateFormulaHash(dbFormula), dbFormula);
    //                     }

    //                     if (hashToFormulaMap.containsKey(currentHash)) {
    //                         System.out.println("第 " + currentRow + " 行: 公式记录未变更，标准标识ID: " + detectionId);
    //                     } else {
    //                         Optional<CalculationFormula> sameResultRecord = dbFormulas.stream()
    //                                 .filter(f -> Objects.equals(f.getResultIdentity(), formula.getResultIdentity()))
    //                                 .findFirst();

    //                         if (sameResultRecord.isPresent()) {
    //                             formula.setId(sameResultRecord.get().getId());
    //                             calculationFormulaMapper.updateById(formula);
    //                             System.out.println("第 " + currentRow + " 行: 更新公式记录，标准标识ID: " + detectionId);
    //                         } else {
    //                             calculationFormulaMapper.insert(formula);
    //                             System.out.println("第 " + currentRow + " 行: 插入公式记录，标准标识ID: " + detectionId);
    //                         }
    //                     }
    //                 } finally {
    //                     currentRow++;
    //                 }
    //             }
    //         }

    //     } catch (Exception e) {
    //         errorMessages.add("计算公式表：导入时发生异常: " + e.getMessage());
    //     }
    //     return errorMessages;
    // }

    // 辅助方法：非空参数标识才加入列表
    // private void addParamIfValid(String paramIdentity, List<String> paramList) {
    //     if (paramIdentity != null && !paramIdentity.trim().isEmpty()) {
    //         paramList.add(paramIdentity.trim());
    //     }
    // }

    // 计算公式表哈希（原有逻辑不变）
    private String generateFormulaHash(CalculationFormula formula) {
        String combined = 
            (formula.getId() != null ? formula.getId().toString() : "") + "|" +
            formula.getDetectionId() + "|" +
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
    // 修改：返回当前表的错误信息列表
    @Transactional
    public List<String> importQualityControl(MultipartFile file) {
        List<String> errorMessages = new ArrayList<>();
        try {
            List<QualityControlExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(QualityControlExcelModel.class)
                    .sheet("质量控制表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return errorMessages; // 空表无错误
            }

            int currentRow = 2;
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            List<QualityControl> validControls = new ArrayList<>();

            // 步骤1：全量校验
            for (QualityControlExcelModel model : list) {
                try {
                    if (model.getDetectionId() == null || model.getDetectionId().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }

                    // 获取主表ID
                    String identityName = model.getDetectionId();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
                    }

                    // 构建质量控制对象
                    QualityControl control = new QualityControl();
                    control.setDetectionId(detectionId.toString());
                    control.setControlType(model.getControlType());
                    control.setControlDescription(model.getControlDescription());
                    control.setControlFormula(model.getControlFormula());

                    validControls.add(control);
                } catch (IllegalArgumentException e) {
                    errorMessages.add("质量控制表 - 第 " + currentRow + " 行数据错误：" + e.getMessage());
                } finally {
                    currentRow++;
                }
            }

            // 步骤2：当前表无错误时才执行数据操作
            if (errorMessages.isEmpty()) {
                // 步骤3：全量保存
                currentRow = 2;
                identityNameToDetectionIdMap.clear();
                for (QualityControl control : validControls) {
                    try {
                        String detectionId = control.getDetectionId();
                        String currentHash = generateControlHash(control);

                        List<QualityControl> dbControls = qualityControlMapper.selectByDetectionId(detectionId);
                        Map<String, QualityControl> hashToControlMap = new HashMap<>();
                        for (QualityControl dbControl : dbControls) {
                            hashToControlMap.put(generateControlHash(dbControl), dbControl);
                        }

                        if (hashToControlMap.containsKey(currentHash)) {
                            System.out.println("第 " + currentRow + " 行: 质量控制记录未变更，标准标识ID: " + detectionId);
                        } else {
                            Optional<QualityControl> sameTypeRecord = dbControls.stream()
                                    .filter(c -> Objects.equals(c.getControlType(), control.getControlType()))
                                    .findFirst();

                            if (sameTypeRecord.isPresent()) {
                                control.setId(sameTypeRecord.get().getId());
                                qualityControlMapper.updateById(control);
                                System.out.println("第 " + currentRow + " 行: 更新质量控制记录，标准标识ID: " + detectionId);
                            } else {
                                qualityControlMapper.insert(control);
                                System.out.println("第 " + currentRow + " 行: 插入质量控制记录，标准标识ID: " + detectionId);
                            }
                        }
                    } finally {
                        currentRow++;
                    }
                }
                System.out.println("质量控制表导入完成。");
            }
        } catch (IOException e) {
            errorMessages.add("质量控制表：读取Excel文件时发生异常: " + e.getMessage());
        } catch (Exception e) {
            errorMessages.add("质量控制表：导入时发生异常: " + e.getMessage());
        }
        return errorMessages; // 返回当前表错误
    }

    // 质量控制表哈希（原有逻辑不变）
    private String generateControlHash(QualityControl control) {
        String combined = 
            (control.getId() != null ? control.getId().toString() : "") + "|" +
            control.getDetectionId() + "|" +
            control.getControlType() + "|" +
            (control.getControlDescription() != null ? control.getControlDescription() : "") + "|" +
            (control.getControlFormula() != null ? control.getControlFormula() : "");
        return generateHash(combined);
    }


    // --------------------------- 子表4：修约表导入 ---------------------------
    // 修改：返回当前表的错误信息列表
    @Transactional
    public List<String> importRoundingRules(MultipartFile file) {
        List<String> errorMessages = new ArrayList<>();
        try {
            List<RoundingRulesExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(RoundingRulesExcelModel.class)
                    .sheet("修约表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return errorMessages; // 空表无错误
            }

            int currentRow = 2;
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            List<RoundingRules> validRules = new ArrayList<>();

            // 步骤1：全量校验
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
                    rules.setDetectionId(detectionId.toString());
                    rules.setResultIdentity(model.getResultIdentity());
                    rules.setRoundingType(model.getRoundingType());
                    rules.setRoundingCondition(model.getRoundingCondition());
                    rules.setDecimalPlaces(model.getDecimalPlaces());

                    validRules.add(rules);
                } catch (IllegalArgumentException e) {
                    errorMessages.add("修约表 - 第 " + currentRow + " 行数据错误：" + e.getMessage());
                } finally {
                    currentRow++;
                }
            }

            // 步骤2：当前表无错误时才执行数据操作
            if (errorMessages.isEmpty()) {
                // 步骤3：全量保存
                currentRow = 2;
                identityNameToDetectionIdMap.clear();
                for (RoundingRules rules : validRules) {
                    try {
                        String detectionId = rules.getDetectionId();
                        String currentHash = generateRoundingHash(rules);

                        List<RoundingRules> dbRules = roundingRulesMapper.selectByDetectionId(detectionId);
                        Map<String, RoundingRules> hashToRulesMap = new HashMap<>();
                        for (RoundingRules dbRule : dbRules) {
                            hashToRulesMap.put(generateRoundingHash(dbRule), dbRule);
                        }

                        if (hashToRulesMap.containsKey(currentHash)) {
                            System.out.println("第 " + currentRow + " 行: 修约规则未变更，标准标识ID: " + detectionId);
                        } else {
                            Optional<RoundingRules> sameResultRecord = dbRules.stream()
                                    .filter(r -> Objects.equals(r.getResultIdentity(), rules.getResultIdentity()))
                                    .findFirst();

                            if (sameResultRecord.isPresent()) {
                                rules.setId(sameResultRecord.get().getId());
                                roundingRulesMapper.updateById(rules);
                                System.out.println("第 " + currentRow + " 行: 更新修约规则，标准标识ID: " + detectionId);
                            } else {
                                roundingRulesMapper.insert(rules);
                                System.out.println("第 " + currentRow + " 行: 插入修约规则，标准标识ID: " + detectionId);
                            }
                        }
                    } finally {
                        currentRow++;
                    }
                }
            }

        } catch (Exception e) {
            errorMessages.add("修约表：导入时发生异常: " + e.getMessage());
        }
        return errorMessages; // 返回当前表错误
    }

    // 修约表哈希（原有逻辑不变）
    private String generateRoundingHash(RoundingRules rules) {
        String combined = 
            (rules.getId() != null ? rules.getId().toString() : "") + "|" +
            rules.getDetectionId() + "|" +
            rules.getResultIdentity() + "|" +
            rules.getRoundingType() + "|" +
            (rules.getRoundingCondition() != null ? rules.getRoundingCondition() : "") + "|" +
            rules.getDecimalPlaces().toString();
        return generateHash(combined);
    }


    // --------------------------- 公共工具方法 ---------------------------
    // 通用哈希生成方法（原有逻辑不变）
    private String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("哈希算法初始化失败", e);
        }
    }

    // 获取主表ID（原有逻辑不变）
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

    // 通过主表ID获取标准标识（原有逻辑不变）
    private String getIdentityNameById(String detectionId) {
        return detectionMapper.findStandardIdentityById(Integer.parseInt(detectionId));
    }

    // 导入所有表（整体事务）：汇总所有表错误后统一抛出
    // 导入所有表（整体事务）：主表失败则终止子表导入
    @Transactional
    public void importExcel(MultipartFile file) throws IOException {
        // 收集所有错误
        List<String> allErrors = new ArrayList<>();
        
        // 1. 先执行主表导入，获取主表错误
        List<String> mainTableErrors = importStandardDetection(file);
        allErrors.addAll(mainTableErrors);
        
        // 2. 若主表有错误，直接终止，不执行子表导入（避免子表关联错误）
        if (!mainTableErrors.isEmpty()) {
            throw new IllegalArgumentException("导入失败，共" + allErrors.size() + "条错误：\n" + String.join("\n", allErrors));
        }
        
        // 3. 主表无错误，再执行子表导入并收集错误
        allErrors.addAll(importStandardPretreatment(file));
        allErrors.addAll(importRoundingRules(file));
        allErrors.addAll(importQualityControl(file));
        allErrors.addAll(importCalculationFormula(file));
        
        // 4. 子表有错误则统一抛出
        if (!allErrors.isEmpty()) {
            throw new IllegalArgumentException("导入失败，共" + allErrors.size() + "条错误：\n" + String.join("\n", allErrors));
        }
    }
}