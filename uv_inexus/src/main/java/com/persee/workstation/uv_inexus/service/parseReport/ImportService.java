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

    public void importStandardDetection(MultipartFile file) {
        try {
            // 1. 读取Excel主表数据
            List<StandardDetectionExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(StandardDetectionExcelModel.class)
                    .sheet("标准信息表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                System.out.println("标准信息表数据为空，请检查Excel文件是否正确");
                return;
            }

            // 2. 读取UV-VIS参数表（保持原有逻辑）
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

            // 3. 合并检测参数（保持原有逻辑）
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

            // 4. 处理标准标识映射：缓存“名称→ID”，避免重复查询数据库
            // Map<String, String> identityNameToIdMap = new HashMap<>();

            // 5. 遍历数据列表，处理每条记录
            for (int i = 0; i < list.size(); i++) {
                StandardDetectionExcelModel model = list.get(i);
                int currentRow = i + 2;

                // 校验必填字段（保持原有逻辑）
                if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：标准标识不能为空");
                }
                if (model.getIndustry() == null || model.getIndustry().isEmpty()) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：行业不能为空");
                }
                // 其他校验...

                // 7. 构建Detection对象，设置标准标识ID
                Detection detection = new Detection();
                detection.setStandardIdentity(model.getStandardIdentity()); // 存储ID而非名称
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

                // 8. 设置检测参数（保持原有逻辑）
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
                boolean exists = detectionMapper.existsByStandardIdentity(model.getStandardIdentity());
                if (exists) {
                        System.out.println("第 " + currentRow + " 行: 更新质量控制信息，标准标识 " + model.getStandardIdentity());
                        detectionMapper.updateByStandardIdentity(detection);

                    } else {
                        System.out.println("第 " + currentRow + " 行: 插入质量控制信息，标准标识 " + model.getStandardIdentity());
                        detectionMapper.insert(detection);

                    }
                // 9. 插入Detection表
                // detectionMapper.insert(detection);
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
                return;
            }
            
            // 缓存：标准标识文字 → 关联表ID（避免重复查询）
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            int currentRow = 2; // 从第二行开始计数（标题行不计入）

            for (StandardPretreatmentExcelModel model : list) {
                try {
                    // 校验必填字段
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    
                    String identityName = model.getStandardIdentity(); // 导入的是文字
                    
                    // 1. 先查询关联表（t_report_standard_detection）是否存在该文字对应的ID
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("标准标识在关联表中不存在: " + identityName);
                    }

                    // 2. 构建前处理对象（存储关联表ID）
                    StandardPretreatment standardPretreatment = new StandardPretreatment();
                    standardPretreatment.setStandardIdentity(detectionId.toString()); // 存ID
                    standardPretreatment.setPretreatmentDescription(model.getPretreatmentDescription());
                    // 设置其他字段...

                    // 3. 查重逻辑：检查前处理表中是否存在该ID的记录
                    boolean exists = standardPretreatmentMapper.existsByDetectionId(detectionId);

                    if (exists) {
                        // 4. 存在则更新
                        standardPretreatmentMapper.updateByDetectionId(standardPretreatment);
                        System.out.println("更新前处理记录: 关联ID=" + detectionId + ", 标准标识=" + identityName);
                    } else {
                        // 5. 不存在则插入
                        standardPretreatmentMapper.insert(standardPretreatment);
                        System.out.println("插入前处理记录: 关联ID=" + detectionId + ", 标准标识=" + identityName);
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } catch (Exception e) {
                    throw new RuntimeException("第 " + currentRow + " 行数据处理失败：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }
        } catch (IOException e) {
            System.out.println("读取Excel文件时发生IO异常: " + e.getMessage());
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("导入前处理信息表时发生异常: " + e.getMessage());
            throw new IllegalArgumentException("导入前处理信息表时发生异常: " + e.getMessage(), e);
        }
    }

    // 辅助方法：查询关联表（t_report_standard_detection）中是否存在该文字对应的ID
    private Integer getDetectionId(String identityName, Map<String, Integer> cache) {
        // 先从缓存获取
        if (cache.containsKey(identityName)) {
            return cache.get(identityName);
        }
        
        // 从关联表查询：通过标准标识文字获取ID
        Integer detectionId = detectionMapper.findIdByStandardIdentityName(identityName);
        if (detectionId != null) {
            cache.put(identityName, detectionId); // 缓存结果
        }
        
        return detectionId;
    }


    // 导入计算公式表
    public void importCalculationFormula(MultipartFile file) {
        try {
            // 提前提取Excel中所有工作表的图片
            Map<String, List<ExcelImageImportUtils.ImageInfo>> allImages = 
                ExcelImageImportUtils.extractAllImages(file);
            List<ExcelImageImportUtils.ImageInfo> formulaSheetImages = 
                allImages.getOrDefault("计算公式表", new ArrayList<>());
            
            // 打印图片提取结果（用于调试）
            System.out.println("提取到计算公式表的图片数量: " + formulaSheetImages.size());
            for (ExcelImageImportUtils.ImageInfo img : formulaSheetImages) {
                System.out.println("图片位置: 行=" + img.getRow() + ", 列=" + img.getCol() + 
                                ", 大小=" + img.getSize() + "字节, MIME=" + img.getMimeType());
            }

            // 读取Excel主数据
            List<CalculationFormulaExcelModel> list = EasyExcel.read(file.getInputStream())
                    .head(CalculationFormulaExcelModel.class)
                    .sheet("计算公式表")
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                System.out.println("计算公式表数据为空，请检查Excel文件是否正确");
                return;
            }

            System.out.println("++++++++ 读取到 " + list.size() + " 条计算公式数据 ++++++++");

            // 缓存标准标识与DetectionID的映射
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();
            int currentRow = 2;  // 从第二行开始计数（标题行不计入）

            // 遍历数据行处理
            for (CalculationFormulaExcelModel model : list) {
                try {
                    // 校验必填字段
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getResultIdentity() == null || model.getResultIdentity().isEmpty()) {
                        throw new IllegalArgumentException("结果标识不能为空");
                    }
                    if (model.getResultName() == null || model.getResultName().isEmpty()) {
                        throw new IllegalArgumentException("结果名称不能为空");
                    }
                    if (model.getUnit() == null || model.getUnit().isEmpty()) {
                        throw new IllegalArgumentException("单位不能为空");
                    }
                    if (model.getInputParam1Name() == null || model.getInputParam1Name().isEmpty()) {
                        throw new IllegalArgumentException("输入参数1名称不能为空");
                    }
                    if (model.getInputParam1Identity() == null || model.getInputParam1Identity().isEmpty()) {
                        throw new IllegalArgumentException("输入参数1标识不能为空");
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
                    paramIdentities.removeIf(Objects::isNull);

                    // 校验公式文本
                    String formulaText = model.getFormulaText();
                    if (formulaText == null || formulaText.isEmpty()) {
                        throw new IllegalArgumentException("公式文本不能为空");
                    }
                    if (!formulaText.contains("=")) {
                        throw new IllegalArgumentException("公式文本不是等式");
                    }

                    // 分割等式并校验结果标识
                    String[] parts = formulaText.split("=", 2);
                    String leftSide = parts[0].trim();
                    String rightSide = parts[1].trim();
                    String resultIdentity = model.getResultIdentity();
                    
                    String processedLeftSide = leftSide.replaceAll("[\\（\\(].*?[\\）\\)]", "").trim();
                    String processedResultIdentity = resultIdentity.replaceAll("[\\（\\(].*?[\\）\\)]", "").trim();
                    if (!processedLeftSide.equals(processedResultIdentity)) {
                        throw new IllegalArgumentException("结果标识有误，与公式文本左边不匹配！处理后左边: " 
                            + processedLeftSide + "，处理后结果标识: " + processedResultIdentity);
                    }

                    // 校验参数标识是否在公式中
                    List<String> invalidParams = new ArrayList<>();
                    for (int j = 0; j < paramIdentities.size(); j++) {
                        String paramIdentity = paramIdentities.get(j);
                        if (!rightSide.contains(paramIdentity)) {
                            invalidParams.add("参数" + (j + 1) + "标识: " + paramIdentity);
                        }
                    }
                    if (!invalidParams.isEmpty()) {
                        throw new IllegalArgumentException("以下参数标识有误，不在公式文本中: " + String.join(", ", invalidParams));
                    }

                    // 核心修改：使用公共方法获取关联表ID
                    String identityName = model.getStandardIdentity();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
                    }

                    // 提取当前行对应的图片
                    String base64Image = ExcelImageImportUtils.findImageByRow(formulaSheetImages, currentRow - 1);

                    // 打印图片匹配结果
                    if (base64Image != null) {
                        System.out.println("成功为第 " + currentRow + " 行匹配到图片，长度: " + base64Image.length() + " 字节");
                    } else {
                        System.out.println("未为第 " + currentRow + " 行匹配到图片，尝试使用单元格中的图片引用");
                        // 尝试使用模型中的图片引用
                        if (model.getFormulaImage() != null && model.getFormulaImage().startsWith("data:image/")) {
                            base64Image = model.getFormulaImage();
                            System.out.println("使用单元格中的Base64图片，长度: " + base64Image.length() + " 字节");
                        }
                    }

                    // 构建CalculationFormula对象
                    CalculationFormula calculationFormula = new CalculationFormula();
                    calculationFormula.setStandardIdentity(detectionId.toString()); // 使用关联表ID
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
                    calculationFormula.setFormulaImage(base64Image);
                    calculationFormula.setDetectionLimit(model.getDetectionLimit());

                    // 保存到数据库（使用关联表ID查重）
                    boolean exists = calculationFormulaMapper.existsByStandardIdentity(detectionId.toString());
                    if (exists) {
                        calculationFormulaMapper.updateByStandardIdentity(calculationFormula);
                        System.out.println("更新第 " + currentRow + " 行数据，标准标识ID: " + detectionId);
                    } else {
                        calculationFormulaMapper.insert(calculationFormula);
                        System.out.println("插入第 " + currentRow + " 行数据，标准标识ID: " + detectionId);
                    }

                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
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
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }

                    // 1. 通过公共方法获取关联表ID（替换重复逻辑）
                    String identityName = model.getStandardIdentity();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
                    }

                    // 2. 构建QualityControl对象（使用关联表ID）
                    QualityControl qualityControl = new QualityControl();
                    qualityControl.setStandardIdentity(detectionId.toString());
                    qualityControl.setControlType(model.getControlType());
                    qualityControl.setControlDescription(model.getControlDescription());
                    qualityControl.setControlFormula(model.getControlFormula());

                    // 3. 查重逻辑（使用关联表ID而非文字）
                    boolean exists = qualityControlMapper.existsByStandardIdentity(detectionId.toString());

                    if (exists) {
                        qualityControlMapper.updateByStandardIdentity(qualityControl);
                        System.out.println("更新质量控制记录: 标准标识ID=" + detectionId + ", 文字=" + identityName);
                    } else {
                        qualityControlMapper.insert(qualityControl);
                        System.out.println("插入质量控制记录: 标准标识ID=" + detectionId + ", 文字=" + identityName);
                    }

                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("第 " + currentRow + " 行数据错误：" + e.getMessage(), e);
                } catch (Exception e) {
                    throw new RuntimeException("第 " + currentRow + " 行数据处理失败：" + e.getMessage(), e);
                } finally {
                    currentRow++;
                }
            }
            System.out.println("质量控制表导入完成。");
        } catch (IOException e) {
            throw new RuntimeException("读取Excel文件时发生异常: " + e.getMessage(), e);
        } catch (Exception e) {
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
                return;
            }

            // 从第二行开始计数（标题行不计入）
            int currentRow = 2;
            // 缓存：标准标识文字 → 关联表ID（减少数据库查询）
            Map<String, Integer> identityNameToDetectionIdMap = new HashMap<>();

            for (RoundingRulesExcelModel model : list) {
                try {
                    // 校验必填字段
                    if (model.getStandardIdentity() == null || model.getStandardIdentity().isEmpty()) {
                        throw new IllegalArgumentException("标准标识不能为空");
                    }
                    if (model.getRoundingType() == null || model.getRoundingType().isEmpty()) {
                        throw new IllegalArgumentException("修约类型不能为空");
                    }
                    if (model.getDecimalPlaces() == null) {
                        throw new IllegalArgumentException("位数不能为空");
                    }

                    // 1. 调用公共方法获取关联表ID（核心修改）
                    String identityName = model.getStandardIdentity();
                    Integer detectionId = getDetectionId(identityName, identityNameToDetectionIdMap);
                    if (detectionId == null) {
                        throw new IllegalArgumentException("未找到匹配的标准标识: " + identityName);
                    }

                    // 2. 构建修约规则对象（使用ID存储）
                    RoundingRules roundingRules = new RoundingRules();
                    roundingRules.setStandardIdentity(detectionId.toString()); // 存储关联表ID
                    roundingRules.setResultIdentity(model.getResultIdentity());
                    roundingRules.setRoundingType(model.getRoundingType());
                    roundingRules.setRoundingCondition(model.getRoundingCondition());
                    roundingRules.setDecimalPlaces(model.getDecimalPlaces());

                    // 3. 查重逻辑（使用ID而非文字）
                    boolean exists = roundingRulesMapper.existsByStandardIdentity(detectionId.toString());

                    // 4. 存在则更新，不存在则插入
                    if (exists) {
                        roundingRulesMapper.updateByStandardIdentity(roundingRules);
                        System.out.println("更新修约规则: 标准标识ID=" + detectionId + "，文字=" + identityName);
                    } else {
                        roundingRulesMapper.insert(roundingRules);
                        System.out.println("插入修约规则: 标准标识ID=" + detectionId + "，文字=" + identityName);
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
            throw new IllegalArgumentException("导入修约表时发生异常: " + e.getMessage(), e);
        }
    }
    
    // 导入所有表
    public void importExcel(MultipartFile file) throws IOException {
        importStandardDetection(file);
        importStandardPretreatment(file);
        importRoundingRules(file);
        importQualityControl(file);
        // importCalculationFormula(file);
    }
}
