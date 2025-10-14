    /*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

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
    