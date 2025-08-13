package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.measurementDao.SampleHeadTemplateDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.SampleHeadTemplateDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.measurementPo.SampleHeadTemplatePO;
import com.persee.workstation.uv_inexus.service.measurementService.SampleHeadTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class SampleHeadTemplateServiceImpl implements SampleHeadTemplateService {

    @Autowired
    SampleHeadTemplateDAO dao;

    @Override
    public R<Boolean> createTemplate(SampleHeadTemplateDTO templateDto) {
        if (templateDto.getTemplateHeads() == null || templateDto.getTemplateHeads().isEmpty()) {
            return R.fail("没有可保存为模板的信息");
        }
        if (!StringUtils.hasText(templateDto.getTemplateName())) {
            return R.fail("模板名称不能为空");
        }
        if (!StringUtils.hasText(templateDto.getMeasureType())) {
            return R.fail("测量类型不能为空");
        }

        if (!StringUtils.hasText(templateDto.getUpdateBy())){
            return R.fail("请登录后操作！");
        }
        String jsonString = JSON.toJSONString(templateDto.getTemplateHeads());
        LambdaQueryWrapper<SampleHeadTemplatePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SampleHeadTemplatePO::getHeads, jsonString).eq(SampleHeadTemplatePO::getMeasureType, templateDto.getMeasureType());
        List<SampleHeadTemplatePO> check = dao.selectList(queryWrapper);
        if (!check.isEmpty()) {
            SampleHeadTemplatePO templatePO = check.get(0);
            return R.fail("该模板已存在,模板名称为：" + templatePO.getTemplateName());
        }
        SampleHeadTemplatePO templatePO = new SampleHeadTemplatePO();
        BeanUtils.copyProperties(templateDto, templatePO);
        templatePO.setHeads(jsonString);
        templatePO.setCreateBy(templateDto.getUpdateBy());
        boolean save = dao.insert(templatePO) > 0;
        if (save) {
            return R.ok(true);
        } else {
            return R.fail("保存失败");
        }
    }

    @Override
    public R<List<SampleHeadTemplatePO>> queryTemplate(String measureType, String search) {

        LambdaQueryWrapper<SampleHeadTemplatePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SampleHeadTemplatePO::getMeasureType, measureType);

        if (StringUtils.hasText(search)) {
            queryWrapper.like(SampleHeadTemplatePO::getTemplateName, search)
                    .or().like(SampleHeadTemplatePO::getTemplateDesc, search)
                    .or().like(SampleHeadTemplatePO::getHeads, search);
        }
        List<SampleHeadTemplatePO> poList = dao.selectList(queryWrapper);
        return R.ok(poList);
    }

    @Override
    public R<Boolean> deleteTemplate(String templateId, String updateBy) {
        if (!StringUtils.hasText(templateId)) {
            return R.fail("没有要删除的模板");
        }

        SampleHeadTemplatePO templatePO = dao.selectById(templateId);
        if (!templatePO.getUpdateBy().equals(updateBy)) {
            return R.fail("不可删除他人创建的模板");
        }
        int result = dao.deleteById(templateId);
        if (result > 0) {
            return R.ok(true);
        } else {
            return R.fail("删除失败");
        }
    }
}
