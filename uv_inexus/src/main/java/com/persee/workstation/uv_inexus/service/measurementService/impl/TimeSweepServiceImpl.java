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
import com.persee.workstation.uv_inexus.po.measurementPo.model.PeakValleyMarking;
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
import com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep.QueryTimeMethodParamterVo;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * 查询方法参数
     * @param methodId
     * @return
     */
    @Override
    public Result queryMethodParameter(String methodId) {
        // 根据方法id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method==null){
            return Result.fail("没有此方法");
        }
        if (method.getMeasurementParamInfo()==null||method.getMeasurementParamInfo()==""){
            return Result.fail("此方法没有参数");
        }
        // 将方法参数转换成对象拷贝到QueryTimeMethodParamterVo中
        QueryTimeMethodParamterVo timeSweepParamter = JSON.parseObject(method.getMeasurementParamInfo(), QueryTimeMethodParamterVo.class);
        timeSweepParamter.setMethodId(methodId);
        timeSweepParamter.setMethodName(method.getMethodName());
        return Result.ok(timeSweepParamter);
    }

    /**
     * 修改方法参数
     * @param params
     * @return
     */
    @Override
    public Result updateMethodParameter(TimeSweepMethodParameterDto params) {
        // TODO 修改方法目前只覆盖,后续再续逻辑
//        // 根据方法名称查询方法表中是否有此数据
//        List<TMeasureMethod> list = tMeasureMethodService.list(Wrappers.lambdaQuery(TMeasureMethod.class).eq(TMeasureMethod::getMethodName, params.getMethodName()));
//        if (list.size() > 0) {
//            return Result.fail("方法名称重复");
//        }
//        // 根据用户名称进行查询用户表数据
//        SystemUserManagePO user = userConfigDAO.selectOne(new QueryWrapper<SystemUserManagePO>().eq("user_name", params.getUpdateBy()));
//        TMeasureMethod measureMethod = new TMeasureMethod();
//        if (params.getProjectId()==null||params.getProjectId().equals("")){
//            measureMethod.setIsSharingMethod("1");
//        }else{
//            measureMethod.setIsSharingMethod("0");
//            measureMethod.setProjectItemKey(params.getProjectId());
//        }
//        measureMethod.setMethodName(params.getMethodName());
//        measureMethod.setMeasurementType("时间扫描");
//        // 将前端传过来的值复制到SpectrumParameter类中
//        SpectrumParameter spectrumParameter = new SpectrumParameter();
//        BeanUtils.copyProperties(params, spectrumParameter);
//        measureMethod.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
//        measureMethod.setMethodCreateTime(LocalDateTime.now());
//        measureMethod.setMethodCreator(user.getUserName());
//        boolean save = tMeasureMethodService.save(measureMethod);
//        if (!save) {
//            return Result.fail("创建失败");
//        }
//        return Result.ok(measureMethod.getId());

        // 根据方法id查询方法
        TMeasureMethod measureMethod = tMeasureMethodService.getById(params.getMethodId());
        if (measureMethod==null){
            return Result.fail("方法不存在");
        }
        // 将前端传过来的值复制到TimeSweepParamter类中
        TimeSweepParamter timeSweepParamter = new TimeSweepParamter();
        BeanUtils.copyProperties(params, timeSweepParamter);
        measureMethod.setMeasurementParamInfo(JSON.toJSONString(timeSweepParamter));
        measureMethod.setMethodName(params.getMethodName());
        boolean update = tMeasureMethodService.updateById(measureMethod);
        if (!update){
            return Result.ok(new ArrayList<>());
        }
        return Result.ok(measureMethod.getId());
    }

    /**
     * 样品列表
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result sampleListPort(String taskId,String type,Integer digit) {
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
        if (task == null){
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey()==null||task.getSampleKey().equals("[]")){
            return Result.ok(new ArrayList<>());
        }
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
                vo.setData(getSampleData(measureData,digit));
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
     * @param sampleId
     * @param taskId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String sampleId, String taskId) {
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

    /**
     * 变换列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result changeTheListPort(String taskId,String type, Integer digit) {
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
                if (digit!=null){
                    BigDecimal value = new BigDecimal(split[1]);
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    map.put("x", split[0]);
                    map.put("y", value.stripTrailingZeros().toPlainString());
                }else{
                    map.put("x", split[0]);
                    map.put("y", split[1]);
                }
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
     * 恢复删除的数据
     * @param taskId
     * @param paramId
     * @param type
     * @return
     */
    @Override
    public Result restoreDeletedListData(String taskId, String paramId, String type) {
        // 校验传过来的数据
        if (taskId == null || paramId == null || type == null) {
            return Result.ok("参数错误");
        }
        // 根据类型判断是样品数据还是变换数据
        if (type.equals("0")) {
            // 根据任务id查询任务数据
            TMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                return Result.fail("任务不存在");
            }
            List<TaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen taskSpecimen : sampleList) {
                if (taskSpecimen.getSpecimenKey().equals(paramId)) {
                    taskSpecimen.setDeleteType("0");
                    break;
                }
            }
            task.setSampleKey(JSON.toJSONString(sampleList));
            tMeasureTaskService.updateById(task);
        }else{
            // 根据变换数据id查询变换数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(paramId);
            if (byId == null) {
                return Result.fail("变换数据不存在");
            }
            byId.setIsDelete("0");
            tMeasureAnalysisDataService.updateById(byId);
        }
        return Result.ok();
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
        return Result.ok(task.getId());
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
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
        // 根据用户名查询用户数据
        SystemUserManagePO user = systemUserManageDAO.selectOne(new QueryWrapper<>(SystemUserManagePO.class).eq("user_name", paramDto.getUpdateBy()));
        TMeasureTask task;
        if (paramDto.getTaskId()!=null){
            task = tMeasureTaskService.getById(paramDto.getTaskId());
        }else{
            task = new TMeasureTask();
            task.setUserId(user.getId());
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());
            task.setTaskName(method.getMeasurementType() + "task_" + collect);
            task.setCreateTime(LocalDateTime.now());
            task.setMethodKey(method.getId());
            task.setMeasureType(method.getMeasurementType());
            task.setProjectItemKey(method.getProjectItemKey());
            task.setIsDelete("0");
            tMeasureTaskService.save(task);
        }
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        if (taskSpecimenList != null&&(paramDto.getDelSampleIds()!=null||paramDto.getDelSampleIds().isEmpty())) {
            for (TaskSpecimen taskSpecimen : taskSpecimenList) {
                for (String delSampleId : paramDto.getDelSampleIds()) {
                    if (taskSpecimen.getSpecimenKey().equals(delSampleId)) {
                        taskSpecimen.setDeleteType("1");
                    }
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
                TaskSpecimen newTaskSpecimen = new TaskSpecimen();
                newTaskSpecimen.setSpecimenKey(sample.getId());
                newTaskSpecimen.setDeleteType("0");
                newTaskSpecimen.setIsTested("0");
                taskSpecimenList.add(newTaskSpecimen);
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
     * 获取峰谷数据列表
     * @param dataId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result queryPeakValleyDataList(String dataId, String type, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<PeakValleyMarking> peaks = new ArrayList<>();
        List<PeakValleyMarking> grains = new ArrayList<>();
        List<PeakValleyMarking> dataAll = new ArrayList<>();
        if (type.equals("0")){
            TMeasureData measureData = tMeasureDataService.getById(dataId);
            if (measureData == null) {
                return Result.ok(new ArrayList<>());
            }
            if (measureData.getPeak()!=null){
                peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
                dataAll.addAll( peaks);
            }
            if (measureData.getGrain()!=null){
                grains = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
                dataAll.addAll( grains);
            }
            if (dataAll!=null && dataAll.size()!=0){
                if (digit!=null){
                    for (PeakValleyMarking data : dataAll){
                        BigDecimal value = new BigDecimal(data.getY());
                        value = value.setScale(digit, RoundingMode.HALF_UP);
                        data.setY(value.stripTrailingZeros().toPlainString());
                    }
                }
            }
            return Result.ok(dataAll);
        }else if (type.equals("1")){
            TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(dataId);
            if (measureData == null) {
                return Result.ok(new ArrayList<>());
            }
            if (measureData.getPeak()!=null){
                peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
                dataAll.addAll( peaks);
            }
            if (measureData.getGrain()!=null){
                grains = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
                dataAll.addAll( grains);
            }
            if (dataAll!=null && dataAll.size()!=0){
                if (digit!=null){
                    for (PeakValleyMarking data : dataAll){
                        BigDecimal value = new BigDecimal(data.getY());
                        value = value.setScale(digit, RoundingMode.HALF_UP);
                        data.setY(value.stripTrailingZeros().toPlainString());
                    }
                }
            }
            return Result.ok(dataAll);
        }else{
            return Result.ok(new ArrayList<>());
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
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<Double> yPeak = (List<Double>) resultMap.get("peak_y");
        if(xPeak==null||yPeak==null){
            return Result.fail("height参数太高");
        }
        for (int i = 0; i < xPeak.size(); i++) {
            PeakValleyMarking marking = new PeakValleyMarking();
            marking.setX(xPeak.get(i).toString());
            marking.setY(yPeak.get(i).toString());
            marking.setType("波峰");
            peakData.add(marking);
        }
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> xValley = (List<Double>) resultMap.get("valley_x");
        List<Double> yValley = (List<Double>) resultMap.get("valley_y");
        for (int i = 0; i < xValley.size(); i++) {
            PeakValleyMarking marking = new PeakValleyMarking();
            marking.setX(xValley.get(i).toString());
            marking.setY(yValley.get(i).toString());
            marking.setType("波谷");
            valleyData.add(marking);
        }
        Boolean flag;
        if (paramDto.getType().equals("0")){
            // 根据测量数据id查询出测量数据
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            tMeasureData.setPeak(JSON.toJSONString(peakData));
            tMeasureData.setGrain(JSON.toJSONString(valleyData));
            flag = tMeasureDataService.updateById(tMeasureData);
        }else if (paramDto.getType().equals("1")){
            TMeasureAnalysisData tMeasureAnalysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (tMeasureAnalysisData==null){
                return Result.fail("分析数据不存在");
            }
            tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
            tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
            flag=tMeasureAnalysisDataService.updateById(tMeasureAnalysisData);
        }else{
            return Result.fail("参数错误");
        }
        return Result.ok(flag);
    }

    /**
     * 添加峰谷备注
     * @param paramDto
     * @return
     */
    @Override
    public Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark()==null){
            return Result.fail("请先添加备注");
        }
        if (paramDto.getDataType().equals("0")){
            // 根据测量id去测量数据表中进行查询
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
            List<PeakValleyMarking> list;
            if (paramDto.getType().equals("波峰")){
                list = JSON.parseArray(tMeasureData.getPeak(), PeakValleyMarking.class);
            }else{
                list = JSON.parseArray(tMeasureData.getGrain(), PeakValleyMarking.class);
            }
            // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
            for (PeakValleyMarking marking : list) {
                if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                    marking.setRemark(paramDto.getRemark());
                    break;
                }
            }
            if (paramDto.getType().equals("波峰")){
                tMeasureData.setPeak(JSON.toJSONString(list));
            }else{
                tMeasureData.setGrain(JSON.toJSONString(list));
            }
            tMeasureDataService.updateById(tMeasureData);
            return Result.ok();
        }else if (paramDto.getDataType().equals("1")){
            // 根据测量id去分析数据表中进行查询
            TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
            List<PeakValleyMarking> list;
            if (paramDto.getType().equals("波峰")){
                list = JSON.parseArray(tMeasureData.getPeak(), PeakValleyMarking.class);
            }else{
                list = JSON.parseArray(tMeasureData.getGrain(), PeakValleyMarking.class);
            }
            // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
            for (PeakValleyMarking marking : list) {
                if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                    marking.setRemark(paramDto.getRemark());
                    break;
                }
            }
            if (paramDto.getType().equals("波峰")){
                tMeasureData.setPeak(JSON.toJSONString(list));
            }else{
                tMeasureData.setGrain(JSON.toJSONString(list));
            }
            tMeasureAnalysisDataService.updateById(tMeasureData);
            return Result.ok();
        }else{
            return Result.fail("参数错误");
        }
    }

    /**
     * 查询标记点数据列表
     * @param dataId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result queryGaugePointDataList(String dataId, String type, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<PeakValleyMarking> gaugePoints = new ArrayList<>();
        if (type.equals("0")){
            TMeasureData measureData = tMeasureDataService.getById(dataId);
            if (measureData == null) {
                return Result.ok(new ArrayList<>());
            }
            if (measureData.getGaugePoint()!=null){
                gaugePoints = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
                if (digit!=null) {
                    for (PeakValleyMarking data : gaugePoints){
                        BigDecimal value = new BigDecimal(data.getY());
                        value = value.setScale(digit, RoundingMode.HALF_UP);
                        data.setY(value.stripTrailingZeros().toPlainString());
                    }
                }
            }
            return Result.ok(gaugePoints);
        }else if (type.equals("1")){
            TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(dataId);
            if (measureData == null) {
                return Result.ok(new ArrayList<>());
            }
            if (measureData.getGaugePoint()!=null){
                gaugePoints = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
                if (digit!=null) {
                    for (PeakValleyMarking data : gaugePoints){
                        BigDecimal value = new BigDecimal(data.getY());
                        value = value.setScale(digit, RoundingMode.HALF_UP);
                        data.setY(value.stripTrailingZeros().toPlainString());
                    }
                }
            }
            return Result.ok(gaugePoints);
        }else{
            return Result.ok(new ArrayList<>());
        }
    }



    /**
     * 添加标记点
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePoint(AddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            return Result.fail("请选择标记点");
        }

        boolean isMeasureData = "0".equals(paramDto.getType());
        boolean isAnalysisData = "1".equals(paramDto.getType());
        if (!isMeasureData && !isAnalysisData) {
            return Result.fail("type参数错误");
        }

        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        if (isMeasureData) {
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData == null) {
                return Result.fail("测量数据不存在");
            }
            gaugePointJson = tMeasureData.getGaugePoint();
            rawData = JSON.parseArray(tMeasureData.getOriginalCalculationData(), String.class);
        } else {
            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (analysisData == null) {
                return Result.fail("分析数据不存在");
            }
            gaugePointJson = analysisData.getGaugePoint();
            rawData = JSON.parseArray(analysisData.getAnalysisResult(), String.class);
        }

        // 原标记点集合
        List<PeakValleyMarking> oldList = gaugePointJson == null
                ? new ArrayList<>()
                : JSON.parseArray(gaugePointJson, PeakValleyMarking.class);

        // 先构造原有标记点 Map（X -> 对象）
        Map<String, PeakValleyMarking> oldMap = oldList.stream()
                .collect(Collectors.toMap(
                        m -> normalizeNumberString(m.getX()),
                        m -> m,
                        (a, b) -> a
                ));

        // 构造新的最终集合（保证顺序）
        Map<String, PeakValleyMarking> finalMap = new LinkedHashMap<>();

        // 遍历前端传的 X 值
        for (String xStr : paramDto.getGaugePoints()) {
            String normX = normalizeNumberString(xStr);
            PeakValleyMarking existing = oldMap.get(normX);

            if (existing != null) {
                // 已存在则直接用旧值
                finalMap.put(normX, existing);
            } else {
                // 新增点时，从原始数据中查找对应 Y
                for (String s : rawData) {
                    String[] split = s.split(",");
                    String rawNormX = normalizeNumberString(split[0]);
                    if (normX.equals(rawNormX)) {
                        PeakValleyMarking newMark = new PeakValleyMarking();
                        newMark.setX(split[0]);
                        newMark.setY(split[1]);
                        finalMap.put(normX, newMark);
                        break;
                    }
                }
            }
        }

        // 保存更新
        String updatedJson = JSON.toJSONString(new ArrayList<>(finalMap.values()));
        if (isMeasureData) {
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            tMeasureData.setGaugePoint(updatedJson);
            return Result.ok(tMeasureDataService.updateById(tMeasureData));
        } else {
            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            analysisData.setGaugePoint(updatedJson);
            return Result.ok(tMeasureAnalysisDataService.updateById(analysisData));
        }
    }

    /**
     * 添加标记点备注
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark()==null){
            return Result.fail("请先添加备注");
        }
        if (paramDto.getDataType().equals("0")){
            // 根据测量id去测量数据表中进行查询
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            List<PeakValleyMarking> list = JSON.parseArray(tMeasureData.getGaugePoint(), PeakValleyMarking.class);
            // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
            for (PeakValleyMarking marking : list) {
                if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                    marking.setRemark(paramDto.getRemark());
                    break;
                }
            }
            tMeasureData.setGaugePoint(JSON.toJSONString(list));
            tMeasureDataService.updateById(tMeasureData);
            return Result.ok();
        }else if (paramDto.getDataType().equals("1")){
            // 根据测量id去分析数据表中进行查询
            TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("分析数据不存在");
            }
            List<PeakValleyMarking> list = JSON.parseArray(tMeasureData.getGaugePoint(), PeakValleyMarking.class);
            // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
            for (PeakValleyMarking marking : list) {
                if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                    marking.setRemark(paramDto.getRemark());
                    break;
                }
            }
            tMeasureData.setGaugePoint(JSON.toJSONString(list));
            tMeasureAnalysisDataService.updateById(tMeasureData);
            return Result.ok();
        }else{
            return Result.fail("参数错误");
        }
    }

    /**
     * 更新数据颜色
     * @param paramDto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(UpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()
                || paramDto.getType() == null || paramDto.getType().trim().isEmpty()) {
            return Result.fail("参数不能为空");
        }

        String type = paramDto.getType();
        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;

        if ("0".equals(type)) {
            // 样品数据
            TMeasureSample sample = tMeasureSampleService.getById(dataId);
            if (sample == null) {
                return Result.fail("样品数据不存在");
            }
            sample.setColor(color);
            updated = tMeasureSampleService.updateById(sample);

        } else if ("1".equals(type)) {
            // 分析数据
            TMeasureAnalysisData analysis = tMeasureAnalysisDataService.getById(dataId);
            if (analysis == null) {
                return Result.fail("样品数据不存在");
            }
            analysis.setColor(color);
            updated = tMeasureAnalysisDataService.updateById(analysis);

        } else {
            return Result.fail("参数类型错误");
        }

        if (!updated) {
            return Result.fail("更新失败");
        }

        return Result.ok();
    }

    /**
     * 导出到word
     * @param taskId
     * @param response
     */
    @Override
    public void exportToWord(String taskId, HttpServletResponse response) throws Exception {

        // 查询任务信息
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        List<TaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<TMeasureSample> samples = new ArrayList<>();
        List<TMeasureData> measureDataList = new ArrayList<>();
        for (TaskSpecimen tMeasureSample : sampleList) {
            if (tMeasureSample.getIsTested().equals("1") && tMeasureSample.getDeleteType().equals("0")) {
                TMeasureSample byId = tMeasureSampleService.getById(tMeasureSample.getSpecimenKey());
                TMeasureData tMeasureData = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", byId.getId())
                        .orderByDesc("serial_number")
                        .last("LIMIT 1"));
                samples.add(byId);
                measureDataList.add(tMeasureData);
            }
        }

        // 生成 Word 文件
        generateWord(task, samples, measureDataList, response);
    }

    /**
     * 查询算术源数据
     * @param taskId
     * @return
     */
    @Override
    public Result queryArithmeticSourceData(String taskId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null){
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey()==null||task.getSampleKey().equals("[]")){
            return Result.ok(new ArrayList<>());
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String,String>> list = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if(taskSpeciman.getIsTested().equals("1")){
                Map<String, String> map = new HashMap<>();
                TMeasureData one = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", taskSpeciman.getSpecimenKey())
                        .orderByDesc("serial_number")
                        .last("limit 1"));
                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                map.put("measureId", one.getId());
                map.put("sampleName", byId.getSampleName());
                list.add(map);
            }
        }
        return Result.ok(list);
    }

    /**
     * 四则运算
     * @param paramDto
     * @return
     */
    @Override
    public Result fourOperationsPort(FourOperationsParamDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 根据测量数据id查询测量数据
        TMeasureData measureData1 = tMeasureDataService.getById(paramDto.getMeasureDataId1());
        List<String> xList = new ArrayList<>();
        List<String> yList1 = new ArrayList<>();
        List<String> yList2 = new ArrayList<>();
        List<String> list = JSON.parseArray(measureData1.getOriginalCalculationData(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList1.add(split[1]);
        }
        if(paramDto.getDataType().equals("0")){
            for (int i = 0; i < yList1.size(); i++) {
                yList2.add(String.valueOf(paramDto.getMeasureDataId2()));
            }
        }else if (paramDto.getDataType().equals("1")){
            TMeasureData measureData2 = tMeasureDataService.getById(paramDto.getMeasureDataId2());
            // 将测量数据1和2的测量数据提取出来
            List<String> list2 = JSON.parseArray(measureData2.getOriginalCalculationData(), String.class);
            for (String s : list2) {
                String[] split = s.split(",");
                yList2.add(split[1]);
            }
        }
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        // 设置请求头类型为json
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建请求体
        Map<String, Object> body = new HashMap<>();
        // 将参数添加到请求体中
        body.put("x_data", xList);
        body.put("y1_data", yList1);
        body.put("y2_data", yList2);
        body.put("operation",paramDto.getArithmeticOperators());
        // 创建请求对象
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        // 将请求结果转换成对象
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.SPECTRUM_FOUR_OPERATION, request, Map.class).getBody();
        if (resultMap == null){
            return Result.fail("python接口调用失败");
        }
        // 对结果进行寻峰计算
        List<Double> x_data = (List<Double>) resultMap.get("x_data");
        List<Double> y_data = (List<Double>) resultMap.get("y_data");
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", x_data);
        body1.put("y_data", y_data);
        body1.put("height", "0.5");
        body1.put("distance", "1");
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        // 直接拿寻峰数据进行一次寻谷
        Map<String, Object> resultMap2 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request1, Map.class).getBody();
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> peakY = (List<Double>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<Double> valleyY = (List<Double>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i).toString());
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i).toString());
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        // 将分析数据存储到分析数据表中
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisData.setTaskId(measureData1.getTaskId());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < x_data.size(); i++) {
            String s = x_data.get(i) + "," + y_data.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisData.setAnalysisType("0");
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save){
            return Result.fail("保存失败");
        }
        List<Map<String,Object>> dataList = new ArrayList<>();
        for (int i = 0; i < x_data.size(); i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("x",x_data.get(i));
            map.put("y",y_data.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 高级运算
     * @param paramDto
     * @return
     */
    @Override
    public Result advancedOperationsPort(AdvancedOperationsDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 根据测量数据id查询测量数据
        TMeasureData measureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("transform_type", Integer.valueOf(paramDto.getDataType()));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.SPECTRUM_TRANSFORMATION, request, Map.class).getBody();
        body.put("x_data", xList);
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        body1.put("height", "0.5");
        body1.put("distance", "1");
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        // 直接拿寻峰数据进行一次寻谷
        Map<String, Object> resultMap2 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request1, Map.class).getBody();
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> peakY = (List<Double>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<Double> valleyY = (List<Double>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i).toString());
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i).toString());
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        // 将分析数据存储到分析数据表中
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisData.setTaskId(measureData.getTaskId());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            String s = xList.get(i) + "," + y_data.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisData.setAnalysisType("1");
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save){
            return Result.fail("数据保存失败");
        }
        List<Map<String,Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("x",xList.get(i));
            map.put("y",y_data.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 平滑
     * @param paramDto
     * @return
     */
    @Override
    public Result smoothnessPort(SmoothnessDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        if (Double.valueOf(paramDto.getCoefficient())>Double.valueOf(paramDto.getPointNum())){
            return Result.fail("系数不能大于点数");
        }
        // 根据测量数据的id查询出测量数据
        TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(tMeasureData.getOriginalCalculationData(), String.class);
        for (String s : list){
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("window_length", paramDto.getPointNum());
        body.put("polyorder", paramDto.getCoefficient());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.SMOOTHED, request, Map.class).getBody();
        Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
        List<Double> smoothedY = (List<Double>) data.get("smoothed_y");
        Map<String, Object> body1 =  new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", smoothedY);
        body1.put("height", "0.5");
        body1.put("distance", "1");
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        Map<String, Object> resultMap2 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request1, Map.class).getBody();
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> peakY = (List<Double>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<Double> valleyY = (List<Double>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i).toString());
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i).toString());
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisData.setTaskId(tMeasureData.getTaskId());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            String s = xList.get(i) + "," + smoothedY.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisData.setAnalysisType("2");
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save){
            return Result.fail("数据保存失败");
        }
        List<Map<String,Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("x",xList.get(i));
            map.put("y",smoothedY.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 微分
     * @param paramDto
     * @return
     */
    @Override
    public Result differentialPort(DifferentialDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        if (Double.valueOf(paramDto.getCoefficient())>Double.valueOf(paramDto.getPointNum())){
            return Result.fail("系数不能大于点数");
        }
        // 根据测量数据的id查询出测量数据
        TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(tMeasureData.getOriginalCalculationData(), String.class);
        for (String s : list){
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("window_length", paramDto.getPointNum());
        body.put("polyorder", paramDto.getCoefficient());
        body.put("deriv", paramDto.getDifferentialNum());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.DIFFERENTIATED, request, Map.class).getBody();
        Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
        List<Double> differentiatedY = (List<Double>) data.get("differentiated_y");
        Map<String, Object> body1 =  new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", differentiatedY);
        body1.put("height", "0.5");
        body1.put("distance", "1");
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        Map<String, Object> resultMap2 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request1, Map.class).getBody();
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> peakY = (List<Double>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<Double> valleyY = (List<Double>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i).toString());
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i).toString());
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisData.setTaskId(tMeasureData.getTaskId());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            String s = xList.get(i) + "," + differentiatedY.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisData.setAnalysisType("3");
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save){
            return Result.fail("数据保存失败");
        }
        List<Map<String,Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("x",xList.get(i));
            map.put("y",differentiatedY.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 光度模式转化
     * @param paramDto
     * @return
     */
    @Override
    public Result luminanceModeConversionPort(LuminosityConversionDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 根据方法id查询方法表的数据
        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
        // 获取方法表中的方法参数信息
        String measurementParamInfo = method.getMeasurementParamInfo();
        SpectrumParameter spectrumParameter = JSON.parseObject(measurementParamInfo, SpectrumParameter.class);
        Integer type = null;
        if(paramDto.getDataType().equals("1")){
            if (spectrumParameter.getLuminosity().equals("1")){
                type = 0;
            }else if (spectrumParameter.getLuminosity().equals("2")){
                type = 1;
            }else {
                return Result.fail("您当前的光度模式不支持进行转换");
            }
        }else if (paramDto.getDataType().equals("2")){
            if (spectrumParameter.getLuminosity().equals("1")){
                type = 2;
            }else if (spectrumParameter.getLuminosity().equals("3")){
                type = 3;
            }else {
                return Result.fail("您当前的光度模式不支持进行转换");
            }
        }else{
            return Result.fail("您当前的光度模式不支持进行转换!");
        }
        // 根据测量数据id查询测量数据
        TMeasureData measureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("transform_type",type);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.SPECTRUM_TRANSFORMATION, request, Map.class).getBody();
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        body1.put("height", "0.5");
        body1.put("distance", "1");
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        Map<String, Object> resultMap2 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request1, Map.class).getBody();
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> peakY = (List<Double>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<Double> valleyY = (List<Double>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i).toString());
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i).toString());
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisData.setTaskId(measureData.getTaskId());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            String s = xList.get(i) + "," + y_data.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisData.setAnalysisType("4");
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save){
            return Result.fail("数据保存失败");
        }
        List<Map<String,Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("x",xList.get(i));
            map.put("y",y_data.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 保存数据
     * @param taskId
     * @return
     */
    @Override
    public Result saveData(String taskId) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        task.setEndTime(LocalDateTime.now());
        task.setIsDelete("1");
        boolean update = tMeasureTaskService.updateById(task);
        if ( update){
            return Result.ok(true);
        }else {
            return Result.fail(false);
        }
    }


    /**
     * 获取样品的详细数据
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureData measureData,Integer digit) {
        List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            if (digit!=null){
                BigDecimal value = new BigDecimal(split[1]);
                value = value.setScale(digit, RoundingMode.HALF_UP);
                map.put("x", split[0]);
                map.put("y", value.stripTrailingZeros().toPlainString());
            }else{
                map.put("x", split[0]);
                map.put("y", split[1]);
            }
            if (gauges != null) {
                for (PeakValleyMarking gauge : gauges) {
                    if (split[0].equals(gauge.getX())) {
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
                for (PeakValleyMarking peak : peaks) {
                    if (split[0].equals(peak.getX())) {
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
                for (PeakValleyMarking grade : grades) {
                    if (split[0].equals(grade.getX())) {
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

    /**
     * 生成Word文件
     * @param task
     * @param samples
     * @param response
     * @throws Exception
     */
    private void generateWord(TMeasureTask task, List<TMeasureSample> samples,List<TMeasureData> measureDataList, HttpServletResponse response) throws Exception {

        // 使用 Apache POI 创建 Word 文件
        XWPFDocument document = new XWPFDocument();

        // 添加标题
        XWPFParagraph title = document.createParagraph();
        XWPFRun titleRun = title.createRun();
        titleRun.setText("任务报告");
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.addBreak();

        // 添加任务信息
        XWPFParagraph taskInfo = document.createParagraph();
        taskInfo.setAlignment(ParagraphAlignment.LEFT);
        // 每条信息单独一行
        taskInfo.createRun().setText("任务名称: " + task.getTaskName());
        taskInfo.createRun().addBreak();  // 换行
        taskInfo.createRun().setText("测量类型: " + task.getMeasureType());
        taskInfo.createRun().addBreak();  // 换行
        taskInfo.createRun().setText("创建时间: " + task.getCreateTime().toString());
        taskInfo.createRun().addBreak();  // 换行

        // 创建表格
        XWPFTable table = document.createTable();

        // 设置表头
        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("样品ID");
        headerRow.addNewTableCell().setText("样品名称");
        headerRow.addNewTableCell().setText("样品位置");

        // 设置表格宽度：通过 XWPFTableCell 设置列宽
        for (int i = 0; i < table.getRows().size(); i++) {
            XWPFTableRow row = table.getRow(i);
            for (int j = 0; j < row.getTableCells().size(); j++) {
                XWPFTableCell cell = row.getCell(j);
                // 设置单元格的宽度（单位为 twips：1 twip = 1/1440 英寸）
                cell.setWidth("4000"); // 设置单元格宽度
            }
        }


        // 添加表格数据
        for (TMeasureSample sample : samples) {
            XWPFTableRow row = table.createRow();
            row.getCell(0).setText(sample.getSampleNumber());
            row.getCell(1).setText(sample.getSampleName());
            row.getCell(2).setText(sample.getSampleLocation());
        }

        // 添加样品图片信息
        XWPFParagraph sampleInfo = document.createParagraph();
        sampleInfo.setAlignment(ParagraphAlignment.LEFT);
        for (int i = 0; i < measureDataList.size(); i++) {
            if (measureDataList.get(i).getDataImage() == null) continue;
            InputStream inputStream = new ByteArrayInputStream(measureDataList.get(i).getDataImage());
            sampleInfo.createRun().setText(samples.get(i).getSampleName());
            sampleInfo.createRun().addBreak();  // 换行
            sampleInfo.createRun().addPicture(
                    inputStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "sample_image" + i + ".jpg",
                    Units.toEMU(300),
                    Units.toEMU(200)
            );
            sampleInfo.createRun().addBreak();  // 换行
            inputStream.close();
        }

        // 设置响应头，返回文件下载
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=task_report.docx");

        // 写文件
        document.write(response.getOutputStream());
    }

    /**
     * 数字字符串归一化（防止 360 / 360.0 / 3.6E2 比较失败）
     */
    private String normalizeNumberString(String numStr) {
        return new BigDecimal(numStr)
                .stripTrailingZeros()
                .toPlainString();
    }

}
