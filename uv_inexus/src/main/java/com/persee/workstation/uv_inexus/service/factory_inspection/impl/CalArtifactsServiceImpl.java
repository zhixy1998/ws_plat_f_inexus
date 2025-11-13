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

package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.CalArtifactsMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalArtifactsSaveDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalArtifacts;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalArtifactsService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.DeleteStatusEnum.DELETED;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.DeleteStatusEnum.NOT_DELETED;

@Service
public class CalArtifactsServiceImpl extends ServiceImpl<CalArtifactsMapper, CalArtifacts>
        implements CalArtifactsService {
    @Autowired
    private CalArtifactsMapper calArtifactsMapper;


    @Override
    public Result addCalArtifacts(CalArtifactsSaveDto artifacts ) {
        CalArtifacts calArtifacts = new CalArtifacts();
        BeanUtils.copyProperties(artifacts, calArtifacts);
        if (StringUtil.isBlank(calArtifacts.getId())) {
            calArtifacts.setId(IdGeneratorUtil.generateIdWithPrefix("cal"));
            calArtifacts.setIsDelete(NOT_DELETED.getCode());
            int i = calArtifactsMapper.insert(calArtifacts);
            return i != 0 ? Result.ok() : Result.fail();
        }
        return calArtifactsMapper.updateById(calArtifacts) != 0 ? Result.ok() : Result.fail();
    }

    @Override
    public boolean delCalArtifacts(String id) {
        LambdaUpdateWrapper<CalArtifacts> wrapper1 = new LambdaUpdateWrapper<>();
        wrapper1.set(CalArtifacts::getIsDelete, DELETED.getCode())
                .eq(CalArtifacts::getId, id);
        return update(wrapper1) ;
    }
}
