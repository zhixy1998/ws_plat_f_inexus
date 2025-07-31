package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleListVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;
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
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private SystemUserManageDAO systemUserManageDAO;
    @Autowired
    private TMeasureProjectService tMeasureProjectService;

    /**
     * 查询项目任务方法接口
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryProjectTaskMethod(String updateBy,String measureType) {
        SystemUserManagePO user = systemUserManageDAO.selectOne(new QueryWrapper<SystemUserManagePO>().eq("user_name", updateBy));
        if (user==null){
            return Result.fail("用户不存在");
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(measureType);
        if (measureTypeEnum==null){
            return Result.fail("暂不支持此测量类型");
        }
        // 根据用户id和测量类型然后通过创建时间查找到最近的一次任务
        TMeasureTask task = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>().eq("user_id", user.getId()).eq("measure_type", measureTypeEnum.getDescription()).orderByDesc("create_time").last("limit 1"));
        if (task==null){
            return Result.fail("暂无任务数据");
        }
        // 根据任务id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 根据项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
        SpectrumBasicParameterVo spectrumBasicParameterVo = new SpectrumBasicParameterVo();
        spectrumBasicParameterVo.setTaskId(task.getId());
        spectrumBasicParameterVo.setTaskName(task.getTaskName());
        if (method==null){
            spectrumBasicParameterVo.setMethodId(null);
            spectrumBasicParameterVo.setMethodName(null);
        }else{
            spectrumBasicParameterVo.setMethodId(method.getId());
            spectrumBasicParameterVo.setMethodName(method.getMethodName());
        }
        if (project==null){
            spectrumBasicParameterVo.setProjectId(null);
            spectrumBasicParameterVo.setProjectName(null);
        }else{
            spectrumBasicParameterVo.setProjectId(project.getId());
            spectrumBasicParameterVo.setProjectName(project.getProjectName());
        }
        return Result.ok(spectrumBasicParameterVo);
    }


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
                vo.setMeasureId(null);
            }else {
                vo.setMeasureId(measureData.getId());
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
            vo.setMeasureId(tMeasureAnalysisData.getId());
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

    /**
     * 设置方法参数
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result setMethodParameters(String methodId, String deviceSerialNumber) {
        // 根据序列号查询设备ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id", methodId).eq("measurement_type","时间扫描"));
        if (method == null) {
            return Result.fail("方法不存在");
        }
        TimeSweepParamter time = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
        List<Command> commands = new ArrayList<>();
        commands.add(buildCommand(SetParamEnum.SetPhotoMode, time.getSpectralBandwidth()));
        commands.add(buildCommand(SetParamEnum.SetChangeLampWl, time.getChangeLampWaveLength()));
        commands.add(buildCommand(SetParamEnum.SetLampPos, time.getEnergyLamp()));
        commands.add(buildCommand(SetParamEnum.SetGain, time.getGain()));
        R<String> stringR = devMessageUtil.timeSweepSet(commands, ip);
        return Result.ok(stringR.getData());
    }

    /**
     * 零点校零
     * @param serialNumber
     * @param methodId
     * @return
     */
    @Override
    public Result zero(String serialNumber, String methodId) {
        if (serialNumber == null || serialNumber.equals("")){
            return Result.fail("缺少设备序列号");
        }
        if (methodId == null || methodId.equals("")){
            return Result.fail("缺少方法id");
        }
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        TimeSweepParamter timeSweepParamter = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
        if (timeSweepParamter.getWaveLength() == null || timeSweepParamter.getWaveLength().isEmpty()) {
            return Result.fail("缺少波长");
        }
        // 设置参数
        Result result = setMethodParameters(methodId, serialNumber);
        if (result.getData().equals("false")) {
            return result;
        }
        // 通过序列号获取ip
        String ip = deviceInfoService.getIp(serialNumber);

        // 定位当前波长
        Command command = new Command(SetParamEnum.SetWl.getComId(), new Object[]{Double.parseDouble(timeSweepParamter.getWaveLength())});
        devMessageUtil.wavelengthLocate(command, ip);

        // 进行校零
        command = new Command(SetParamEnum.Zero.getComId(), new Object[]{});
        R<ChangeParam> zero = devMessageUtil.zero(command, ip);
        if (!zero.getSuccess()) {
            return Result.fail(zero.getMessage());
        }
        ChangeParam changeParam = zero.getData();
        changeParam.setWl(timeSweepParamter.getWaveLength());
        timeSweepParamter.setBaselineData(changeParam);
        method.setMeasurementParamInfo(JSON.toJSONString(timeSweepParamter));
        boolean update = tMeasureMethodService.updateById(method);
        return Result.ok(update);
    }


    /**
     * 构建命令
     * @param cmdEnum
     * @param value
     * @return
     */
    private Command buildCommand(SetParamEnum cmdEnum, Object value) {
        Command cmd = new Command();
        cmd.setCmdId(cmdEnum.getComId());
        cmd.setParams(new Object[]{value});
        return cmd;
    }


}
