    package com.persee.workstation.uv_inexus.service.guide.parseReport;

    import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;

    public interface StandardPretreatmentService {

        List<StandardPretreatment> findAll();

        StandardPretreatment findById(Integer id);

        void insert(StandardPretreatment standardPretreatment);

        void update(StandardPretreatment standardPretreatment);

        void deleteById(Integer id);

        IPage<StandardPretreatment> findPage(Page<StandardPretreatment> page, Map<String, String> params);

    }
    