    package com.persee.workstation.uv_inexus.service.guide.parseReport;

    import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.po.guide.parseReport.RoundingRules;

    public interface RoundingRulesService {

        List<RoundingRules> findAll();

        RoundingRules findById(Integer id);

        void insert(RoundingRules RoundingRules);

        void update(RoundingRules RoundingRules);

        void deleteById(Integer id);

        IPage<RoundingRules> findPage(Page<RoundingRules> page, Map<String, String> params);

    }
    