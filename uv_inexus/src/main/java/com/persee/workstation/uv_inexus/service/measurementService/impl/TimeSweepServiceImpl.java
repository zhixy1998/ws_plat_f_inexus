package com.persee.workstation.uv_inexus.service.measurementService.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddSampleDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamsDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureProjectService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureSampleService;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.service.measurementService.TimeSweepService;
import com.persee.workstation.uv_inexus.vo.measurementVo.Sample;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;

@Service
("timeSweepService")
public class TimeSweepServiceImpl implements TimeSweepService {

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    
    @Autowired
    private TMeasureTaskService tMeasureTaskService;

    @Autowired
    private TMeasureSampleService tMeasureSampleService;

    @Override
    public Result updateMethodParameter(MethodParamsDto methodDto) {
        return null;
    }

    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, TimeSweepParamter timeSweepParamter) {
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(timeSweepParamter));
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if ( save){
            return Result.ok("创建成功");
        }else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result queryProjectTaskMethod(String updateBy) {
        // 根据用户名查询所有项目
        List<TMeasureProject> projectList = tMeasureProjectService.list(Wrappers.lambdaQuery(TMeasureProject.class).eq(TMeasureProject::getCreatorKey, updateBy));
        // 查询所有项目下最近创建的方法
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>()
                .in("project_item_key", projectList.stream().map(TMeasureProject::getId).collect(Collectors.toList()))
                .eq("measurement_type","时间扫描")
                .orderByDesc("method_create_time")
                .last("LIMIT 1"));
        // 根据方法中的项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
        // 将测量数据中的json转换成对象存入返回值中
        SpectrumBasicParameterVo spectrumBasicParameterVo = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            spectrumBasicParameterVo = objectMapper.readValue(method.getMeasurementParamInfo(), SpectrumBasicParameterVo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // 将项目,方法名称存入返回对象中
        spectrumBasicParameterVo.setMethodId(method.getId());
        spectrumBasicParameterVo.setTaskName(method.getMethodName());
        spectrumBasicParameterVo.setTaskId(method.getId());
        spectrumBasicParameterVo.setProjectId(project.getId());
        spectrumBasicParameterVo.setProjectName(project.getProjectName());
        spectrumBasicParameterVo.setMethodName(method.getMethodName());
        return Result.ok(spectrumBasicParameterVo);
    }

     /**
     * 查询方法的参数
     * @param methodId
     * @return
     */
    @Override
    public Result queryMethodParameter(String methodId) {
        // 根据方法id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        // 将标准表中的json转换成对象存入返回值中
        SpectrumBasicParameterVo spectrumBasicParameterVo = null;
        try {
            spectrumBasicParameterVo = new ObjectMapper().readValue(method.getMeasurementParamInfo(), SpectrumBasicParameterVo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        spectrumBasicParameterVo.setMethodName(method.getMethodName());
        return Result.ok(spectrumBasicParameterVo);
    }
    

    /**
     * 新增样本接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addSamplePort(AddSampleDto paramDto) {
        // 创建一个String数组用于存放新增后的样品id
        List<String> sampleIdList = new ArrayList<>();
        // 根据传过来的参数进行遍历添加样品
        for (Sample sample : paramDto.getSamples()) {
            TMeasureSample sampleParam = new TMeasureSample();
            sampleParam.setSampleName(sample.getSampleName());
            sampleParam.setSampleNumber(sample.getSampleNumber());
            sampleParam.setSampleLocation(sample.getSampleLocation());
            tMeasureSampleService.save(sampleParam);
            sampleIdList.add(sampleParam.getId());
        }
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }
        if (task.getSampleKey() != null){
            // 将原有在任务中的样品字段提取出来
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            // 遍历样品id数组,将新增的样品id添加到任务中的样品字段中
            for (String sampleId : sampleIdList) {
                TaskSpecimen taskSpeciman = new TaskSpecimen();
                taskSpeciman.setSpecimenKey(sampleId);
                taskSpeciman.setDeleteType("0");
                taskSpecimen.add(taskSpeciman);
            }
            // 将修改后的样品字段存入任务数据中
            task.setSampleKey(JSON.toJSONString(taskSpecimen));
        }else if (task.getSampleKey() == null) {
            // 如果任务中的样品字段为空,则直接将样品id存入任务数据中
            List<TaskSpecimen> taskSpecimen = new ArrayList<>();
            for (String sampleId : sampleIdList) {
                TaskSpecimen taskSpeciman = new TaskSpecimen();
                taskSpeciman.setSpecimenKey(sampleId);
                taskSpeciman.setDeleteType("0");
                taskSpecimen.add(taskSpeciman);
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimen));
        }
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }


    /**
     * 删除样品接口
     * @param sampleId
     * @param taskId
     * @return
     */

    @Override
    public Result deleteSamplePort(String sampleId, String taskId) {
        // 获取任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : taskSpecimenList) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setDeleteType("1");
                break;
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimenList));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }

}
