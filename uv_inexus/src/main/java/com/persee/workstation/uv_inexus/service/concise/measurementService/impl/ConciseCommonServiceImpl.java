package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
("ConciseCommonService")
public class ConciseCommonServiceImpl implements ConciseCommonService {

    @Autowired
    private SpectrumConciseService spectrumConciseService;

    @Autowired
    private FMeasureMethodService fMeasureMethodService;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;


    /**
     * 查询默认参数
     * @param measureType
     * @return
     */
    @Override
    public Result queryDefaultParameters(String measureType) {
        // 根据测量类型以及统一的"默认方法"的方法名称进行查询默认方法参数
        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_name", "默认方法");
        wrapper.eq("measurement_type", measureType);
        FMeasureMethod one = fMeasureMethodService.getOne(wrapper);
        if (one == null) {
            return Result.fail(measureType+"没有默认方法");
        }
        return Result.ok(one);

    }

    /**
     * 查找方法按钮
     * @param pageNum
     * @param pageSize
     * @param updateBy
     * @param measureType
     * @param methodName
     * @return
     */
    @Override
    public Result searchMethodButton(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName) {
        // 对传入的参数进行校验
        if (updateBy == null) {
            return Result.fail("用户不能为空");
        }
        if (measureType == null) {
            return Result.fail("方法类型不能为空");
        }
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        // 设置查询条件
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", updateBy);
        wrapper.eq("measurement_type", measureType);
        if (methodName != null) {
            wrapper.like("method_name", methodName);
        }
        // 分页查询
        Page<TMeasureMethod> page = tMeasureMethodService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(page);
    }
}
