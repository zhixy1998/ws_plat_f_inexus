/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.parseReport.impl;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Iterator;
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
import com.persee.workstation.uv_inexus.dao.guide.parseReport.CalculationFormulaDao;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.DetectionDao;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.QualityControlDao;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.RoundingRulesDao;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.StandardPretreatmentDao;
import com.persee.workstation.uv_inexus.dto.parseReport.CalculationResultDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionDetailDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionParametersDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.InputParamDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.MultiTableDataDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.PretreatmentDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.QualityControlDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingParamVO;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingVO;
import com.persee.workstation.uv_inexus.po.guide.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.guide.parseReport.Detection;
import com.persee.workstation.uv_inexus.po.guide.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.po.guide.parseReport.RoundingRules;
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConcisePublicUtilService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.DetectionService;

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

    @Autowired
    private ConcisePublicUtilService concisePublicUtilService;

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON转换工具
    // 日期格式转换器（支持多种常见格式）
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    //    public static final Set<String> ALLOWED_QUERY_FIELDS = Set.of(
//            "standard_identity", "industry", "release_date", "startDate", "endDate",
//            "standard_number", "industry_type", "standard_name"
//    );
    public static final Set<String> ALLOWED_QUERY_FIELDS = new HashSet<String>() {{
        add("standard_identity");
        add("detection_type");
        add("standard_name");
        add("industry");
        add("release_date");
        add("startDate");
        add("endDate");
    }};


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

        // 3. 前处理表：汇总描述 + 完整数据列表
        List<StandardPretreatment> pretreatmentList = standardPretreatmentDao.findByDetectionId(detectionId);
        int pretreatmentCount = pretreatmentList.size();
        String preTreatmentDesc = pretreatmentCount + "种前处理方式";
        // 转换为DTO列表（使用静态内部类）
        List<DetectionParametersDTO.StandardPretreatmentSubDTO> preTreatmentDTOList = pretreatmentList.stream()
                .map(pretreatment -> {
                    DetectionParametersDTO.StandardPretreatmentSubDTO dto = new DetectionParametersDTO.StandardPretreatmentSubDTO();
                    dto.setDetectionId(pretreatment.getDetectionId());
                    dto.setPretreatmentDescription(pretreatment.getPretreatmentDescription());
                    return dto;
                }).collect(Collectors.toList());

        // 4. 质量控制表：汇总类型 + 完整数据列表
        List<QualityControl> qualityControlList = qualityControlDao.findByDetectionId(detectionId);
        String qcType = qualityControlList.stream()
                .map(QualityControl::getControlType)
                .filter(controlType -> controlType != null && !controlType.isEmpty())
                .collect(Collectors.joining("；")); // 分号隔开
        // 转换为DTO列表（使用静态内部类）
        List<DetectionParametersDTO.QualityControlSubDTO> qualityControlDTOList = qualityControlList.stream()
                .map(qualityControl -> {
                    DetectionParametersDTO.QualityControlSubDTO dto = new DetectionParametersDTO.QualityControlSubDTO();
                    dto.setDetectionId(qualityControl.getDetectionId());
                    dto.setControlType(qualityControl.getControlType());
                    dto.setControlDescription(qualityControl.getControlDescription());
                    dto.setControlFormula(qualityControl.getControlFormula());
                    return dto;
                }).collect(Collectors.toList());

        // 5. 计算公式表：汇总结果表示 + 完整数据列表
        List<CalculationFormula> formulaList = calculationFormulaDao.findByDetectionId(detectionId);
        String resultExpression = formulaList.stream()
                .map(CalculationFormula::getResultIdentity)
                .filter(expr -> expr != null && !expr.isEmpty())
                .collect(Collectors.joining("、")); // 顿号隔开
        // 转换为DTO列表（使用静态内部类）
        List<DetectionParametersDTO.CalculationFormulaSubDTO> calculationFormulaDTOList = formulaList.stream()
                .map(formula -> {
                    DetectionParametersDTO.CalculationFormulaSubDTO dto = new DetectionParametersDTO.CalculationFormulaSubDTO();
                    dto.setId(formula.getId()); // 现在类型匹配（String）
                    dto.setDetectionId(formula.getDetectionId());
                    dto.setResultIdentity(formula.getResultIdentity());
                    dto.setResultName(formula.getResultName());
                    dto.setUnit(formula.getUnit());
                    dto.setInputParam1Name(formula.getInputParam1Name());
                    dto.setInputParam1Identity(formula.getInputParam1Identity());
                    dto.setInputParam1Unit(formula.getInputParam1Unit());
                    dto.setIsSampleAttribute1(formula.getIsSampleAttribute1());
                    dto.setCurveIdentifier1(formula.getCurveIdentifier1());
                    dto.setInputParam2Name(formula.getInputParam2Name());
                    dto.setInputParam2Identity(formula.getInputParam2Identity());
                    dto.setInputParam2Unit(formula.getInputParam2Unit());
                    dto.setIsSampleAttribute2(formula.getIsSampleAttribute2());
                    dto.setCurveIdentifier2(formula.getCurveIdentifier2());
                    dto.setInputParam3Name(formula.getInputParam3Name());
                    dto.setInputParam3Identity(formula.getInputParam3Identity());
                    dto.setInputParam3Unit(formula.getInputParam3Unit());
                    dto.setIsSampleAttribute3(formula.getIsSampleAttribute3());
                    dto.setCurveIdentifier3(formula.getCurveIdentifier3());
                    dto.setInputParam4Name(formula.getInputParam4Name());
                    dto.setInputParam4Identity(formula.getInputParam4Identity());
                    dto.setInputParam4Unit(formula.getInputParam4Unit());
                    dto.setIsSampleAttribute4(formula.getIsSampleAttribute4());
                    dto.setCurveIdentifier4(formula.getCurveIdentifier4());
                    dto.setInputParam5Name(formula.getInputParam5Name());
                    dto.setInputParam5Identity(formula.getInputParam5Identity());
                    dto.setInputParam5Unit(formula.getInputParam5Unit());
                    dto.setIsSampleAttribute5(formula.getIsSampleAttribute5());
                    dto.setCurveIdentifier5(formula.getCurveIdentifier5());
                    dto.setInputParam6Name(formula.getInputParam6Name());
                    dto.setInputParam6Identity(formula.getInputParam6Identity());
                    dto.setInputParam6Unit(formula.getInputParam6Unit());
                    dto.setIsSampleAttribute6(formula.getIsSampleAttribute6());
                    dto.setCurveIdentifier6(formula.getCurveIdentifier6());
                    dto.setInputParam7Name(formula.getInputParam7Name());
                    dto.setInputParam7Identity(formula.getInputParam7Identity());
                    dto.setInputParam7Unit(formula.getInputParam7Unit());
                    dto.setIsSampleAttribute7(formula.getIsSampleAttribute7());
                    dto.setCurveIdentifier7(formula.getCurveIdentifier7());
                    dto.setInputParam8Name(formula.getInputParam8Name());
                    dto.setInputParam8Identity(formula.getInputParam8Identity());
                    dto.setInputParam8Unit(formula.getInputParam8Unit());
                    dto.setIsSampleAttribute8(formula.getIsSampleAttribute8());
                    dto.setCurveIdentifier8(formula.getCurveIdentifier8());
                    dto.setInputParam9Name(formula.getInputParam9Name());
                    dto.setInputParam9Identity(formula.getInputParam9Identity());
                    dto.setInputParam9Unit(formula.getInputParam9Unit());
                    dto.setIsSampleAttribute9(formula.getIsSampleAttribute9());
                    dto.setCurveIdentifier9(formula.getCurveIdentifier9());
                    dto.setInputParam10Name(formula.getInputParam10Name());
                    dto.setInputParam10Identity(formula.getInputParam10Identity());
                    dto.setInputParam10Unit(formula.getInputParam10Unit());
                    dto.setIsSampleAttribute10(formula.getIsSampleAttribute10());
                    dto.setCurveIdentifier10(formula.getCurveIdentifier10());
                    dto.setFormulaText(formula.getFormulaText());
                    dto.setFormulaImage(formula.getFormulaImage());
                    dto.setDetectionLimit(formula.getDetectionLimit());
                    return dto;
                }).collect(Collectors.toList());

        // 6. 修约规则表：完整数据列表（关键修改：使用findByDetectionIdList替代findByDetectionId）
        List<RoundingRules> roundingRulesList = roundingRulesDao.findByDetectionIdList(detectionId); // 修正方法名
        List<DetectionParametersDTO.RoundingRulesSubDTO> roundingRulesDTOList = roundingRulesList.stream()
                .map(roundingRules -> {
                    DetectionParametersDTO.RoundingRulesSubDTO dto = new DetectionParametersDTO.RoundingRulesSubDTO();
                    dto.setDetectionId(roundingRules.getDetectionId());
                    dto.setResultIdentity(roundingRules.getResultIdentity());
                    dto.setRoundingType(roundingRules.getRoundingType());
                    dto.setRoundingCondition(roundingRules.getRoundingCondition());
                    dto.setDecimalPlaces(roundingRules.getDecimalPlaces());
                    return dto;
                }).collect(Collectors.toList());

        // 7. 封装返回DTO（包含主表基础信息 + 所有关联子表完整数据）
        DetectionParametersDTO dto = new DetectionParametersDTO();
        // 原有字段
        dto.setDetectionParameters(detectionParameters);
        dto.setPreTreatmentDesc(preTreatmentDesc);
        dto.setQcType(qcType);
        dto.setResultExpression(resultExpression);
        // 新增：主表基础信息
        dto.setStandardIdentity(detection.getStandardIdentity());
        dto.setDetectionType(detection.getDetectionType());
        dto.setDetectionInstrument(detection.getDetectionInstrument());
        dto.setStandardNumber(detection.getStandardNumber());
        dto.setStandardName(detection.getStandardName());
        dto.setDetectionItem(detection.getDetectionItem());
        dto.setReleaseStatus(detection.getReleaseStatus());
        // 新增：关联子表完整数据列表
        dto.setPreTreatmentList(preTreatmentDTOList);
        dto.setQualityControlList(qualityControlDTOList);
        dto.setCalculationFormulaList(calculationFormulaDTOList);
        dto.setRoundingRulesList(roundingRulesDTOList);

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
        String jsonStr = detection.getDetectionParameters(); // 获取原始JSON字符串
        // 2. 格式化主表测量参数
        List<String> detectionParameters = new ArrayList<>(); // 波长1、2、3列表
        List<String> wavelengthParamList = new ArrayList<>(); // 波长参数1、2列表
        Map<String, String> curveParameter = new HashMap<>(); // 曲线参数（推荐单位、推荐标样浓度、拟合公式）
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonStr);

            // 提取所有波长（动态获取，按数字顺序排序）
            List<String> wavelengthKeys = new ArrayList<>();
            for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                if (key.startsWith("波长")) {
                    wavelengthKeys.add(key);
                }
            }
            // 按数字排序：波长1、波长2、波长3...
            wavelengthKeys.sort(Comparator.comparingInt(k -> Integer.parseInt(k.replace("波长", ""))));
            for (String key : wavelengthKeys) {
                detectionParameters.add(jsonNode.get(key).asText());
            }

            // 提取所有系数（动态获取，按数字顺序排序）
            List<String> coefficientKeys = new ArrayList<>();
            for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                if (key.startsWith("系数")) {
                    coefficientKeys.add(key);
                }
            }
            // 按数字排序：系数1、系数2、系数3...
            coefficientKeys.sort(Comparator.comparingInt(k -> Integer.parseInt(k.replace("系数", ""))));
            for (String key : coefficientKeys) {
                wavelengthParamList.add(jsonNode.get(key).asText());
            }

            // 提取曲线参数：推荐单位、推荐标样浓度、拟合公式
            if (jsonNode.has("推荐单位")) curveParameter.put("recommended_unit", jsonNode.get("推荐单位").asText());
            if (jsonNode.has("推荐标样浓度")) curveParameter.put("recommended_standard_concentration", jsonNode.get("推荐标样浓度").asText());
            if (jsonNode.has("拟合公式")) curveParameter.put("fitting_formula", jsonNode.get("拟合公式").asText());
        } catch (Exception e) {
            log.error("解析波长JSON失败: {}", e.getMessage());
            // 解析失败时返回空列表（避免null）
            detectionParameters = new ArrayList<>();
            wavelengthParamList = new ArrayList<>();
            curveParameter = new HashMap<>();
        }

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
        List<FRoundingVO> roundingSettings = new ArrayList<>();

        // 构建 resultIdentity -> resultName 的映射
        Map<String, String> resultNameMap = calculationResults.stream()
                .filter(dto -> dto.getResultIdentity() != null)
                .collect(Collectors.toMap(
                        CalculationResultDTO::getResultIdentity,
                        dto -> dto.getResultName() != null ? dto.getResultName() : "",
                        (v1, v2) -> v1 // 如果有重复key，保留第一个
                ));

        // 按 resultIdentity 分组
        Map<String, List<RoundingRules>> resultGroup = roundingRules.stream()
                .collect(Collectors.groupingBy(RoundingRules::getResultIdentity));

        int paramIdCounter = 1; // 计数器

        for (Map.Entry<String, List<RoundingRules>> entry : resultGroup.entrySet()) {
            String resultIdentity = entry.getKey();
            List<RoundingRules> identityRules = entry.getValue();

            FRoundingVO vo = new FRoundingVO();
            vo.setResultIdentity(resultIdentity);
            
            // 使用映射表中的名称，如果没有则回退到使用 identity
            String resultName = resultNameMap.getOrDefault(resultIdentity, resultIdentity);
            vo.setResultIdentityName(resultName);

            List<FRoundingParamVO> params = new ArrayList<>();
            for (RoundingRules rule : identityRules) {
                FRoundingParamVO paramVO = new FRoundingParamVO();
                // 这里的ID生成逻辑保持与ImportService一致，使用detectionId_counter
                paramVO.setId(detectionId + "_" + paramIdCounter++);
                paramVO.setRoundingType(rule.getRoundingType());
                paramVO.setDecimalPlaces(rule.getDecimalPlaces());

                // 解析修约条件
                boolean parsed = concisePublicUtilService.parseRoundingCondition(rule.getRoundingCondition(), paramVO, resultIdentity);
                if (!parsed && rule.getRoundingCondition() != null && !rule.getRoundingCondition().trim().isEmpty()) {
                    log.warn("修约条件解析失败（DetectionId: {}, 规则: {}）：格式错误或使用了非{}变量", detectionId, rule.getRoundingCondition(), resultIdentity);
                }

                params.add(paramVO);
            }
            vo.setRoundingParams(params);

            // 校验修约范围是否正确 (按要求进行校验)
            // 判断是否需要校验：如果所有参数的上下限及包含标识都为空，则跳过校验
            boolean needCheck = false;
            if (vo.getRoundingParams() != null) {
                for (FRoundingParamVO p : vo.getRoundingParams()) {
                    if (p.getLowerLimit() != null || p.getUpperLimit() != null ||
                            p.getIsLowerLimitIncluded() != null || p.getIsUpperLimitIncluded() != null) {
                        needCheck = true;
                        break;
                    }
                }
            }

            if (needCheck) {
                try {
                    R<Boolean> checkResult = concisePublicUtilService.checkApproximateRange(vo);
                    if (checkResult != null && !checkResult.getSuccess()) {
                        log.error("修约规则校验失败（DetectionId: {}, ResultIdentity: {}）：{}",
                                detectionId, resultIdentity, checkResult.getMessage());
                        // 即使校验失败，也返回数据，以免影响页面展示，但在日志中记录错误
                    }
                } catch (Exception e) {
                    log.error("修约规则校验过程异常（DetectionId: {}, ResultIdentity: {}）", detectionId, resultIdentity, e);
                }
            }

            roundingSettings.add(vo);
        }

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
                dto.setResultIdentity(formula.getInputParam1Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute1());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier1()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam2Name() != null && !formula.getInputParam2Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam2Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam2Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute2());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier2()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam3Name() != null && !formula.getInputParam3Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam3Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam3Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute3());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier3()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam4Name() != null && !formula.getInputParam4Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam4Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam4Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute4());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier4()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam5Name() != null && !formula.getInputParam5Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam5Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam5Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute5());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier5()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam6Name() != null && !formula.getInputParam6Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam6Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam6Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute6());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier6()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam7Name() != null && !formula.getInputParam7Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam7Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam7Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute7());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier7()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam8Name() != null && !formula.getInputParam8Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam8Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam8Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute8());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier8()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam9Name() != null && !formula.getInputParam9Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam9Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam9Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute9());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier9()));
                inputParamsSet.add(dto);
            }
            if (formula.getInputParam10Name() != null && !formula.getInputParam10Name().isEmpty()) {
                InputParamDTO dto = new InputParamDTO();
                dto.setParamName(formula.getInputParam10Name());
                dto.setUnit(unit);
                dto.setResultIdentity(formula.getInputParam10Identity());
                dto.setIsSampleAttribute(formula.getIsSampleAttribute10());
                dto.setCurveIdentifier(processCurveIdentifier(formula.getCurveIdentifier10()));
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
        result.setDetectionParameters(detectionParameters); // 波长列表
        result.setWavelengthParamList(wavelengthParamList); // 波长参数列表
        result.setCurveParameter(curveParameter); // 曲线参数（推荐单位、推荐标样浓度、拟合公式）
        result.setCalculationResults(calculationResults); // 待计算结果
        result.setRoundingSettings(roundingSettings); // 修约设置
        result.setInputParams(inputParams); // 输入参数
        result.setPretreatments(pretreatmentDTOs);
        result.setQualityControls(qualityControlDTOs);

        return result;
    }

    /**
     * 处理curveIdentifier字段
     * 如果为空或不包含冒号，返回空字符串；如果包含冒号，按冒号分割返回最后一个值
     * 支持中文冒号（：）和英文冒号（:）
     */
    private String processCurveIdentifier(String curveIdentifier) {
        if (curveIdentifier == null || curveIdentifier.trim().isEmpty()) {
            return "";
        }
        if (!curveIdentifier.contains(":") && !curveIdentifier.contains("：")) {
            return "";
        }
        String result = curveIdentifier;
        if (curveIdentifier.contains("：")) {
            String[] parts = curveIdentifier.split("：");
            result = parts[parts.length - 1].trim();
        }
        if (result.contains(":")) {
            String[] parts = result.split(":");
            return parts[parts.length - 1].trim();
        }
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
            if (jsonNode.has("系数1")) {
                sb.append("系数1：").append(jsonNode.get("系数1").asText()).append("；");
            }
            if (jsonNode.has("系数2")) {
                sb.append("系数2：").append(jsonNode.get("系数2").asText()).append("；");
            }
            if (jsonNode.has("系数3")) {
                sb.append("系数3：").append(jsonNode.get("系数3").asText()).append("；");
            }
            // 2. 拟合类型（对应JSON中的"拟合公式"）
            if (jsonNode.has("拟合公式")) {
                sb.append("拟合公式").append(jsonNode.get("拟合公式").asText()).append("；");
            }
            // 3. 推荐浓度（对应JSON中的"推荐标样浓度"）
            if (jsonNode.has("推荐标样浓度")) {
                sb.append("推荐标样浓度").append(jsonNode.get("推荐标样浓度").asText()).append("；");
            }
            // 4. 浓度单位（对应JSON中的"推荐单位"）
            if (jsonNode.has("推荐单位")) {
                sb.append("推荐单位").append(jsonNode.get("推荐单位").asText()).append("；");
            }
            // 5. 拟合类型（对应JSON中的"光谱带宽"）
            if (jsonNode.has("光谱带宽")) {
                sb.append("光谱带宽").append(jsonNode.get("光谱带宽").asText()).append("；");
            }
            // 6. 推荐浓度（对应JSON中的"曲线方程"）
            if (jsonNode.has("曲线方程")) {
                sb.append("曲线方程").append(jsonNode.get("曲线方程").asText()).append("；");
            }
            // 7. 浓度单位（对应JSON中的"是否需要扣空白"）
            if (jsonNode.has("是否需要扣空白")) {
                sb.append("是否需要扣空白").append(jsonNode.get("是否需要扣空白").asText()).append("；");
            }
            // 8. 拟合类型（对应JSON中的"加入零点"）
            if (jsonNode.has("加入零点")) {
                sb.append("加入零点").append(jsonNode.get("加入零点").asText()).append("；");
            }
            // 9. 拟合类型（对应JSON中的"强制过零点"）
            if (jsonNode.has("强制过零点")) {
                sb.append("强制过零点").append(jsonNode.get("强制过零点").asText()).append("；");
            }
            // 10. 推荐浓度（对应JSON中的"斜率"）
            if (jsonNode.has("斜率")) {
                sb.append("斜率").append(jsonNode.get("斜率").asText()).append("；");
            }
            // 11. 浓度单位（对应JSON中的"截距"）
            if (jsonNode.has("截距")) {
                sb.append("截距").append(jsonNode.get("截距").asText()).append("；");
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
     *
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