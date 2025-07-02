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
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service // Spring注解，标记这是一个服务组件，会被Spring管理
public class DetectionServiceImpl implements DetectionService {

    @Autowired // Spring注解，自动注入DetectionDao的实例
    private DetectionDao detectionDao;

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

     // 允许查询的字段名
    public  static final Set<String> ALLOWED_QUERY_FIELDS = Set.of(
        "standard_identity", "industry", "release_date", "startDate", "endDate"
    );

    @Override
    public IPage<Detection> findPage(Page<Detection> page, Map<String, String> params) {
        QueryWrapper<Detection> queryWrapper = new QueryWrapper<>();
        
        // 动态构建查询条件
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 跳过分页参数
            if ("pageNum".equals(key) || "pageSize".equals(key)) {
                continue;
            }
            
            // 检查字段是否合法
            if (!ALLOWED_QUERY_FIELDS.contains(key)) {
                log.warn("忽略非法查询字段: {}", key);
                continue;
            }
            
            // System.out.println("时间: " + "startDate".equals(key) + "endDate".equals(key));
            
            // 处理日期区间查询
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
        
        // 打印 SQL（调试用）
        // System.out.println("执行的 SQL: " + queryWrapper.getCustomSqlSegment());
        
        IPage<Detection> resultPage = detectionDao.selectPage(page, queryWrapper);
        
        log.info("分页查询结果 - 总数: {}, 当前页记录数: {}, 页码: {}, 页大小: {}", 
                resultPage.getTotal(), 
                resultPage.getRecords().size(),
                resultPage.getCurrent(),
                resultPage.getSize());
                
        return resultPage;
    }


    // 在服务层调用该方法
    @Override
    public List<Map<String, Object>> countIndustryType() {
        return detectionDao.countIndustryType();
    }

    @Override
    public List<Map<String, Object>> countIndustry() {
        return detectionDao.countIndustry();
    }

}

// package com.persee.workstation.uv_inexus.service.parseReport.impl;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
// import com.baomidou.mybatisplus.core.metadata.IPage;
// import com.baomidou.mybatisplus.core.toolkit.StringUtils;
// import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// import com.persee.workstation.uv_inexus.dao.parseReport.DetectionDao;
// import com.persee.workstation.uv_inexus.po.parseReport.Detection;
// import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;

// import lombok.extern.slf4j.Slf4j;



// @Slf4j
// @Service
// public class DetectionServiceImpl implements DetectionService {
//     private static final Map<String, String> STANDARD_TYPE_MAP = new HashMap<>();

//     static {
//         STANDARD_TYPE_MAP.put("GB", "国家标准");
//         STANDARD_TYPE_MAP.put("SN", "进出口行业标准");
//         STANDARD_TYPE_MAP.put("NY", "农业标准");
//         STANDARD_TYPE_MAP.put("SC", "水产标准");
//         STANDARD_TYPE_MAP.put("SB", "商业标准");
//         STANDARD_TYPE_MAP.put("QB", "轻工标准");
//         STANDARD_TYPE_MAP.put("HG", "化工标准");
//         STANDARD_TYPE_MAP.put("DB", "地方标准");
//         STANDARD_TYPE_MAP.put("T", "团体标准");
//         STANDARD_TYPE_MAP.put("Q", "企业标准");
//         STANDARD_TYPE_MAP.put("WS", "卫生标准");
//         STANDARD_TYPE_MAP.put("YC", "烟草标准");
//         STANDARD_TYPE_MAP.put("YY", "医药标准");
//         STANDARD_TYPE_MAP.put("SJ", "电子行业标准");
//         STANDARD_TYPE_MAP.put("FZ", "纺织行业标准");
//         STANDARD_TYPE_MAP.put("HJ", "环境保护标准");
//         STANDARD_TYPE_MAP.put("DL", "电力行业标准");
//         STANDARD_TYPE_MAP.put("JB", "建筑行业标准");
//         STANDARD_TYPE_MAP.put("CB", "船舶行业标准");
//         STANDARD_TYPE_MAP.put("YS", "有色金属行业标准");
//         STANDARD_TYPE_MAP.put("YB", "黑色冶金行业标准");
//         STANDARD_TYPE_MAP.put("SH", "石油化工标准");
//         STANDARD_TYPE_MAP.put("NB", "能源行业标准");
//         STANDARD_TYPE_MAP.put("SY", "石油天然气行业标准");
//         STANDARD_TYPE_MAP.put("CJ", "轻工业标准化技术委员会");
//     }
//     @Autowired
//     private DetectionDao detectionDao;

