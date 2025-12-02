/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.parseReport;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionDetailDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionParametersDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.MultiTableDataDTO;
import com.persee.workstation.uv_inexus.po.guide.parseReport.Detection;

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