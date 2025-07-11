package com.persee.workstation.uv_inexus.service.parseReport.impl;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.dao.parseReport.CalculationFormulaDao;
import com.persee.workstation.uv_inexus.dao.parseReport.DetectionDao;
import com.persee.workstation.uv_inexus.dao.parseReport.QualityControlDao;
import com.persee.workstation.uv_inexus.dao.parseReport.RoundingRulesDao;
import com.persee.workstation.uv_inexus.dao.parseReport.StandardPretreatmentDao;
import com.persee.workstation.uv_inexus.dto.parseReport.CalculationResultDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionDetailDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionParametersDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.InputParamDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.MultiTableDataDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.PretreatmentDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.QualityControlDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.RoundingSettingDTO;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules;
import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DetectionServiceImpl implements DetectionService {

    @Autowired
    private DetectionDao detectionDao;

    @Autowired
    private StandardPretreatmentDao standardPretreatmentDao; // 前处理表DAO

    @Autowired
    private QualityControlDao qualityControlDao; // 质量控制表DAO

    @Autowired
    private CalculationFormulaDao calculationFormulaDao; // 计算公式表DAO

    @Autowired
    private RoundingRulesDao roundingRulesDao; // 计算公式表DAO

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON转换工具
    // 日期格式转换器（支持多种常见格式）
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static final Set<String> ALLOWED_QUERY_FIELDS = Set.of(
            "standard_identity", "industry", "release_date", "startDate", "endDate",
            "standard_number", "industry_type", "standard_name"
    );

    @Override
    public List<Detection> findAll() {
        return detectionDao.findAll();
    }

    @Override
    public Detection findById(Integer id) {
        return detectionDao.findById(id);
    }

    @Override
    public void insert(Detection detection) {
        detectionDao.insert(detection);
    }

    @Override
    public void update(Detection detection) {
        detectionDao.update(detection);
    }

    @Override
    public void deleteById(Integer id) {
        detectionDao.deleteById(id);
    }

    @Override
    public IPage<Detection> findPage(Page<Detection> page, Map<String, String> params) {
        QueryWrapper<Detection> queryWrapper = new QueryWrapper<>();
        params.forEach((key, value) -> {
            // 跳过分页参数
            if ("pageNum".equals(key) || "pageSize".equals(key)) {
                return;
            }
            // 过滤非法字段
            if (!ALLOWED_QUERY_FIELDS.contains(key)) {
                log.warn("忽略非法查询字段: {}", key);
                return;
            }
            // 处理日期参数（startDate和endDate）
            if ("startDate".equals(key) || "endDate".equals(key)) {
                // 转换日期字符串为Timestamp（空值或格式错误时跳过）
                Timestamp timestamp = parseDateToTimestamp(value);
                if (timestamp == null) {
                    log.info("日期参数格式错误或为空，跳过查询条件: {}={}", key, value);
                    return;
                }
                // 添加日期条件
                if ("startDate".equals(key)) {
                    queryWrapper.ge("release_date", timestamp);
                } else {
                    queryWrapper.le("release_date", timestamp);
                }
            } else {
                // 非日期字段：仅当值非空时添加like条件
                if (value != null && !value.trim().isEmpty()) {
                    queryWrapper.like(key, value);
                }
            }
        });
        return detectionDao.selectPage(page, queryWrapper);
    }

    @Override
    public List<Map<String, Object>> countIndustryType() {
        return detectionDao.countIndustryType();
    }

    @Override
    public List<Map<String, Object>> countIndustry() {
        return detectionDao.countIndustry();
    }

    @Override
    public int deleteBatchIds(List<Integer> ids) {
        return detectionDao.deleteBatchIds(ids);
    }

    @Override
    public IPage<DetectionListDTO> findDistinctPage(Page<DetectionListDTO> page, Map<String, String> params) {
        QueryWrapper<Detection> queryWrapper = new QueryWrapper<>();
        params.forEach((key, value) -> {
            // 跳过分页参数
            if ("pageNum".equals(key) || "pageSize".equals(key)) {
                return;
            }
            // 过滤非法字段
            if (!ALLOWED_QUERY_FIELDS.contains(key)) {
                log.warn("忽略非法查询字段: {}", key);
                return;
            }
            // 处理日期参数（startDate和endDate）
            if ("startDate".equals(key) || "endDate".equals(key)) {
                // 转换日期字符串为Timestamp（空值或格式错误时跳过）
                Timestamp timestamp = parseDateToTimestamp(value);
                if (timestamp == null) {
                    log.info("日期参数格式错误或为空，跳过查询条件: {}={}", key, value);
                    return;
                }
                // 添加日期条件
                if ("startDate".equals(key)) {
                    queryWrapper.ge("release_date", timestamp);
                } else {
                    queryWrapper.le("release_date", timestamp);
                }
            } else {
                // 非日期字段：仅当值非空时添加like条件
                if (value != null && !value.trim().isEmpty()) {
                    queryWrapper.like(key, value);
                }
            }
        });
        return detectionDao.selectDistinctPage(page, queryWrapper);
    }

    @Override
    public DetectionDetailDTO getDetailById(Integer id) {
        Detection detection = detectionDao.findById(id);
        if (detection == null) {
            return null;
        }
        
        List<Detection> originalDataList = detectionDao.findByStandardNumber(detection.getStandardNumber());
        if (originalDataList == null || originalDataList.isEmpty()) {
            return null;
        }
        
        DetectionListDTO mainData = new DetectionListDTO();
        mainData.setId(detection.getId());
        mainData.setIndustry(detection.getIndustry());
        mainData.setStandardName(detection.getStandardName());
        mainData.setStandardNumber(detection.getStandardNumber());
        mainData.setReleaseDate(detection.getReleaseDate());
        mainData.setExpirationDate(detection.getExpirationDate());
        mainData.setEffectiveDate(detection.getEffectiveDate());
        mainData.setIndustryType(detection.getIndustryType());
        mainData.setReleaseStatus(detection.getReleaseStatus());
        mainData.setCount(originalDataList.size());
        
        List<Map<String, Object>> identityTypeList = originalDataList.stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("standard_identity", item.getStandardIdentity());
                    map.put("detection_type", item.getDetectionType());
                    return map;
                })
                .collect(Collectors.toList());
        
        DetectionDetailDTO detailDTO = new DetectionDetailDTO();
        detailDTO.setMainData(mainData);
        detailDTO.setOriginalDataList(originalDataList);
        detailDTO.setIdentityTypeList(identityTypeList);
        
        return detailDTO;
    }

    @Override
    public DetectionParametersDTO getParametersById(Integer id) {
        // 1. 查询主表记录
        Detection detection = detectionDao.findById(id);
        if (detection == null) {
            return null;
        }
        // 关联字段：使用detection_id（主表id）关联子表
        String detectionId = detection.getId().toString(); 
        log.info("关联的detection_id: {}", detectionId);

        // 2. 格式化主表测量参数（原有逻辑不变）
        String detectionParameters = formatJsonToText(detection.getDetectionParameters());

        // 3. 优化前处理字段：返回个数 + "种前处理方式"
        List<StandardPretreatment> pretreatmentList = standardPretreatmentDao.findByDetectionId(detectionId);
        int pretreatmentCount = pretreatmentList.size();
        String preTreatmentDesc = pretreatmentCount + "种前处理方式";

        // 4. 优化质控类型：多个用分号隔开
        List<QualityControl> qualityControlList = qualityControlDao.findByDetectionId(detectionId);
        String qcType = qualityControlList.stream()
                .map(QualityControl::getControlType)
                .filter(controlType -> controlType != null && !controlType.isEmpty())
                .collect(Collectors.joining("；")); // 分号隔开

        // 5. 优化结果表示：多个用顿号隔开
        List<CalculationFormula> formulaList = calculationFormulaDao.findByDetectionId(detectionId);
        String resultExpression = formulaList.stream()
                .map(CalculationFormula::getResultIdentity)
                .filter(expr -> expr != null && !expr.isEmpty())
                .collect(Collectors.joining("、")); // 顿号隔开

        // 6. 封装返回DTO
        DetectionParametersDTO dto = new DetectionParametersDTO();
        dto.setDetectionParameters(detectionParameters);
        dto.setPreTreatmentDesc(preTreatmentDesc);
        dto.setQcType(qcType);
        dto.setResultExpression(resultExpression);

        return dto;
    }

    @Override
    public MultiTableDataDTO getMultiTablesData(Integer id) {
        // 1. 查询主表记录
        Detection detection = detectionDao.findById(id);
        if (detection == null) {
            return null;
        }
        String detectionId = detection.getId(); // 获取detection_id用于关联子表

        // 2. 格式化主表测量参数
        String detectionParameters = formatJsonToText(detection.getDetectionParameters());

        // 3. 查询计算公式表数据（待计算结果）
        List<CalculationFormula> formulas = calculationFormulaDao.findByDetectionIdList(detectionId);
        List<CalculationResultDTO> calculationResults = formulas.stream().map(formula -> {
            CalculationResultDTO dto = new CalculationResultDTO();
            dto.setResultIdentity(formula.getResultIdentity());
            dto.setResultName(formula.getResultName());
            dto.setUnit(formula.getUnit());
            dto.setFormulaText(formula.getFormulaText());
            return dto;
        }).collect(Collectors.toList());

        // 4. 查询修约表数据
        List<RoundingRules> roundingRules = roundingRulesDao.findByDetectionIdList(detectionId);
        List<RoundingSettingDTO> roundingSettings = roundingRules.stream().map(rule -> {
            RoundingSettingDTO dto = new RoundingSettingDTO();
            dto.setResultIdentity(rule.getResultIdentity());
            dto.setRoundingType(rule.getRoundingType());
            dto.setRoundingCondition(rule.getRoundingCondition());
            dto.setDecimalPlaces(rule.getDecimalPlaces());
            return dto;
        }).collect(Collectors.toList());

        // 5. 处理输入参数（10个参数）
        Set<InputParamDTO> inputParamsSet = new HashSet<>();
        for (CalculationFormula formula : formulas) {
            String unit = formula.getUnit();
            String resultIdentity = formula.getResultIdentity();
            
            // 处理10个参数
            if (formula.getInputParam1Name() != null && !formula.getInputParam1Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam1Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam2Name() != null && !formula.getInputParam2Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam2Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam3Name() != null && !formula.getInputParam3Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam3Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam4Name() != null && !formula.getInputParam4Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam4Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam5Name() != null && !formula.getInputParam5Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam5Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam6Name() != null && !formula.getInputParam6Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam6Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam7Name() != null && !formula.getInputParam7Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam7Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam8Name() != null && !formula.getInputParam8Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam8Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam9Name() != null && !formula.getInputParam9Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam9Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam10Name() != null && !formula.getInputParam10Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam10Name());
                dto.setUnit(unit);
                dto.setResultIdentity(resultIdentity);
                inputParamsSet.add(dto);
            }
        }
        // 转换为List并保持顺序
        List<InputParamDTO> inputParams = new ArrayList<>(inputParamsSet);

        // 6. 查询前处理表数据
        List<StandardPretreatment> pretreatments = standardPretreatmentDao.findByDetectionIdList(detectionId);
        List<PretreatmentDTO> pretreatmentDTOs = pretreatments.stream().map(pretreatment -> {
            PretreatmentDTO dto = new PretreatmentDTO();
            dto.setPretreatmentDescription(pretreatment.getPretreatmentDescription());
            return dto;
        }).collect(Collectors.toList());

        // 7. 查询质量控制表数据
        List<QualityControl> qualityControls = qualityControlDao.findByDetectionIdList(detectionId);
        List<QualityControlDTO> qualityControlDTOs = qualityControls.stream().map(qualityControl -> {
            QualityControlDTO dto = new QualityControlDTO();
            dto.setControlFormula(qualityControl.getControlFormula());
            dto.setControlType(qualityControl.getControlType());
            // 超限操作默认空
            return dto;
        }).collect(Collectors.toList());

        // 8. 组装返回DTO
        MultiTableDataDTO result = new MultiTableDataDTO();
        result.setDetectionParameters(detectionParameters);
        result.setCalculationResults(calculationResults); // 待计算结果
        result.setRoundingSettings(roundingSettings); // 修约设置
        result.setInputParams(inputParams); // 输入参数
        result.setPretreatments(pretreatmentDTOs);
        result.setQualityControls(qualityControlDTOs);

        return result;
    }

    /**
     * 将JSON字符串解析为自定义格式文本
     */
    private String formatJsonToText(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return "无测量参数";
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            StringBuilder sb = new StringBuilder();
            
            // 1. 波长（对应JSON中的"波长1"）
            if (jsonNode.has("波长1")) {
                sb.append("波长1：").append(jsonNode.get("波长1").asText()).append("；");
            }
            if (jsonNode.has("波长2")) {
                sb.append("波长2：").append(jsonNode.get("波长2").asText()).append("；");
            }
            if (jsonNode.has("波长3")) {
                sb.append("波长3：").append(jsonNode.get("波长3").asText()).append("；");
            }
            // 2. 拟合类型（对应JSON中的"拟合公式"）
            if (jsonNode.has("拟合公式")) {
                sb.append("拟合类型：").append(jsonNode.get("拟合公式").asText()).append("；");
            }
            // 3. 推荐浓度（对应JSON中的"推荐标样浓度"）
            if (jsonNode.has("推荐标样浓度")) {
                sb.append("推荐浓度：").append(jsonNode.get("推荐标样浓度").asText()).append("；");
            }
            // 4. 浓度单位（对应JSON中的"推荐单位"）
            if (jsonNode.has("推荐单位")) {
                sb.append("浓度单位：").append(jsonNode.get("推荐单位").asText()).append("；");
            }

            // 去除最后一个分号（如果有）
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();

        } catch (JsonProcessingException e) {
            log.error("解析测量参数JSON失败: {}", e.getMessage());
            return "测量参数格式错误";
        }
    }

    /**
     * 工具方法：将日期字符串转换为Timestamp
     * 支持格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     * @param dateStr 日期字符串（可能为空或格式错误）
     * @return 转换后的Timestamp，失败则返回null
     */
    private Timestamp parseDateToTimestamp(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 尝试完整时间格式
            return new Timestamp(dateFormat.parse(dateStr).getTime());
        } catch (ParseException e1) {
            try {
                // 尝试短日期格式（自动补全时间为00:00:00）
                return new Timestamp(shortDateFormat.parse(dateStr).getTime());
            } catch (ParseException e2) {
                log.error("日期格式错误，支持格式为 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd，输入值: {}", dateStr);
                return null;
            }
        }
    }
}