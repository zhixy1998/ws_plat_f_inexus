package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
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
import com.persee.workstation.uv_inexus.vo.measurementVo.Sample;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleListVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private RestTemplate restTemplate;

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
        // TODO 暂时先切换成一个测量数据一个样品
//        // 根据任务id查询任务数据
//        TMeasureTask task = tMeasureTaskService.getById(taskId);
//        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
//        List<SampleListVo> listVos = new ArrayList<>();
//        for (TaskSpecimen taskSpeciman : taskSpecimen) {
//            // 如果type为空同时删除类型为1则跳过
//            if (type == null && taskSpeciman.getDeleteType().equals("1")) continue;
//            SampleListVo vo = new SampleListVo();
//            // 根据样品id查询样品数据
//            TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
//            vo.setTitle(byId.getSampleName());
//            if (taskSpeciman.getIsTested().equals("0")) {
//                vo.setData(null);
//                vo.setMeasureId(null);
//                vo.setSampleId(byId.getId());
//                vo.setColor(byId.getColor());
//                vo.setType("0");
//                vo.setDeleteType(taskSpeciman.getDeleteType());
//                listVos.add(vo);
//            } else {
//                // 根据样品id和任务id两个条件去查询测量数据
//                QueryWrapper<TMeasureData> eq = new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", byId.getId());
//                if (type == null) {
//                    eq.eq("is_delete", "0");
//                }
//                // 增加查询条件,按照序列号从大到小排序
//                eq.orderByDesc("serial_number");
//                // 根据样品id和任务id两个条件去查询测量数据
//                List<TMeasureData> measureDataList = tMeasureDataService.list(eq);
//                for (TMeasureData measureData : measureDataList) {
//                    if (measureDataList.size() > 1) {
//                        SampleListVo vos = new SampleListVo();
//                        vos.setTitle(byId.getSampleName() + "(" + measureData.getSerialNumber() + ")");
//                        vos.setMeasureId(measureData.getId());
//                        vos.setSampleId(byId.getId());
//                        vos.setData(getSampleData(measureData));
//                        vos.setColor(byId.getColor());
//                        vos.setDeleteType(taskSpeciman.getDeleteType());
//                        vos.setType("0");
//                        listVos.add(vos);
//                    }else{
//                        vo.setMeasureId(measureData.getId());
//                        vo.setData(getSampleData(measureData));
//                        vo.setColor(byId.getColor());
//                        vo.setSampleId(byId.getId());
//                        vo.setDeleteType(taskSpeciman.getDeleteType());
//                        vo.setType("0");
//                        listVos.add(vo);
//                    }
//                }
//            }
//        }
//        return Result.ok(listVos);

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            // 如果type为空同时删除类型为1则跳过
            if (type == null && taskSpeciman.getDeleteType().equals("1")) continue;
            SampleListVo vo = new SampleListVo();
            // 根据样品id查询样品数据
            TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
            vo.setTitle(byId.getSampleName());
            if (taskSpeciman.getIsTested().equals("0")) {
                vo.setData(null);
                vo.setMeasureId(null);
                vo.setSampleId(byId.getId());
                vo.setColor(byId.getColor());
                vo.setType("0");
                vo.setDeleteType(taskSpeciman.getDeleteType());
                listVos.add(vo);
            } else {
                // 根据样品id和任务id两个条件去查询测量数据
                QueryWrapper<TMeasureData> eq = new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", byId.getId());
                if (type == null) {
                    eq.eq("is_delete", "0");
                }
                // 增加查询条件,按照序列号从大到小排序
                eq.orderByDesc("serial_number");
                eq.last("limit 1");
                // 根据样品id和任务id两个条件去查询测量数据
                TMeasureData measureData = tMeasureDataService.getOne(eq);
                vo.setMeasureId(measureData.getId());
                vo.setData(getSampleData(measureData));
                vo.setColor(byId.getColor());
                vo.setSampleId(byId.getId());
                vo.setDeleteType(taskSpeciman.getDeleteType());
                vo.setType("0");
                listVos.add(vo);
            }
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
     * @param taskId
     * @param serialNumber
     * @param methodId
     * @return
     */
    @Override
    public Result zero(String taskId,String serialNumber, String methodId) {
        if (serialNumber == null || serialNumber.equals("")){
            return Result.fail("缺少设备序列号");
        }
        if (methodId == null || methodId.equals("")){
            return Result.fail("缺少方法id");
        }
        TMeasureMethod methodData = tMeasureMethodService.getById(methodId);
        if (methodData == null) {
            return Result.fail("方法不存在");
        }
        TimeSweepParamter timeSweepParamter = JSON.parseObject(methodData.getMeasurementParamInfo(), TimeSweepParamter.class);
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
        TMeasureTask task =null;
        if (taskId != null && !taskId.equals("")){
            task = tMeasureTaskService.getById(taskId);
        }else{
            task = new TMeasureTask();
            TMeasureMethod method = tMeasureMethodService.getById(methodId);
            SystemUserManagePO user = systemUserManageDAO.selectOne(new QueryWrapper<>(SystemUserManagePO.class).eq("user_name", method.getMethodCreator()));
            task.setUserId(user.getId());
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());

            task.setTaskName(method.getMeasurementType() + "task_" + collect);
            task.setCreateTime(LocalDateTime.now());
            task.setMethodKey(methodId);
            task.setMeasureType(method.getMeasurementType());
            task.setProjectItemKey(method.getProjectItemKey());
            task.setStartTime(LocalDateTime.now());
            task.setDeviceSerialNumber(serialNumber);
            tMeasureTaskService.save(task);
        }
        ChangeParam changeParam = zero.getData();
        changeParam.setWl(timeSweepParamter.getWaveLength());
        task.setZeroParam(JSON.toJSONString(changeParam));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }

    /**
     * 查询样品列表
     * @param taskId
     * @return
     */
    @Override
    public Result querySampleList(String taskId) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey() == null) {
            return Result.ok(null);
        }
        // 分割任务中的样品数据
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Sample> sampleList = new ArrayList<>();
        if (taskSpecimen == null || taskSpecimen.size() == 0) {
            return Result.ok(new ArrayList<>());
        }
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if (taskSpeciman.getDeleteType().equals("0")){
                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                Sample sample = new Sample();
                sample.setSampleId(byId.getId());
                sample.setSampleName(byId.getSampleName());
                sample.setSampleNumber(byId.getSampleNumber());
                sample.setSampleLocation(byId.getSampleLocation());
                sampleList.add(sample);
            }
        }
        return Result.ok(sampleList);
    }

    /**
     * 添加或删除样品
     * @param paramDto
     * @return
     */
    @Override
    public Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto) {
        if (paramDto.getNewSamples().isEmpty() && paramDto.getDelSampleIds().isEmpty()) {
            return Result.ok("没有要新增或删除的样品！");
        }
        if (paramDto.getDelSampleIds()==null){
            paramDto.setDelSampleIds(new ArrayList<>());
        }
        if (paramDto.getNewSamples()==null){
            paramDto.setNewSamples(new ArrayList<>());
        }

        if (paramDto.getHeads() != null && paramDto.getHeads().size() > 10) {
            return Result.fail("表格新增列数不能超过10列");
        }
        // 根据任务id进行查询
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : taskSpecimenList) {
            for (String delSampleId : paramDto.getDelSampleIds()) {
                if (taskSpecimen.getSpecimenKey().equals(delSampleId)) {
                    taskSpecimen.setDeleteType("1");
                }
            }
        }

        // 遍历新增的样品列表,如果id为null则进行新增,如果id不为null则进行方法修改
        for (Map<String, String> map : paramDto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            SampleVO newSample = JSON.parseObject(jsonString, SampleVO.class);

            JSONObject newColData = new JSONObject();
            if (paramDto.getHeads() != null) {

                for (HeadVO head : paramDto.getHeads()) {
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }
            }

            if (newSample.getSampleId()!=null){
                TMeasureSample byId = tMeasureSampleService.getById(newSample.getSampleId());
                if (byId == null){
                    return Result.ok("样品不存在");
                }
                byId.setSampleName(newSample.getSampleName());
                byId.setSampleNumber(newSample.getSampleNumber());
                byId.setSampleLocation(newSample.getSampleLocation());

                byId.setHeads(JSON.toJSONString(paramDto.getHeads()));
                byId.setNewColData(newColData.toJSONString());
                boolean update = tMeasureSampleService.updateById(byId);
                if (!update){
                    return Result.ok("修改失败");
                }
            }else{
                TMeasureSample sample = new TMeasureSample();
                sample.setSampleName(newSample.getSampleName());
                sample.setSampleNumber(newSample.getSampleNumber());
                sample.setSampleLocation(newSample.getSampleLocation());
                sample.setColor("#000000");
                sample.setSampleType(SampleTypeEnum.kind_stand.getIdentity());

                sample.setHeads(JSON.toJSONString(paramDto.getHeads()));
                sample.setNewColData(newColData.toJSONString());
                boolean save = tMeasureSampleService.save(sample);
                if (!save){
                    return Result.ok("新增失败");
                }
                taskSpecimenList.add(new TaskSpecimen(sample.getId(),"0","0"));
            }
        }

        task.setSampleKey(JSON.toJSONString(taskSpecimenList));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }

    /**
     * 上传图片
     * @param upload
     * @return
     */
    @Override
    public Result uploadImage(SpectrumImageUpload upload) {
        String s = upload.getImage().split(",")[1];
        byte[] bytes = Base64.decodeBase64(s);
        if (upload.getType().equals("0")){
            TMeasureData byId = tMeasureDataService.getById(upload.getId());
            byId.setDataImage(bytes);
            boolean update = tMeasureDataService.updateById(byId);
            return Result.ok(update);
        }else if (upload.getType().equals("1")){
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(upload.getId());
            byId.setAnalysisDataImage(bytes);
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            return Result.ok(update);
        }else{
            return Result.fail("type类型错误");
        }
    }

    /**
     * 寻峰寻谷接口
     * @param paramDto
     * @return
     */
    @Override
    public Result seekingPeaksAndValleysPort(SeekingPeaksValleysDto paramDto) {
        paramDto.setHeight(paramDto.getHeight()/100);
        List<String> list = new ArrayList<>();
        if (paramDto.getType().equals("0")){
            // 根据测量数据id查询出测量数据
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            list = JSON.parseArray(tMeasureData.getOriginalCalculationData(), String.class);
        }else if (paramDto.getType().equals("1")){
            TMeasureAnalysisData tMeasureAnalysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (tMeasureAnalysisData==null){
                return Result.fail("测量数据不存在");
            }
            list = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
        }else{
            return Result.fail("参数错误");
        }
        // 分割测量数据
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        for (String s : list){
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", xList);
        body.put("y_data", yList);
        body.put("peak_height", paramDto.getHeight());
        body.put("valley_height", paramDto.getHeight());
        body.put("distance", paramDto.getDistance());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING_AND_VALLEY_SEARCHING, request, Map.class).getBody();
        List<Map<String, Object>> listVos = new ArrayList<>();
        List<String> xPeak = (List<String>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        List<String> xValley = (List<String>) resultMap.get("valley_x");
        List<String> yValley = (List<String>) resultMap.get("valley_y");
        int number=0;
        for (int i = 0; i < xPeak.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("serialNumber", ++number);
            map.put("x", xPeak.get(i));
            map.put("y", yPeak.get(i));
            map.put("type", "波峰");
            listVos.add(map);
        }
        for (int i = 0; i < xValley.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("serialNumber", ++number);
            map.put("x", xValley.get(i));
            map.put("y", yValley.get(i));
            map.put("type", "波谷");
            listVos.add(map);
        }
        if (paramDto.getType().equals("0")){
            // 根据测量数据id查询出测量数据
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            tMeasureData.setPeak(JSON.toJSONString(xPeak));
            tMeasureData.setGrain(JSON.toJSONString(xValley));
            tMeasureDataService.updateById(tMeasureData);
        }else if (paramDto.getType().equals("1")){
            TMeasureAnalysisData tMeasureAnalysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (tMeasureAnalysisData==null){
                return Result.fail("分析数据不存在");
            }
            tMeasureAnalysisData.setPeak(JSON.toJSONString(xPeak));
            tMeasureAnalysisData.setGrain(JSON.toJSONString(xValley));
            tMeasureAnalysisDataService.updateById(tMeasureAnalysisData);
        }else{
            return Result.fail("参数错误");
        }
        return Result.ok(listVos);
    }

    /**
     * 添加标记点
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePoint(AddGaugePointDto paramDto) {
        // 根据类型判断是测量数据还是分析数据
        if (paramDto.getType().equals("0")){
            // 根据数据id去测量数据中进行查询
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            tMeasureData.setGaugePoint(JSON.toJSONString(paramDto.getGaugePoints()));
            boolean update = tMeasureDataService.updateById(tMeasureData);
            return Result.ok(update);
        }else if (paramDto.getType().equals("1")){
            // 根据数据id去分析数据中进行查询
            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (analysisData==null){
                return Result.fail("分析数据不存在");
            }
            analysisData.setGaugePoint(JSON.toJSONString(paramDto.getGaugePoints()));
            boolean update = tMeasureAnalysisDataService.updateById(analysisData);
            return Result.ok(update);
        }else {
            return Result.fail("type参数错误");
        }
    }

    /**
     * 获取样品的详细数据
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureData measureData) {
        List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
        List<Double> gauges = JSON.parseArray(measureData.getGaugePoint(), Double.class);
        List<Double> peaks = JSON.parseArray(measureData.getPeak(), Double.class);
        List<Double> grades = JSON.parseArray(measureData.getGrain(), Double.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            map.put("x", String.format("%.2f", Double.parseDouble(split[0])));
            map.put("y", String.format("%.2f", Double.parseDouble(split[1])));
            if (gauges != null) {
                for (Double gauge : gauges) {
                    if (split[0].equals(String.valueOf(gauge))) {
                        map.put("marked", "1");
                        break;
                    } else {
                        map.put("marked", "0");
                    }
                }
            } else {
                map.put("marked", "0");
            }
            if (peaks != null) {
                for (Double peak : peaks) {
                    if (split[0].equals(String.valueOf(peak))) {
                        map.put("feng", "1");
                        break;
                    } else {
                        map.put("feng", "0");
                    }
                }
            } else {
                map.put("feng", "0");
            }
            if (grades != null) {
                for (Double grade : grades) {
                    if (split[0].equals(String.valueOf(grade))) {
                        map.put("gu", "1");
                        break;
                    } else {
                        map.put("gu", "0");
                    }
                }
            } else {
                map.put("gu", "0");
            }
            resultList.add(map);
        }
        return resultList;
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
