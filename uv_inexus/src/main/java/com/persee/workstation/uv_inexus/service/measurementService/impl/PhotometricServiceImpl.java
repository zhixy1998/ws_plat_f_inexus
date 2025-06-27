package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.userManagePo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
("photometricService")
public class PhotometricServiceImpl implements PhotometricService {
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private UserConfigDAO userConfigDAO;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private TMeasureStandardService tMeasureStandardService;
    @Override
    public Result createTask(CreateTaskParamDto paramDto) {
        // 根据项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(paramDto.getProjectId());
        //根据项目中的用户名称查询用户
        UserConfigPO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getUserName, project.getCreatorKey()));
        // 创建任务类
        TMeasureTask tMeasureTask = new TMeasureTask();
        tMeasureTask.setUserId(userConfigPO.getId());
        tMeasureTask.setTaskName(paramDto.getTaskName());
        tMeasureTask.setCreateTime(LocalDateTime.now());
        tMeasureTask.setMethodKey(paramDto.getMethodId());
        tMeasureTask.setProjectItemKey(project.getId());
        tMeasureTask.setDeviceSerialNumber(paramDto.getDeviceSerialNumber());
        List<String> list = new ArrayList<>();
        // 遍历CreateTaskParamDto中的样品list
        for (SpectrumSpecimenDto specimen : paramDto.getSpecimens()) {
            // 创建样品表
            TMeasureSample sample = new TMeasureSample();
            sample.setSampleNumber(specimen.getSampleNumber());
            sample.setSampleName(specimen.getSampleName());
            sample.setSampleType(specimen.getSampleType());
            sample.setDilutionRatio(specimen.getDilutionRatio());
            sample.setSampleVolume(specimen.getSampleVolume());
            sample.setSampleLocation(specimen.getSampleLocation());
            tMeasureSampleService.save(sample);
            list.add(sample.getId());
        }
        String join = String.join(",", list);
        tMeasureTask.setSampleKey(join);
        boolean save = tMeasureTaskService.save(tMeasureTask);
        return Result.ok(save);
    }

    @Override
    public Result queryTaskPort(QueryTaskDto queryTask) {
        // 判断方法id是all还是id
        QueryWrapper<TMeasureTask> wrapper =  new QueryWrapper<>();
        wrapper.eq("project_item_key", queryTask.getProjectId());
        if (!queryTask.getMethodIds().isEmpty()){
            wrapper.in("method_key", queryTask.getMethodIds());
        }
        if ((queryTask.getStartTime()!=null&&queryTask.getStartTime()!="")&&(queryTask.getEndTime()!=null&&queryTask.getEndTime()!="")){
            wrapper.between("create_time", queryTask.getStartTime(), queryTask.getEndTime());
        }
        Page<TMeasureTask> page = tMeasureTaskService.page(new Page<>(queryTask.getPageNum(), queryTask.getPageSize()), wrapper);
        if (page==null||page.getRecords().size() == 0){
            return Result.fail("未查询到数据");
        }
        page.getRecords().forEach(task -> {
            // 根据任务中的用户id查询用户
            UserConfigPO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getId, task.getUserId()));
            task.setUserId(userConfigPO.getUserName());
        });
        return Result.ok(page);
    }

    @Override
    public Result queryMethodProt(String projectId, String measureType) {
        String type = MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription();
        if (!measureType.equals(type)){
            return Result.fail("请选择光度测量");
        }
        // 根据项目id和测量类型查询方法
        List<TMeasureMethod> list = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", measureType));
        if (list==null||list.size() == 0){
            return Result.fail("未查询到数据");
        }
        return Result.ok(list);
    }

    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter) {
        // 创建测量标准表
        TMeasureStandard tMeasureStandard = new TMeasureStandard();
        tMeasureStandard.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        tMeasureStandard.setTestType("紫外-光谱");
        tMeasureStandardService.save(tMeasureStandard);
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        tMeasureMethod.setStandardKey(tMeasureStandard.getId());
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if ( save){
            return Result.ok("创建成功");
        }else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result saveMethodParameter(PhotometricMethodParameterDto methodDto) {

        return null;
    }


}
