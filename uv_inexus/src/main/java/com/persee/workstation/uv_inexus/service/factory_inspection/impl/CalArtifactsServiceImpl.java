/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.CalArtifactsMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.BatchDelDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalArtifactsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.StandardSubstanceDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalArtifacts;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalArtifactsService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.DELETED;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.NOT_DELETED;

@Service
public class CalArtifactsServiceImpl extends ServiceImpl<CalArtifactsMapper, CalArtifacts>
        implements CalArtifactsService {
    @Autowired
    private CalArtifactsMapper calArtifactsMapper;


    @Override
    public Result addCalArtifacts(CalArtifactsSaveDto artifacts) {
        //把特征值和标准值按照中文或者英文分隔，然后把每一项转为double类型，转失败的话就提示错误
        String[] characteristicValue = artifacts.getCharacteristicValue().split("[,，]");
        String[] standardValue = artifacts.getStandardValue().split("[,，]");
        if (characteristicValue.length > 0) {
            for (int i = 0; i < characteristicValue.length; i++) {
                try {
                    characteristicValue[i] = String.valueOf(Double.parseDouble(characteristicValue[i]));
                } catch (Exception e) {
                    return Result.fail("特征值第" + (i + 1) + "项不是数字，请检查输入格式是否正确");
                }
            }
        }
        if (standardValue.length > 0) {

            for (int i = 0; i < standardValue.length; i++) {
                try {
                    standardValue[i] = String.valueOf(Double.parseDouble(standardValue[i]));
                } catch (Exception e) {
                    return Result.fail("标准值第" + (i + 1) + "项不是数字，请检查输入格式是否正确");
                }
            }
        }
        CalArtifacts calArtifacts = new CalArtifacts();
        BeanUtils.copyProperties(artifacts, calArtifacts);
        if (StringUtil.isBlank(calArtifacts.getId())) {
            calArtifacts.setId(IdGeneratorUtil.generateIdWithPrefix("mat"));
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
        return update(wrapper1);
    }

    // Service层代码
    @Override
    public Result<IPage<StandardSubstanceDto>> listCalArtifacts(QueryCalStdCatsDto dto) {
        try {


            // MyBatis-Plus会自动将pageNum和pageSize转换为offset和size
            Page<StandardSubstanceDto> page = new Page<>(dto.getPageNum(), dto.getPageSize());
            IPage<StandardSubstanceDto> result = calArtifactsMapper.selectCustomDtoPage(page, dto);
            return Result.ok(result);
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    @Override
    public Result<String> batchDelCalStdCats(BatchDelDto ids) {
        try {
            List<String> list = ids.getIds();
            if (CollectionUtils.isEmpty(list)) {
                return Result.warn("请选择要删除的记录");
            }
            boolean b = this.removeByIds(list);
            if (b) {
                return Result.ok("删除成功");
            }else {
                return Result.warn("删除失败");
            }
        } catch (Exception e) {
            log.error("删除失败", e);
        }
        return null;
    }


}