//     @Override
//     public List<Detection> findAll() {
//         List<Detection> detections = detectionDao.findAll();
//         detections.forEach(this::setStandardType);
//         return detections;
//     }

//     @Override
//     public Detection findById(Integer id) {
//         Detection detection = detectionDao.findById(id);
//         if (detection != null) {
//             setStandardType(detection);
//         }
//         return detection;
//     }

//     @Override
//     public void insert(Detection detection) {
//         detectionDao.insert(detection);
//     }

//     @Override
//     public void update(Detection detection) {
//         detectionDao.update(detection);
//     }

//     @Override
//     public void deleteById(Integer id) {
//         detectionDao.deleteById(id);
//     }

//     @Override
//     public IPage<Detection> findPage(Page<Detection> page, Map<String, String> params) {
//         QueryWrapper<Detection> queryWrapper = new QueryWrapper<>();
        
//         // 动态构建查询条件
//         for (Map.Entry<String, String> entry : params.entrySet()) {
//             String key = entry.getKey();
//             String value = entry.getValue();
            
//             if ("pageNum".equals(key) || "pageSize".equals(key)) {
//                 continue;
//             }
            
//             if (StringUtils.isNotBlank(value)) {
//                 queryWrapper.like(key, value);
//             }
//         }
        
//         // 确保分页稳定，按 ID 排序
//         // queryWrapper.orderByAsc("id");
        
//         // 打印 SQL（调试用）
//         System.out.println("执行的 SQL: " + queryWrapper.getCustomSqlSegment());
        
//         IPage<Detection> resultPage = detectionDao.selectPage(page, queryWrapper);
//         resultPage.getRecords().forEach(this::setStandardType);
//         log.info("分页查询结果 - 总数: {}, 当前页记录数: {}, 页码: {}, 页大小: {}", 
//                 resultPage.getTotal(), 
//                 resultPage.getRecords().size(),
//                 resultPage.getCurrent(),
//                 resultPage.getSize());
                
//         return resultPage;
//     }

//     // 新增方法：设置 standard_type
//     private void setStandardType(Detection detection) {
//         if (detection == null || detection.getStandardIdentity() == null) {
//             detection.setStandardType("其他"); // 如果 standard_identity 为空，设为其他
//             return;
//         }

//         String standardIdentity = detection.getStandardIdentity();
//         String prefix = extractPrefix(standardIdentity);

//         if (prefix == null) {
//             detection.setStandardType("其他");
//         } else {
//             // 从 Map 中查找类型，注意键的大小写
//             String standardType = STANDARD_TYPE_MAP.get(prefix.toUpperCase()); // 假设 Map 的键是全大写
//             if (standardType != null) {
//                 detection.setStandardType(standardType);
//             } else {
//                 detection.setStandardType("其他");
//             }
//         }
//     }

//     // 新增方法：提取 standard_identity 中 / 前面的部分
//     private String extractPrefix(String standardIdentity) {
//         if (standardIdentity == null || standardIdentity.isEmpty()) {
//             return null;
//         }
//         int slashIndex = standardIdentity.indexOf('/');
//         if (slashIndex > 0) { // 确保有 '/' 且前面有内容
//             return standardIdentity.substring(0, slashIndex);
//         } else if (slashIndex == 0) { // 以 '/' 开头，没有前缀
//             return null;
//         } else { // 没有 '/'
//             return standardIdentity; // 或者返回 null，取决于你的业务逻辑
//         }
//     }
// }
