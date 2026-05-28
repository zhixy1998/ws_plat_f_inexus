/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureSampleHeadMapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.SampleHeadTemplateDTO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSampleHead;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureSampleHeadService;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleTemplateVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service("FMeasureSampleHeadService")
@Slf4j
@DS("concise")
public class FMeasureSampleHeadServiceImpl
        extends ServiceImpl<FMeasureSampleHeadMapper, FMeasureSampleHead> 
        implements FMeasureSampleHeadService{

    @Autowired
    FMeasureSampleHeadMapper dao;

    @Override
    public R<Boolean> createTemplate(SampleHeadTemplateDTO templateDto) {
        if (templateDto.getTemplateHeads() == null || templateDto.getTemplateHeads().isEmpty()) {
            //没有可保存为模板的信息
            return R.fail("00307001");
        }
        if (!StringUtils.hasText(templateDto.getTemplateName())) {
            //模板名称不能为空
            return R.fail("00307002");
        }
        if (!StringUtils.hasText(templateDto.getMeasureType())) {
            //测量类型不能为空
            return R.fail("00307003");
        }

        if (!StringUtils.hasText(templateDto.getUpdateBy())){
            //请登录后操作！
            return R.fail("00307004");
        }
        String jsonString = JSON.toJSONString(templateDto.getTemplateHeads());
        LambdaQueryWrapper<FMeasureSampleHead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureSampleHead::getHeads, jsonString).eq(FMeasureSampleHead::getMeasureType, templateDto.getMeasureType());
        List<FMeasureSampleHead> check = dao.selectList(queryWrapper);
        if (!check.isEmpty()) {
            FMeasureSampleHead templatePO = check.get(0);
            //该模板已存在,模板名称为：+templatePO.getTemplateName()
            return R.fail("00307005", templatePO.getTemplateName());
        }
        FMeasureSampleHead templatePO = new FMeasureSampleHead();
        BeanUtils.copyProperties(templateDto, templatePO);
        templatePO.setHeads(jsonString);
        templatePO.setCreateBy(templateDto.getUpdateBy());
        boolean save = dao.insert(templatePO) > 0;
        if (save) {
            return R.ok(true);
        } else {
            //保存失败
            return R.fail("00300008");
        }
    }

    @Override
    public R<List<SampleTemplateVO>> queryTemplate(String measureType, String search) {

        LambdaQueryWrapper<FMeasureSampleHead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureSampleHead::getMeasureType, measureType);

        if (StringUtils.hasText(search)) {
            queryWrapper.like(FMeasureSampleHead::getTemplateName, search)
                    .or().like(FMeasureSampleHead::getTemplateDesc, search)
                    .or().like(FMeasureSampleHead::getHeads, search);
        }
        List<FMeasureSampleHead> poList = dao.selectList(queryWrapper);
        List<SampleTemplateVO> voList = new ArrayList<>();
        for (FMeasureSampleHead templatePO : poList) {
            SampleTemplateVO vo = new SampleTemplateVO();
            BeanUtils.copyProperties(templatePO,vo);
            vo.setTemplateHeads(JSON.parseArray(templatePO.getHeads(), HeadVO.class));
            voList.add(vo);
        }

        return R.ok(voList);
    }

    @Override
    public R<Boolean> deleteTemplate(String templateId, String updateBy) {
        if (!StringUtils.hasText(templateId)) {
            //没有要删除的模板
            return R.fail("00307006");
        }

        FMeasureSampleHead templatePO = dao.selectById(templateId);
        if (!templatePO.getUpdateBy().equals(updateBy)) {
            //不可删除他人创建的模板
            return R.fail("00307007");
        }
        int result = dao.deleteById(templateId);
        if (result > 0) {
            return R.ok(true);
        } else {
            //删除失败
            return R.fail("00100008");
        }
    }
}
