package com.persee.workstation.uv_inexus.service.parseReport.impl;

import java.util.HashMap;
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
import com.persee.workstation.uv_inexus.dao.parseReport.StandardPretreatmentDao;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionDetailDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionParametersDTO;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
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

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON转换工具

    public static final Set<String> ALLOWED_QUERY_FIELDS = Set.of(
            "standard_identity", "industry", "release_date", "startDate", "endDate",
            "standard_number", "industry_type"
    );

    // 原有方法保持不变...
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
            if ("pageNum".equals(key) || "pageSize".equals(key)) return;
            if (!ALLOWED_QUERY_FIELDS.contains(key)) {
                log.warn("忽略非法查询字段: {}", key);
                return;
            }
            if ("startDate".equals(key)) {
                queryWrapper.ge("release_date", value);
            } else if ("endDate".equals(key)) {
                queryWrapper.le("release_date", value);
            } else {
                queryWrapper.like(key, value);
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
            if ("pageNum".equals(key) || "pageSize".equals(key)) return;
            if (!ALLOWED_QUERY_FIELDS.contains(key)) {
                log.warn("忽略非法查询字段: {}", key);
                return;
            }
            if ("startDate".equals(key)) {
                queryWrapper.ge("release_date", value);
            } else if ("endDate".equals(key)) {
                queryWrapper.le("release_date", value);
            } else {
                queryWrapper.like(key, value);
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

    // 核心：查询参数及子表字段（修正版）
    @Override
    public DetectionParametersDTO getParametersById(Integer id) {
        // 1. 查询主表记录
        Detection detection = detectionDao.findById(id);
        if (detection == null) {
            return null;
        }
        String standardIdentity = detection.getId(); // 关键：用standard_identity关联子表

        // 2. 转换主表的detectionParameters（JSON字符串转对象）
        String detectionParameters = formatJsonToText(detection.getDetectionParameters());

        // 3. 查询前处理表（通过standard_identity）
        StandardPretreatment pretreatment = standardPretreatmentDao.findByStandardIdentity(standardIdentity);
        String preTreatmentDesc = pretreatment != null ? pretreatment.getPretreatmentDescription() : null;
        System.out.println("999999999999999" + preTreatmentDesc+standardIdentity);
        System.out.println("88888888888888" + pretreatment);

        // 4. 查询质量控制表（通过standard_identity）
        QualityControl qualityControl = qualityControlDao.findByStandardIdentity(standardIdentity);
        String qcType = qualityControl != null ? qualityControl.getControlType() : null;

        // 5. 查询计算公式表（通过standard_identity）
        CalculationFormula formula = calculationFormulaDao.findByStandardIdentity(standardIdentity);
        String resultIdentity = formula != null ? formula.getResultIdentity() : null;

        // 6. 封装返回DTO
        DetectionParametersDTO dto = new DetectionParametersDTO();
        dto.setDetectionParameters(detectionParameters);
        dto.setPreTreatmentDesc(preTreatmentDesc);
        dto.setQcType(qcType);
        dto.setResultExpression(resultIdentity);

        return dto;
    }

    /**
     * 将JSON字符串解析为自定义格式文本
     * 例如：{"波长1":"620","拟合公式":"一次拟合"} → "波长：620；拟合类型：一次拟合；"
     */
    private String formatJsonToText(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return "无测量参数";
        }

        try {
            // 解析JSON为JsonNode
            JsonNode jsonNode = objectMapper.readTree(jsonStr);

            // 定义JSON字段与目标文本的映射关系（根据你的需求调整）
            StringBuilder sb = new StringBuilder();
            // 1. 波长（对应JSON中的"波长1"）
            if (jsonNode.has("波长1")) {
                sb.append("波长：").append(jsonNode.get("波长1").asText()).append("；");
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

            // 去除最后一个分号（可选）
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();

        } catch (JsonProcessingException e) {
            log.error("解析测量参数JSON失败: {}", e.getMessage());
            return "测量参数格式错误"; // 解析失败时返回提示
        }
    }
}