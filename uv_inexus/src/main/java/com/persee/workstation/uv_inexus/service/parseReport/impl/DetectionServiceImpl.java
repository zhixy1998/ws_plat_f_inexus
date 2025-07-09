package com.persee.workstation.uv_inexus.service.parseReport.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.parseReport.DetectionDao;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DetectionServiceImpl implements DetectionService {

    @Autowired
    private DetectionDao detectionDao;

    // 允许查询的字段名（包含统计所需字段）
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
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if ("pageNum".equals(key) || "pageSize".equals(key)) {
                continue;
            }
            
            if (!ALLOWED_QUERY_FIELDS.contains(key)) {
                log.warn("忽略非法查询字段: {}", key);
                continue;
            }
            
            if ("startDate".equals(key)) {
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.ge("release_date", value);
                }
            } else if ("endDate".equals(key)) {
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.le("release_date", value);
                }
            } else {
                queryWrapper.like(key, value);
            }
        }
        
        IPage<Detection> resultPage = detectionDao.selectPage(page, queryWrapper);
        log.info("分页查询结果 - 总数: {}, 当前页记录数: {}, 页码: {}, 页大小: {}", 
                resultPage.getTotal(), 
                resultPage.getRecords().size(),
                resultPage.getCurrent(),
                resultPage.getSize());
                
        return resultPage;
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

    // 新增：去重分页查询实现
    @Override
    public IPage<DetectionListDTO> findDistinctPage(Page<DetectionListDTO> page, Map<String, String> params) {
        QueryWrapper<Detection> queryWrapper = new QueryWrapper<>();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if ("pageNum".equals(key) || "pageSize".equals(key)) {
                continue;
            }
            
            if (!ALLOWED_QUERY_FIELDS.contains(key)) {
                log.warn("忽略非法查询字段: {}", key);
                continue;
            }
            
            if ("startDate".equals(key)) {
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.ge("release_date", value);
                }
            } else if ("endDate".equals(key)) {
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.le("release_date", value);
                }
            } else {
                queryWrapper.like(key, value);
            }
        }
        
        IPage<DetectionListDTO> resultPage = detectionDao.selectDistinctPage(page, queryWrapper);
        
        log.info("去重分页查询结果 - 总数: {}, 当前页记录数: {}, 页码: {}, 页大小: {}", 
                resultPage.getTotal(), 
                resultPage.getRecords().size(),
                resultPage.getCurrent(),
                resultPage.getSize());
                
        return resultPage;
    }
}