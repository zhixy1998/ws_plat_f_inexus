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

package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalStdCatsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;

import java.util.Map;


public interface CalStdCatsService extends IService<CalStdCats> {
    Result addCalStdCats(CalStdCatsSaveDto calStdCats);

    Result delCalStdCats(String id);

    Result<Page<CalStdCats>> listCalStdCats(QueryCalStdCatsDto queryCalStdCatsDto);

    Result<Map> getCalStdCatsNameList();
}
