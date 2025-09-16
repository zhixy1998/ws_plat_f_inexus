package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FMeasureMethodDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FMeasureMethodParam;
import com.persee.workstation.uv_inexus.dto.concise.common.QueryMethodDto;
import com.persee.workstation.uv_inexus.dto.concise.common.SelectTaskDto;

import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
        ("ConciseCommonService")
public class ConciseCommonServiceImpl implements ConciseCommonService {

    @Autowired
    private SpectrumConciseService spectrumConciseService;
    @Autowired
    private FMeasureMethodService fMeasureMethodService;
    @Autowired
    private SysUserDAO dao;
    @Autowired
    private FMeasureTaskService fMeasureTaskService;

    @Override
    public Result applyMethod(FUseMethodDto paramDto) {
        // 根据测量类型进行区分
        switch (paramDto.getMeasureType()) {
            case "光度测量":
                return Result.ok();
            case "定量测量":
                return Result.ok();
            case "光谱扫描":
                return spectrumConciseService.use(paramDto);
            case "时间扫描":
                return Result.ok();
            default:
                return Result.fail("请选择正确的光度模式");
        }
    }

    @Override
    public Result<Page<FMeasureMethodDto>> queryHistoryMethod(QueryMethodDto queryMethodDto) {
        String methodType = queryMethodDto.getMethodType();
        Integer pageSize = queryMethodDto.getPageSize();
        Integer pageNum = queryMethodDto.getPageNum();
        String userName = queryMethodDto.getUpdateBy();
        String selectKey = queryMethodDto.getSelectKey();
        Page<FMeasureMethod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FMeasureMethod> methodLambdaQueryWrapper = new LambdaQueryWrapper<>();
        methodLambdaQueryWrapper.eq(FMeasureMethod::getMeasurementType, methodType)
                .eq(FMeasureMethod::getMethodCreator, userName);
        if (selectKey != null) {
            methodLambdaQueryWrapper.like(FMeasureMethod::getMethodName, selectKey);
        }
        Page<FMeasureMethod> fMeasureMethodPage = fMeasureMethodService.page(page, methodLambdaQueryWrapper);
        Page<FMeasureMethodDto> dtoPage = new Page<>();
        dtoPage.setCurrent(fMeasureMethodPage.getCurrent());
        dtoPage.setSize(fMeasureMethodPage.getSize());
        dtoPage.setTotal(fMeasureMethodPage.getTotal());
        dtoPage.setPages(fMeasureMethodPage.getPages());


        List<FMeasureMethodDto> dtoList = new ArrayList<>();
        fMeasureMethodPage.getRecords().forEach(fMeasureMethod -> {
            FMeasureMethodDto fMeasureMethodDto = new FMeasureMethodDto();
            BeanUtils.copyProperties(fMeasureMethod, fMeasureMethodDto);
            dtoList.add(fMeasureMethodDto);
        });

        dtoPage.setRecords(dtoList);

        return Result.ok(dtoPage);


    }

    @Override
    public Result<FMeasureMethodParam> detailInformation(String methodName, String methodType) {
        LambdaQueryWrapper<FMeasureMethod> fMeasureMethodLambdaQueryWrapper = new LambdaQueryWrapper<>();
      fMeasureMethodLambdaQueryWrapper.eq(FMeasureMethod::getMethodName, methodName).eq(FMeasureMethod::getMeasurementType,methodType);
        FMeasureMethod fMeasureMethod = fMeasureMethodService.getOne(fMeasureMethodLambdaQueryWrapper);
        String measurementParamInfo = fMeasureMethod.getMeasurementParamInfo();
        JSONObject jsonObject = JSONObject.parseObject(measurementParamInfo);
        FMeasureMethodParam fMeasureMethodParam = new FMeasureMethodParam();
        BeanUtils.copyProperties(fMeasureMethod, fMeasureMethodParam);
        fMeasureMethodParam.setMeasurementParamInfo(jsonObject);
        return Result.ok(fMeasureMethodParam);


    }

    @Override
    public Result<Page<FMeasureTask>> queryHistoryTask(SelectTaskDto selectTaskDto) {
        String selectKey = selectTaskDto.getSelectKey();
        String methodType = selectTaskDto.getMethodType();
        String userName = selectTaskDto.getUpdateBy();
        Integer pageNum = selectTaskDto.getPageNum();
        Integer pageSize = selectTaskDto.getPageSize();
        SysUserPO sysUserPO = dao.selectByUsername(userName);
        String userId = sysUserPO.getId();
        LambdaQueryWrapper<FMeasureTask> fMeasureTaskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        fMeasureTaskLambdaQueryWrapper.eq(FMeasureTask::getMeasureType, methodType)
                .eq(FMeasureTask::getUserId,userId);
        if (selectKey != null) {
            fMeasureTaskLambdaQueryWrapper.like(FMeasureTask::getTaskName, selectKey);
        }
        Page<FMeasureTask> page = new Page<>(pageNum,pageSize);

        Page<FMeasureTask> fMeasureTaskPage = fMeasureTaskService.page(page, fMeasureTaskLambdaQueryWrapper);
        return Result.ok(fMeasureTaskPage);
    }


}
