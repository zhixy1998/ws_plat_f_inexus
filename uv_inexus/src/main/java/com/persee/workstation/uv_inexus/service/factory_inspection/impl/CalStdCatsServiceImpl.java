/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.CalArtifactsMapper;
import com.persee.workstation.uv_inexus.dao.factory_inspection.CalStdCatsMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalStdCatsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.MaterialStatisticsDto;
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

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

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
        if (StringUtils.isNotBlank(cats.getCharacteristicValue())) {
            //把特征值和标准值按照中文或者英文分隔，然后把每一项转为double类型，转失败的话就提示错误
            String[] characteristicValue = cats.getCharacteristicValue().split("[,，]");
            if (characteristicValue.length > 0) {
                for (int i = 0; i < characteristicValue.length; i++) {
                    try {
                        characteristicValue[i] = String.valueOf(Double.parseDouble(characteristicValue[i]));
                    } catch (Exception e) {
                        return Result.fail("特征值第" + (i + 1) + "项不是数字，请检查输入格式是否正确");
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(cats.getStandardValue())) {

            String[] standardValue = cats.getStandardValue().split("[,，]");
            if (standardValue.length > 0) {

                for (int i = 0; i < standardValue.length; i++) {
                    try {
                        standardValue[i] = String.valueOf(Double.parseDouble(standardValue[i]));
                    } catch (Exception e) {
                        return Result.fail("标准值第" + (i + 1) + "项不是数字，请检查输入格式是否正确");
                    }
                }
            }
        }
        CalStdCats calStdCats = new CalStdCats();
        BeanUtils.copyProperties(cats, calStdCats);
        if (StringUtil.isBlank(calStdCats.getId())) {
            calStdCats.setId(IdGeneratorUtil.generateIdWithPrefix("cal"));
            calStdCats.setIsDelete(NOT_DELETED.getCode());
            calStdCats.setCreateBy(cats.getUpdateBy());
            LambdaQueryWrapper<CalStdCats> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CalStdCats::getName, calStdCats.getName())
                    .eq(CalStdCats::getIsDelete, NOT_DELETED.getCode());
            long count = this.count(wrapper);
            if (count > 0) {
                throw new RuntimeException("标准物名称已存在");
            }
            int i = calStdCatsMapper.insert(calStdCats);
            return i != 0 ? Result.ok() : Result.fail();
        }
        return updateById(calStdCats) ? Result.ok() : Result.fail();
    }


    @Override
    public Result delCalStdCats(String id) {
        LambdaQueryWrapper<CalArtifacts> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CalArtifacts::getCategoryId, id);

        LambdaQueryWrapper<CalStdCats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalStdCats::getId, id)
                .eq(CalStdCats::getIsDelete, NOT_DELETED.getCode())
                .eq(CalStdCats::getMaterialType, BASE_FIXED_SUBSTANCE.getCode());
        if (0 != calArtifactsMapper.selectCount(queryWrapper)) {
            return Result.fail("请先删除该标准物下的所有标准实体物!!!");
        } else if (count(wrapper) > 0) {
            return Result.fail("该标准物质类型为内置，不可删除!!!");
        } else {
            LambdaUpdateWrapper<CalStdCats> wrapper1 = new LambdaUpdateWrapper<>();
            wrapper1.eq(CalStdCats::getId, id)
                    .notIn(CalStdCats::getMaterialType, BASE_FIXED_SUBSTANCE.getCode())
                    .set(CalStdCats::getIsDelete, DELETED.getCode());
            boolean b = update(wrapper1);
            return b ? Result.ok() : calArtifactsService.delCalArtifacts(id) ? Result.ok() : Result.fail();
        }

//        LambdaUpdateWrapper<CalStdCats> wrapper = new LambdaUpdateWrapper<>();
////        UpdateChainWrapper<CalStdCats> wrapper = new UpdateChainWrapper<>(calStdCatsMapper);
//        wrapper.set(CalStdCats::getIsDelete, DELETED.getCode())
//                .notIn(CalStdCats::getMaterialType, BASE_FIXED_SUBSTANCE.getCode())
//                .eq(CalStdCats::getId, id);
//        boolean update = update(wrapper);
//
    }

    @Override
    public Result<Page<CalStdCats>> listCalStdCats(QueryCalStdCatsDto queryCalStdCatsDto) {
        Page<CalStdCats> page = new Page<>(queryCalStdCatsDto.getPageNum(), queryCalStdCatsDto.getPageSize());
        LambdaQueryWrapper<CalStdCats> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(CalStdCats::getName, queryCalStdCatsDto.getSelectKey())
                .eq(CalStdCats::getIsDelete, NOT_DELETED.getCode())
                .notIn(CalStdCats::getName, BLACK_BLOCK.getDescription(), AIR.getDescription()); //排除水和空气两个标准物类别
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

    @Override
    public Result<List<MaterialStatisticsDto>> getCalStdCatsStats() {
        List<MaterialStatisticsDto> list = calStdCatsMapper.getCalStdCatsStats();
        return Result.ok(list);
    }

}
