/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.experimentTest.TimeSweepMethodParamsDto;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.RationChangeMethodTypeEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TaskStatusEnum;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.SamplePoolConfigPO;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConcisePublicUtilService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSampleVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleListVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep.QueryTimeMethodParamterVo;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.lang.Double.valueOf;

@Service
("timeSweepService")
@Slf4j
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
    private TMeasureAnalysisImageService tMeasureAnalysisImageService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private SystemUserManageDAO systemUserManageDAO;
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private TMeasureTaskAttachmentService tMeasureTaskAttachmentService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private PythonPathConstant pythonPathConstant;
    @Autowired
    private DevOperationUtilService devOperationUtilService;
    @Autowired
    private TMeasureTaskMapper tMeasureTaskMapper;
    @Autowired
    DeviceInfoService infoService;
    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired
    private SamplePoolConfigService samplePoolConfigService;
    @Autowired
    private ConcisePublicUtilService concisePublicUtilService;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;

    @Value("default")
    String defaultMethodProjectName;

    /**
     * 查询项目任务方法接口
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryProjectTaskMethod(String updateBy,String measureType) {
        SysUserPO user = systemUserManageDAO.selectByUsername(updateBy);
        if (user == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(measureType);
        if (measureTypeEnum == null) {
            //暂不支持
            return Result.fail(YamlMessageSource.getMessageStatic("0000002B"));
        }
        // 根据用户id和测量类型然后通过创建时间查找到最近的一次任务
        TMeasureTask task = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>()
                .eq("user_id", user.getId())
                .eq("measure_type", measureTypeEnum.getDescription())
                .orderByDesc("create_time")
                .last("limit 1"));

        if (task != null) {
            // 根据任务id查询方法
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            // 根据项目id查询项目
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            SpectrumBasicParameterVo spectrumBasicParameterVo = new SpectrumBasicParameterVo();
            spectrumBasicParameterVo.setTaskId(task.getId());
            spectrumBasicParameterVo.setTaskName(task.getTaskName());
            spectrumBasicParameterVo.setTaskStatus(task.getTaskStatus());
            if (method == null) {
                spectrumBasicParameterVo.setMethodId(null);
                spectrumBasicParameterVo.setMethodName(null);
            } else {
                spectrumBasicParameterVo.setMethodId(method.getId());
                spectrumBasicParameterVo.setMethodName(method.getMethodName());
            }
            if (project == null) {
                spectrumBasicParameterVo.setProjectId(null);
                spectrumBasicParameterVo.setProjectName(null);
            } else {
                spectrumBasicParameterVo.setProjectId(project.getId());
                spectrumBasicParameterVo.setProjectName(project.getProjectName());
            }
            return Result.ok(spectrumBasicParameterVo);
        }
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>()
                .eq("method_creator", user.getUsername())
                .eq("measurement_type", measureType)
                .orderByDesc("method_create_time")
                .last("limit 1"));
        SpectrumBasicParameterVo vo = new SpectrumBasicParameterVo();
        if (method != null) {
            // 根据方法id查询任务
            TMeasureTask measureTask = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>()
                    .eq("method_key", method.getId())
                    .orderByDesc("create_time")
                    .last("limit 1"));
            TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            if (measureTask!=null){
                vo.setTaskId(measureTask.getId());
                vo.setTaskName(measureTask.getTaskName());
                vo.setTaskStatus(measureTask.getTaskStatus());
            }else{
                vo.setTaskId(null);
                vo.setTaskName(null);
                vo.setTaskStatus(null);
            }
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
            return Result.ok(vo);
        }
        // 根据默认任务名称查询默认任务
        TMeasureTask defaultTask = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>().eq("task_name", Constants.DEFAULT_TASK)
                .eq("measure_type", measureType)
                .last("LIMIT 1"));
        if (defaultTask == null) {
            return Result.fail(YamlMessageSource.getMessageStatic("00101006"));
        }
        // 根据方法id和项目id查询方法和项目
        TMeasureProject byId = tMeasureProjectService.getById(defaultTask.getProjectItemKey());
        if (byId == null){
            return Result.fail(YamlMessageSource.getMessageStatic("00301010"));
        }
        TMeasureMethod method1 = tMeasureMethodService.getById(defaultTask.getMethodKey());
        if (method1 == null){
            return Result.fail(YamlMessageSource.getMessageStatic("00000011"));
        }
        vo.setMethodId(method1.getId());
        vo.setMethodName(method1.getMethodName());
        vo.setProjectId(byId.getId());
        vo.setProjectName(byId.getProjectName());
        vo.setTaskId(defaultTask.getId());
        vo.setTaskName(defaultTask.getTaskName());
        vo.setTaskStatus(defaultTask.getTaskStatus());
        return Result.ok(vo);
    }


    /**
     * 查询项目下的所有方法
     * @param projectId
     * @param measureForm
     * @return
     */
    @Override
    public Result queryProjectAllMethods(String projectId,String measureForm, String updateBy) {

        // 参数校验
        if (StringUtils.isEmpty(projectId)|| StringUtils.isEmpty(measureForm)|| StringUtils.isEmpty(updateBy)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        List<TMeasureMethod> methods = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", measureForm).eq("method_creator", updateBy));

        if (methods == null) {
            //没有此类型的数据
            return Result.fail(YamlMessageSource.getMessageStatic("0030000A"));
        }
        return Result.ok(methods);
    }

    @Override
    public Result<MethodChangeCheckVO> checkMethodParam(TimeSweepMethodParamsDto methodParam) {

        if (!StringUtils.hasText(methodParam.getCurrentMethodId())) {
            //请选择当前方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000023"));
        }

        if (methodParam.getTaskId()==null||!StringUtils.hasText(methodParam.getTaskId())) {
            return Result.fail(YamlMessageSource.getMessageStatic("00101006"));
        }

        TimeSweepParamter newMethodParam = new TimeSweepParamter();
        BeanUtils.copyProperties(methodParam, newMethodParam);

        R<Boolean> r = checkParam(newMethodParam);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }

        double minWaveLength = 190.0;
        double maxWaveLength = 1100.0;

        if (StringUtils.hasText(methodParam.getDeviceSerialNumber())) {
            R<DeviceMeasureVO> typeInfo = deviceInfoService.queryDeviceTypeInfo(methodParam.getDeviceSerialNumber());
            if (typeInfo.getSuccess()) {
                DeviceMeasureVO vo = typeInfo.getData();
                if (vo != null) {
                    minWaveLength = StringUtils.hasText(vo.getMinWavelength()) ? Double.parseDouble(vo.getMinWavelength()) : minWaveLength;
                    maxWaveLength = StringUtils.hasText(vo.getMaxWavelength()) ? Double.parseDouble(vo.getMaxWavelength()) : maxWaveLength;
                }
            }
        }
        double scanWl = Double.parseDouble(newMethodParam.getWaveLength());

        if (scanWl < minWaveLength || scanWl > maxWaveLength) {
            return Result.fail(YamlMessageSource.getMessageStatic("00102006",scanWl , minWaveLength, maxWaveLength));
        }
        // 校验小数点后最多1位
        String[] parts = String.valueOf(scanWl).split("\\.");
        if (parts.length > 1 && parts[1].length() > 1) {
            //"波长值" + waveStr + "小数点后最多保留1位"
            return Result.fail(YamlMessageSource.getMessageStatic("00105005", scanWl));
        }

        MethodChangeCheckVO vo = new MethodChangeCheckVO();
        vo.setCurrentMethodId(methodParam.getCurrentMethodId());
        vo.setParamChange(false);
        vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
        vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());


        TMeasureMethod currentMethod = tMeasureMethodService.getById(methodParam.getCurrentMethodId());
        if (StringUtils.hasText(currentMethod.getProjectItemKey()) && currentMethod.getProjectItemKey().equals(defaultMethodProjectName)) {
            log.info("生成新方法");
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.newMethod.getMessage());
            vo.setClearDataRange(RationChangeMethodTypeEnums.newMethod.getIdentity());
            return Result.ok(vo);
        }

        TimeSweepParamter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), TimeSweepParamter.class);

        String dbJson = JSON.toJSONString(oldParam, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newMethodParam, SerializerFeature.SortField);

        if (dbJson.equals(frontJson)) {
            vo.setParamChange(false);
            vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
            vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());
            return Result.ok(vo);
        }
        //检查测量数据
        boolean b = checkTimeSweepParam(oldParam, newMethodParam);
        System.out.println(b);
        if (!b) {
            log.info("测量参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.measure.getMessage());
            return Result.ok(vo);
        }

        //检查分析参数
        boolean analyze = checkTimeSweepParam(oldParam, newMethodParam);
        if (!analyze) {
            log.info("分析参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.analyze.getMessage());
        }
        return Result.ok(vo);
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
            //没有此方法
            return Result.fail(YamlMessageSource.getMessageStatic("00305005"));
        }
        if (method.getMeasurementParamInfo()==null||method.getMeasurementParamInfo()==""){
            //此方法没有参数
            return Result.fail(YamlMessageSource.getMessageStatic("00305006"));
        }
        // 将方法参数转换成对象拷贝到QueryTimeMethodParamterVo中
        QueryTimeMethodParamterVo timeSweepParamter = JSON.parseObject(method.getMeasurementParamInfo(), QueryTimeMethodParamterVo.class);
        timeSweepParamter.setMethodId(methodId);
        timeSweepParamter.setMethodName(method.getMethodName());
        return Result.ok(timeSweepParamter);
    }

    /**
     * 修改方法参数
     * @param paramDto
     * @return
     */
    @Override
    public Result updateMethodParameter(TimeSweepMethodParameterDto paramDto) {
        double response = Double.parseDouble(paramDto.getResponseTime());
        double interval = Double.parseDouble(paramDto.getIntervalTime());
        if (response >= interval) {
            return Result.fail("响应时间必须小于时间间隔");
        }
        SetParam setParam = new SetParam();
        setParam.setPhotoMode(Integer.valueOf(paramDto.getLuminosity()));
        setParam.setSetWl(Double.valueOf(paramDto.getWaveLength()));
        if (paramDto.getMethodName()==null) {
            return Result.fail("方法名称不能为空");
        }
        boolean b = setInstrumentParameters(paramDto.getDeviceSerialNumber(), setParam);
        if (!b) {
            log.error("下发参数失败,请查看是否连接仪器");
        }
        // 使用isEmpty判断，如果为空，则返回失败
        if (paramDto.getUpdateBy().isEmpty()){
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        SysUserPO sysUserPO = systemUserManageDAO.selectByUsername(paramDto.getUpdateBy());

        // 判断参数是否发生变化，如果没有变化，直接返回，不再执行后续操作
        TMeasureMethod currentMethod = tMeasureMethodService.getById(paramDto.getMethodId());
        if (currentMethod == null) {
            return Result.fail(YamlMessageSource.getMessageStatic("0030100C"));
        }
        // 1. 获取数据库中的旧参数
        TimeSweepParamter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), TimeSweepParamter.class);

        // 2. 获取传入的新参数
        TimeSweepParamter newParam = new TimeSweepParamter();
        BeanUtils.copyProperties(paramDto, newParam);

        // 3. 比较参数
        if (compareParam(oldParam, newParam)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00104001"));
        }

        boolean methodBoolean = false;
        // 比较方法名称跟查询的方法的方法名称是否一样
        if (!currentMethod.getMethodName().equals(paramDto.getMethodName())) {
            methodBoolean = true;
        }

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task.getTaskStatus().equals(TaskStatusEnum.UNTESTED.getCode())) {
            TMeasureMethod newMethod = createNewMethod(paramDto , methodBoolean);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail(YamlMessageSource.getMessageStatic("0010000A"));
            }
            task.setMethodKey(newMethod.getId());
            task.setCreateTime(LocalDateTime.now());
            task.setTaskName(paramDto.getTaskName());
            boolean update = tMeasureTaskService.updateById(task);
            return update?Result.ok():Result.fail(YamlMessageSource.getMessageStatic("00000048"));
        }else{
            TMeasureMethod newMethod = createNewMethod(paramDto , methodBoolean);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail(YamlMessageSource.getMessageStatic("0010000A"));
            }
            TMeasureTask newTask = createNewTask(paramDto, newMethod.getId(), task, sysUserPO.getId());
            if (newTask == null){
                return Result.fail(YamlMessageSource.getMessageStatic("00000010"));
            }
            return Result.ok();
        }

    }

    /**
     * 获取样本列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result sampleListPort(String taskId,String type,Integer digit,String deviceSerialNumber) {
        // 先对前端传过来的数据进行校验
        if (taskId == null || taskId.equals("")) {
            //任务id不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000003"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<SampleListVo> listVos = new ArrayList<>();
        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<SamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        SamplePoolConfigPO configPO = samplePoolConfigService.getOne(wrapper);
        Boolean isAutoChangePool;
        if (configPO == null) {
            isAutoChangePool=false;
        }else{
            isAutoChangePool= configPO.getIsAutoChangePool();
        }
        if (taskSpecimen != null) {
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getDeleteType().equals("1") && type == null) {
                    continue;
                }
                SampleListVo vo = new SampleListVo();
                TMeasureSample sample = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    //样品不存在
                    return Result.fail(YamlMessageSource.getMessageStatic("0000000B"));
                }
                QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("task_id", task.getId());
                queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                queryWrapper.eq("analysis_type", "5");
                queryWrapper.orderByDesc("analysis_number");
                if (type == null) {
                    queryWrapper.eq("is_delete", "0");
                }
                queryWrapper.last("limit 1");
                TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);
                if (measureData == null ||taskSpeciman.getIsTested().equals("0")&& measureData.getAnalysisResult() == null) {
                    vo.setTitle(sample.getSampleName());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisId(null);
                    vo.setAnalysisNumber(1);
                    if (isAutoChangePool){
                        vo.setSampleLocation(sample.getSampleLocation());
                    }else{
                        vo.setSampleLocation(null);
                    }
                    vo.setColor(null);
                    vo.setData(null);
                    vo.setDeleteType("0");
                } else {
                    vo.setTitle(sample.getSampleName());
                    vo.setAnalysisId(measureData.getId());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    if (isAutoChangePool){
                        vo.setSampleLocation(sample.getSampleLocation());
                    }else{
                        vo.setSampleLocation(null);
                    }
                    vo.setAnalysisNumber(measureData.getAnalysisNumber());
                    vo.setColor(measureData.getColor());
                    if (measureData.getAnalysisResult() == null) {
                        vo.setData(null);
                        vo.setOriginalData(null);
                    } else {
                        vo.setData(getSampleData(measureData, digit));
                        vo.setOriginalData(getOriginalSampleData(measureData, digit));
                    }
                    vo.setDeleteType(taskSpeciman.getDeleteType());
                }
                vo.setType("0");
                listVos.add(vo);
            }
        }
        return Result.ok(listVos);
    }

    /**
     * 删除样品列表接口
     * @param sampleId
     * @param taskId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String sampleId, String taskId) {
        // 获取任务数据

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : taskSpecimenList) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setDeleteType("1");
                break;
            }
        }
        // 根据样品id和任务id去查询分析数据id进行删除
        QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.eq("sample_key", sampleId);
        queryWrapper.eq("analysis_type", "5");
        queryWrapper.eq("is_delete", "0");
        queryWrapper.orderByDesc("analysis_number");
        queryWrapper.last("limit 1");
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);
        if (measureData == null) {
            //无测量数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000004A"));
        }
        measureData.setIsDelete("1");
        boolean update1 = tMeasureAnalysisDataService.updateById(measureData);
        task.setSampleKey(JSON.toJSONString(taskSpecimenList));
        boolean update = tMeasureTaskService.updateById(task);
        if ( update1 && update) {
            //删除成功
            return Result.ok(YamlMessageSource.getMessageStatic("00100022"));
        } else {
            //删除失败
            return Result.fail(YamlMessageSource.getMessageStatic("00100008"));
        }
    }

    /**
     * 获取变换列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result changeTheListPort(String taskId,String type, Integer digit) {

        // 根据任务id去查询分析数据
        QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.ne("analysis_type", "5");
        if(type==null||type.equals("")){
            queryWrapper.eq("is_delete", "0");
        }
        List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(queryWrapper);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TMeasureAnalysisData tMeasureAnalysisData : list) {
            SampleListVo vo = new SampleListVo();
            vo.setAnalysisId(tMeasureAnalysisData.getId());
            vo.setTitle(tMeasureAnalysisData.getAnalysisName());
            vo.setColor(tMeasureAnalysisData.getColor());
            vo.setDeleteType(tMeasureAnalysisData.getIsDelete());
            List<String> results = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
            List<PeakValleyMarking> gauges = JSON.parseArray(tMeasureAnalysisData.getGaugePoint(), PeakValleyMarking.class);
            List<PeakValleyMarking> peaks = JSON.parseArray(tMeasureAnalysisData.getPeak(), PeakValleyMarking.class);
            List<PeakValleyMarking> grades = JSON.parseArray(tMeasureAnalysisData.getGrain(), PeakValleyMarking.class);
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
                    for (PeakValleyMarking gauge : gauges) {
                        if (split[0].equals(gauge.getX())){
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
                    for (PeakValleyMarking peak : peaks) {
                        if (split[0].equals(peak.getX())){
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
                    for (PeakValleyMarking grade : grades){
                        if (split[0].equals(grade.getX())){
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
            vo.setOriginalData(resultList);
            vo.setType("1");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    /**
     * 删除变换列表接口
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        TMeasureAnalysisData  byId = tMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
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
     * 恢复已删除数据接口
     */
    @Override
    public Result restoreDeletedListData(String taskId,String sampleId, String type,String analysisId) {
        // 根据类型判断是样品数据还是变换数据
        if (type.equals("0")) {
            // 根据任务id查询任务数据
            TMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                //没有找到该任务
                return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
            }
            List<TaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen taskSpecimen : sampleList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("0");
                    break;
                }
            }
            task.setSampleKey(JSON.toJSONString(sampleList));
            boolean update1 = tMeasureTaskService.updateById(task);
            // 根据分析id查询分析数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
            }
            byId.setIsDelete("0");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            if (update1 && update){
                return Result.ok(true);
            }else{
                return Result.fail(false);
            }
        }else{
            // 根据变换数据id查询变换数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //变换数据不存在
                return Result.fail(YamlMessageSource.getMessageStatic("0030000B"));
            }
            byId.setIsDelete("0");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            if (update){
                return Result.ok(update);
            }else{
                return Result.fail(false);
            }
        }
    }

    /**
     * 设置方法参数接口
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result setMethodParameters(String methodId, String deviceSerialNumber) {
        if (methodId == null || methodId.equals("")){
            return Result.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        // 根据methodId去方法表中查询数据
        TMeasureMethod tMeasureMethod = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id", methodId).eq("measurement_type", MeasureTypeEnum.TIME_SCAN.getDescription()));
        if (tMeasureMethod == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        TimeSweepParamter timeSweepParamter = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), TimeSweepParamter.class);

        // 设置参数
        DevOperationInputParam param = new DevOperationInputParam();
        param.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam setParam = new SetParam();
        param.setDeviceSerialNumber(deviceSerialNumber);

        // 1. 光度模式
        setParam.setPhotoMode(Integer.valueOf(timeSweepParamter.getLuminosity()));
        // 2. 光谱带宽
        if (StringUtils.hasText(timeSweepParamter.getSpectralBandwidth())&&deviceInfoService.isNeedSetSit(deviceSerialNumber)) {
            setParam.setSlit1(Double.valueOf(timeSweepParamter.getSpectralBandwidth()));
        }
        // 3. 响应时间
        setParam.setResponseTime(Double.valueOf(timeSweepParamter.getResponseTime()));
        // 4. 设置波长
        setParam.setSetWl(Double.valueOf(timeSweepParamter.getWaveLength()));
        // 5. 换灯波长
        setParam.setChangeLampWl(Double.valueOf(timeSweepParamter.getChangeLampWaveLength()));
        if (timeSweepParamter.getLuminosity().equals("3") || timeSweepParamter.getLuminosity().equals("4")) {
            // 6. 能量灯
            setParam.setLampPos(Integer.valueOf(timeSweepParamter.getEnergyLamp()));
            // 7. 增益
            setParam.setGain(Integer.valueOf(timeSweepParamter.getGain()));
        }
        param.setParam(setParam);
        R<OperationResultEntity> r = devOperationUtilService.operation(param);
        if (r.getSuccess()) {
            DetectionConstants.currentWavelength = timeSweepParamter.getWaveLength();
            return Result.ok(true);
        } else {
            return Result.fail(r.getMessage());
        }
    }

    /**
     * 校零接口
     * @param taskId
     * @param serialNumber
     * @param methodId
     * @return
     */
    @Override
    public Result zero(String taskId,String serialNumber, String methodId) {
        if (serialNumber == null || serialNumber.equals("")){
            //缺少设备序列号
            return Result.fail(YamlMessageSource.getMessageStatic("00305007"));
        }
        if (methodId == null || methodId.equals("")){
            //缺少方法id
            return Result.fail(YamlMessageSource.getMessageStatic("00305008"));
        }
        TMeasureMethod methodData = tMeasureMethodService.getById(methodId);
        if (methodData == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        TimeSweepParamter timeSweepParamter = JSON.parseObject(methodData.getMeasurementParamInfo(), TimeSweepParamter.class);
        if (timeSweepParamter.getWaveLength() == null || timeSweepParamter.getWaveLength().isEmpty()) {
            //缺少波长
            return Result.fail(YamlMessageSource.getMessageStatic("0000001E"));
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
        R<ChangeParam> zero = null;
        try {
            zero = devMessageUtil.zero(command, ip);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!zero.getSuccess()) {
            return Result.fail(zero.getMessage());
        }
        TMeasureTask task =null;
        if (taskId != null && !taskId.equals("")){
            task = tMeasureTaskService.getById(taskId);
        }else{
            task = new TMeasureTask();
            TMeasureMethod method = tMeasureMethodService.getById(methodId);
            SysUserPO user = systemUserManageDAO.selectOne(new QueryWrapper<>(SysUserPO.class).eq("user_name", method.getMethodCreator()));
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
            TMeasureTaskAttachment taskAttachment = new TMeasureTaskAttachment();
            taskAttachment.setTaskId(task.getId());
            boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
        }
        ChangeParam changeParam = zero.getData();
        changeParam.setWl(timeSweepParamter.getWaveLength());
        task.setZeroParam(JSON.toJSONString(changeParam));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(task.getId());
    }

    /**
     * 获取样本按钮列表接口
     * @param taskId
     * @return
     */
    @Override
    public Result querySampleList(String taskId) {
//        // 根据任务id查询任务数据
//        TMeasureTask task = tMeasureTaskService.getById(taskId);
//        if (task == null) {
//            return Result.ok(new ArrayList<>());
//        }
//        if (task.getSampleKey() == null) {
//            return Result.ok(null);
//        }
//        // 分割任务中的样品数据
//        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
//        List<Sample> sampleList = new ArrayList<>();
//        if (taskSpecimen == null || taskSpecimen.size() == 0) {
//            return Result.ok(new ArrayList<>());
//        }
//        for (TaskSpecimen taskSpeciman : taskSpecimen) {
//            if (taskSpeciman.getDeleteType().equals("0")){
//                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
//                Sample sample = new Sample();
//                sample.setSampleId(byId.getId());
//                sample.setSampleName(byId.getSampleName());
//                sample.setSampleNumber(byId.getSampleNumber());
//                sample.setSampleLocation(byId.getSampleLocation());
//                sampleList.add(sample);
//            }
//        }
//        return Result.ok(sampleList);
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                SampleVO vo = new SampleVO();
                BeanUtils.copyProperties(measureSample, vo);
                vo.setSampleId(measureSample.getId());
                String jsonString = JSONObject.toJSONString(vo);

                JSONObject jsonObject = JSONObject.parseObject(jsonString);
                if (StringUtils.hasText(measureSample.getNewColData())) {
                    JSONObject object = JSONObject.parseObject(measureSample.getNewColData());
                    jsonObject.putAll(object);
                }
                sampleVOList.add(jsonObject);

            }
        }
        return Result.ok(sampleVOList);
    }

    /**
     * 查询样品列表头
     * @param taskId
     * @return
     */
    @Override
    public Result<List<HeadVO>> querySampleHeads(String taskId) {
        List<HeadVO> ans = new ArrayList<>();

        if (!StringUtils.hasText(taskId)) {
            return Result.ok(ans);
        }



        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(task.getTableInfo())) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
            return Result.ok(preHead);
        } else {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
                return Result.ok(preHead);
            }
        }
    }

    /**
     * 添加或删除样本接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto) {

        if (!StringUtils.hasText(paramDto.getMethodId())) {
            //请先创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000011"));
        }
        if(paramDto.getNewSamples() == null) {
            paramDto.setNewSamples(new ArrayList<>());
        }
        if (paramDto.getDelSampleIds() == null) {
            paramDto.setDelSampleIds(new ArrayList<>());
        }
        if (paramDto.getNewSamples().isEmpty() && paramDto.getDelSampleIds().isEmpty()) {
            //没有要新增或删除的样品！
            return Result.ok(YamlMessageSource.getMessageStatic("00305009"));
        }

        if (paramDto.getHeads() != null && paramDto.getHeads().size() > 10) {
            //表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000013"));
        }


        if (paramDto.getNewSamples()!=null&&!paramDto.getNewSamples().isEmpty()) {
            for (Map<String, String> map : paramDto.getNewSamples()) {
                String jsonString = JSON.toJSONString(map);
                SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
                if (sample.getSampleNumber()!=null&&sample.getSampleNumber().length()>256){
                    return Result.fail("新增样品编号超出限制");
                }
                if (sample.getSampleName()!=null&&sample.getSampleName().length()>256){
                    return Result.fail("新增样品名称超出限制");
                }

            }
        }
        if (!StringUtils.hasText(paramDto.getTaskId())) {
            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
            SysUserPO po = systemUserManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, paramDto.getUpdateBy()));
            String id = po.getId();
            newTask.setUserId(id);
            newTask.setProjectItemKey(method.getProjectItemKey());
            newTask.setMethodKey(method.getId());
            newTask.setMeasureType(method.getMeasurementType());
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());

            newTask.setTaskName(method.getMeasurementType() + "task_" + collect);
            newTask.setCreateTime(LocalDateTime.now());
            boolean save = false;
            for (int i = 0; i < 3; i++) {
                if (tMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                //系统出错
                return Result.fail(YamlMessageSource.getMessageStatic("00000014"));
            }
            paramDto.setTaskId(newTask.getId());
        }


        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        /// 校验是否有重复的样品编号
        // 取出所有样品编号
        List<String> sampleNos = paramDto.getNewSamples().stream()
                .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleNumber()) // 取出 sampleNo
                .filter(Objects::nonNull)    // 过滤掉空值
                .collect(Collectors.toList());
        // 查重：找到重复的样品编号
        Set<String> duplicates = sampleNos.stream()
                .filter(no -> Collections.frequency(sampleNos, no) > 1)
                .collect(Collectors.toSet());
        // 判断如果isTested为true，
        if (!duplicates.isEmpty()) {
            //样品编号重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000003F"));
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (paramDto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId())
                        .orderByDesc(TMeasureData::getSerialNumber);
                List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    TMeasureData one = list.get(0);
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }

        for (Map<String, String> map : paramDto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (paramDto.getHeads() != null) {

                newSample.setHeads(JSON.toJSONString(paramDto.getHeads()));

                JSONObject newColData = new JSONObject();

                for (HeadVO head : paramDto.getHeads()) {
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                log.info("新增的样品：{}", newSample);
                tMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                taskSpecimen.add(specimen);
            }
        }

        // 更新任务中的样品信息
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);
        boolean b = tMeasureTaskService.updateById(task);
        if (!b) {
            return Result.fail(YamlMessageSource.getMessageStatic("00100003"));
        }

        // 处理样品池自动切换
        if (StringUtils.hasText(paramDto.getDeviceSerialNumber())) {
            R<FSamplePoolInfoVO> poolInfo = samplePoolConfigService.querySamplePoolInfo(paramDto.getDeviceSerialNumber());
            if (!poolInfo.getSuccess()) {
                return Result.ok();
            }
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            Boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool();
            if (isAutoChangePool) {
                List<Integer> usefulLocation = samplePoolConfigService.getUsefulLocation(paramDto.getDeviceSerialNumber());
                concisePublicUtilService.changeSampleLocationTwo(task.getId(), usefulLocation);
            }
        }
        return Result.ok(tMeasureTaskService.updateById(task));
    }

    /**
     * 图片上传
     * @param upload
     * @return
     */
    @Override
    public Result uploadImage(SpectrumImageUpload upload) {
        String[] split = upload.getImage().split(",");
        String s = split[1];
        String fileType = null;
        if (split[0].startsWith("data:image/png")) {
            fileType = "png";
        } else if (split[0].startsWith("data:image/jpeg")) {
            fileType = "jpg";
        } else if (split[0].startsWith("data:image/gif")) {
            fileType = "gif";
        } else {
            fileType = "unknown"; // 兜底
        }
        byte[] bytes = Base64.decodeBase64(s);
        if (upload.getImageType().equals("0")){
            QueryWrapper<TMeasureAnalysisImage> analysisId = new QueryWrapper<TMeasureAnalysisImage>().eq("analysis_id", upload.getId());
            analysisId.last("limit 1");
            TMeasureAnalysisImage byId = tMeasureAnalysisImageService.getOne(analysisId);
            if (byId == null){
                //图片不存在
                return Result.fail(YamlMessageSource.getMessageStatic("00000040"));
            }
            byId.setImageType(fileType);
            byId.setImageBase64(bytes);
            // 将当前日期时间存储
            byId.setCreateTime(LocalDateTime.now());
            boolean update = tMeasureAnalysisImageService.updateById(byId);
            return Result.ok(update);
        }else{
            // 根据任务id查询任务附属表的内容
            TMeasureTaskAttachment byId = tMeasureTaskAttachmentService.getOne(Wrappers.lambdaQuery(TMeasureTaskAttachment.class).eq(TMeasureTaskAttachment::getTaskId, upload.getId()).last("limit 1"));
            if (byId == null){
                //任务附属不存在
                return Result.fail(YamlMessageSource.getMessageStatic("00000041"));
            }
            if ("0".equals(upload.getType())){
                byId.setOverlapImage(bytes);
            }else if ("1".equals(upload.getType())){
                byId.setTiledImage(bytes);
            }else if ("2".equals(upload.getType())){
                byId.setAddrImage(bytes);
            }else{
                //Type类型错误
                return Result.fail(YamlMessageSource.getMessageStatic("00000042"));
            }
            boolean update = tMeasureTaskAttachmentService.updateById(byId);
            return Result.ok(update);
        }
    }

    /**
     * 查询峰谷数据列表接口
     * @param dataId
     * @param digit
     * @return
     */
    @Override
    public Result queryPeakValleyDataList(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<PeakValleyMarking> peaks = new ArrayList<>();
        List<PeakValleyMarking> grains = new ArrayList<>();
        List<PeakValleyMarking> dataAll = new ArrayList<>();
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
                    data.setY(value.toPlainString());
                }
            }
        }
        return Result.ok(dataAll);
    }

    /**
     * 寻峰寻谷接口
     * @param paramDto
     * @return
     */
    @Override
    public Result seekingPeaksAndValleysPort(SeekingPeaksValleysDto paramDto) {
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        if (paramDto.getDataId() == null || paramDto.getDataId().equals("")) {
            //数据ID不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00100011"));
        }
        if (paramDto.getHeight() == null) {
            paramDto.setHeight(confVO.getPeakValleyDiff());
        }
        if (paramDto.getDistance() == null || paramDto.getDistance().isEmpty()) {
            paramDto.setDistance(confVO.getPeakInterval());
        }
        double distance = Double.parseDouble(paramDto.getDistance());
        int i1 = (int) distance;
        if (i1 < 1) {
            //峰谷间距不可以小于1
            return Result.fail(YamlMessageSource.getMessageStatic("00100012"));
        }
        Double height = Double.valueOf(paramDto.getHeight());
        if (height> 100) {
            return Result.fail(YamlMessageSource.getMessageStatic("00100027"));
        }
        paramDto.setHeight(String.valueOf(height / 100));
        TMeasureAnalysisData tMeasureAnalysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureAnalysisData==null){
            //测量数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0000003B"));
        }
        List<String> list = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
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
        Map<String, Object> resultMap;
        try {
            resultMap = restTemplate.postForEntity(pythonPathConstant.getPeakSearchingAndValleySearching(), request, Map.class).getBody();
        } catch (Exception e){
            return Result.fail("算法调用失败,请联系管理员");
        }
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        if(xPeak==null||yPeak==null){
            //height参数太高
            return Result.fail(YamlMessageSource.getMessageStatic("0000003C"));
        }
        for (int i = 0; i < xPeak.size(); i++) {
            PeakValleyMarking marking = new PeakValleyMarking();
            marking.setX(xPeak.get(i).toString());
            marking.setY(yPeak.get(i));
            marking.setType("波峰");
            peakData.add(marking);
        }
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> xValley = (List<Double>) resultMap.get("valley_x");
        List<String> yValley = (List<String>) resultMap.get("valley_y");
        for (int i = 0; i < xValley.size(); i++) {
            PeakValleyMarking marking = new PeakValleyMarking();
            marking.setX(xValley.get(i).toString());
            marking.setY(yValley.get(i));
            marking.setType("波谷");
            valleyData.add(marking);
        }
        Boolean flag;
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData==null){
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        analysisData.setPeak(JSON.toJSONString(peakData));
        analysisData.setGrain(JSON.toJSONString(valleyData));
        flag=tMeasureAnalysisDataService.updateById(analysisData);
        //数据保存失败
        return flag?Result.ok():Result.fail(YamlMessageSource.getMessageStatic("00000034"));
    }

    /**
     * 添加峰谷备注接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }
//        if (paramDto.getRemark()==null){
//            return Result.fail("请先添加备注");
//        }
        // 根据测量id去分析数据表中进行查询
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData==null){
            //测量数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0000003B"));
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
        boolean update = tMeasureAnalysisDataService.updateById(tMeasureData);
        return update?Result.ok():Result.fail();
    }

    /**
     * 获取标记点数据列表
     * @param dataId
     * @param digit
     * @return
     */
    @Override
    public Result queryGaugePointDataList(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<PeakValleyMarking> gaugePoints = new ArrayList<>();
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
    }

    /**
     * 添加标记点接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePoint(AddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            //请选择标记点
            return Result.fail(YamlMessageSource.getMessageStatic("0000003E"));
        }
        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        // 波长范围校验
        Result validateResult = validateGaugePointsRange(paramDto.getGaugePoints(), analysisData);
        if (validateResult != null) {
            return validateResult; // 校验失败，返回错误信息
        }

        gaugePointJson = analysisData.getGaugePoint();
        rawData = JSON.parseArray(analysisData.getAnalysisResult(), String.class);


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
        TMeasureAnalysisData analysisData1 = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        analysisData1.setGaugePoint(updatedJson);
        boolean update = tMeasureAnalysisDataService.updateById(analysisData1);
        return update?Result.ok(true):Result.fail(false);
    }

    /**
     * 添加标记点备注接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }
        if (paramDto.getRemark()==null){
            //请先添加备注
            return Result.fail(YamlMessageSource.getMessageStatic("00000046"));
        }
        // 根据测量id去分析数据表中进行查询
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData==null){
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
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
        boolean update = tMeasureAnalysisDataService.updateById(tMeasureData);
        return update?Result.ok():Result.fail();
    }

    /**
     * 更新数据颜色接口
     * @param paramDto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(UpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }

        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;


        // 分析数据
        TMeasureAnalysisData analysis = tMeasureAnalysisDataService.getById(dataId);
        if (analysis == null) {
            //样品数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000047"));
        }
        analysis.setColor(color);
        updated = tMeasureAnalysisDataService.updateById(analysis);


        if (!updated) {
            //更新失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000048"));
        }

        return Result.ok();
    }

    /**
     * 获取报表内容数据
     * @param taskId
     * @return
     */
    @Override
    public Result gainStatementData(String taskId) {
        return null;
    }

    /**
     * 导出Word接口
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
     * 查询运算源数据接口
     * @param taskId
     * @return
     */
    @Override
    public Result queryArithmeticSourceData(String taskId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey() == null || task.getSampleKey().equals("[]")) {
            return Result.ok(new ArrayList<>());
        }
        // 根据任务的方法key查询方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String, String>> list = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(new QueryWrapper<TMeasureAnalysisData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", taskSpeciman.getSpecimenKey())
                        .eq("analysis_type", "5")
                        .eq("is_delete", "0")
                        .eq("analysis_number", 1)
                        .last("limit 1"));
                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (one == null) {
                    one = new TMeasureAnalysisData();
                }
                if (one.getAnalysisResult() != null) {
                    Map<String, String> map = new HashMap<>();
                    map.put("measureId", one.getId());
                    map.put("sampleName", byId.getSampleName());
                    list.add(map);
                }
            }
        }

        return Result.ok(list);
    }

    /**
     * 四则运算接口
     * @param paramDto
     * @return
     */
    @Override
    public Result fourOperationsPort(FourOperationsParamDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 对所有传过来的对象数据进行校验,判断是否为null或""
        if (paramDto.getMeasureDataId1() == null || paramDto.getMeasureDataId1().equals("")) {
            //请选择测量数据1
            return Result.fail(YamlMessageSource.getMessageStatic("0000002E"));
        }
        if (paramDto.getMeasureDataId2() == null || paramDto.getMeasureDataId2().equals("")) {
            //请选择测量数据2
            return Result.fail(YamlMessageSource.getMessageStatic("0000002F"));
        }
        if (paramDto.getArithmeticOperators() == null || paramDto.getArithmeticOperators().equals("")) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000030"));
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //请选择数据类型
            return Result.fail(YamlMessageSource.getMessageStatic("00000031"));
        }
        // 根据测量数据id查询测量数据
        TMeasureAnalysisData measureData1 = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId1());
        List<String> xList = new ArrayList<>();
        List<String> yList1 = new ArrayList<>();
        List<String> yList2 = new ArrayList<>();
        List<String> list = JSON.parseArray(measureData1.getAnalysisResult(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList1.add(split[1]);
        }
        if (paramDto.getDataType().equals("0")) {
            for (int i = 0; i < yList1.size(); i++) {
                yList2.add(String.valueOf(paramDto.getMeasureDataId2()));
            }
        } else if (paramDto.getDataType().equals("1")) {
            TMeasureAnalysisData measureData2 = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId2());
            // 将测量数据1和2的测量数据提取出来
            List<String> list2 = JSON.parseArray(measureData2.getAnalysisResult(), String.class);
            for (String s : list2) {
                String[] split = s.split(",");
                yList2.add(split[1]);
            }
        }
        // 创建请求体
        Map<String, Object> body = new HashMap<>();
        // 将参数添加到请求体中
        body.put("x_data", xList);
        body.put("y1_data", yList1);
        body.put("y2_data", yList2);
        body.put("operation", paramDto.getArithmeticOperators());

        Map<String, Object> resultMap;
        try {
            resultMap = callPythonService(
                    pythonPathConstant.getSpectrumFourOperation(),
                    body
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        // 对结果进行寻峰计算
        List<Double> x_data = (List<Double>) resultMap.get("x_data");
        List<Double> y_data = (List<Double>) resultMap.get("y_data");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", x_data);
        body1.put("y_data", y_data);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());

        Map<String, Object> resultMap1;
        try {
            resultMap1 = callPythonService(
                    pythonPathConstant.getPeakSearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Map<String, Object> resultMap2;
        try {
            resultMap2 = callPythonService(
                    pythonPathConstant.getValleySearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
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
        tMeasureAnalysisData.setIsSave(Constants.PARAM_ZERO);
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save) {
            //保存失败
            return Result.fail(YamlMessageSource.getMessageStatic("00300008"));
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < x_data.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", x_data.get(i));
            map.put("y", y_data.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 高级运算接口
     * @param paramDto
     * @return
     */
    @Override
    public Result advancedOperationsPort(AdvancedOperationsDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // 根据任务中的方法key查询方法并且提取方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        // 提取方法参数
        FTimeSweepParamter timeSweepParamter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
        if (timeSweepParamter == null) {
            //方法参数错误
            return Result.fail(YamlMessageSource.getMessageStatic("00105017"));
        }
        // 判断光度模式是否是1
        if (timeSweepParamter.getLuminosity().equals("1")) {
            //Abs光度模式不可使用高级运算
            return Result.fail(YamlMessageSource.getMessageStatic("00100015"));
        }

        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 将所有传过来的数据校验是否为空是否是""
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail(YamlMessageSource.getMessageStatic("00000035"));
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //请选择变换类型
            return Result.fail(YamlMessageSource.getMessageStatic("00000033"));
        }
        // 根据测量数据id查询测量数据
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("transform_type", Integer.valueOf(paramDto.getDataType()));
        body.put("x_data", xList);
        Map<String, Object> resultMap;
        try {
            resultMap = callPythonService(
                    pythonPathConstant.getSpectrumTransformation(),
                    body
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        Map<String, Object> resultMap1;
        try {
            resultMap1 = callPythonService(
                    pythonPathConstant.getPeakSearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Map<String, Object> resultMap2;
        try {
            resultMap2 = callPythonService(
                    pythonPathConstant.getValleySearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
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
        tMeasureAnalysisData.setIsSave(Constants.PARAM_ZERO);
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save) {
            //数据保存失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000034"));
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xList.get(i));
            map.put("y", y_data.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 平滑接口
     * @param paramDto
     * @return
     */
    @Override
    public Result smoothnessPort(SmoothnessDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        // 将所有参数校验是否为空或""
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail(YamlMessageSource.getMessageStatic("00000035"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        if (paramDto.getCoefficient() == null || paramDto.getCoefficient().equals("")) {
            //请输入系数
            return Result.fail(YamlMessageSource.getMessageStatic("00000038"));
        }
        if (paramDto.getPointNum() == null || paramDto.getPointNum().equals("")) {
            //请输入点数
            return Result.fail(YamlMessageSource.getMessageStatic("00000039"));
        }
        if (Double.valueOf(paramDto.getCoefficient()) > Double.valueOf(paramDto.getPointNum())) {
            //系数不能大于点数
            return Result.fail(YamlMessageSource.getMessageStatic("0000003A"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据测量数据的id查询出测量数据
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(tMeasureData.getAnalysisResult(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("window_length", paramDto.getPointNum());
        body.put("polyorder", paramDto.getCoefficient());
        Map<String, Object> resultMap;
        try {
            resultMap = callPythonService(
                    pythonPathConstant.getSmoothed(),
                    body
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
        List<Double> smoothedY = (List<Double>) data.get("smoothed_y");
        Map<String, Object> body1 = new HashMap<>();
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        body1.put("x_data", xList);
        body1.put("y_data", smoothedY);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        Map<String, Object> resultMap1;
        try {
            resultMap1 = callPythonService(
                    pythonPathConstant.getPeakSearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Map<String, Object> resultMap2;
        try {
            resultMap2 = callPythonService(
                    pythonPathConstant.getValleySearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
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
        tMeasureAnalysisData.setIsSave(Constants.PARAM_ZERO);
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save) {
            //数据保存失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000034"));
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xList.get(i));
            map.put("y", smoothedY.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 微分接口
     * @param paramDto
     * @return
     */
    @Override
    public Result differentialPort(DifferentialDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        // 将所有参数校验是否为空或""
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail(YamlMessageSource.getMessageStatic("00000035"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        if (paramDto.getCoefficient() == null || paramDto.getCoefficient().equals("")) {
            //请输入系数
            return Result.fail(YamlMessageSource.getMessageStatic("00000038"));
        }
        if (paramDto.getPointNum() == null || paramDto.getPointNum().equals("")) {
            //请输入点数
            return Result.fail(YamlMessageSource.getMessageStatic("00000039"));
        }
        if (Double.valueOf(paramDto.getCoefficient()) > Double.valueOf(paramDto.getPointNum())) {
            //系数不能大于点数
            return Result.fail(YamlMessageSource.getMessageStatic("0000003A"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据测量数据的id查询出测量数据
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(tMeasureData.getAnalysisResult(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("window_length", paramDto.getPointNum());
        body.put("polyorder", paramDto.getCoefficient());
        body.put("deriv", paramDto.getDifferentialNum());
        Map<String, Object> resultMap;
        try {
            resultMap = callPythonService(
                    pythonPathConstant.getDifferentiated(),
                    body
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
        List<Double> differentiatedY = (List<Double>) data.get("differentiated_y");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", differentiatedY);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        Map<String, Object> resultMap1;
        try {
            resultMap1 = callPythonService(
                    pythonPathConstant.getPeakSearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Map<String, Object> resultMap2;
        try {
            resultMap2 = callPythonService(
                    pythonPathConstant.getValleySearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
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
        tMeasureAnalysisData.setIsSave(Constants.PARAM_ZERO);
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save) {
            //数据保存失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000034"));
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xList.get(i));
            map.put("y", differentiatedY.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 光度模式转换
     * @param paramDto
     * @return
     */
    @Override
    public Result luminanceModeConversionPort(LuminosityConversionDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail(YamlMessageSource.getMessageStatic(YamlMessageSource.getMessageStatic("00000035")));
        }
        if (paramDto.getMethodId() == null || paramDto.getMethodId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic(YamlMessageSource.getMessageStatic("00000007")));
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //变换类型错误
            return Result.fail(YamlMessageSource.getMessageStatic("00304004"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据方法id查询方法表的数据
        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
        // 获取方法表中的方法参数信息
        String measurementParamInfo = method.getMeasurementParamInfo();
        FSpectrumParameter spectrumParameter = JSON.parseObject(measurementParamInfo, FSpectrumParameter.class);
        Integer type = null;
        if (paramDto.getDataType().equals("1")) {
            if (spectrumParameter.getLuminosity().equals("1")) {
                type = 0;
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                type = 1;
            } else {
                //您当前的光度模式不支持进行转换
                return Result.fail(YamlMessageSource.getMessageStatic("00000036"));
            }
        } else if (paramDto.getDataType().equals("2")) {
            if (spectrumParameter.getLuminosity().equals("1")) {
                type = 2;
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                type = 3;
            } else {
                //您当前的光度模式不支持进行转换
                return Result.fail(YamlMessageSource.getMessageStatic("00000036"));
            }
        } else {
            //您当前的光度模式不支持进行转换!
            return Result.fail(YamlMessageSource.getMessageStatic("00000036"));
        }
        // 根据测量数据id查询测量数据
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("y_data", yList);
        body.put("transform_type", type);
        Map<String, Object> resultMap;
        try {
            resultMap = callPythonService(
                    pythonPathConstant.getSpectrumTransformation(),
                    body
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        Map<String, Object> resultMap1;
        try {
            resultMap1 = callPythonService(
                    pythonPathConstant.getPeakSearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Map<String, Object> resultMap2;
        try {
            resultMap2 = callPythonService(
                    pythonPathConstant.getValleySearching(),
                    body1
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
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
        tMeasureAnalysisData.setIsSave(Constants.PARAM_ZERO);
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save) {
            //数据保存失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000034"));
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xList.get(i));
            map.put("y", y_data.get(i));
            dataList.add(map);
        }
        return Result.ok(dataList);
    }

    /**
     * 保存数据接口
     * @param taskId
     * @return
     */
    @Override
    public Result saveData(String taskId) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
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
     * 多格式文件下载接口（支持word/excel/txt）
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds,String deviceSerialNumber, Integer digit) {
        // log.info("===== 导出流程开始 =====");
        // log.info("开始导出，taskId: {}, fileType: {}, 需要筛选的analysisId数量: {}",
        // taskId, fileType, analysisIds != null ? analysisIds.size() : 0);

        // 1. 基础参数校验阶段（保持不变）
        // log.info("【步骤1/5】开始基础参数校验");
        if (!StringUtils.hasText(taskId)) {
            // log.error("【步骤1/5】校验失败：缺少任务id");
            //缺少任务id
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            // log.error("【步骤1/5】校验失败：任务不存在，taskId: {}", taskId);
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // log.info("【步骤1/5】任务查询成功，任务名称：{}", task.getTaskName());

        // 处理fileType（保持不变）
        if (!StringUtils.hasText(fileType)) {
            fileType = "word";
            // log.info("【步骤1/5】未指定文件类型，默认使用：{}", fileType);
        } else {
            fileType = fileType.toLowerCase();
            // log.info("【步骤1/5】指定文件类型：{}", fileType);
        }

        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            // log.error("【步骤1/5】校验失败：不支持的文件类型：{}", fileType);
            //不支持的导出格式，仅支持word/txt/xlsx
            return R.fail(YamlMessageSource.getMessageStatic("00300001"));
        }

        if (allDisplay == null) {
            allDisplay = true;
            // log.info("【步骤1/5】allDisplay未指定，默认使用：{}", allDisplay);
        }
        // log.info("【步骤1/5】基础参数校验通过");

        // 2. 获取样品数据阶段（增加changeTheListPort数据处理和筛选）
        // log.info("【步骤2/5】开始查询样品数据，taskId: {}", taskId);

        // 2.1 获取两个来源的数据
        List<SampleListVo> allSamples = new ArrayList<>();

        // 处理sampleListPort数据
        Result<List<SampleListVo>> sampleListResult = sampleListPort(taskId, null, digit,deviceSerialNumber);
        if (sampleListResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode()) && sampleListResult.getData() != null) {
            // log.info("【步骤2/5】sampleListPort查询成功，返回样品数量：{}", sampleListResult.getData().size());
            allSamples.addAll(sampleListResult.getData());
        } else {
            // log.warn("【步骤2/5】sampleListPort查询异常：{}", sampleListResult.getMessage());
        }

        // 处理changeTheListPort数据（与sampleListPort逻辑相同）
        Result<List<SampleListVo>> changeListResult = changeTheListPort(taskId, null, null);
        if (changeListResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode()) && changeListResult.getData() != null) {
            // log.info("【步骤2/5】changeTheListPort查询成功，返回样品数量：{}", changeListResult.getData().size());
            allSamples.addAll(changeListResult.getData());
        } else {
            // log.warn("【步骤2/5】changeTheListPort查询异常：{}", changeListResult.getMessage());
        }

        // 2.2 根据analysisIds筛选数据
        List<SampleListVo> filteredSamples = new ArrayList<>();
        if (CollectionUtils.isEmpty(analysisIds)) {
            // 无筛选条件，使用全部数据
            filteredSamples = allSamples;
            // log.info("【步骤2/5】未指定筛选条件，使用全部样品数据，总数量：{}", filteredSamples.size());
        } else {
            // 有筛选条件，匹配analysisId
            for (SampleListVo sample : allSamples) {
                // 假设SampleListVo有getAnalysisId()方法获取analysisId
                String sampleAnalysisId = sample.getAnalysisId();
                if (analysisIds.contains(sampleAnalysisId)) {
                    filteredSamples.add(sample);
                    // log.debug("【步骤2/5】样品{}的analysisId={}匹配筛选条件，已保留",
                    // sample.getTitle(), sampleAnalysisId);
                }
            }
            // log.info("【步骤2/5】筛选完成，原始样品数量：{}，匹配数量：{}",
            //         allSamples.size(), filteredSamples.size());
        }

        // 2.3 打印样品数据信息
        // log.info("【步骤2/5】最终使用的样品数量：{}", filteredSamples.size());
        if (!filteredSamples.isEmpty()) {
            int logCount = Math.min(3, filteredSamples.size());
            for (int i = 0; i < logCount; i++) {
                SampleListVo vo = filteredSamples.get(i);
                // log.info("【步骤2/5】样品{}: title={}, analysisId={}, data是否为空={}",
                //         i+1, vo.getTitle(), vo.getAnalysisId(),
                //         vo.getData() == null || vo.getData().isEmpty());

                // 打印第一个样品的前3条数据，确认x和y的值
                if (i == 0 && vo.getOriginalData() != null && !vo.getOriginalData().isEmpty()) {
                    int dataLogCount = Math.min(3, vo.getOriginalData().size());
                    for (int j = 0; j < dataLogCount; j++) {
                        Map<String, Object> data = vo.getOriginalData().get(j);
                        // log.info("【步骤2/5】样品{}数据{}: x={}, y={}",
                        //         i+1, j+1, data.get("x"), data.get("y"));
                    }
                }
            }
        } else {
            // log.info("【步骤2/5】无符合条件的样品数据，后续将生成空文件");
        }

        // 3. 提取波长和样品数据阶段（逻辑不变，使用筛选后的样品）
        // log.info("【步骤3/5】开始提取波长和样品数据");
        List<String> wavelengths = new ArrayList<>(); // 存储x字段的值作为波长
        Map<String, List<String>> sampleYMap = new LinkedHashMap<>(); // 存储各样品的y值

        if (!filteredSamples.isEmpty()) {
            for (SampleListVo vo : filteredSamples) {
                String title = vo.getTitle();
                List<Map<String, Object>> dataList = vo.getOriginalData();

                if (CollectionUtils.isEmpty(dataList)) {
                    // log.info("【步骤3/5】样品{}: 无数据，跳过", title);
                    continue;
                }

                // 初始化当前样品的y值列表
                sampleYMap.putIfAbsent(title, new ArrayList<>());
                List<String> yList = sampleYMap.get(title);

                // 遍历数据，提取x（波长）和y（数值）
                for (Map<String, Object> data : dataList) {
                    // 明确从data中获取"x"字段作为波长
                    Object xObj = data.get("x");
                    String x = (xObj != null) ? xObj.toString() : "-";

                    // 明确从data中获取"y"字段作为数值
                    Object yObj = data.get("y");
                    String y = (yObj != null) ? yObj.toString() : "-";

                    // 记录日志，确认x和y的取值
                    // log.debug("【步骤3/5】样品{}: x={}, y={}", title, x, y);

                    // 添加新波长（去重）
                    if (!wavelengths.contains(x)) {
                        wavelengths.add(x);
                        // log.debug("【步骤3/5】新增波长: {}", x);
                    }

                    // 按波长顺序填充y值
                    int index = wavelengths.indexOf(x);
                    // 补全前面的空值
                    while (yList.size() <= index) {
                        yList.add("-");
                    }
                    // 设置当前波长对应的y值
                    yList.set(index, y);
                }
            }
        }

        // log.info("【步骤3/5】数据提取完成，波长数量: {}, 有效样品数量: {}",
        // wavelengths.size(), sampleYMap.size());

        // 4. 构建表头和数据阶段（保持不变）
        // log.info("【步骤4/5】开始构建导出表头和数据");
        List<HeadVO> heads = new ArrayList<>();
        heads.add(new HeadVO("波长nm", "wavelength")); // 波长列标题
        sampleYMap.keySet().forEach(title -> heads.add(new HeadVO(title, title))); // 样品列标题

        // log.info("【步骤4/5】表头构建完成，表头数量: {}", heads.size());

        List<Map<String, Object>> exportData = new ArrayList<>();
        if (!wavelengths.isEmpty()) {
            for (int i = 0; i < wavelengths.size(); i++) {
                final int index = i;
                Map<String, Object> row = new LinkedHashMap<>();
                // 波长列使用wavelengths中的值（即x字段的值）
                row.put("wavelength", wavelengths.get(index));

                // 样品列使用对应的y值
                sampleYMap.forEach((title, yList) -> {
                    row.put(title, index < yList.size() ? yList.get(index) : "-");
                });

                exportData.add(row);

                // 打印前5行数据，确认波长列是否正确
                // if (i < 5) {
                //     log.info("【步骤4/5】数据行{}: 波长={}, 样品值={}",
                //             i+1, row.get("wavelength"),
                //             row.entrySet().stream()
                //             .filter(e -> !"wavelength".equals(e.getKey()))
                //             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                // }
            }
        } else {
            // log.info("【步骤4/5】无波长数据，构建空数据列表");
            Map<String, Object> emptyRow = new LinkedHashMap<>();
            emptyRow.put("wavelength", "-");
            sampleYMap.keySet().forEach(title -> emptyRow.put(title, "-"));
            exportData.add(emptyRow);
        }

        // log.info("【步骤4/5】导出数据构建完成，数据行数: {}", exportData.size());

        // 5. 生成文件阶段（保持不变）
        // log.info("【步骤5/5】开始生成文件，类型: {}", fileType);
        String fileName = task.getTaskName() + "." + getFileSuffix(fileType);
        String filePath = System.getProperty("user.dir") + File.separator + fileName;
        filePath = filePath.replaceAll("[\\\\/:*?\"<>|]", "_");
        File file = new File(filePath);
        // log.info("【步骤5/5】文件保存路径: {}", file.getAbsolutePath());

        try {
            switch (fileType) {
                case "word":
                    // log.info("【步骤5/5】开始生成Word文件");
                    // 获取测量方法，避免空指针
                    String methodKey = task.getMethodKey();
                    TMeasureMethod method = null;
                    if (StringUtils.hasText(methodKey)) {
                        method = tMeasureMethodService.getById(methodKey);
                        // log.info("【步骤5/5】查询到测量方法：{}", method != null ? method.getMethodName() : "未找到");
                    }
                    if (method == null) {
                        // log.warn("【步骤5/5】测量方法不存在，使用默认值");
                        method = new TMeasureMethod();
                        method.setMethodName("未知测量方法");
                    }
                    generateWordFile(file, task, method, heads, exportData, 0);
                    break;
                case "txt":
                    // log.info("【步骤5/5】开始生成TXT文件");
                    generateTxtFile(file, heads, exportData);
                    break;
                case "xlsx":
                    // log.info("【步骤5/5】开始生成Excel文件");
                    generateExcelFile(file, heads, exportData);
                    break;
            }

            if (file.exists()) {
                if (file.length() > 0) {
                    // log.info("【步骤5/5】文件生成成功，大小: {} bytes", file.length());
                    return R.ok(file);
                } else {
                    // log.warn("【步骤5/5】文件生成成功，但文件为空");
                    return R.ok(file);
                }
            } else {
                // log.error("【步骤5/5】文件生成失败：文件不存在");
                //文件生成失败：文件不存在
                return R.fail(YamlMessageSource.getMessageStatic("0030400A"));
            }
        } catch (Exception e) {
            // log.error("【步骤5/5】文件生成异常", e);
            if (file.exists()) {
                boolean deleted = file.delete();
                // log.info("【步骤5/5】删除异常文件: {}", deleted ? "成功" : "失败");
            }
            //"文件生成失败：" + e.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00302006",e.getMessage()));
        } finally {
            // log.info("===== 导出流程结束 =====");
        }
    }

    /**
     * 仪器设置参数
     * @param deviceSerialNumber
     * @param setParam
     * @return
     */
    @Override
    public boolean setInstrumentParameters(String deviceSerialNumber, SetParam setParam) {
        DevOperationInputParam param = new DevOperationInputParam();
        param.setOperationType(OperationTypeEnums.SetParam.getType());
        param.setDeviceSerialNumber(deviceSerialNumber);
        param.setParam(setParam);
        R<OperationResultEntity> r = devOperationUtilService.operation(param);
        if (r.getSuccess()) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * (实验测试)创建方法
     * @param methodDto
     * @param updateBy
     * @param timeSweepParamter
     * @return
     */
    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, TimeSweepParamter timeSweepParamter) {
        R<Boolean> r = checkParam(timeSweepParamter);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }
        if (updateBy.length()>=256){
            //用户名过长！
            return Result.fail(YamlMessageSource.getMessageStatic("00305001"));
        }
        if (methodDto.getMethodName().length()>=256){
            //方法名称过长！
            return Result.fail(YamlMessageSource.getMessageStatic("00305004"));
        }
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(timeSweepParamter));
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        tMeasureMethod.setIsDelete(Constants.PARAM_ZERO);
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if ( save){
            //创建成功
            return Result.ok(YamlMessageSource.getMessageStatic("0000000F"));
        }else {
            //创建失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000010"));
        }
    }

    /**
     * (实验测试)查询报告表格数据接口
     * @param taskId
     * @return
     */
    @Override
    public List<PeakValleyMarking> queryReportFormData(String taskId,String sampleId) {
        // 根据任务id查询任务表
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return null;
        }
        // 将任务表的样品解析出来
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        // 遍历样品id找到未删除并且已经测量过的样品
        // 根据样品id和任务id锁定侧脸数据编号最大的数据
        List<PeakValleyMarking> resultList = new ArrayList<>();
        QueryWrapper<TMeasureData> eq = new QueryWrapper<>();
        eq.eq("task_id",taskId);
        eq.eq("sample_key",sampleId);
        eq.orderByDesc("serial_number");
        eq.last("limit 1");
        TMeasureData measureData = tMeasureDataService.getOne(eq);
        if (measureData==null){
            return null;
        }
        if (measureData.getPeak()!=null){
            List<PeakValleyMarking> peak = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
            resultList.addAll(peak);
        }
        if (measureData.getGrain()!=null){
            List<PeakValleyMarking> grain = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
            resultList.addAll(grain);
        }
        if (measureData.getGaugePoint()!=null){
            List<PeakValleyMarking> gaugePoint = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
            resultList.addAll(gaugePoint);
        }
        return resultList;
    }

    /**
     * (实验测试)表格头接口
     * @param taskId
     * @param methodId
     * @param analysisIdList
     * @return
     */
    @Override
    public Result<List<HeadVO>> tableHead(String taskId, String methodId, List<String> analysisIdList) {
        // 根据方法id查找方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        TimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
        List<HeadVO> ans = new ArrayList<>();

        // 1. 编号
        ans.add(new HeadVO("编号", "id", "system", "编号"));

        // 2. 波长
        ans.add(new HeadVO("时长(s)", "waveLength", "measure", "时长(s)"));

        // 如果样品长度为1则是单样品表头,如果样品长度超过1则是多样品表头
        if (analysisIdList.size() == 1) {
            addLuminosityMode(ans, parameter.getLuminosity());
            ans.add(new HeadVO("峰谷/标记点", "peakValleyGaugePoint", "measure", "峰谷"));
            ans.add(new HeadVO("备注", "remark", "analysis", "备注"));
            return Result.ok(ans);
        } else {
            for (String analysisId : analysisIdList) {
                TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(analysisId);
                TMeasureSample sample = tMeasureSampleService.getById(analysisData.getSampleKey());
                ans.add(new HeadVO(sample.getSampleName(), "sampleNumber", "name", "样品名称"));
                ans.add(new HeadVO("峰/谷", "peakValley", "system", "峰谷"));
            }
            return Result.ok(ans);
        }
    }

    /**
     * (实验测试)查询表格数据接口
     * @param taskId
     * @param analysisIds
     * @param digit
     * @return
     */
    @Override
    public List<Map<String, Object>> queryTableData(String taskId, List<String> analysisIds, Integer digit) {
        // 校验参数
        if (StringUtils.isEmpty(taskId) || analysisIds == null || analysisIds.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> arrayList = new ArrayList<>();

        // 判断样品长度是否为1
        if (analysisIds.size() == 1) {
            TMeasureAnalysisData measureAnalysisData = tMeasureAnalysisDataService.getById(analysisIds.get(0));
            if (measureAnalysisData == null) {
                return null;
            }
            int id = 1;
            List<PeakValleyMarking> peaks = JSON.parseArray(measureAnalysisData.getPeak(), PeakValleyMarking.class);
            List<PeakValleyMarking> valleys = JSON.parseArray(measureAnalysisData.getGrain(), PeakValleyMarking.class);
            List<PeakValleyMarking> gaugePoints = JSON.parseArray(measureAnalysisData.getGaugePoint(), PeakValleyMarking.class);
            if (peaks != null && !peaks.isEmpty()) {
                for (PeakValleyMarking peak : peaks) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(SampleTableEnum.ID.getIdentity(), id++);
                    map.put(SampleTableEnum.WAVELENGTH.getIdentity(), formatBigDecimal(peak.getX(), 1));
                    map.put(SampleTableEnum.LUMINOSITY.getIdentity(), formatBigDecimal(peak.getY(), digit));
                    map.put(SampleTableEnum.PEAKVALLEYGAUGEPOINT.getIdentity(), "峰");
                    map.put(SampleTableEnum.REMARK.getIdentity(), peak.getRemark() != null ? peak.getRemark() : "");
                    arrayList.add(map);
                }
            }
            if (valleys != null && !valleys.isEmpty()) {
                for (PeakValleyMarking valley : valleys) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(SampleTableEnum.ID.getIdentity(), id++);
                    map.put(SampleTableEnum.WAVELENGTH.getIdentity(), formatBigDecimal(valley.getX(), 1));
                    map.put(SampleTableEnum.LUMINOSITY.getIdentity(), formatBigDecimal(valley.getY(), digit));
                    map.put(SampleTableEnum.PEAKVALLEYGAUGEPOINT.getIdentity(), "谷");
                    map.put(SampleTableEnum.REMARK.getIdentity(), valley.getRemark() != null ? valley.getRemark() : "");
                    arrayList.add(map);
                }
            }
            if (gaugePoints != null && !gaugePoints.isEmpty()) {
                for (PeakValleyMarking gaugePoint : gaugePoints) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(SampleTableEnum.ID.getIdentity(), id++);
                    map.put(SampleTableEnum.WAVELENGTH.getIdentity(), formatBigDecimal(gaugePoint.getX(), 1));
                    map.put(SampleTableEnum.LUMINOSITY.getIdentity(), formatBigDecimal(gaugePoint.getY(), digit));
                    map.put(SampleTableEnum.PEAKVALLEYGAUGEPOINT.getIdentity(), "标记点");
                    map.put(SampleTableEnum.REMARK.getIdentity(), gaugePoint.getRemark() != null ? gaugePoint.getRemark() : "");
                    arrayList.add(map);
                }
            }

            return arrayList;
        } else {
            // 多样品表数据
            List<TMeasureAnalysisData> analysisDataList = new ArrayList<>();
            // 存储每个样品的数据映射，用于快速查找
            List<Map<String, String[]>> sampleDataMaps = new ArrayList<>();
            // 存储每个样品的峰谷标记映射
            List<Map<String, String>> samplePeakValleyMaps = new ArrayList<>();
            // 收集所有波长
            Set<String> allWavelengthsSet = new LinkedHashSet<>();

            // 根据任务id和样品id查询分析数据
            for (String analysisId : analysisIds) {
                TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(analysisId);
                if (analysisData != null) {
                    analysisDataList.add(analysisData);

                    // 解析当前样品的数据
                    List<String> datas = JSON.parseArray(analysisData.getAnalysisResult(), String.class);
                    Map<String, String[]> dataMap = new HashMap<>();
                    Map<String, String> peakValleyMap = new HashMap<>();

                    if (datas != null) {
                        for (String data : datas) {
                            String[] split = data.split(",");
                            if (split.length >= 2) {
                                String wavelength = formatBigDecimal(split[0], 1);
                                String value = formatBigDecimal(split[1], digit);
                                dataMap.put(wavelength, new String[]{value, split[0]}); // 存储格式化后的值和原始值
                                allWavelengthsSet.add(wavelength); // 收集所有波长
                            }
                        }
                    }

                    // 处理峰标记
                    List<PeakValleyMarking> peaks = JSON.parseArray(analysisData.getPeak(), PeakValleyMarking.class);
                    if (peaks != null) {
                        for (PeakValleyMarking peak : peaks) {
                            String wavelength = formatBigDecimal(peak.getX(), 1);
                            peakValleyMap.put(wavelength, "峰");
                        }
                    }

                    // 处理谷标记
                    List<PeakValleyMarking> valleys = JSON.parseArray(analysisData.getGrain(), PeakValleyMarking.class);
                    if (valleys != null) {
                        for (PeakValleyMarking valley : valleys) {
                            String wavelength = formatBigDecimal(valley.getX(), 1);
                            // 如果该波长已经有峰标记，则不覆盖
                            if (!peakValleyMap.containsKey(wavelength)) {
                                peakValleyMap.put(wavelength, "谷");
                            }
                        }
                    }

                    sampleDataMaps.add(dataMap);
                    samplePeakValleyMaps.add(peakValleyMap);
                }
            }

            if (analysisDataList.isEmpty()) {
                return null;
            }

            // 将波长集合转换为列表并保持插入顺序
            List<String> allWavelengths = new ArrayList<>(allWavelengthsSet);

            int id = 1;
            for (String wavelength : allWavelengths) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put(SampleTableEnum.ID.getIdentity(), id++);

                // 为每个样品初始化列表
                List<String> sampleNumbers = new ArrayList<>();
                List<String> peakValleys = new ArrayList<>();

                // 为每个样品查找对应波长的数据
                for (int i = 0; i < sampleDataMaps.size(); i++) {
                    Map<String, String[]> dataMap = sampleDataMaps.get(i);
                    Map<String, String> peakValleyMap = samplePeakValleyMaps.get(i);

                    // 查找数据
                    if (dataMap.containsKey(wavelength)) {
                        String[] data = dataMap.get(wavelength);
                        sampleNumbers.add(data[0]); // 格式化后的值

                        // 查找峰谷标记
                        String peakValleyType = peakValleyMap.getOrDefault(wavelength, "");
                        peakValleys.add(peakValleyType);
                    } else {
                        // 没有对应波长的数据，补充空值
                        sampleNumbers.add("");
                        peakValleys.add("");
                    }
                }

                // 将列表放入resultMap
                resultMap.put("sampleNumber", sampleNumbers);
                resultMap.put("peakValley", peakValleys);
                resultMap.put(SampleTableEnum.WAVELENGTH.getIdentity(), wavelength);

                Map<String, Object> map = new HashMap<>();
                map.put(wavelength, resultMap);
                arrayList.add(map);
            }

            return arrayList;
        }
    }

    @Override
    public Result stopMeasure(String deviceSerialNumber) {
        // 1. 校验参数
        if (!StringUtils.hasText(deviceSerialNumber)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        ConcurrentMap<String, Boolean> timeMeasureIsStop = UdpSendUtil.timeMeasureIsStop;
        Boolean put = timeMeasureIsStop.put(info.getDeviceId(), true);
        if (put== null) {
            return Result.ok("设备正在停止中");
        }
        if (!put) {
            log.info("设备已停止测量");
            Constants.measureStopStatus.put(deviceSerialNumber, false);
            return Result.ok();
        } else {
            return Result.ok("设备正在停止中");
        }
    }

    /**
     * 格式化 BigDecimal，保留指定的小数位数
     *
     * @param valueStr
     * @param digit
     * @return
     */

    public static String formatBigDecimal(String valueStr, int digit) {
        // 将字符串转换成 BigDecimal
        BigDecimal value = new BigDecimal(valueStr);
        BigDecimal decimal = value.setScale(digit, RoundingMode.HALF_UP);
        // 设置小数位数并进行舍入
        return DataCountUtil.addZero(Double.valueOf(decimal.stripTrailingZeros().toPlainString()), digit);
    }

    /**
     * 获取样品的详细数据
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureAnalysisData measureData,Integer digit) {
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            if (digit != null) {
                Double y = new Double(split[1]);
                Double value = DataCountUtil.formatValue(digit, y);
                String valueY = DataCountUtil.addZero(value, digit);
                map.put("x", split[0]);
                map.put("y", valueY);
            } else {
                map.put("x", split[0]);
                map.put("y", DataCountUtil.addZero(valueOf(split[1]), 8));
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
     * 获取样品的原始数据
     * 该方法与 getSampleData 方法逻辑相同，但不会进行小数位数限制
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getOriginalSampleData(TMeasureAnalysisData measureData,
                                                           Integer digit) {
        // 解析分析结果、峰、谷、标记点
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);

        // 存放数据的结果列表
        List<Map<String, Object>> resultList = new ArrayList<>();

        // 遍历每个分析数据
        for (String s : list) {
            String[] split = s.split(","); // 以逗号分割
            Map<String, Object> map = new HashMap<>();
            Double x = new Double(split[0]);
            Double y = new Double(split[1]);
            Double valueX = DataCountUtil.formatValue(1, x);
            String zeroX = DataCountUtil.addZero(valueX, 1);
            map.put("x", valueX);
            if (digit != null) {
                Double value = DataCountUtil.formatValue(digit, y);
                String valueY = DataCountUtil.addZero(value, digit);
                map.put("astrictY", valueY);
            } else {
                map.put("astrictY", DataCountUtil.addZero(Double.valueOf(split[1]), 8));
            }
            map.put("y", y);
            Double value = DataCountUtil.formatValue(digit, y);
            String valueY = DataCountUtil.addZero(value, digit);


            // 标记点数据处理
            if (gauges != null && !gauges.isEmpty()) {
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

            // 峰数据处理
            if (peaks != null && !peaks.isEmpty()) {
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

            // 谷数据处理
            if (grades != null && !grades.isEmpty()) {
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

            // 将当前的map添加到结果列表
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

    // 保留原有的downloadWord方法，并适配新参数
    public R<File> downloadWord(String taskId, Boolean allDisplay, List<String> analysisIds,String deviceSerialNumber,Integer digit) {
        return downloadFile(taskId, allDisplay, "word", analysisIds,deviceSerialNumber,digit);
    }


    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, TMeasureTask task, TMeasureMethod method,
                                 List<HeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        CTDocument1 document = doc.getDocument();
        CTBody body = document.getBody();

        // 页面设置（保持不变）
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        CTSectPr section = body.getSectPr();
        if (!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        CTPageSz pageSize = section.getPgSz();
        if (vosSize > 5) {
            pageSize.setW(BigInteger.valueOf(15840));
            pageSize.setH(BigInteger.valueOf(12240));
        }

        // 添加标题（保持不变）
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("任务：" + task.getTaskName() + "\r");
        run.setText("方法：" + method.getMethodName() + "\r");
        run.setText("测试数据：");

        // 创建表格（保持不变）
        int columnCount = heads.size();
        XWPFTable table = doc.createTable(1, columnCount);
        table.setWidth("100%");

        // 填充表头（修复：用 getName() 替代 getLabel()）
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < columnCount; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            if (cell == null) {
                cell = headerRow.createCell();
            }
            HeadVO head = heads.get(i);
            // 使用 getName() 获取表头显示文本（如"波长nm"）
            String headName = head.getName();
            cell.setText(headName);
        }

        // 填充数据行（修复：用 getIdentity() 替代 getKey()）
        for (Map<String, Object> rowData : data) {
            XWPFTableRow row = table.createRow();

            // 第一列：波长值（从rowData中获取"wavelength"字段）
            String wavelength = rowData.get("wavelength").toString();
            row.getCell(0).setText(wavelength);
            // log.debug("【generateWordFile】写入波长: {}", wavelength);

            // 其他列：样品值（从第二列开始）
            for (int i = 1; i < columnCount; i++) {
                HeadVO head = heads.get(i);
                // 使用 getIdentity() 获取数据中的键（如"样品1"）
                String headKey = head.getIdentity();
                String value = rowData.getOrDefault(headKey, "-").toString();
                row.getCell(i).setText(value);
            }
        }

        // 表格样式（保持不变）
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                XWPFParagraph paragraph1 = cell.getParagraphArray(0);
                paragraph1.setAlignment(ParagraphAlignment.CENTER);
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        // 写入文件（保持不变）
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.write(out);
        } finally {
            doc.close();
        }
    }

    /**
     * 生成TXT文件（UTF-8编码，制表符分隔）
     */
    private void generateTxtFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 前置校验：避免核心参数为null导致后续流处理报错
        if (heads == null) {
            throw new IllegalArgumentException("表头列表heads不能为null");
        }
        if (data == null) {
            throw new IllegalArgumentException("数据列表data不能为null");
        }
        if (file == null) {
            throw new IllegalArgumentException("输出文件File不能为null");
        }

        // 使用BufferedWriter保证效率，UTF-8避免中文乱码（try-with-resources自动关流）
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // 1. 写入表头（制表符分隔）—— 重点防护heads中的null元素
            String headerLine = heads.stream()
                    // 过滤并处理heads中的null元素
                    .map(head -> {
                        if (head == null) {
                            log.warn("表头列表中存在null元素，使用默认表头名称");
                            return "未命名列";
                        }
                        // 处理表头名称为null/空的情况
                        String headName = head.getName();
                        return (headName == null || headName.trim().isEmpty()) ? "未命名列" : headName;
                    })
                    .collect(Collectors.joining("\t"));
            writer.write(headerLine);
            writer.newLine(); // 换行

            // 2. 写入数据（与表头顺序一致）—— 防护null的Map、null的字段值，仅保留原始值+空值用"-"
            for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
                Map<String, Object> map = data.get(rowIdx);
                // 防护：当前数据行的Map为null
                if (map == null) {
                    log.warn("数据列表第{}行的Map为null，跳过该行数据", rowIdx + 1);
                    continue;
                }

                List<String> rowData = new ArrayList<>();
                for (HeadVO head : heads) {
                    // 防护：表头为null（避免后续head.getIdentity()报错）
                    String identity = head != null ? head.getIdentity() : "unknown";
                    // 从Map取值：保留原始值（不预设默认空字符串，避免覆盖真实空值）
                    Object originalValue = map.get(identity);
                    String cellData;

                    // 核心逻辑：空值处理（null/空字符串/"null" 统一替换为"-"，其他保留原始值）
                    if (originalValue == null) {
                        cellData = "-";
                    } else {
                        // 原始值直接转字符串（不做任何额外格式化）
                        cellData = originalValue.toString();
                        // 清除TXT列分裂风险的特殊字符（制表符/换行符，不修改原始业务内容）
                        cellData = cellData.replace("\t", " ").replace("\r", "").replace("\n", "");
                        // 处理空字符串或"null"（不区分大小写）
                        if (cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        }
                    }

                    rowData.add(cellData);
                }

                // 写入当前行数据（制表符分隔）
                writer.write(String.join("\t", rowData));
                writer.newLine();
                log.debug("TXT文件第{}行数据写入完成", rowIdx + 1);
            }

            log.info("TXT文件生成成功，路径：{}", file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            log.error("TXT文件路径不存在或无法写入：{}", file.getAbsolutePath(), e);
            throw e;
        } catch (IOException e) {
            log.error("TXT文件写入IO异常", e);
            throw e;
        } catch (Exception e) {
            log.error("TXT文件生成未知异常", e);
            throw e;
        }
    }

    /**
     * 生成 XLSX 文件（保留原始值+空值用“-”，修复空指针异常）
     */
    private void generateExcelFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 1. 校验核心参数是否为null（避免后续调用方法时报错）
        if (heads == null) {
            throw new IllegalArgumentException("表头列表heads不能为null");
        }
        if (data == null) {
            throw new IllegalArgumentException("数据列表data不能为null");
        }
        if (file == null) {
            throw new IllegalArgumentException("输出文件File不能为null");
        }

        // 2. 转换表头（处理空表头和null元素，修复原代码循环语法错误）
        List<List<String>> excelHeads = new ArrayList<>();
        for (int i = 0; i < heads.size(); i++) {
            HeadVO head = heads.get(i);
            // 防护：当前表头为null
            if (head == null) {
                log.warn("表头列表第{}个元素为null，使用默认表头", i);
                excelHeads.add(Collections.singletonList("未命名列_" + i));
                continue; // 修复原代码此处缺少的分号，确保逻辑正常跳转
            }
            // 防护：表头名称为null/空
            String headName = head.getName();
            if (headName == null || headName.trim().isEmpty()) {
                headName = "未命名列（" + head.getIdentity() + "）";
                log.warn("表头第{}列名称为空，使用默认名称：{}", i, headName);
            }
            excelHeads.add(Collections.singletonList(headName));
        }

        // 3. 转换数据（处理空Map和null值，仅保留原始值+空值用"-"）
        List<List<Object>> excelData = new ArrayList<>();
        for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
            Map<String, Object> map = data.get(rowIdx);
            // 防护：当前数据行为null
            if (map == null) {
                log.warn("数据列表第{}行的Map为null，跳过该行", rowIdx);
                continue;
            }

            List<Object> rowData = new ArrayList<>();
            for (int colIdx = 0; colIdx < heads.size(); colIdx++) {
                HeadVO head = heads.get(colIdx);
                // 防护：表头为null时，直接填充默认值
                if (head == null) {
                    rowData.add("-");
                    continue;
                }

                // 提取字段原始值（不预设默认值，完全保留数据源状态）
                String identity = head.getIdentity();
                Object originalValue = map.get(identity);
                String cellData;

                // 核心逻辑：空值处理（null/空字符串/"null" 统一替换为"-"，其他保留原始值）
                if (originalValue == null) {
                    cellData = "-";
                } else {
                    // 原始值直接转字符串（不做枚举转换、前缀拼接等额外处理）
                    cellData = originalValue.toString();
                    // 清除Excel解析异常的特殊字符（换行/制表符，不修改原始业务内容）
                    cellData = cellData.replace("\r", "").replace("\n", "").replace("\t", " ");
                    // 处理空字符串或"null"（不区分大小写）
                    if (cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                        cellData = "-";
                    }
                }

                rowData.add(cellData);
            }
            excelData.add(rowData);
        }

        // 4. 写入Excel（确保流正确关闭，防护空列表）
        if (excelHeads.isEmpty()) {
            log.warn("表头为空，生成空Excel文件");
        }
        try (OutputStream out = new FileOutputStream(file)) {
            EasyExcel.write(out)
                    .head(excelHeads)
                    .sheet("测量数据")
                    .doWrite(excelData);
            log.info("Excel文件生成成功，路径：{}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("EasyExcel写入文件失败", e);
            throw e; // 向上抛出，由外层处理
        }
    }

    // ---------------------- 工具方法 ----------------------
    /**
     * 获取文件后缀（Java 8兼容）
     */
    private String getFileSuffix(String fileType) {
        if ("word".equals(fileType)) {
            return "docx";
        } else if ("txt".equals(fileType)) {
            return "txt";
        } else if ("xlsx".equals(fileType)) {
            return "xlsx";
        } else {
            return "docx";
        }
    }

    /**
     * 添加光度模式
     * @param ans
     * @param mode
     */
    public void addLuminosityMode(List<HeadVO> ans, String mode) {
        switch (mode) {
            case "1":
                ans.add(new HeadVO("Abs", "luminosity", "measure", "Abs"));
                break;
            case "2":
                ans.add(new HeadVO("T%", "luminosity", "measure", "T%"));
                break;
            case "3":
                ans.add(new HeadVO("Es", "luminosity", "measure", "Es"));
                break;
            case "4":
                ans.add(new HeadVO("Er", "luminosity", "measure", "Er"));
                break;
            default:
                // 可以在这里处理无效的模式
                System.out.println("无效的光度模式: " + mode);
        }
    }

    /**
     * 比较参数
     *
     * @param method
     * @param newParam
     * @return
     */
    private boolean compareParam(TimeSweepParamter method, TimeSweepParamter newParam) {
        String dbJson = JSON.toJSONString(method, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newParam, SerializerFeature.SortField);

        return dbJson.equals(frontJson);
    }

    /**
     * 创建新的方法
     */
    private TMeasureMethod createNewMethod(TimeSweepMethodParameterDto paramDto, Boolean isCopy) {
        TMeasureMethod fMeasureMethod = new TMeasureMethod();
        if (isCopy){
            fMeasureMethod.setMethodName(paramDto.getMethodName());
        }else{
            fMeasureMethod.setMethodName(paramDto.getMethodName()+"- 副本");
        }

        TimeSweepParamter parameter = new TimeSweepParamter();
        // 将前端参数复制给数据库参数
        BeanUtils.copyProperties(paramDto, parameter);
        fMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(parameter));
        fMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        fMeasureMethod.setProjectItemKey(paramDto.getProjectId());
        fMeasureMethod.setMethodUseTime(LocalDateTime.now());
        fMeasureMethod.setMethodCreator(paramDto.getUpdateBy());
        fMeasureMethod.setMeasurementType(paramDto.getMeasureType());
        fMeasureMethod.setIsSharingMethod(paramDto.getIsShare());
        fMeasureMethod.setIsDelete(Constants.PARAM_ZERO);
        boolean save = tMeasureMethodService.save(fMeasureMethod);
        if (save) {
            return fMeasureMethod;
        } else {
            return null;
        }
    }

    /**
     * 创建新的任务
     */
    private TMeasureTask createNewTask(TimeSweepMethodParameterDto paramDto, String methodId, TMeasureTask task, String userId) {
        TMeasureTask newTask = new TMeasureTask();
        newTask.setUserId(userId);
        if(task.getTaskName().equals(paramDto.getTaskName())||paramDto.getTaskName()==null|| paramDto.getTaskName().isEmpty()){
            newTask.setTaskName(task.getTaskName()+"- 副本");
        }else{
            newTask.setTaskName(paramDto.getTaskName());
        }
        newTask.setCreateTime(LocalDateTime.now());
        newTask.setMethodKey(methodId);
        List<TaskSpecimen> specimens = new ArrayList<>();
        newTask.setSampleKey(specimens.toString());
        newTask.setMeasureType(paramDto.getMeasureType());
        newTask.setProjectItemKey(task.getProjectItemKey());
        newTask.setDeviceSerialNumber(paramDto.getDeviceSerialNumber());
        newTask.setIsDelete(Constants.PARAM_ZERO);
        newTask.setTaskStatus(TaskStatusEnum.UNTESTED.getCode());
        boolean save = tMeasureTaskService.save(newTask);
        if (save) {
            return newTask;
        } else {
            return null;
        }
    }

    /**
     * 调用Python服务
     * @param url
     * @param requestBody
     * @return
     */
    private Map<String, Object> callPythonService(String url, Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            // 检查响应体是否为空
            if (responseBody == null) {
                throw new RuntimeException(YamlMessageSource.getMessageStatic("00000032")); // "算法服务返回异常"
            }

            Integer code = (Integer) responseBody.get("code");
            if (code == null || code != 200) {
                String message = (String) responseBody.get("message");
                String errorMsg = StringUtils.hasText(message) ? message : "未知错误";
                throw new RuntimeException(errorMsg);
            }

            return responseBody;

        } catch (ResourceAccessException ex) {
            throw new RuntimeException("算法服务异常,请重启服务");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RuntimeException("算法服务异常：" + ex.getStatusCode());
        } catch (Exception e) {
            // 兜底异常（包括上面 throw 的 RuntimeException）
            throw e;
        }
    }


    /**
     * 校验时间扫描参数
     * @param parameter 时间扫描参数
     * @return 校验结果
     */
    private R<Boolean> checkParam(TimeSweepParamter parameter) {
        // 1. 根对象非空校验
        if (parameter == null) {
            return R.fail(YamlMessageSource.getMessageStatic("00102003"));
        }

        // 2. 光度模式校验
        String luminosity = parameter.getLuminosity();
        if (!StringUtils.hasText(luminosity)) {
            return R.fail("光度模式不能为空");
        }
        List<String> validLuminosityModes = Arrays.asList("1", "2", "3", "4"); // 1.abs 2.T% 3.Es 4.Er
        if (!validLuminosityModes.contains(luminosity)) {
            return R.fail("光度模式不合法（可选值：1-abs、2-T%、3-Es、4-Er），当前值：" + luminosity);
        }

        // 3. 波长值校验
        String waveLengthStr = parameter.getWaveLength();
        if (!StringUtils.hasText(waveLengthStr)) {
            return R.fail("波长值不能为空");
        }

        // 波长默认范围 190-1100
        double minWaveLength = 190.0;
        double maxWaveLength = 1100.0;

        try {
            double waveLength = Double.parseDouble(waveLengthStr);
            if (waveLength < minWaveLength || waveLength > maxWaveLength) {
                return R.fail(YamlMessageSource.getMessageStatic("00102006",
                        1, waveLength, minWaveLength, maxWaveLength));
            }
            if (waveLengthStr.contains(".") && waveLengthStr.split("\\.")[1].length() > 1) {
                return R.fail(YamlMessageSource.getMessageStatic("00105005", waveLengthStr));
            }
        } catch (NumberFormatException e) {
            return R.fail("波长值格式错误，必须为数字");
        }

        // 4. 扫描单位校验
        String scanUnit = parameter.getScanUnit();
        if (!StringUtils.hasText(scanUnit)) {
            return R.fail("扫描单位不能为空");
        }
        List<String> validScanUnits = Arrays.asList("0", "1"); // 0.分 1.秒
        if (!validScanUnits.contains(scanUnit)) {
            return R.fail("扫描单位不合法（可选值：0-分、1-秒），当前值：" + scanUnit);
        }

        // 5. 扫描总时长和总个数的关联校验
        String scanTimeStr = parameter.getScanTime();
        String totalNumberStr = parameter.getTotalNumber();
        String intervalTimeStr = parameter.getIntervalTime();

        // 5.1 扫描总时长校验
        if (!StringUtils.hasText(scanTimeStr)) {
            return R.fail("扫描总时长不能为空");
        }

        // 5.2 时间间隔校验
        if (!StringUtils.hasText(intervalTimeStr)) {
            return R.fail("时间间隔不能为空");
        }

        // 5.3 扫描总个数校验
        if (!StringUtils.hasText(totalNumberStr)) {
            return R.fail("扫描总个数不能为空");
        }

        try {
            double scanTime = Double.parseDouble(scanTimeStr);
            if (scanTime <= 0) {
                return R.fail("扫描总时长必须大于0，当前值：" + scanTime);
            }

            double intervalTime = Double.parseDouble(intervalTimeStr);
            if (intervalTime <= 0) {
                return R.fail("时间间隔必须大于0，当前值：" + intervalTime);
            }

            int totalNumber = Integer.parseInt(totalNumberStr);
            if (totalNumber <= 0) {
                return R.fail("扫描总个数必须大于0，当前值：" + totalNumber);
            }
            if (totalNumber > 9999) {
                return R.fail("扫描总个数不能超过9999，当前值：" + totalNumber);
            }

            // 验证扫描总时长、时间间隔和总个数的关系
            if (parameter.getScanUnit().equals("1")) { // 秒模式
                // 计算理论上的最小总个数（floor方式）
                int minTotalNumber = (int) Math.floor(scanTime / intervalTime) + 1;
                // 计算理论上的最大总个数（ceil方式）
                int maxTotalNumber = (int) Math.ceil(scanTime / intervalTime) + 1;

                // 检查当前总个数是否在合理范围内
                if (!(totalNumber >= minTotalNumber) && !(totalNumber <= maxTotalNumber)) {
                    return R.fail(String.format("扫描总时长与时间间隔、总个数不匹配"));
                }
            } else { // 分钟模式
                double totalSeconds = scanTime * 60;
                int minTotalNumber = (int) Math.floor(totalSeconds / intervalTime) + 1;
                int maxTotalNumber = (int) Math.ceil(totalSeconds / intervalTime) + 1;

                if (!(totalNumber >= minTotalNumber) && !(totalNumber <= maxTotalNumber)) {
                    return R.fail(String.format("扫描总时长与时间间隔、总个数不匹配"));
                }
            }

            // 根据单位设置最大扫描时长限制
            if ("0".equals(scanUnit)) { // 分
                if (scanTime > 1440) { // 最大24小时
                    return R.fail("扫描总时长（分）不能超过1440分钟（24小时），当前值：" + scanTime);
                }
            } else { // 秒
                if (scanTime > 3600) { // 最大1小时
                    return R.fail("扫描总时长（秒）不能超过3600秒（1小时），当前值：" + scanTime);
                }
            }

        } catch (NumberFormatException e) {
            return R.fail("时间参数格式错误，必须为数字");
        }

        // 6. 多联池模式校验
        String multiPoolMode = parameter.getMultiPoolMode();
        if (!StringUtils.hasText(multiPoolMode)) {
            return R.fail("多联池模式不能为空");
        }
        List<String> validMultiPoolModes = Arrays.asList("0", "1"); // 0.顺序单池连续采样 1.循环多池交替采样
        if (!validMultiPoolModes.contains(multiPoolMode)) {
            return R.fail("多联池模式不合法（可选值：0-顺序单池连续采样、1-循环多池交替采样），当前值：" + multiPoolMode);
        }

        // 7. 父类 MeasurePublicParameters 字段校验
        // 7.1 光谱带宽校验
        String spectralBandwidth = parameter.getSpectralBandwidth();
        if (!StringUtils.hasText(spectralBandwidth)) {
            return R.fail("光谱带宽不能为空");
        }
        try {
            double bandwidth = Double.parseDouble(spectralBandwidth);
            if (bandwidth <= 0) {
                return R.fail("光谱带宽必须大于0，当前值：" + bandwidth);
            }
            // 常见光谱带宽值校验（可选，根据业务决定是否强制）
            List<Double> validBandwidths = Arrays.asList(0.5, 1.0, 2.0, 4.0, 5.0);
            if (!validBandwidths.contains(bandwidth)) {
                return R.fail("光谱带宽值不合法，常见值：" + validBandwidths);
            }
        } catch (NumberFormatException e) {
            return R.fail("光谱带宽格式错误，必须为数字");
        }

        // 7.3 换灯波长校验
        String changeLampWave = parameter.getChangeLampWaveLength();
        if (!StringUtils.hasText(changeLampWave)) {
            return R.fail(YamlMessageSource.getMessageStatic("00105003"));
        }
        try {
            double waveVal = Double.parseDouble(changeLampWave);
            if (waveVal < 330 || waveVal > 390) {
                return R.fail(YamlMessageSource.getMessageStatic("0010202A", waveVal));
            }
        } catch (NumberFormatException e) {
            return R.fail("换灯波长格式错误");
        }

        // 7.4 能量灯校验
        String energyLamp = parameter.getEnergyLamp();
        if (!StringUtils.hasText(energyLamp)) {
            return R.fail("能量灯不能为空");
        }
        if (!energyLamp.matches("^[0-2]$")) {
            return R.fail("能量灯值无效（可选值：0-钨灯、1-氘灯、2-汞灯），当前值：" + energyLamp);
        }

        // 7.5 增益校验
        if (parameter.getGain() != null && parameter.getGain() < 0) {
            return R.fail("增益不能小于0，当前值：" + parameter.getGain());
        }

        // 7.7 参考标准名称校验（长度限制等）
        if (parameter.getReferenceStandardName() != null &&
                parameter.getReferenceStandardName().length() > 100) {
            return R.fail("参考标准名称长度不能超过100字符");
        }

        return R.ok(true);
    }

    /**
     * 检查时间扫描参数是否改变
     * @param oldParam 旧参数
     * @param newParam 新参数
     * @return true--未改变，false--改变了
     */
    private boolean checkTimeSweepParam(TimeSweepParamter oldParam, TimeSweepParamter newParam) {
        if (oldParam == newParam) {
            return true;  // 同一个对象引用
        }
        if (oldParam == null || newParam == null) {
            return false; // 任一对象为null
        }

        // 比较所有参数（包括父类和子类的所有字段）
        return Objects.equals(oldParam.getReferenceStandardName(), newParam.getReferenceStandardName()) &&
                Objects.equals(oldParam.getPreProcessParameters(), newParam.getPreProcessParameters()) &&
                Objects.equals(oldParam.getSpectralBandwidth(), newParam.getSpectralBandwidth()) &&
                Objects.equals(oldParam.getResponseTime(), newParam.getResponseTime()) &&
                Objects.equals(oldParam.getChangeLampWaveLength(), newParam.getChangeLampWaveLength()) &&
                Objects.equals(oldParam.getEnergyLamp(), newParam.getEnergyLamp()) &&
                Objects.equals(oldParam.getEnergyLampName(), newParam.getEnergyLampName()) &&
                Objects.equals(oldParam.getGain(), newParam.getGain()) &&

                Objects.equals(oldParam.getLuminosity(), newParam.getLuminosity()) &&
                Objects.equals(oldParam.getWaveLength(), newParam.getWaveLength()) &&
                Objects.equals(oldParam.getScanTime(), newParam.getScanTime()) &&
                Objects.equals(oldParam.getScanUnit(), newParam.getScanUnit()) &&
                Objects.equals(oldParam.getIntervalTime(), newParam.getIntervalTime()) &&
                Objects.equals(oldParam.getTotalNumber(), newParam.getTotalNumber()) &&
                Objects.equals(oldParam.getMultiPoolMode(), newParam.getMultiPoolMode());
    }


    /**
     * 校验波长范围是否超限
     * @param gaugePoints
     * @param analysisData
     * @return
     */
    private Result validateGaugePointsRange(List<String> gaugePoints, TMeasureAnalysisData analysisData) {
        // 从原始数据中解析波长范围
        List<String> rawData = JSON.parseArray(analysisData.getAnalysisResult(), String.class);
        if (rawData == null || rawData.isEmpty()) {
            return Result.fail("原始数据为空");
        }

        // 获取所有波长点并找出最小和最大值
        double minWave = Double.MAX_VALUE;
        double maxWave = -Double.MAX_VALUE;

        for (String dataPoint : rawData) {
            String[] split = dataPoint.split(",");
            try {
                double wave = Double.parseDouble(split[0]);
                minWave = Math.min(minWave, wave);
                maxWave = Math.max(maxWave, wave);
            } catch (NumberFormatException e) {
                return Result.fail("原始数据格式错误：" + dataPoint);
            }
        }

        // 验证每个标记点
        for (String xStr : gaugePoints) {
            try {
                double x = Double.parseDouble(xStr);
                // 允许有0.01的浮点数误差
                if (x < minWave - 0.01 || x > maxWave + 0.01) {
                    String message = String.format("标记点 %.2f 超出时间范围 [%.2f, %.2f]",
                            x, minWave, maxWave);
                    return Result.fail(message);
                }
            } catch (NumberFormatException e) {
                return Result.fail("标记点格式错误：" + xStr);
            }
        }

        return null; // 校验通过
    }
}
