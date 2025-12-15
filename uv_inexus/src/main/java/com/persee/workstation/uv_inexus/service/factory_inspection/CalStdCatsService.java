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
    Result addCalStdCats(CalStdCatsSaveDto calStdCats);

    Result delCalStdCats(String id);

    Result<Page<CalStdCats>> listCalStdCats(QueryCalStdCatsDto queryCalStdCatsDto);

    Result<Map> getCalStdCatsNameList();

    Result<List<MaterialStatisticsDto>> getCalStdCatsStats();
}
