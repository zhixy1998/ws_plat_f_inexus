/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalStdCatsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.MaterialStatisticsDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;

import java.util.List;
import java.util.Map;


public interface CalStdCatsService extends IService<CalStdCats> {
    /**
     * 添加标定标准类别
     * @param calStdCats
     * @return
     */
    Result addCalStdCats(CalStdCatsSaveDto calStdCats);

    /**
     * 删除标定标准类别
     * @param id
     * @return
     */
    Result delCalStdCats(String id);

    /**
     * 查询标定标准类别
     * @param queryCalStdCatsDto
     * @return
     */
    Result<Page<CalStdCats>> listCalStdCats(QueryCalStdCatsDto queryCalStdCatsDto);

    /**
     * 获取标定标准类别名称列表
     * @return
     */
    Result<Map> getCalStdCatsNameList();

    /**
     * 获取标定标准类别统计信息
     * @return
     */
    Result<List<MaterialStatisticsDto>> getCalStdCatsStats();
}
