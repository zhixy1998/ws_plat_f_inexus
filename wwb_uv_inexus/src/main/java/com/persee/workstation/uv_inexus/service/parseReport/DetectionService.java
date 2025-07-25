package com.persee.workstation.uv_inexus.service.parseReport;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionDetailDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionParametersDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.MultiTableDataDTO;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;

public interface DetectionService {

    List<Detection> findAll();

    Detection findById(Integer id);

    void insert(Detection detection);

    void update(Detection detection);

    void deleteById(Integer id);
    
    IPage<Detection> findPage(Page<Detection> page, Map<String, String> params);

    List<Map<String, Object>> countIndustryType();

    List<Map<String, Object>> countIndustry();
    
    int deleteBatchIds(List<Integer> ids);

    // 新增：去重分页查询
    IPage<DetectionListDTO> findDistinctPage(Page<DetectionListDTO> page, Map<String, String> params);

    DetectionDetailDTO getDetailById(Integer id);

    // 新增：查询参数及子表字段
    DetectionParametersDTO getParametersById(Integer id);

    /**
     * 查询主表及多子表的多条数据
     */
    MultiTableDataDTO getMultiTablesData(Integer id);
}