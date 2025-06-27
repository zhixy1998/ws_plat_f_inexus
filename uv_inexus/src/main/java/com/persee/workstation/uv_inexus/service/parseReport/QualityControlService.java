    package com.persee.workstation.uv_inexus.service.parseReport;

    import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;

    public interface QualityControlService {

        List<QualityControl> findAll();

        QualityControl findById(Integer id);

        void insert(QualityControl QualityControl);

        void update(QualityControl QualityControl);

        void deleteById(Integer id);

        IPage<QualityControl> findPage(Page<QualityControl> page, Map<String, String> params);

    }
    