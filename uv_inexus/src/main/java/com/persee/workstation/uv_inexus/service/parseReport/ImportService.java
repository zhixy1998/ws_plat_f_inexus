package com.persee.workstation.uv_inexus.service.parseReport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
// import com.alibaba.fastjson.JSON;
// import com.alibaba.fastjson.JSONArray;
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

    // 导入标准信息表
    public void importStandardDetection(MultipartFile file) {
        try {
            // 读取Excel文件数据
            List<StandardDetectionExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(StandardDetectionExcelModel.class)
                    .sheet("标准信息表")
                    .doReadSync();
    
            if (list == null || list.isEmpty()) {
                System.out.println("标准信息表数据为空，请检查Excel文件是否正确");
                return;
            }
    
            // 读取UV-VIS-标曲法-检测参数表（如果存在）
            List<StandardDetectionUVVISCurveExcelModel> standardCurveList = null;
            try {
                standardCurveList = EasyExcel.read(file.getInputStream())
                        .head(StandardDetectionUVVISCurveExcelModel.class)
                        .sheet("UV-VIS-标曲法-检测参数表")
                        .doReadSync();
            } catch (Exception e) {
                System.out.println("未找到UV-VIS-标曲法-检测参数表，忽略该部分数据");
            }
    
            // 读取UV-VIS-光度法-检测参数表（如果存在）
            List<StandardDetectionUVVISPhotometryExcelModel> photometryList = null;
            try {
                photometryList = EasyExcel.read(file.getInputStream())
                        .head(StandardDetectionUVVISPhotometryExcelModel.class)
                        .sheet("UV-VIS-光度法-检测参数表")
                        .doReadSync();
            } catch (Exception e) {
                System.out.println("未找到UV-VIS-光度法-检测参数表，忽略该部分数据");
            }
    
            // 合并数据
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
                    detectionParametersMap.put(model.getStandardIdentity(), jsonObject);
                }
            }
    
            if (photometryList != null) {
                for (StandardDetectionUVVISPhotometryExcelModel model : photometryList) {
                    if (detectionParametersMap.containsKey(model.getStandardIdentity())) {
                        JSONObject jsonObject = detectionParametersMap.get(model.getStandardIdentity());
                        jsonObject.put("波长1", model.getWavelength1());
                        jsonObject.put("波长2", model.getWavelength2());
                        jsonObject.put("波长3", model.getWavelength3());
                    } else {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("波长1", model.getWavelength1());
                        jsonObject.put("波长2", model.getWavelength2());
                        jsonObject.put("波长3", model.getWavelength3());
                        detectionParametersMap.put(model.getStandardIdentity(), jsonObject);
                    }
                }
            }
    
            // 插入数据
            for (int i = 0; i < list.size(); i++) {
                StandardDetectionExcelModel model = list.get(i);
                int currentRow = i + 2; // Excel通常第一行是标题，数据从第二行开始
    
                // 校验必填字段
                if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：标准标识不能为空");
                }
                if (model.getIndustry() == null || model.getIndustry().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：行业不能为空");
                }
                if (model.getDetectionType() == null || model.getDetectionType().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：检测类型不能为空");
                }
                if (model.getDetectionInstrument() == null || model.getDetectionInstrument().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：检测仪器不能为空");
                }
                if (model.getStandardNumber() == null || model.getStandardNumber().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：标准号不能为空");
                }
                if (model.getDetectionItem() == null || model.getDetectionItem().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：检测项目不能为空");
                }
    
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
                    // 如果没有UV-VIS参数表，直接使用Excel中的检测参数
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
    
                detectionMapper.insert(detection);
            }
        } catch (IOException e) {
            System.out.println("读取Excel文件时发生IO异常: " + e.getMessage());
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("导入标准信息表时发生异常: " + e.getMessage());
            throw new IllegalArgumentException("导入标准信息表时发生异常: " + e.getMessage(), e);
        }
    }    

    // 导入前处理信息表
    public void importStandardPretreatment(MultipartFile file) throws IOException {
        try {
            List<StandardPretreatmentExcelModel> list = EasyExcel.read(file.getInputStream())
                .head(StandardPretreatmentExcelModel.class)
                .sheet("前处理信息表")
                .doReadSync();
    
            if (list == null || list.isEmpty()) {
                System.out.println("前处理信息表数据为空，请检查Excel文件是否正确");
                return;
            }
    
            // 从第二行开始计数，假设第一行是标题行
            int currentRow = 2;
            for (StandardPretreatmentExcelModel model : list) {
                try {
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
    
                    StandardPretreatment standardPretreatment = new StandardPretreatment();
                    standardPretreatment.setStandardIdentity(model.getStandardIdentity());
                    standardPretreatment.setPretreatmentDescription(model.getPretreatmentDescription());
                    standardPretreatmentMapper.insert(standardPretreatment);
                } catch (IllegalArgumentException e) {
                    // 捕获并重新抛出，包含行号信息
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } catch (Exception e) {
                    // 捕获其他可能的异常，包含行号信息
                    throw new RuntimeException("第 " + currentRow + " 行数据处理失败：" + e.getMessage(), e);
                } finally {
                    // 无论成功失败，都递增行号
                    currentRow++;
                }
            }
        } catch (IOException e) {
            System.out.println("读取Excel文件时发生IO异常: " + e.getMessage());
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            // 捕获外层循环可能抛出的异常，包含行号信息
            System.out.println("导入前处理信息表时发生异常: " + e.getMessage());
            throw new IllegalArgumentException("导入前处理信息表时发生异常: " + e.getMessage(), e);
        }
    }    

    // 导入计算公式表
    public void importCalculationFormula(MultipartFile file) {
        try {
            List<CalculationFormulaExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(CalculationFormulaExcelModel.class)
                    .sheet("计算公式表")
                    .doReadSync();
    
            if (list == null || list.isEmpty()) {
                System.out.println("计算公式表数据为空，请检查Excel文件是否正确");
                return;
            }
            System.out.println("++++++++" + list);
            int picIdx = 0;
    
            for (int i = 0; i < list.size(); i++) {
                CalculationFormulaExcelModel model = list.get(i);
                // 打印读取到的数据
                System.out.println("读取到第 " + (i + 2) + " 行数据: " + model);
    
                // 校验必填字段
                if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：标准标识不能为空");
                }
                if (model.getResultIdentity() == null || model.getResultIdentity().isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：结果标识不能为空");
                }
                if (model.getResultName() == null || model.getResultName().isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：结果名称不能为空");
                }
                if (model.getUnit() == null || model.getUnit().isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：单位不能为空");
                }
                if (model.getInputParam1Name() == null || model.getInputParam1Name().isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：输入参数1名称不能为空");
                }
                if (model.getInputParam1Identity() == null || model.getInputParam1Identity().isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：输入参数1标识不能为空");
                }
    
                // 提取所有参数标识
                List<String> paramIdentities = new ArrayList<>();
                paramIdentities.add(model.getInputParam1Identity());
                paramIdentities.add(model.getInputParam2Identity());
                paramIdentities.add(model.getInputParam3Identity());
                paramIdentities.add(model.getInputParam4Identity());
                paramIdentities.add(model.getInputParam5Identity());
                paramIdentities.add(model.getInputParam6Identity());
                paramIdentities.add(model.getInputParam7Identity());
                paramIdentities.add(model.getInputParam8Identity());
                paramIdentities.add(model.getInputParam9Identity());
                paramIdentities.add(model.getInputParam10Identity());
    
                // 过滤掉空值
                paramIdentities.removeIf(Objects::isNull);
    
                // 校验公式文本
                String formulaText = model.getFormulaText();
                if (formulaText == null || formulaText.isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：公式文本不能为空");
                }
    
                // 检查是否是等式
                if (!formulaText.contains("=")) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：不是等式");
                }
    
                // 分割等式
                String[] parts = formulaText.split("=", 2);
                String leftSide = parts[0].trim();
                String rightSide = parts[1].trim();
                
                // 校验结果标识
                String resultIdentity = model.getResultIdentity();
                if (resultIdentity == null || resultIdentity.isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：结果标识不能为空");
                }

                // 处理左边内容和结果标识，去除括号及其内容
                String processedLeftSide = leftSide.replaceAll("[\\（\\(].*?[\\）\\)]", "").trim();
                String processedResultIdentity = resultIdentity.replaceAll("[\\（\\(].*?[\\）\\)]", "").trim();

                // 比较处理后的字符串
                if (!processedLeftSide.equals(processedResultIdentity)) {
                    System.out.println("调试信息 - 第 " + (i + 2) + " 行:");
                    System.out.println("  原始左边: " + leftSide);
                    System.out.println("  处理后左边: " + processedLeftSide);
                    System.out.println("  原始结果标识: " + resultIdentity);
                    System.out.println("  处理后结果标识: " + processedResultIdentity);
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：结果标识有误，与公式文本左边不匹配！处理后左边: " 
                        + processedLeftSide + "，处理后结果标识: " + processedResultIdentity);
                }
    
                // 记录不在公式文本中的参数标识
                List<String> invalidParams = new ArrayList<>();
    
                for (int j = 0; j < paramIdentities.size(); j++) {
                    String paramIdentity = paramIdentities.get(j);
                    if (!rightSide.contains(paramIdentity)) {
                        invalidParams.add("参数" + (j + 1) + "标识: " + paramIdentity);
                    }
                }
    
                if (!invalidParams.isEmpty()) {
                    throw new IllegalArgumentException("第 " + (i + 2) + " 行数据错误：以下参数标识有误，不在公式文本中: " + String.join(", ", invalidParams));
                }
                
                CalculationFormula calculationFormula = new CalculationFormula();
                calculationFormula.setStandardIdentity(model.getStandardIdentity());
                calculationFormula.setResultIdentity(model.getResultIdentity());
                calculationFormula.setResultName(model.getResultName());
                calculationFormula.setUnit(model.getUnit());
                calculationFormula.setInputParam1Name(model.getInputParam1Name());
                calculationFormula.setInputParam1Identity(model.getInputParam1Identity());
                calculationFormula.setInputParam2Name(model.getInputParam2Name());
                calculationFormula.setInputParam2Identity(model.getInputParam2Identity());
                calculationFormula.setInputParam3Name(model.getInputParam3Name());
                calculationFormula.setInputParam3Identity(model.getInputParam3Identity());
                calculationFormula.setInputParam4Name(model.getInputParam4Name());
                calculationFormula.setInputParam4Identity(model.getInputParam4Identity());
                calculationFormula.setInputParam5Name(model.getInputParam5Name());
                calculationFormula.setInputParam5Identity(model.getInputParam5Identity());
                calculationFormula.setInputParam6Name(model.getInputParam6Name());
                calculationFormula.setInputParam6Identity(model.getInputParam6Identity());
                calculationFormula.setInputParam7Name(model.getInputParam7Name());
                calculationFormula.setInputParam7Identity(model.getInputParam7Identity());
                calculationFormula.setInputParam8Name(model.getInputParam8Name());
                calculationFormula.setInputParam8Identity(model.getInputParam8Identity());
                calculationFormula.setInputParam9Name(model.getInputParam9Name());
                calculationFormula.setInputParam9Identity(model.getInputParam9Identity());
                calculationFormula.setInputParam10Name(model.getInputParam10Name());
                calculationFormula.setInputParam10Identity(model.getInputParam10Identity());
                calculationFormula.setFormulaText(model.getFormulaText());
                // calculationFormula.setFormulaImage(model.getFormulaImage());
                // calculationFormula.setFormulaImage(null);
                String imageData = model.getFormulaImage();
                calculationFormula.setDetectionLimit(model.getDetectionLimit());

                String base64Image = null;
                System.out.println("第 " + (i+2) + " 行图片----------------------------: " + imageData);
                
                if (imageData != null && !imageData.isEmpty()) {
                    try {
                        if (imageData.startsWith("=DISPIMG")) {
                            System.out.println("当前第 " + (i+2) + " 行图片88888: " + imageData);
                            // 传入行号用于图片定位
                            base64Image = ExcelImageImportUtils.extractImageToBase64(
                                file, 
                                imageData,
                                picIdx
                            );
                            picIdx++;

                            System.out.println("图片提取结果长度: " + 
                                (base64Image != null ? base64Image.length() : "null"));
                        } else {
                            base64Image = imageData;
                        }
                    } catch (Exception e) {
                        System.out.println("第 " + i + 2 + " 行图片处理失败: " + e.getMessage());
                    }
                }
                calculationFormula.setFormulaImage(base64Image);
                
                calculationFormulaMapper.insert(calculationFormula);
            }
        } catch (IOException e) {
            System.out.println("读取Excel文件时发生IO异常: " + e.getMessage());
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("导入计算公式表时发生异常: " + e.getMessage());
            throw new IllegalArgumentException("导入计算公式表时发生异常: " + e.getMessage(), e);
        }
    }    
    
    
    // 导入质量控制表
    public void importQualityControl(MultipartFile file) {
        try {
            List<QualityControlExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(QualityControlExcelModel.class)
                    .sheet("质量控制表")
                    .doReadSync();
    
            if (list == null || list.isEmpty()) {
                System.out.println("质量控制表数据为空，请检查Excel文件是否正确");
                return;
            }
    
            // 从第二行开始计数，假设第一行是标题行
            int currentRow = 2;
            for (QualityControlExcelModel model : list) {
                try {
                    // 打印读取到的数据
                    System.out.println("读取到质量控制表数据: " + model);
    
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
    
                    QualityControl qualityControl = new QualityControl();
                    qualityControl.setStandardIdentity(model.getStandardIdentity());
                    qualityControl.setControlType(model.getControlType());
                    qualityControl.setControlDescription(model.getControlDescription());
                    qualityControl.setControlFormula(model.getControlFormula());
                    qualityControlMapper.insert(qualityControl);
                } catch (IllegalArgumentException e) {
                    System.out.println("导入质量控制表时发生异常: 第 " + currentRow + " 行数据错误：" + e.getMessage());
                    throw new IllegalArgumentException("导入质量控制表时发生异常: 第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }
        } catch (IOException e) {
            System.out.println("读取Excel文件时发生IO异常: " + e.getMessage());
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("导入质量控制表时发生异常: " + e.getMessage());
            throw new IllegalArgumentException("导入质量控制表时发生异常: " + e.getMessage(), e);
        }
    }    

    // 导入修约表
    public void importRoundingRules(MultipartFile file) {
        try {
            List<RoundingRulesExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(RoundingRulesExcelModel.class)
                    .sheet("修约表")
                    .doReadSync();
    
            if (list == null || list.isEmpty()) {
                System.out.println("修约表数据为空，请检查Excel文件是否正确");
                return;
            }
    
            // 从第二行开始计数，假设第一行是标题行
            int currentRow = 2;
            for (RoundingRulesExcelModel model : list) {
                try {
                    // 打印读取到的数据
                    System.out.println("读取到修约表数据: " + model);
    
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getRoundingType() == null || model.getRoundingType().isEmpty()) {
                        throw new IllegalArgumentException("修约类型不能为空");
                    }
                    if (model.getDecimalPlaces() == null) {
                        throw new IllegalArgumentException("位数不能为空");
                    }
    
                    RoundingRules roundingRules = new RoundingRules();
                    roundingRules.setStandardIdentity(model.getStandardIdentity());
                    roundingRules.setResultIdentity(model.getResultIdentity());
                    roundingRules.setRoundingType(model.getRoundingType());
                    roundingRules.setRoundingCondition(model.getRoundingCondition());
                    roundingRules.setDecimalPlaces(model.getDecimalPlaces());
                    roundingRulesMapper.insert(roundingRules);
                } catch (IllegalArgumentException e) {
                    System.out.println("导入修约表时发生异常: 第 " + currentRow + " 行数据错误：" + e.getMessage());
                    throw new IllegalArgumentException("导入修约表时发生异常: 第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }
        } catch (IOException e) {
            System.out.println("读取Excel文件时发生IO异常: " + e.getMessage());
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("导入修约表时发生异常: " + e.getMessage());
            throw new IllegalArgumentException("导入修约表时发生异常: " + e.getMessage(), e);
        }
    }    
    
    // 导入所有表
    public void importExcel(MultipartFile file) throws IOException {
        importStandardPretreatment(file);
        importRoundingRules(file);
        importQualityControl(file);
        importCalculationFormula(file);
        importStandardDetection(file);
    }
}
