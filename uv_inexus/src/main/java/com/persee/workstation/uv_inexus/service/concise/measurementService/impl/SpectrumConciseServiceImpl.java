package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 简洁模式: 光谱扫描服务业务层实现
 */

@Service("SpectrumConciseService")
public class SpectrumConciseServiceImpl implements SpectrumConciseService {

    @Autowired
    private FMeasureMethodService fMeasureMethodService;

    /**
     * 查询最近方法数据
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryNearestMethodData(String updateBy,String measureType) {
        if (updateBy == null){
            return Result.fail("更新人不能为空");
        }

        if (measureType == null){
            return Result.fail("方法类型不能为空");
        }

        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator",updateBy);
        wrapper.eq("is_draft","0");
        wrapper.eq("measurement_type",measureType);
        wrapper.orderByDesc("method_create_time");
        wrapper.last("LIMIT 1");

        FMeasureMethod method = fMeasureMethodService.getById(wrapper);

        if (method==null){
            return Result.ok(new ArrayList<>());
        }
        return Result.ok(method);
    }

    /**
     * 查找方法按钮
     * @param updateBy
     * @param measureType
     * @param methodName
     * @return
     */
    @Override
    public Result searchMethodButton(Integer peageNum,Integer pageSize,String updateBy, String measureType, String methodName) {
        // 对传入的参数进行校验
        if (updateBy == null){
            return Result.fail("用户不能为空");
        }
        if (measureType == null){
            return Result.fail("方法类型不能为空");
        }
        // 设置查询条件
        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator",updateBy);
        wrapper.eq("measurement_type",measureType);
        if (methodName != null){
            wrapper.like("method_name",methodName);
        }
        // 分页查询
        Page<FMeasureMethod> page = fMeasureMethodService.page(new Page<>(peageNum, pageSize), wrapper);
        return Result.ok(page);
    }
}
