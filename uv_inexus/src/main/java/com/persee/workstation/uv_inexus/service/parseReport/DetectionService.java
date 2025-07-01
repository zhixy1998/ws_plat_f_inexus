package com.persee.workstation.uv_inexus.service.parseReport;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;

    public interface DetectionService {

        List<Detection> findAll();

        Detection findById(Integer id);

        void insert(Detection detection);

        void update(Detection detection);

        void deleteById(Integer id);
        
        IPage<Detection> findPage(Page<Detection> page, Map<String, String> params);

    }
    