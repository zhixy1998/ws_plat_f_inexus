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
import com.persee.workstation.uv_inexus.dao.parseReport.DetectionDao;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionDetailDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DetectionServiceImpl implements DetectionService {

    @Autowired
    private DetectionDao detectionDao;

    public static final Set<String> ALLOWED_QUERY_FIELDS = Set.of(
            "standard_identity", "industry", "release_date", "startDate", "endDate",
            "standard_number", "industry_type"
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
        // 1. 通过id查询对应的记录
        Detection detection = detectionDao.findById(id);
        if (detection == null) {
            return null;
        }
        
        // 2. 查询该standard_number对应的所有原始数据
        List<Detection> originalDataList = detectionDao.findByStandardNumber(detection.getStandardNumber());
        if (originalDataList == null || originalDataList.isEmpty()) {
            return null;
        }
        
        // 3. 构建主数据（去重后的信息）
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
        
        // 4. 组装identityTypeList数组
        List<Map<String, String>> identityTypeList = originalDataList.stream()
                .map(item -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", item.getId()); 
                    map.put("standard_identity", item.getStandardIdentity());
                    map.put("detection_type", item.getDetectionType());
                    return map;
                })
                .collect(Collectors.toList());
        
        // 5. 封装详情DTO
        DetectionDetailDTO detailDTO = new DetectionDetailDTO();
        detailDTO.setMainData(mainData);
        detailDTO.setOriginalDataList(originalDataList);
        detailDTO.setIdentityTypeList(identityTypeList);
        
        return detailDTO;
    }
}