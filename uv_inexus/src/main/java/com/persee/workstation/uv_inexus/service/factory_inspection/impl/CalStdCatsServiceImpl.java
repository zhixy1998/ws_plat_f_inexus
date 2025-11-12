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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.CalArtifactsMapper;
import com.persee.workstation.uv_inexus.dao.factory_inspection.CalStdCatsMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalStdCatsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalArtifacts;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalArtifactsService;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalStdCatsService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.DeleteStatusEnum.DELETED;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.DeleteStatusEnum.NOT_DELETED;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.StandardMaterialCategory.NON_STANDARD_EXTERNAL_REFERENCE;

@Service
public class CalStdCatsServiceImpl extends ServiceImpl<CalStdCatsMapper, CalStdCats>
        implements CalStdCatsService {
    @Autowired
    private CalStdCatsMapper calStdCatsMapper;
    @Autowired
    private CalArtifactsMapper calArtifactsMapper;

    @Autowired
    private CalArtifactsService calArtifactsService;

    @Override
    public Result addCalStdCats(CalStdCatsSaveDto cats) {
        CalStdCats calStdCats = new CalStdCats();
        BeanUtils.copyProperties(cats, calStdCats);
        if (StringUtil.isBlank(calStdCats.getId())) {
            calStdCats.setId(IdGeneratorUtil.generateIdWithPrefix("cal"));
            calStdCats.setIsDelete(NOT_DELETED.getCode());
            int i = calStdCatsMapper.insert(calStdCats);
            return i != 0 ? Result.ok() : Result.fail();
        }
        return updateById(calStdCats) ? Result.ok() : Result.fail();
    }


    @Override
    public Result delCalStdCats(String id) {
        LambdaQueryWrapper<CalArtifacts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CalArtifacts::getCategoryId, id);
        if (0 != calArtifactsMapper.selectCount(queryWrapper)) {
            return Result.fail("请先删除该标准物下的所有标准实体物!!!");
        }

        LambdaUpdateWrapper<CalStdCats> wrapper = new LambdaUpdateWrapper<>();
//        UpdateChainWrapper<CalStdCats> wrapper = new UpdateChainWrapper<>(calStdCatsMapper);
        wrapper.set(CalStdCats::getIsDelete, DELETED.getCode())
                .eq(CalStdCats::getId, id);
        boolean update = update(wrapper);
        return update ? Result.ok() : calArtifactsService.delCalArtifacts(id) ? Result.ok() : Result.fail();
    }

    @Override
    public Result<Page<CalStdCats>> listCalStdCats(QueryCalStdCatsDto queryCalStdCatsDto) {
        Page<CalStdCats> page = new Page<>(queryCalStdCatsDto.getPageNum(), queryCalStdCatsDto.getPageSize());
        LambdaQueryWrapper<CalStdCats> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(CalStdCats::getName, queryCalStdCatsDto.getSelectKey())
                .eq(CalStdCats::getIsDelete, NOT_DELETED.getCode())
                .notIn(CalStdCats::getMaterialType, NON_STANDARD_EXTERNAL_REFERENCE.getCode());
        Page<CalStdCats> calStdCatsPage = calStdCatsMapper.selectPage(page, wrapper);
        if (!ObjectUtils.isEmpty(calStdCatsPage) || !CollectionUtils.isEmpty(calStdCatsPage.getRecords())) {
            calStdCatsPage.getRecords().forEach(c -> {
                List<CalArtifacts> calArtifacts = calArtifactsMapper.selectList(new LambdaQueryWrapper<CalArtifacts>().eq(CalArtifacts::getCategoryId, c.getId()).eq(CalArtifacts::getIsDelete, NOT_DELETED.getCode()));
                if (!CollectionUtils.isEmpty(calArtifacts)) {
                    calArtifacts.forEach(a -> {
                        a.setCategoryName(c.getName());
                    });
                    c.setChildren(calArtifacts);
                }
            });
        }
        return Result.ok(calStdCatsPage);
    }

    public Result<Map> getCalStdCatsNameList() {
        List<CalStdCats> calStdCats = calStdCatsMapper.selectList(null);
        HashMap<String, String> map = new HashMap<>();
        if (!CollectionUtils.isEmpty(calStdCats)) {
            for (CalStdCats cat : calStdCats) {
                map.put(cat.getId(), cat.getName());
            }
        }
        return Result.ok(Collections.singletonMap("calStdCats", map));
    }

}
