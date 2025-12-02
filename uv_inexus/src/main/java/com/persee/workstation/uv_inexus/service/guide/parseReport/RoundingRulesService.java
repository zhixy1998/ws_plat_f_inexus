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
import com.persee.workstation.uv_inexus.po.guide.parseReport.RoundingRules;

    public interface RoundingRulesService {

        List<RoundingRules> findAll();

        RoundingRules findById(Integer id);

        void insert(RoundingRules RoundingRules);

        void update(RoundingRules RoundingRules);

        void deleteById(Integer id);

        IPage<RoundingRules> findPage(Page<RoundingRules> page, Map<String, String> params);

    }
    