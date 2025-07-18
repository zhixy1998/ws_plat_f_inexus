package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddSampleDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumSpecimenDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
("timeSweepService")
public class TimeSweepServiceImpl implements TimeSweepService {

    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private TMeasureDataService tMeasureDataService;
    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;

    /**
     * 创建方法接口
     * @param methodDto
     * @param updateBy
     * @param timeSweepParamter
     * @return
     */
    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, TimeSweepParamter timeSweepParamter) {
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
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

    /**
     * 查询项目下所有方法
     * @param projectId
     * @param measureForm
     * @return
     */
    @Override
    public Result queryProjectAllMethods(String projectId,String measureForm) {
        List<TMeasureMethod> methods = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type",  measureForm));
        if (methods==null){
            return Result.fail("没有此类型的数据");
        }
        return Result.ok(methods);
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
        for (SpectrumSpecimenDto sample : paramDto.getSamples()) {
            TMeasureSample sampleParam = new TMeasureSample();
            sampleParam.setSampleName(sample.getSampleName());
            sampleParam.setSampleNumber(sample.getSampleNumber());
            sampleParam.setSampleLocation(sample.getSampleLocation());
            sampleParam.setColor("#000000");
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
                taskSpeciman.setIsTested("0");
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
     * 样品列表
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result sampleListPort(String taskId,String type) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if (type.equals("")&&taskSpeciman.getDeleteType().equals("1")) continue;
            SampleListVo vo = new SampleListVo();
            TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
            QueryWrapper<TMeasureData> eq = new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", byId.getId());
            if (type.equals("")){
                eq.eq("is_delete", "0");
            }
            // 根据样品id和任务id两个条件去查询测量数据
            TMeasureData measureData = tMeasureDataService.getOne(eq);
            vo.setTitle(byId.getSampleName());
            if (measureData== null){
                vo.setData(null);
                vo.setKey(null);
            }else {
                vo.setKey(measureData.getId());
                List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
                List<Double> gauges = JSON.parseArray(measureData.getGaugePoint(), Double.class);
                List<Double> peaks = JSON.parseArray(measureData.getPeak(), Double.class);
                List<Double> grades = JSON.parseArray(measureData.getGrain(), Double.class);
                List<Map<String, Object>> resultList = new ArrayList<>();
                for (String s : list) {
                    String[] split = s.split(",");
                    Map<String, Object> map = new HashMap<>();
                    map.put("x", split[0]);
                    map.put("y", split[1]);
                    if (gauges!=null){
                        for (Double gauge : gauges) {
                            if (split[0].equals(String.valueOf(gauge))){
                                map.put("marked","1");
                                break;
                            }else{
                                map.put("marked","0");
                            }
                        }
                    }else{
                        map.put("marked","0");
                    }
                    if (peaks!=null){
                        for (Double peak : peaks) {
                            if (split[0].equals(String.valueOf(peak))){
                                map.put("feng","1");
                                break;
                            }else{
                                map.put("feng","0");
                            }
                        }
                    }else{
                        map.put("feng","0");
                    }
                    if (grades!=null){
                        for (Double grade : grades){
                            if (split[0].equals(String.valueOf(grade))){
                                map.put("gu","1");
                                break;
                            }else{
                                map.put("gu","0");
                            }
                        }
                    }else{
                        map.put("gu","0");
                    }
                    resultList.add(map);
                }
                vo.setData(resultList);
            }
            vo.setColor(byId.getColor());
            vo.setDeleteType(taskSpeciman.getDeleteType());
            vo.setType("0");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    /**
     * 删除样品列表中的一个测量数据
     * @param measureId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String measureId) {
        // 根据measureId查询测量数据
        TMeasureData  byId = tMeasureDataService.getById(measureId);
        if (byId == null) {
            return Result.fail("测量数据不存在");
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = tMeasureDataService.updateById(byId);
        if (update){
            return Result.ok(true);
        }else {
            return Result.fail(false);
        }
    }

    /**
     * 变换列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result changeTheListPort(String taskId,String type) {
        // 根据任务id去查询分析数据
        QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        if(type==null||type.equals("")){
            queryWrapper.eq("is_delete", "0");
        }
        List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(queryWrapper);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TMeasureAnalysisData tMeasureAnalysisData : list) {
            SampleListVo vo = new SampleListVo();
            vo.setKey(tMeasureAnalysisData.getId());
            vo.setTitle(tMeasureAnalysisData.getAnalysisName());
            vo.setColor(tMeasureAnalysisData.getColor());
            vo.setDeleteType(tMeasureAnalysisData.getIsDelete());
            List<String> results = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
            List<Double> gauges = JSON.parseArray(tMeasureAnalysisData.getGaugePoint(), Double.class);
            List<Double> peaks = JSON.parseArray(tMeasureAnalysisData.getPeak(), Double.class);
            List<Double> grades = JSON.parseArray(tMeasureAnalysisData.getGrain(), Double.class);
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (String s : results){
                String[] split = s.split(",");
                Map<String, Object> map = new HashMap<>();
                map.put("x", split[0]);
                map.put("y", split[1]);
                if (gauges != null){
                    for (Double gauge : gauges) {
                        if (split[0].equals(String.valueOf(gauge))){
                            map.put("marked","1");
                            break;
                        }else{
                            map.put("marked","0");
                        }
                    }
                }else{
                    map.put("marked","0");
                }
                if (grades != null){
                    for (Double peak : peaks) {
                        if (split[0].equals(String.valueOf(peak))){
                            map.put("feng","1");
                            break;
                        }else{
                            map.put("feng","0");
                        }
                    }
                }else{
                    map.put("feng","0");
                }
                if (grades != null){
                    for (Double grade : grades){
                        if (split[0].equals(String.valueOf(grade))){
                            map.put("gu","1");
                            break;
                        }else{
                            map.put("gu","0");
                        }
                    }
                }else{
                    map.put("gu","0");
                }
                resultList.add(map);
            }
            vo.setData(resultList);
            vo.setType("1");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    /**
     * 删除变换列表数据接口
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        TMeasureAnalysisData  byId = tMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            return Result.fail("分析数据不存在");
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = tMeasureAnalysisDataService.updateById(byId);
        if (update){
            return Result.ok(true);
        }else {
            return Result.fail(false);
        }
    }

    @Override
    public Result setMethodParameters(String methodId, String deviceSerialNumber) {
        return Result.ok();
    }


}
