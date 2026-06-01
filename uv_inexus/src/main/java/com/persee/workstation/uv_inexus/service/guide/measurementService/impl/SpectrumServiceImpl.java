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
import com.persee.workstation.uv_inexus.enums.instrumentEnums.MappingParamEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.SamplePoolConfigPO;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConcisePublicUtilService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.parseReport.CalculationFormulaService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.StandardPretreatmentService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationUtilService;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationTypeEnums;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.utils.OperateLog.LogManualUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceConfParam;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.OriginalMeasuredDataVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.QueryMethodParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleListVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodChangeCheckVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.spectrum.SpectrumParameterVo;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
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
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Double.valueOf;

/**
 * 光谱扫描服务实现类
 */
@Slf4j
@Service("spectrumService")
public class SpectrumServiceImpl implements SpectrumService {


    @Autowired
    private TMeasureProjectService tMeasureProjectService;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private SystemUserManageDAO systemUserManageDAO;

    @Autowired
    private TMeasureTaskService tMeasureTaskService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TMeasureSampleService tMeasureSampleService;

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private FormulaUtil formulaUtil;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private TMeasureTaskMapper tMeasureTaskMapper;

    @Autowired
    private PythonPathConstant pythonPathConstant;

    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;

    // 计算公式的服务
    @Autowired
    private CalculationFormulaService calculationFormulaService;

    // 标准预处理服务
    @Autowired
    private StandardPretreatmentService standardPretreatmentService;
    @Autowired
    private TMeasureTaskAttachmentService tMeasureTaskAttachmentService;
    @Autowired
    private TMeasureAnalysisImageService tMeasureAnalysisImageService;

    @Autowired
    private DevOperationUtilService devOperationUtilService;

    @Autowired
    private SamplePoolConfigService samplePoolConfigService;

    @Autowired
    private ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    private DeviceManageService deviceManageService;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;

    @Value("default")
    String defaultMethodProjectName;


    /**
     * 查询项目任务方法
     *
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryProjectTaskMethod(String updateBy, String measureType) {
        SysUserPO user = systemUserManageDAO.selectByUsername(updateBy);
        if (user == null) {
            //用户不存在
            return Result.fail("00000001");
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(measureType);
        if (measureTypeEnum == null) {
            //暂不支持
            return Result.fail("0000002B");
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
            if (measureTask != null) {
                vo.setTaskId(measureTask.getId());
                vo.setTaskName(measureTask.getTaskName());
                vo.setTaskStatus(measureTask.getTaskStatus());
                vo.setProjectId(project.getId());
                vo.setProjectName(project.getProjectName());
                return Result.ok(vo);
            }
        }
        // 根据默认任务名称查询默认任务
        TMeasureTask defaultTask = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>().eq("task_name", Constants.DEFAULT_TASK)
                .eq("measure_type", measureType)
                .last("LIMIT 1"));
        if (defaultTask == null) {
            return Result.fail("00101006");
        }
        // 根据方法id和项目id查询方法和项目
        TMeasureProject byId = tMeasureProjectService.getById(defaultTask.getProjectItemKey());
        if (byId == null) {
            return Result.fail("00301010");
        }
        TMeasureMethod method1 = tMeasureMethodService.getById(defaultTask.getMethodKey());
        if (method1 == null) {
            return Result.fail("00000011");
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
     * 查询项目下所有方法
     *
     * @param projectId
     * @param measureForm
     * @return
     */
    @Override
    public Result queryProjectAllMethods(String projectId, String measureForm, String updateBy) {
        // 参数校验
        if (StringUtils.isEmpty(projectId) || StringUtils.isEmpty(measureForm) || StringUtils.isEmpty(updateBy)) {
            return Result.fail("00000007");
        }
        List<TMeasureMethod> methods = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", measureForm).eq("method_creator", updateBy));

        if (methods == null) {
            //没有此类型的数据
            return Result.fail("0030000A");
        }
        return Result.ok(methods);
    }

    /**
     * 检查参数接口
     * @param methodParam
     * @return
     */
    @Override
    public Result checkMethodParam(SpectrumMethodParamsDto methodParam) {

        if (!StringUtils.hasText(methodParam.getCurrentMethodId())) {
            //请选择当前方法
            return Result.fail("00000023");
        }

        if (methodParam.getTaskId()==null||!StringUtils.hasText(methodParam.getTaskId())) {
            return Result.fail("00101006");
        }


        SpectrumParameter newMethodParam = new SpectrumParameter();
        BeanUtils.copyProperties(methodParam, newMethodParam);

        // 仪器下发参数校验
        if (methodParam.getDeviceSerialNumber()!=null&& !methodParam.getDeviceSerialNumber().isEmpty()){
            R rdata = IssueCheckParam(newMethodParam, methodParam.getDeviceSerialNumber());
            if (!rdata.getSuccess()) {
                return Result.checkFail(rdata.getData());
            }
        }

        R<DeviceMeasureVO> deviceMeasureVOR = deviceManageService.queryDeviceTypeInfo(methodParam.getDeviceSerialNumber());
        if (!deviceMeasureVOR.getSuccess()) {
            return Result.fail(deviceMeasureVOR.getMessage());
        }

        R<Boolean> r = checkParam(newMethodParam,methodParam.getDeviceSerialNumber());
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }

        double minWaveLength = Double.parseDouble(deviceMeasureVOR.getData().getMinWavelength());
        double maxWaveLength = Double.parseDouble(deviceMeasureVOR.getData().getMaxWavelength());

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
        double startWl = Double.parseDouble(newMethodParam.getStartWaveLength());
        double endWl = Double.parseDouble(newMethodParam.getEndWaveLength());

        if (startWl < minWaveLength || endWl > maxWaveLength) {
            //"第" + (i + 1) + "个波长值超出范围（190-900），当前值：" + waveVal
            return Result.fail("00102006",startWl , minWaveLength, maxWaveLength);
        }
        // 校验小数点后最多1位
        String[] parts = String.valueOf(startWl).split("\\.");
        if (parts.length > 1 && parts[1].length() > 1) {
            //"波长值" + waveStr + "小数点后最多保留1位"
            return Result.fail("00105005",startWl);
        }

        if (endWl < minWaveLength || endWl > maxWaveLength) {
            //"第" + (i + 1) + "个波长值超出范围（190-900），当前值：" + waveVal
            return Result.fail("00102006",endWl , minWaveLength, maxWaveLength);
        }
        // 校验小数点后最多1位
        String[] parts1 = String.valueOf(endWl).split("\\.");
        if (parts1.length > 1 && parts1[1].length() > 1) {
            //"波长值" + waveStr + "小数点后最多保留1位"
            return Result.fail("00105005",endWl);
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

        SpectrumParameter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), SpectrumParameter.class);

        String dbJson = JSON.toJSONString(oldParam, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newMethodParam, SerializerFeature.SortField);

        if (dbJson.equals(frontJson)) {
            vo.setParamChange(false);
            vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
            vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());
            return Result.ok(vo);
        }
        //检查测量数据
        boolean b = checkSpectrumParam(oldParam, newMethodParam);
        System.out.println(b);
        if (!b) {
            log.info("测量参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.measure.getMessage());
            return Result.ok(vo);
        }

        //检查分析参数
        boolean analyze = checkSpectrumParam(oldParam, newMethodParam);
        if (!analyze) {
            log.info("分析参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.analyze.getMessage());
        }
        return Result.ok(vo);
    }

    /**
     * 查询方法的参数
     *
     * @param methodId
     * @return
     */
    @Override
    public Result queryMethodParameter(String methodId) {

        // 根据方法id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        // 将标准表中的json转换成对象存入返回值中
        QueryMethodParameterVo queryMethodParameterVo = JSON.parseObject(method.getMeasurementParamInfo(), QueryMethodParameterVo.class);
        queryMethodParameterVo.setMethodName(method.getMethodName());
        queryMethodParameterVo.setMethodId(method.getId());
        if (queryMethodParameterVo.getLuminosity().equals("1")
                && queryMethodParameterVo.getLuminosity().equals("2")
                && queryMethodParameterVo.getLuminosity().equals("3")
                && queryMethodParameterVo.getLuminosity().equals("4")) {
            //请选择正确的光度模式
            return Result.fail("00B04002");
        }
        queryMethodParameterVo.setLuminosityName(queryMethodParameterVo.getLuminosity().equals("1") ? "Abs" : queryMethodParameterVo.getLuminosity().equals("2") ? "T%" : queryMethodParameterVo.getLuminosity().equals("3") ? "Es" : "Er");
        queryMethodParameterVo.setScanSpeedName(queryMethodParameterVo.getScanSpeed().equals("0") ? "慢速" : queryMethodParameterVo.getScanSpeed().equals("1") ? "中速" : queryMethodParameterVo.getScanSpeed().equals("2") ? "快速" : "极快");
        return Result.ok(queryMethodParameterVo);
    }

    /**
     * 创建方法接口
     *
     * @param methodDto
     * @param updateBy
     * @param spectrumParameter
     * @return
     */
    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter) {
        // 先校验方法是否合法
        R<Boolean> r = checkParam(spectrumParameter,methodDto.getDeviceSerialNumber());
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }
        if (spectrumParameter.getSpikeRemoval()==null){
            spectrumParameter.setSpikeRemoval(false);
        }
        // 仪器下发参数校验
        if (methodDto.getDeviceSerialNumber()!=null&& !methodDto.getDeviceSerialNumber().isEmpty()){
            R rdata = IssueCheckParam(spectrumParameter, methodDto.getDeviceSerialNumber());
            if (!rdata.getSuccess()) {
                return Result.checkFail(rdata.getData());
            }
        }
        if (spectrumParameter.getSpikeRemoval()){
            List<String> newSpikeArray = new ArrayList<>();
            List<String> spikeArray = spectrumParameter.getSpikeArray();
            Double startWaveLength = Double.valueOf(spectrumParameter.getStartWaveLength());
            Double endWaveLength = Double.valueOf(spectrumParameter.getEndWaveLength());
            for (String s : spikeArray) {
                Double spike = Double.valueOf(s);
                if (spike <= startWaveLength && spike >= endWaveLength) {
                    newSpikeArray.add(s);
                }
            }
            spectrumParameter.setSpikeArray(newSpikeArray);
        }
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        if (methodDto.getPretreatmentIds() != null) {
            tMeasureMethod.setPreprocessKey(JSON.toJSONString(methodDto.getPretreatmentIds()));
        }
        if (spectrumParameter.getLadderCorrection() == null) {
            spectrumParameter.setLadderCorrection(false);
        }
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setIsDelete(Constants.PARAM_ZERO);
        tMeasureMethod.setPreprocessKey(spectrumParameter.getPreprocessKey());
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if (save) {
            //创建成功
            return Result.ok("0000000F");
        } else {
            //创建失败
            return Result.fail("00000010");
        }
    }

    /**
     * 查询测量数据列表接口
     *
     * @param dataVo
     * @return
     */
    @Override
    public Result queryMeasuredData(QueryMeasuredDataVo dataVo) {
        // 根据任务id查询测量数据表中的数据
        TMeasureData tMeasureData = tMeasureDataService
                .getOne(Wrappers.lambdaQuery(TMeasureData.class)
                        .eq(TMeasureData::getTaskId, dataVo.getTaskId())
                        .orderByDesc(TMeasureData::getMeasurementEndTime)
                        .last("limit 1"));
        if (tMeasureData == null) {
            //没有数据
            return Result.fail("0000002A");
        }
        // 将测量的数据从json转换为OriginalMeasuredDataVo对象
        OriginalMeasuredDataVo originalMeasuredDataVo = new OriginalMeasuredDataVo();
        List<String> list;
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();

        try {
            list = JSON.parseObject(tMeasureData.getOriginalCalculationData(), ArrayList.class);
        } catch (Exception e) {
            return Result.fail("00B04001");
        }
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        double[] y_data = new double[yList.size()];
        for (int i = 0; i < yList.size(); i++) {
            y_data[i] = Double.parseDouble(yList.get(i));
        }

        double max = Arrays.stream(y_data).max().orElse(0);
        double height = max * 0.2;
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        // 设置请求头类型为json
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建请求体
        Map<String, Object> body = new HashMap<>();
        // 将参数添加到请求体中
        body.put("x_data", xList);
        body.put("y_data", yList);
        body.put("height", 0.5);
        body.put("distance", 1);

        // 创建请求对象
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        // 发送请求获取返回结果 postForEntity方法传入参数分别是:调用路径，请求体，返回结果类型
        // 使用getBody方法获取返回结果
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request, Map.class).getBody();
        List<String> peakX = (List<String>) resultMap.get("peak_x");
        List<String> peakY = (List<String>) resultMap.get("peak_y");
        List<String> valleyX = (List<String>) resultMap.get("valley_x");
        List<String> valleyY = (List<String>) resultMap.get("valley_y");
        List<OriginalMeasuredDataVo> resultList = new ArrayList<>();
        // 将xList和yList转换为OriginalMeasuredDataVo对象
        for (int i = 0; i < xList.size(); i++) {
            OriginalMeasuredDataVo vo = new OriginalMeasuredDataVo();
            vo.setId(i + 1);
            vo.setX(xList.get(i));
            vo.setY(yList.get(i));
            for (int j = 0; j < peakX.size(); j++) {
                if (xList.get(i).equals(peakX.get(j)) && yList.get(i).equals(peakY.get(j))) {
                    vo.setFeature("1");
                }
            }
            if (vo.getFeature() == null) {
                vo.setFeature("0");
            }
            // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
            for (int j = 0; j < valleyX.size(); j++) {
                if (xList.get(i).equals(valleyX.get(j)) && yList.get(i).equals(valleyY.get(j))) {
                    vo.setGrain("1");
                }
            }
            // 如果对象中的谷属性为null,则说明不是谷,则设置为0
            if (vo.getGrain() == null) {
                vo.setGrain("0");
            }
            vo.setTag("-"); // 或其他逻辑判断
            resultList.add(vo);
        }

        // 分页
        // 获取所有数据
        int total = resultList.size();
        // fromIndex 开始，toIndex 结束
        int fromIndex = (dataVo.getPageNum() - 1) * dataVo.getPageSize();
        // 计算结束索引
        int toIndex = Math.min(fromIndex + dataVo.getPageSize(), total);
        if (fromIndex >= total || fromIndex < 0) {
            return Result.ok(resultList);
        }
        // 截取分页数据
        List<OriginalMeasuredDataVo> pagedList = resultList.subList(fromIndex, toIndex);
        Map<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("data", pagedList);
        return Result.ok(map);
    }

    /**
     * 查询测量参数接口
     *
     * @param taskId
     * @return
     */
    @Override
    public Result queryMeasurementParameters(String taskId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey() == null || task.getSampleKey().equals("[]")) {
            return Result.ok(new ArrayList<>());
        }
        // 根据任务的方法key查询方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String, String>> list = new ArrayList<>();
        // 判断是否是重复模式
        if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
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
                    if (one.getAnalysisResult() != null) {
                        Map<String, String> map = new HashMap<>();
                        map.put("measureId", one.getId());
                        map.put("sampleName", byId.getSampleName());
                        list.add(map);
                    }
                }
            }
        } else {
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                    List<TMeasureAnalysisData> measureDataList = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpeciman.getSpecimenKey())
                            .orderByAsc("analysis_number"));
                    TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                    for (TMeasureAnalysisData tMeasureData : measureDataList) {
                        if (tMeasureData.getAnalysisResult() != null) {
                            Map<String, String> map = new HashMap<>();
                            map.put("measureId", tMeasureData.getId());
                            map.put("sampleName", byId.getSampleName() + "-" + tMeasureData.getAnalysisNumber());
                            list.add(map);
                        }
                    }
                }
            }
        }

        return Result.ok(list);
    }

    /**
     * 设置参数接口
     *
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result setUpParametersPort(String methodId, String deviceSerialNumber) {
        if (methodId == null || methodId.equals("")) {
            return Result.fail("00000008");
        }
        // 根据methodId去方法表中查询数据
        TMeasureMethod tMeasureMethod = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id", methodId).eq("measurement_type", "光谱扫描"));
        if (tMeasureMethod == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }
        SpectrumParameter spectrumParam = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), SpectrumParameter.class);
        double waveLengthInterval = Double.parseDouble(spectrumParam.getWaveLengthInterval());
        // 仪器下发参数校验
        if (deviceSerialNumber!=null&& !deviceSerialNumber.isEmpty()){
            R rdata = IssueCheckParam(spectrumParam, deviceSerialNumber);
            if (!rdata.getSuccess()) {
                return Result.checkFail(rdata.getData());
            }
        }
        // 设置参数
        DevOperationInputParam param = new DevOperationInputParam();
        param.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam setParam = new SetParam();
        param.setDeviceSerialNumber(deviceSerialNumber);
        // 1. 光度模式
        setParam.setPhotoMode(Integer.valueOf(spectrumParam.getLuminosity()));
        // 2. 开始波长 (开始波长+1)
        double parsed = Double.parseDouble(spectrumParam.getStartWaveLength());
        parsed = parsed + waveLengthInterval;
        setParam.setStartWl(parsed);
        // 3. 结束波长 (结束波长-1)
        double parsed2 = Double.parseDouble(spectrumParam.getEndWaveLength());
        parsed2 = parsed2 - waveLengthInterval;
        setParam.setEndWl(parsed2);
        // 4. 扫描间隔
        setParam.setScanWlInterval(Double.valueOf(spectrumParam.getWaveLengthInterval()));
        // 5. 光谱带宽
        if (StringUtils.hasText(spectrumParam.getSpectralBandwidth()) && deviceInfoService.isNeedSetSit(deviceSerialNumber)) {
            setParam.setSlit1(Double.valueOf(spectrumParam.getSpectralBandwidth()));
        }
        // 6. 扫描速度
        if (!spectrumParam.getScanSpeed().equals(ScanSpeedEnum.EXTREME_FAST.getCode())) {
            R<DeviceMeasureVO> deviceTypeInfo = deviceManageService.queryDeviceTypeInfo(deviceSerialNumber);
            String ScanSpeed = deviceTypeInfo.getData().getScanSpeed().stream()
                    .filter(obj -> obj.getParamName().equals(spectrumParam.getScanSpeed()))
                    .map(DeviceConfParam::getParamValue)
                    .findFirst()
                    .orElse(null);
            if (ScanSpeed == null) {
                return Result.fail("00B02001");
            }
            setParam.setScanSpeed(Integer.valueOf(ScanSpeed));
        }
        // 7. 换灯波长
        setParam.setChangeLampWl(Double.valueOf(spectrumParam.getChangeLampWaveLength()));
        // 8. 设置波长
        setParam.setSetWl(Double.valueOf(spectrumParam.getStartWaveLength()));
        if (spectrumParam.getLuminosity().equals("3") || spectrumParam.getLuminosity().equals("4")) {
            // 9. 能量灯
            setParam.setLampPos(Integer.valueOf(spectrumParam.getEnergyLamp()));
            // 10. 增益
            setParam.setGain(Integer.valueOf(spectrumParam.getGain()));
        }
        param.setParam(setParam);
        R<OperationResultEntity> r = devOperationUtilService.operation(param);
        if (r.getSuccess()) {
            return Result.ok("00B02002");
        } else {
            return Result.fail(r.getMessage());
        }
    }

    /**
     * 保存数据接口
     *
     * @param taskId
     * @return
     */
    @Override
    public Result saveData(String taskId) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }
        task.setEndTime(LocalDateTime.now());
        task.setIsDelete("1");
        boolean update = tMeasureTaskService.updateById(task);
        if (update) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    /**
     * 获取样本输入表头接口
     *
     * @param taskId
     * @return
     */
    @Override
    public Result<List<HeadVO>> querySampleHeads(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
            return Result.ok(preHead);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
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
     * 获取样本按钮列表接口
     *
     * @param taskId
     * @return
     */
    @Override
    public Result querySampleList(String taskId) {
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
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
     * 添加或删除样本接口
     *
     * @param dto
     * @return
     */
    @Override
    public Result addOrDeleteSamplePort(AddOrDeleteSampleDTO dto) {
        if (!StringUtils.hasText(dto.getMethodId())) {
            //请先创建方法
            return Result.fail("00000011");
        }
        if (dto.getHeads() == null || dto.getHeads().isEmpty()) {
            //至少需要一个表头
            return Result.fail("00000012");
        }
        if (dto.getHeads().size() > 10) {
            //表格新增列数不能超过10列
            return Result.fail("00000013");
        }
        if (dto.getNewSamples() != null && !dto.getNewSamples().isEmpty()) {
            for (Map<String, String> map : dto.getNewSamples()) {
                String jsonString = JSON.toJSONString(map);
                SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
                if (sample.getSampleNumber() != null && sample.getSampleNumber().length() > 256) {
                    return Result.fail("00B02003");
                }
                if (sample.getSampleName() != null && sample.getSampleName().length() > 256) {
                    return Result.fail("00B02004");
                }

            }
        }


        // 获取方法信息，用于判断重复模式
        TMeasureMethod method = tMeasureMethodService.getById(dto.getMethodId());
        if (method == null) {
            // 方法不存在
            return Result.fail("00000004");
        }
        // 将方法参数用json解析出来
        FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            SysUserPO po = systemUserManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, dto.getUpdateBy()));
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
                return Result.fail("00000014");
            }
            dto.setTaskId(newTask.getId());
        }

        TMeasureTask task = tMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }

        List<TaskSpecimen> taskSpecimen = new ArrayList<>();
        if (StringUtils.hasText(task.getSampleKey())) {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        }

        Map<String, TaskSpecimen> oldMap = new HashMap<>();
        for (TaskSpecimen specimen : taskSpecimen) {
            oldMap.put(specimen.getSpecimenKey(), specimen);
        }

        // 处理要删除的样品
        if (dto.getDelSampleIds() != null && !dto.getDelSampleIds().isEmpty()) {
            // 获取方法的重复模式（假设方法中有isRepeatMode字段，0-不重复，1-重复）
            Integer repeatMode = parameter.getRepetitionPattern().equals("单池重复") ? 1 : null; // 请根据实际情况调整字段名

            for (String delSampleId : dto.getDelSampleIds()) {
                TaskSpecimen specimen = oldMap.get(delSampleId);
                if (specimen != null) {
                    specimen.setDeleteType(deleteType);

                    TMeasureSample oldSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                    if (oldSample != null) {
                        String name = oldSample.getSampleName();
                        String no = oldSample.getSampleNumber();
                        LogManualUtil.saveDeleteLog(
                                "实验测试-向导模式-光谱扫描-设置样品",
                                "删除样品：编号["+no+"]，名称["+name+"]",
                                oldSample,
                                dto
                        );
                    }

                    // 根据重复模式执行不同的删除逻辑
                    if (repeatMode != null && repeatMode == 1) {
                        // 重复模式：删除所有相关的测量数据和分析数据
                        handleDeleteForRepeatMode(delSampleId, task.getId());
                    } else {
                        // 不重复模式：只删除最新的测量数据
                        handleDeleteForNonRepeatMode(delSampleId, task.getId());
                    }
                }
            }
        }

        // 更新任务表头信息
        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();

        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
        }

        if (dto.getHeads() != null && !dto.getHeads().isEmpty()) {
            taskHeadSnapshot.setSampleHeads(dto.getHeads());
        }
        taskHeadSnapshot.setTaskId(task.getId());
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));

        // 处理新增或更新的样品
        if (dto.getNewSamples() != null && !dto.getNewSamples().isEmpty()) {
            List<String> sampleNos = dto.getNewSamples().stream()
                    .map(m -> m.get(SampleTableEnum.SAMPLE_NUMBER.getIdentity()))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());

            if (sampleNos.size() < dto.getNewSamples().size()) {
                return Result.fail("0000004E");
            }

            // 查重：找到重复的样品编号
            Set<String> duplicates = sampleNos.stream()
                    .filter(no -> Collections.frequency(sampleNos, no) > 1)
                    .collect(Collectors.toSet());

            if (!duplicates.isEmpty()) {
                //标准样品编号重复
                return Result.fail("00102001");
            }

            for (Map<String, String> map : dto.getNewSamples()) {
                String jsonString = JSON.toJSONString(map);
                SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);

                TMeasureSample newSample = new TMeasureSample();
                BeanUtils.copyProperties(sample, newSample);
                newSample.setId(sample.getSampleId());

                if (dto.getHeads() != null) {
                    JSONObject newColData = new JSONObject();

                    for (HeadVO head : dto.getHeads()) {
                        if (head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                            continue;
                        }
                        String s = map.get(head.getIdentity());
                        newColData.put(head.getIdentity(), s);
                    }
                    newSample.setHeads(JSON.toJSONString(dto.getHeads()));
                    newSample.setNewColData(JSON.toJSONString(newColData));
                }

                if (StringUtils.hasText(newSample.getId())) {
                    // 更新现有样品
                    TMeasureSample oldSample = tMeasureSampleService.getById(newSample.getId());
                    tMeasureSampleService.updateById(newSample);
                    if (!oldSample.equals(newSample)) {
                        LogManualUtil.saveUpdateLog(
                                "实验测试-向导模式-光谱扫描-设置样品",
                                oldSample,
                                newSample,
                                dto
                        );
                    }
                    TaskSpecimen specimen = oldMap.get(newSample.getId());
                    if (specimen != null) {
                        specimen.setSampleType(newSample.getSampleType());
                        oldMap.put(newSample.getId(), specimen);
                    }
                } else {
                    // 新增样品
                    TaskSpecimen specimen = new TaskSpecimen();
                    specimen.setDeleteType(createType);
                    specimen.setIsTested(noTested);
                    specimen.setSampleType(newSample.getSampleType());
                    log.info("新增的样品：{}", newSample);
                    tMeasureSampleService.save(newSample);
                    specimen.setSpecimenKey(newSample.getId());
                    oldMap.put(newSample.getId(), specimen);
                    taskSpecimen.add(specimen);

                    String sampleName = StringUtils.hasText(newSample.getSampleName()) ? newSample.getSampleName() : "未命名样品";
                    String sampleNo = StringUtils.hasText(newSample.getSampleNumber()) ? newSample.getSampleNumber() : "无编号";
                    LogManualUtil.saveAddLog(
                            "实验测试-向导模式-光谱扫描-设置样品",
                            String.format("新增样品：编号[%s]，名称[%s]", sampleNo, sampleName),
                            dto
                    );
                }
            }
        }

        // 更新任务中的样品信息
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);
        boolean b = tMeasureTaskService.updateById(task);
        if (!b) {
            return Result.fail("00100003");
        }

        // 处理样品池自动切换
        if (StringUtils.hasText(dto.getDeviceSerialNumber())) {
            R<FSamplePoolInfoVO> poolInfo = samplePoolConfigService.querySamplePoolInfo(dto.getDeviceSerialNumber());
            if (!poolInfo.getSuccess()) {
                return Result.ok();
            }
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            Boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool();
            if (isAutoChangePool) {
                List<Integer> usefulLocation = samplePoolConfigService.getUsefulLocation(dto.getDeviceSerialNumber());
                concisePublicUtilService.changeSampleLocationTwo(task.getId(), usefulLocation);
            }
        }
        return Result.ok();
    }

    /**
     * 删除样本接口
     *
     * @param sampleId
     * @param taskId
     * @return
     */
    @Override
    public Result deleteSamplePort(String sampleId, String taskId) {
        // 获取任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
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
     * 获取样本列表接口
     *
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result sampleListPort(String taskId, String type, Integer digit,String deviceSerialNumber) {
        System.out.println("sampleListPort实际参数：taskId=" + taskId + ", type=" + type + ", digit=" + digit);
        if (taskId == null) {
            //方法id不能为空,请创建方法
            return Result.fail("00000021");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //该方法下没有草稿,请新建草稿
            return Result.fail("00100010");
        }
        // 根据方法id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }
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

        SpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<SampleListVo> listVos = new ArrayList<>();
        if (taskSpecimen == null || taskSpecimen.isEmpty()) {
            return Result.ok(listVos);
        }
        // 判断是否是重复模式
        if ("不重复".equals(parameter.getRepetitionPattern())) {
            // 不重复模式
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getDeleteType().equals("1") && type == null) {
                    continue;
                }
                SampleListVo vo = new SampleListVo();
                // 根据样品id查询样品数据
                TMeasureSample sample = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    //样品不存在
                    return Result.fail("0000000B");
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
                // 如果没有测量数据则设置为无测量数据的参数返回
                if (measureData == null || taskSpeciman.getIsTested().equals("0") && measureData.getAnalysisResult() == null) {
                    vo.setTitle(sample.getSampleName());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisId(null);
                    if (isAutoChangePool){
                        vo.setSampleLocation(sample.getSampleLocation());
                    }else{
                        vo.setSampleLocation(null);
                    }
                    vo.setAnalysisNumber(1);
                    vo.setColor(null);
                    vo.setData(null);
                    vo.setIsTested(taskSpeciman.getIsTested());
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
                    vo.setIsTested(taskSpeciman.getIsTested());
                }
                vo.setType("0");
                vo.setDeleteType(taskSpeciman.getDeleteType());
                listVos.add(vo);
            }
        } else {
            // 重复模式
            if (parameter.getRepetitionTime() == null || parameter.getRepetitionTime().equals("")) {
                //请填写重复次数
                return Result.fail("00000037");
            }
            Integer count = Integer.valueOf(parameter.getRepetitionTime());
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (type == null && taskSpeciman.getDeleteType().equals("1")) {
                    continue;
                }
                // 根据样品id查询样品数据
                TMeasureSample sample = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    //样品不存在
                    return Result.fail("0000000B");
                }
                int key = 0;
                // 根据方法中的方法参数的重复次数进行循环
                for (int i = 0; i < count; i++) {
                    SampleListVo vo = new SampleListVo();
                    QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", task.getId());
                    queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                    queryWrapper.eq("analysis_type", "5");
                    queryWrapper.eq("analysis_number", (i + 1));
                    queryWrapper.last("limit 1");
                    TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);

                    if (measureData == null && taskSpeciman.getIsTested().equals("0")) {
                        vo.setTitle(sample.getSampleName() + "-" + (i + 1));
                        vo.setAnalysisId(null);
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(i + 1);
                        if (isAutoChangePool){
                            vo.setSampleLocation(sample.getSampleLocation());
                        }else{
                            vo.setSampleLocation(null);
                        }
                        vo.setColor(null);
                        vo.setData(null);
                        vo.setDeleteType("0");
                    } else if (measureData != null) {
                        if (measureData.getIsDelete().equals("1") && type == null) {
                            continue;
                        }
                        vo.setTitle(sample.getSampleName() + "-" + (i + 1));
                        vo.setAnalysisId(measureData.getId());
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(measureData.getAnalysisNumber());
                        if (isAutoChangePool){
                            vo.setSampleLocation(sample.getSampleLocation());
                        }else{
                            vo.setSampleLocation(null);
                        }
                        vo.setColor(measureData.getColor());
                        if (measureData.getAnalysisResult() == null) {
                            vo.setData(null);
                            vo.setOriginalData(null);
                        } else {
                            vo.setData(getSampleData(measureData, digit));
                            vo.setOriginalData(getOriginalSampleData(measureData, digit));
                        }
                        vo.setDeleteType(measureData.getIsDelete());
                    } else {
                        continue;
                    }
                    vo.setType("0");

                    listVos.add(vo);
                }
            }
        }

        return Result.ok(listVos);
    }

    /**
     * 删除样品列表接口
     *
     * @param taskId
     * @param sampleId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String taskId, String sampleId, String analysisId) {
        // 校验传过来的参数
        if (taskId == null || sampleId == null || analysisId == null) {
            //参数错误
            return Result.fail("00104003");
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        // 根据任务id查询方法表数据并取出方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        // 判断是否是重复模式
        if (spectrumParameter.getRepetitionPattern().equals("不重复")) {
            // 根据分析id查询分析数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail("0000003D");
            }
            byId.setIsDelete("1");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            // 循环遍历样品列表找到该样品
            for (TaskSpecimen taskSpecimen : taskSpecimenList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("1");
                }
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimenList));
            boolean update1 = tMeasureTaskService.updateById(task);
            if (update && update1) {
                //成功删除数据
                return Result.ok("00000043");
            } else {
                //删除数据失败
                return Result.fail("00000044");
            }
        } else {
            // 根据分析id查询分析数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail("0000003D");
            }
            byId.setIsDelete("1");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            // 查询跟这个样品和任务id关联的分析数据
            List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).eq("sample_key", sampleId).eq("is_delete", "0"));
            if (list.size() == 0) {
                // 循环遍历样品列表找到该样品
                for (TaskSpecimen taskSpecimen : taskSpecimenList) {
                    if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                        taskSpecimen.setDeleteType("1");
                        break;
                    }
                }
                task.setSampleKey(JSON.toJSONString(taskSpecimenList));
                boolean update1 = tMeasureTaskService.updateById(task);
                if (update && update1) {
                    //成功删除数据
                    return Result.ok("00000043");
                } else {
                    //删除数据失败
                    return Result.fail("00000044");
                }
            }
            if (update) {
                //成功删除数据
                return Result.ok("00000043");
            } else {
                //删除数据失败
                return Result.fail("00000044");
            }
        }
    }

    /**
     * 获取变换列表接口
     *
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result changeTheListPort(String taskId, String type, Integer digit) {
        if (taskId == null) {
            //方法id不能为空,请创建方法
            return Result.fail("00000021");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //该方法下没有草稿,请新建草稿
            return Result.fail("00100010");
        }

        // 根据方法id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }

        // 根据任务id去查询分析数据
        QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", task.getId());
        queryWrapper.ne("analysis_type", "5");
        if (type == null || type.isEmpty()) {
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
            if (tMeasureAnalysisData.getAnalysisResult() == null) {
                vo.setData(null);
                vo.setOriginalData(null);
            } else {
                vo.setData(getSampleData(tMeasureAnalysisData, digit));
                vo.setOriginalData(getOriginalSampleData(tMeasureAnalysisData, digit));
            }
            vo.setType("1");
            listVos.add(vo);
        }

        return Result.ok(listVos);
    }

    /**
     * 删除变换列表接口
     *
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            //没有找到该分析数据
            return Result.fail("0000003D");
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = tMeasureAnalysisDataService.updateById(byId);
        if (update) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    /**
     * 恢复已删除数据接口
     *
     * @param taskId
     * @param sampleId
     * @param type
     * @param analysisId
     * @return
     */
    @Override
    public Result restoreDeletedListData(String taskId, String sampleId, String type, String analysisId) {
        // 根据类型判断是样品数据还是变换数据
        if (type.equals("0")) {
            // 根据任务id查询任务数据
            TMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                //没有找到该任务
                return Result.fail("00000004");
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
                return Result.fail("0000003D");
            }
            byId.setIsDelete("0");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            if (update1 && update) {
                return Result.ok(true);
            } else {
                return Result.fail(false);
            }
        } else {
            // 根据变换数据id查询变换数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //变换数据不存在
                return Result.fail("0030000B");
            }
            byId.setIsDelete("0");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            if (update) {
                return Result.ok(update);
            } else {
                return Result.fail(false);
            }
        }
    }

    /**
     * 更新数据颜色接口
     *
     * @param paramDto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(UpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()) {
            //参数不能为空
            return Result.fail("00000045");
        }

        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;


        // 分析数据
        TMeasureAnalysisData analysis = tMeasureAnalysisDataService.getById(dataId);
        if (analysis == null) {
            //样品数据不存在
            return Result.fail("00000047");
        }
        analysis.setColor(color);
        updated = tMeasureAnalysisDataService.updateById(analysis);


        if (!updated) {
            //更新失败
            return Result.fail("00000048");
        }

        return Result.ok();
    }

    /**
     * 获取分析标记点接口
     *
     * @param analysisId
     * @return
     */
    @Override
    public Result queryAnalysisGaugePoint(String analysisId) {
        // 根据测量id去测量数据表中查询数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
        // 将测量数据中的标记点转换成List集合
        List<String> list = new ArrayList<>();
        try {

            list = JSON.parseObject(byId.getGaugePoint(), List.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Result.ok(list);
    }

    /**
     * 添加分析标记点接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto) {
        // 根据测量数据id查询出测量数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(paramDto.getAnalysisId());
        if (byId == null) {
            //测量数据不存在
            return Result.fail("0000003B");
        }
        // 将标记点转换成List
        List<String> list;
        try {
            list = JSON.parseObject(byId.getGaugePoint(), List.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        list.add(paramDto.getGaugePoint());
        byId.setGaugePoint(JSON.toJSONString(list));
        boolean update = tMeasureAnalysisDataService.updateById(byId);
        return Result.ok(update);
    }

    /**
     * 获取峰谷接口
     *
     * @return
     */
    @Override
    public Result queryPeakAndValleysPort(String dataId, Integer digit) {
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
        if (measureData.getPeak() != null) {
            peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
            dataAll.addAll(peaks);
        }
        if (measureData.getGrain() != null) {
            grains = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
            dataAll.addAll(grains);
        }
        if (dataAll != null && dataAll.size() != 0) {
            if (digit != null) {
                for (PeakValleyMarking data : dataAll) {
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
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result peakSearchingPort(SeekingPeaksValleysDto paramDto) {
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        if (paramDto.getDataId() == null || paramDto.getDataId().equals("")) {
            //数据ID不能为空
            return Result.fail("00100011");
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
            return Result.fail("00100012");
        }
        Double height = Double.valueOf(paramDto.getHeight());
        if (height > 100) {
            return Result.fail("00100027");
        }
        paramDto.setHeight(String.valueOf(height / 100));
        TMeasureAnalysisData tMeasureAnalysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureAnalysisData == null) {
            //测量数据不存在
            return Result.fail("0000003B");
        }
        List<String> list = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
        // 分割测量数据
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        for (String s : list) {
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
        } catch (Exception e) {
            return Result.fail("00B02005");
        }

        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        if (xPeak == null || yPeak == null) {
            //height参数太高
            return Result.fail("0000003C");
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
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail("0000003D");
        }
        analysisData.setPeak(JSON.toJSONString(peakData));
        analysisData.setGrain(JSON.toJSONString(valleyData));
        flag = tMeasureAnalysisDataService.updateById(analysisData);
        //数据保存失败
        return flag ? Result.ok() : Result.fail("00000034");
    }

    /**
     * 添加峰谷备注接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail("00B06003");
        }
        if (paramDto.getRemark() == null) {
            //请先添加备注
            return Result.fail("00000046");
        }
        // 根据测量id去分析数据表中进行查询
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            //测量数据不存在
            return Result.fail("0000003B");
        }
        // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
        List<PeakValleyMarking> list;
        if (paramDto.getType().equals("波峰")) {
            list = JSON.parseArray(tMeasureData.getPeak(), PeakValleyMarking.class);
        } else {
            list = JSON.parseArray(tMeasureData.getGrain(), PeakValleyMarking.class);
        }
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (PeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        if (paramDto.getType().equals("波峰")) {
            tMeasureData.setPeak(JSON.toJSONString(list));
        } else {
            tMeasureData.setGrain(JSON.toJSONString(list));
        }
        boolean update = tMeasureAnalysisDataService.updateById(tMeasureData);
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 获取标记点接口
     *
     * @param dataId
     * @param digit
     * @return
     */
    @Override
    public Result queryGaugePointPort(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<PeakValleyMarking> gaugePoints = new ArrayList<>();
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(dataId);
        if (measureData == null) {
            return Result.ok(new ArrayList<>());
        }
        if (measureData.getGaugePoint() != null) {
            gaugePoints = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
            if (digit != null) {
                for (PeakValleyMarking data : gaugePoints) {
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
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePoint(AddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            //请选择标记点
            return Result.fail("0000003E");
        }
        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail("0000003D");
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
        return update ? Result.ok(true) : Result.fail(false);
    }

    /**
     * 添加标记点备注接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail("00000045");
        }
        if (paramDto.getRemark() == null) {
            //请先添加备注
            return Result.fail("00000046");
        }
        // 根据测量id去分析数据表中进行查询
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            //没有找到该分析数据
            return Result.fail("0000003D");
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
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 查询运算源数据接口
     *
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
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String, String>> list = new ArrayList<>();
        // 判断是否是重复模式
        if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
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
        } else {
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                    // 修复：增加 is_delete = "0" 过滤条件，避免返回已删除的数据
                    List<TMeasureAnalysisData> measureDataList = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpeciman.getSpecimenKey())
                            .eq("is_delete", "0")  // 新增过滤条件
                            .orderByAsc("analysis_number"));
                    TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                    for (TMeasureAnalysisData tMeasureData : measureDataList) {
                        if (tMeasureData.getAnalysisResult() != null) {
                            Map<String, String> map = new HashMap<>();
                            map.put("measureId", tMeasureData.getId());
                            map.put("sampleName", byId.getSampleName() + "-" + tMeasureData.getAnalysisNumber());
                            list.add(map);
                        }
                    }
                }
            }
        }

        return Result.ok(list);
    }


    /**
     * 四则运算接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result fourOperationsPort(FourOperationsParamDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail("0000002C");
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail("00000007");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail("0000002D");
        }
        // 对所有传过来的对象数据进行校验,判断是否为null或""
        if (paramDto.getMeasureDataId1() == null || paramDto.getMeasureDataId1().equals("")) {
            //请选择测量数据1
            return Result.fail("0000002E");
        }
        if (paramDto.getMeasureDataId2() == null || paramDto.getMeasureDataId2().equals("")) {
            //请选择测量数据2
            return Result.fail("0000002F");
        }
        if (paramDto.getArithmeticOperators() == null || paramDto.getArithmeticOperators().equals("")) {
            //请选择运算符
            return Result.fail("00000030");
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //请选择数据类型
            return Result.fail("00000031");
        }
        // 根据测量数据id查询测量数据
        TMeasureAnalysisData measureData1 = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId1());
        List<String> list;
        List<String> list2;
        List<String> xList = new ArrayList<>();
        List<String> yList1 = new ArrayList<>();
        List<String> yList2 = new ArrayList<>();
        list = JSON.parseArray(measureData1.getAnalysisResult(), String.class);
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
            if (measureData2 == null) {
                //请选择源2
                return Result.fail("00100014");
            }
            // 将测量数据1和2的测量数据提取出来
            list2 = JSON.parseArray(measureData2.getAnalysisResult(), String.class);
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
            return Result.fail("00300008");
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
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result advancedOperationsPort(AdvancedOperationsDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail("0000002C");
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail("00000007");
        }
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail("00000004");
        }
        // 根据任务中的方法key查询方法并且提取方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }
        // 提取方法参数
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        if (spectrumParameter == null) {
            //方法参数错误
            return Result.fail("00105017");
        }
        // 判断光度模式是否是1
        if (spectrumParameter.getLuminosity().equals("1")) {
            //Abs光度模式不可使用高级运算
            return Result.fail("00100015");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail("0000002D");
        }
        // 将所有传过来的数据校验是否为空是否是""
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail("00000035");
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //请选择变换类型
            return Result.fail("00000033");
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
            return Result.fail("00000034");
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
     * 平滑度接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result smoothnessPort(SmoothnessDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail("0000002C");
        }
        // 将所有参数校验是否为空或""
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail("00000035");
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail("00000007");
        }
        if (paramDto.getCoefficient() == null || paramDto.getCoefficient().equals("")) {
            //请输入系数
            return Result.fail("00000038");
        }
        if (paramDto.getPointNum() == null || paramDto.getPointNum().equals("")) {
            //请输入点数
            return Result.fail("00000039");
        }
        if (valueOf(paramDto.getCoefficient()) >= valueOf(paramDto.getPointNum())) {
            //系数不能大于点数
            return Result.fail("0000003A");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail("0000002D");
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
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
            return Result.fail("00000034");
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
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result differentialPort(DifferentialDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail("0000002C");
        }
        // 将所有参数校验是否为空或""
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail("00000035");
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail("00000007");
        }
        if (paramDto.getCoefficient() == null || paramDto.getCoefficient().equals("")) {
            //请输入系数
            return Result.fail("00000038");
        }
        if (paramDto.getPointNum() == null || paramDto.getPointNum().equals("")) {
            //请输入点数
            return Result.fail("00000038");
        }
        if (valueOf(paramDto.getCoefficient()) >= valueOf(paramDto.getPointNum())) {
            //系数不能大于点数
            return Result.fail("0000003A");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.
                list(new QueryWrapper<TMeasureAnalysisData>().
                        eq("analysis_name", paramDto.getAnalysisDataName()).
                        eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail("0000002D");
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
            return Result.fail("00000034");
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
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result luminanceModeConversionPort(LuminosityConversionDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail("0000002C");
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail("00000007");
        }
        if (paramDto.getMeasureDataId() == null || paramDto.getMeasureDataId().equals("")) {
            //请选择测量数据
            return Result.fail("00000035");
        }
        if (paramDto.getMethodId() == null || paramDto.getMethodId().equals("")) {
            //缺少参数
            return Result.fail("00000007");
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //变换类型错误
            return Result.fail("00B04004");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail("0000002D");
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
                return Result.fail("00000036");
            }
        } else if (paramDto.getDataType().equals("2")) {
            if (spectrumParameter.getLuminosity().equals("1")) {
                type = 2;
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                type = 3;
            } else {
                //您当前的光度模式不支持进行转换
                return Result.fail("00000036");
            }
        } else {
            //您当前的光度模式不支持进行转换
            return Result.fail("00000036");
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
            return Result.fail("00000034");
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
     * 图片上传
     *
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
        if (upload.getImageType().equals("0")) {
            QueryWrapper<TMeasureAnalysisImage> analysisId = new QueryWrapper<TMeasureAnalysisImage>().eq("analysis_id", upload.getId());
            analysisId.last("limit 1");
            TMeasureAnalysisImage byId = tMeasureAnalysisImageService.getOne(analysisId);
            if (byId == null) {
                //图片不存在
                return Result.fail("00000040");
            }
            byId.setImageType(fileType);
            byId.setImageBase64(bytes);
            // 将当前日期时间存储
            byId.setCreateTime(LocalDateTime.now());
            boolean update = tMeasureAnalysisImageService.updateById(byId);
            return Result.ok(update);
        } else {
            QueryWrapper<TMeasureTaskAttachment> eq = new QueryWrapper<TMeasureTaskAttachment>()
                    .eq("task_id", upload.getId());
            eq.last("limit 1");
            // 根据任务id查询任务附属表的内容
            TMeasureTaskAttachment byId = tMeasureTaskAttachmentService.getOne(eq);
            if (byId == null) {
                //任务附属不存在
                return Result.fail("00000041");
            }
            if ("0".equals(upload.getImageType())) {
                byId.setOverlapImage(bytes);
            } else if ("1".equals(upload.getImageType())) {
                byId.setTiledImage(bytes);
            } else if ("2".equals(upload.getImageType())) {
                byId.setAddrImage(bytes);
            } else {
                //Type类型错误
                return Result.fail("00000042");
            }
            boolean update = tMeasureTaskAttachmentService.updateById(byId);
            return Result.ok(update);
        }
    }

    /**
     * 获取任务图片接口
     *
     * @param id
     * @param type
     * @return
     */
    @Override
    public Result queryTheTaskImage(String id, String type) {
        // 根据type类型查询测量数据或分析数据的图片
        if (type.equals("0")) {
            TMeasureData byId = tMeasureDataService.getById(id);
            if (byId.getDataImage() == null) {
                //图片不存在
                return Result.fail("00000040");
            }
            String encoded = Base64.encodeBase64String(byId.getDataImage());
            return Result.ok(encoded);
        } else if (type.equals("1")) {
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(id);
            // 根据分析数据的id查询分析数据图片
            TMeasureAnalysisImage analysisId = tMeasureAnalysisImageService.getOne(new QueryWrapper<TMeasureAnalysisImage>().eq("analysis_id", id));
            String encoded = Base64.encodeBase64String(analysisId.getImageBase64());
            return Result.ok(encoded);
        } else {
            //type类型错误
            return Result.fail("00000042");
        }
    }


    /**
     * 查询预处理操作接口
     *
     * @param methodId
     * @return
     */
    @Override
    public Result queryPreprocessingOperation(String methodId) {
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail("00000009");
        }
        if (method.getPreprocessKey() == null) {
            //预处理参数不存在
            return Result.fail("00B04006");
        } else {
            List<List> lists = JSON.parseArray(method.getPreprocessKey(), List.class);
            return Result.ok(lists);
        }
    }

    /**
     * 获取基线接口
     *
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result obtainTheBaseline(String taskId, String methodId, String deviceSerialNumber) {
        if (methodId == null || methodId.equals("")) {
            //方法编号不能为空
            return Result.fail("00B04007");
        }
        if (deviceSerialNumber == null || deviceSerialNumber.equals("")) {
            //设备编号不能为空
            return Result.fail("00B04008");
        }
        Result result = setUpParametersPort(methodId, deviceSerialNumber);
        if (result.getCode() != 200) {
            //设置参数失败
            return Result.ok("00B04009");
        }
        // 根据序列号获取设备的ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = buildCommand(SetParamEnum.BaseLine, new Object[]{});
        R<List<ChangeParam>> listR = devMessageUtil.spectrumScanGetBaseline(command, ip);
        if (listR.getSuccess() != true) {
            return Result.ok(listR.getMessage());
        }
        TMeasureTask task = null;
        if (taskId != null && !taskId.equals("")) {
            task = tMeasureTaskService.getById(taskId);
        } else {
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
            task.setDeviceSerialNumber(deviceSerialNumber);
            tMeasureTaskService.save(task);
            TMeasureTaskAttachment taskAttachment = new TMeasureTaskAttachment();
            taskAttachment.setTaskId(task.getId());
            boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
            if (!save) {
                //保存失败
                return Result.fail("00300008");
            }
        }
        // 将listR中的数据保存到task的校零参数中
        task.setZeroParam(JSON.toJSONString(listR.getData()));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }

    /**
     * 导出word接口
     *
     * @param taskId
     * @param response
     * @throws Exception
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
     * 测试对比接口
     *
     * @param taskId
     * @param spectrumParameter
     * @return
     */
    @Override
    public Result testCompare(String taskId, SpectrumParameter spectrumParameter) {
        // 根据任务id查询出任务,再根据任务中的方法id找到方法并且再提取出方法参数
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        TMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());

        SpectrumParameter spectrumParameter1 = JSON.parseObject(byId.getMeasurementParamInfo(), SpectrumParameter.class);

        String dbJson = JSON.toJSONString(spectrumParameter1, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(spectrumParameter, SerializerFeature.SortField);

        return Result.ok(dbJson.equals(frontJson));
    }

    /**
     * 多格式文件下载接口（支持word/excel/txt）
     *
     * @param taskId     任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType   导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds, Integer digit,String deviceSerialNumber) {
        // log.info("===== 导出流程开始 =====");
        // log.info("开始导出，taskId: {}, fileType: {}, 需要筛选的analysisId数量: {}",
        //         taskId, fileType, analysisIds != null ? analysisIds.size() : 0);

        // 1. 基础参数校验阶段（保持不变）
        // log.info("【步骤1/5】开始基础参数校验");
        if (!StringUtils.hasText(taskId)) {
            // log.error("【步骤1/5】校验失败：缺少任务id");
            //缺少任务id
            return R.fail("00000019");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            // log.error("【步骤1/5】校验失败：任务不存在，taskId: {}", taskId);
            //没有找到该任务
            return R.fail("00000004");
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
            return R.fail("00300001");
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
                    //         sample.getTitle(), sampleAnalysisId);
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
//                    Object yObj = data.get("y");
//                    String y = (yObj != null) ? yObj.toString() : "-";
                    // 修改第2959-2961行
                    // 明确从data中获取"y"字段作为数值
                    // 明确从data中获取"y"字段作为数值
                    Object yObj = data.get("y");
                    String y;
                    if (yObj != null && digit != null && digit >= 0) {
                        try {
                            // 对数值进行修约处理
                            double yValue = Double.parseDouble(yObj.toString());

                            // 构造格式字符串（兼容旧版本Java）
                            StringBuilder zeros = new StringBuilder();
                            for (int i = 0; i < digit; i++) {
                                zeros.append("0");
                            }
                            DecimalFormat df = new DecimalFormat("0." + zeros.toString());
                            df.setRoundingMode(RoundingMode.HALF_UP); // 四舍五入
                            y = df.format(yValue);

                            log.debug("Y值修约: 原始={} 修约后={} 小数位数={}", yObj, y, digit);
                        } catch (NumberFormatException e) {
                            log.warn("Y值转换失败: {}, 使用原始值", yObj);
                            y = yObj.toString();
                        }
                    } else {
                        y = (yObj != null) ? yObj.toString() : "-";
                    }

                    log.debug("修约前y值: {}, digit: {}, 修约后y值: {}", yObj, digit, y);


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
        //         wavelengths.size(), sampleYMap.size());

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
                return R.fail("00B0400A");
            }
        } catch (Exception e) {
            // log.error("【步骤5/5】文件生成异常", e);
            if (file.exists()) {
                boolean deleted = file.delete();
                // log.info("【步骤5/5】删除异常文件: {}", deleted ? "成功" : "失败");
            }
            //"文件生成失败：" + e.getMessage()
            return R.fail("00B02006",e.getMessage());
        } finally {
            // log.info("===== 导出流程结束 =====");
        }
    }

    /**
     * (实验测试)方法参数修改接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result updateMethodParameter(SpectralMethodParameterDto paramDto) {
        if (paramDto.getLuminosity().equals("3") || paramDto.getLuminosity().equals("4")) {
            paramDto.setLadderCorrection(false);
        }
        boolean update1 = Double.parseDouble(paramDto.getWaveLengthInterval()) < 1;
        if (paramDto.getScanSpeed().equals(ScanSpeedEnum.EXTREME_FAST.getCode()) && update1) {
            return Result.fail("00B02006");
        }
        double start = Double.parseDouble(paramDto.getStartWaveLength());
        double end = Double.parseDouble(paramDto.getEndWaveLength());
        if (paramDto.getSpikeRemoval()==null){
            paramDto.setSpikeRemoval(false);
        }
        // 校验方法参数中起始波长不能小于终止波长,小于则友好提示
        if (start <= end) {
            //起始波长不能小于等于停止波长
            return Result.fail("00104015");
        }
        // 校验幻灯波长在330~390之间
        double changeWavelength = Double.parseDouble(paramDto.getChangeLampWaveLength());
        if (changeWavelength < 330.0 || changeWavelength > 390.0) {
            return Result.fail("00B02007");
        }
        if (paramDto.getScanSpeed().equals(ScanSpeedEnum.EXTREME_FAST.getCode()) && (start > 1100 || start < 380)) {
            return Result.fail("00B02008");
        }
        if (paramDto.getMethodName()==null) {
            return Result.fail("00B02009");
        }
        // 下发参数
        SetParam setParam = new SetParam();
        setParam.setPhotoMode(Integer.valueOf(paramDto.getLuminosity()));
        setParam.setSetWl(Double.valueOf(paramDto.getStartWaveLength()));
        boolean b = setInstrumentParameters(paramDto.getDeviceSerialNumber(), setParam);
        if (!b) {
            log.error("下发参数失败,请查看是否连接仪器");
        }

        // 使用isEmpty判断，如果为空，则返回失败
        if (paramDto.getUpdateBy().isEmpty()) {
            return Result.fail("00000007");
        }
        SysUserPO sysUserPO = systemUserManageDAO.selectByUsername(paramDto.getUpdateBy());

        // 判断参数是否发生变化，如果没有变化，直接返回，不再执行后续操作
        TMeasureMethod currentMethod = tMeasureMethodService.getById(paramDto.getMethodId());
        if (currentMethod == null) {
            return Result.fail("0030100C");
        }
        // 1. 获取数据库中的旧参数
        SpectrumParameter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), SpectrumParameter.class);

        // 2. 获取传入的新参数
        SpectrumParameter newParam = new SpectrumParameter();
        BeanUtils.copyProperties(paramDto, newParam);

        if (paramDto.getSpikeRemoval()){
            List<String> spikeArray = paramDto.getSpikeArray();
            List<String> newSpikeArray = new ArrayList<>();
            for (String s : spikeArray) {
                Double spike = Double.valueOf(s);
                if (spike <= start && spike >= end) {
                    newSpikeArray.add(s);
                }
            }

            paramDto.setSpikeArray(newSpikeArray);
            newParam.setSpikeArray(newSpikeArray);
        }

        // 3. 比较参数
        if (compareParam(oldParam, newParam)) {
            return Result.ok();
        }

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        boolean methodBoolean = false;
        // 比较方法名称跟查询的方法的方法名称是否一样
        if (!currentMethod.getMethodName().equals(paramDto.getMethodName())) {
            methodBoolean = true;
        }

        if (task.getTaskStatus().equals(TaskStatusEnum.UNTESTED.getCode())) {
            TMeasureMethod newMethod = createNewMethod(paramDto, methodBoolean);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail("0010000A");
            }
            task.setMethodKey(newMethod.getId());
            task.setCreateTime(LocalDateTime.now());
            task.setTaskName(paramDto.getTaskName());
            task.setSampleKey("[]");
            boolean update = tMeasureTaskService.updateById(task);
            return update ? Result.ok() : Result.fail("00000048");
        } else {
            TMeasureMethod newMethod = createNewMethod(paramDto, methodBoolean);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail("0010000A");
            }
            TMeasureTask newTask = createNewTask(paramDto, newMethod.getId(), task, sysUserPO.getId());
            if (newTask == null) {
                return Result.fail("00000010");
            }
            return Result.ok();
        }
    }

    /**
     * (实验测试)查询表格头
     *
     * @param taskId
     * @param methodId
     * @param analysisIds
     * @return
     */
    @Override
    public Result<List<HeadVO>> tableHead(String taskId, String methodId, List<String> analysisIds) {
        // 根据方法id查找方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        SpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<HeadVO> ans = new ArrayList<>();

        // 1. 编号
        ans.add(new HeadVO("编号", "id", "system", "编号"));

        // 2. 波长
        ans.add(new HeadVO("波长(nm)", "waveLength", "measure", "波长(nm)"));
        // 如果样品长度为1则是单样品表头,如果样品长度超过1则是多样品表头
        if (analysisIds.size() == 1) {
            addLuminosityMode(ans, parameter.getLuminosity());
            ans.add(new HeadVO("峰谷/标记点", "peakValleyGaugePoint", "measure", "峰谷"));
            ans.add(new HeadVO("备注", "remark", "analysis", "备注"));
            return Result.ok(ans);
        } else {

            // 遍历样品id进行逐个查询
            for (String analysisId : analysisIds) {
                // 根据单个样品id查询样品信息
                QueryWrapper<TMeasureAnalysisData> qw = new QueryWrapper<>();
                qw.eq("id", analysisId);
                TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(qw);

                // 如果样品信息不存在，则跳过当前id或返回错误
                if (analysisData == null) {
                    return Result.fail("00104016", analysisId);
                }
                if (analysisData.getSampleKey() != null) {
                    // 根据样品id查询样品
                    TMeasureSample sample = tMeasureSampleService.getById(analysisData.getSampleKey());
                    if (parameter.getRepetitionPattern().equals("不重复")) {
                        ans.add(new HeadVO(sample.getSampleName(), "sampleNumber", "name", "样品名称"));
                        ans.add(new HeadVO("峰/谷", "peakValley", "system", "峰谷"));
                    } else {
                        ans.add(new HeadVO(sample.getSampleName() + "-" + analysisData.getAnalysisNumber(), "sampleNumber", "name", "样品名称"));
                        ans.add(new HeadVO("峰/谷", "peakValley", "system", "峰谷"));
                    }
                } else {
                    // 按顺序添加到结果列表
                    ans.add(new HeadVO(analysisData.getAnalysisName(), "sampleNumber", "name", "样品名称"));
                    ans.add(new HeadVO("峰/谷", "peakValley", "system", "峰谷"));
                }
            }

            // 如果查询到的样品信息都有效，返回成功的结果
            return Result.ok(ans);
        }
    }

    /**
     * 查询表格数据
     */
    @Override
    public List<Map<String, Object>> queryTableData(String taskId,
                                                    List<String> analysisIds,
                                                    Integer digit) {
        // 校验参数
        if (StringUtils.isEmpty(taskId) || analysisIds == null || analysisIds.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> arrayList = new ArrayList<>();

        // 非重复模式 - 单样品
        if (analysisIds.size() == 1) {
            // 单样品表数据
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
                    map.put(SampleTableEnum.WAVELENGTH.getIdentity(), formatBigDecimal(formatBigDecimal(gaugePoint.getX(), 1), 1));
                    map.put(SampleTableEnum.LUMINOSITY.getIdentity(), formatBigDecimal(gaugePoint.getY(), digit));
                    map.put(SampleTableEnum.PEAKVALLEYGAUGEPOINT.getIdentity(), "标记点");
                    map.put(SampleTableEnum.REMARK.getIdentity(), gaugePoint.getRemark() != null ? gaugePoint.getRemark() : "");
                    arrayList.add(map);
                }
            }

            return arrayList;
        } else {
            // 多样品逻辑
            List<TMeasureAnalysisData> analysisDataList = new ArrayList<>();
            List<List<String>> allDataResults = new ArrayList<>(); // 存储所有样品的数据结果
            int maxDataSize = 0; // 最大数据量

            for (String analysisId : analysisIds) {
                TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(analysisId);
                if (analysisData != null) {
                    analysisDataList.add(analysisData);
                    // 解析数据结果
                    List<String> datas = JSON.parseArray(analysisData.getAnalysisResult(), String.class);
                    allDataResults.add(datas);
                    // 更新最大数据量
                    if (datas != null && datas.size() > maxDataSize) {
                        maxDataSize = datas.size();
                    }
                }
            }

            if (analysisDataList.isEmpty()) {
                return null;
            }

            // 构建波长映射，以第一个样品的所有波长为基准，如果第一个样品数据不足，则从其他样品补充
            Map<String, String> wavelengthMap = new LinkedHashMap<>(); // 使用LinkedHashMap保持插入顺序

            // 首先收集第一个样品的所有波长
            if (!allDataResults.isEmpty() && !allDataResults.get(0).isEmpty()) {
                for (String data : allDataResults.get(0)) {
                    String[] split = data.split(",");
                    if (split.length >= 1) {
                        String wavelength = formatBigDecimal(split[0], 1);
                        wavelengthMap.put(wavelength, wavelength);
                    }
                }
            }

            // 如果第一个样品数据不足，从其他样品补充波长（避免重复）
            if (wavelengthMap.size() < maxDataSize) {
                for (int i = 1; i < allDataResults.size(); i++) {
                    List<String> datas = allDataResults.get(i);
                    if (datas != null) {
                        for (String data : datas) {
                            String[] split = data.split(",");
                            if (split.length >= 1) {
                                String wavelength = formatBigDecimal(split[0], 1);
                                wavelengthMap.put(wavelength, wavelength);
                            }
                        }
                    }
                    // 如果已经收集到足够多的波长，可以提前退出
                    if (wavelengthMap.size() >= maxDataSize) {
                        break;
                    }
                }
            }

            // 转换为有序列表
            List<String> wavelengthList = new ArrayList<>(wavelengthMap.keySet());

            int id = 1;
            for (String wavelength : wavelengthList) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put(SampleTableEnum.ID.getIdentity(), id++);

                // 为每个样品初始化列表
                List<String> sampleNumbers = new ArrayList<>();
                List<String> peakValleys = new ArrayList<>();

                for (int i = 0; i < analysisDataList.size(); i++) {
                    TMeasureAnalysisData analysisData = analysisDataList.get(i);
                    List<String> datas = allDataResults.get(i);
                    List<PeakValleyMarking> peaks = JSON.parseArray(analysisData.getPeak(), PeakValleyMarking.class);
                    List<PeakValleyMarking> valleys = JSON.parseArray(analysisData.getGrain(), PeakValleyMarking.class);

                    boolean found = false;
                    if (datas != null) {
                        for (String data : datas) {
                            String[] split = data.split(",");
                            if (split.length >= 2) {
                                String currentWavelength = formatBigDecimal(split[0], 1);
                                if (Objects.equals(currentWavelength, wavelength)) {
                                    // 找到对应波长的数据
                                    sampleNumbers.add(formatBigDecimal(split[1], digit));

                                    // 检查是峰还是谷
                                    String peakValleyType = "";
                                    if (peaks != null) {
                                        for (PeakValleyMarking peak : peaks) {
                                            if (Objects.equals(formatBigDecimal(peak.getX(), 1), wavelength)) {
                                                peakValleyType = "峰";
                                                break;
                                            }
                                        }
                                    }
                                    if (valleys != null && peakValleyType.isEmpty()) {
                                        for (PeakValleyMarking valley : valleys) {
                                            if (Objects.equals(formatBigDecimal(valley.getX(), 1), wavelength)) {
                                                peakValleyType = "谷";
                                                break;
                                            }
                                        }
                                    }
                                    peakValleys.add(peakValleyType);
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }

                    // 如果没找到对应波长的数据，添加空值
                    if (!found) {
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
        return DataCountUtil.addZero(valueOf(decimal.stripTrailingZeros().toPlainString()), digit);
    }

    /**
     * 获取样品的详细数据
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureAnalysisData measureData, Integer digit) {
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
            if (gauges != null && peaks != null) {
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
            if (peaks != null && peaks.size() != 0) {
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
            if (grades != null && peaks.size() != 0) {
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
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureData measureData, Integer digit) {
        List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            if (digit != null) {
                BigDecimal value = new BigDecimal(split[1]);
                value = value.setScale(digit, RoundingMode.HALF_UP);
                map.put("x", split[0]);
                map.put("y", value.stripTrailingZeros().toPlainString());
            } else {
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
     *
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
     *
     * @param task
     * @param samples
     * @param response
     * @throws Exception
     */
    private void generateWord(TMeasureTask task, List<TMeasureSample> samples, List<TMeasureData> measureDataList, HttpServletResponse response) throws Exception {
        // 根据taskId查询方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 使用 Apache POI 创建 Word 文件
        XWPFDocument document = new XWPFDocument();

        // 添加标题
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("任务报告");
        titleRun.setFontSize(18);
        titleRun.setBold(true);
        titleRun.setFontFamily("黑体");
        titleRun.addBreak();

        // 添加任务信息
        XWPFParagraph taskInfo = document.createParagraph();
        taskInfo.setAlignment(ParagraphAlignment.LEFT);
        // 每条信息单独一行
        addTextRun(taskInfo, "任务名称: " + task.getTaskName(), 12, false, true);
        addTextRun(taskInfo, "测量类型: " + task.getMeasureType(), 12, false, true);
        addTextRun(taskInfo, "创建时间: " + task.getCreateTime().toString(), 12, false, true);
        addTextRun(taskInfo, "方法参数: ", 14, true, true);
        addTextRun(taskInfo, "方法名称: " + method.getMethodName(), 12, false, true);
        addTextRun(taskInfo, "是否共享: " + (method.getIsSharingMethod() == null || method.getIsSharingMethod() == "0" ? "否" : "是"), 12, false, true);
        addTextRun(taskInfo, "创建时间: " + method.getMethodCreateTime().format(formatter), 12, false, true);
        addTextRun(taskInfo, "创建人: " + method.getMethodCreator(), 12, false, true);
        addTextRun(taskInfo, "光度模式: " + (spectrumParameter.getLuminosity() == "1" ? "Abs" : spectrumParameter.getLuminosity() == "2" ? "T%" : spectrumParameter.getLuminosity() == "3" ? "Es" : spectrumParameter.getLuminosity() == "4" ? "Er" : ""), 12, false, true);
        addTextRun(taskInfo, "起始波长: " + spectrumParameter.getStartWaveLength(), 12, false, true);
        addTextRun(taskInfo, "结束波长: " + spectrumParameter.getEndWaveLength(), 12, false, true);
        addTextRun(taskInfo, "波长间隔: " + spectrumParameter.getWaveLengthInterval(), 12, false, true);
        addTextRun(taskInfo, "重复模式: " + (spectrumParameter.getRepetitionPattern() == "0" ? "不重复" : "单池重复"), 12, false, true);
        addTextRun(taskInfo, "重复次数: " + spectrumParameter.getRepetitionTime(), 12, false, true);
        addTextRun(taskInfo, "时间间隔: " + spectrumParameter.getTimeInterval(), 12, false, true);
        addTextRun(taskInfo, "阶梯矫正: " + (String.valueOf(spectrumParameter.getLadderCorrection()) == "true" ? "启用" : "不启用"), 12, false, true);

        addTextRun(taskInfo, "样品信息: ", 14, true, true);
        // 创建表格
        XWPFTable table = document.createTable();

//        // 设置表头
//        XWPFTableRow headerRow = table.getRow(0);
//        headerRow.getCell(0).setText("样品ID");
//        headerRow.addNewTableCell().setText("样品名称");
//        headerRow.addNewTableCell().setText("样品位置");
//
//
//        // 设置表格宽度：通过 XWPFTableCell 设置列宽
//        for (int i = 0; i < table.getRows().size(); i++) {
//            XWPFTableRow row = table.getRow(i);
//            for (int j = 0; j < row.getTableCells().size(); j++) {
//                XWPFTableCell cell = row.getCell(j);
//                // 设置单元格的宽度（单位为 twips：1 twip = 1/1440 英寸）
//                cell.setWidth("4000"); // 设置单元格宽度
//            }
//        }

        // 设置表头
        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("样品ID");
        headerRow.addNewTableCell().setText("样品名称");
        headerRow.addNewTableCell().setText("样品位置");
        CTTblWidth tableWidth = table.getCTTbl().addNewTblPr().addNewTblW();
        tableWidth.setW(BigInteger.valueOf(9072)); // 总表格宽度
        tableWidth.setType(STTblWidth.DXA);

        table.setWidth("100%");

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                XWPFParagraph para = cell.getParagraphs().get(0);
                para.setAlignment(ParagraphAlignment.CENTER);
                para.setVerticalAlignment(TextAlignment.CENTER);
                XWPFRun run = para.createRun();
                run.setFontSize(11);
                run.setFontFamily("宋体");
                run.setBold(false);
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

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"task_report.docx\"");
        document.write(response.getOutputStream());
    }

    /**
     * 添加文本
     *
     * @param paragraph
     * @param text
     * @param fontSize
     * @param bold
     * @param isLineFeed
     */
    private void addTextRun(XWPFParagraph paragraph, String text, int fontSize, boolean bold, boolean isLineFeed) {
        XWPFRun run = paragraph.createRun();
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("宋体");
        run.setText(text);
        run.addBreak();
        if (isLineFeed) {
            run.addBreak();
        }
    }

    /**
     * 数字字符串归一化（防止 360 / 360.0 / 3.6E2 比较失败）
     */
    private String normalizeNumberString(String numStr) {
        return new BigDecimal(numStr)
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * 保留原有的downloadWord方法，并适配新参数
     *
     * @param taskId
     * @param allDisplay
     * @param analysisIds
     * @return
     */
    public R<File> downloadWord(String taskId, Boolean allDisplay, List<String> analysisIds, Integer digit,String deviceSerialNumber) {
        return downloadFile(taskId, allDisplay, "word", analysisIds, digit,deviceSerialNumber);
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
            log.debug("【generateWordFile】写入波长: {}", wavelength);

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
                    // 从Map取值：保留原始值（不预设默认空字符串）
                    Object originalValue = map.get(identity);
                    String cellData;

                    // 核心：空值处理（null/空字符串/"null" 统一替换为"-"，其他保留原始值）
                    if (originalValue == null) {
                        cellData = "-";
                    } else {
                        // 原始值直接转字符串（不做枚举转换、前缀拼接等任何额外处理）
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

        // 2. 转换表头（处理空表头和null元素）
        List<List<String>> excelHeads = new ArrayList<>();
        for (int i = 0; i < heads.size(); i++) {
            HeadVO head = heads.get(i);
            // 防护：当前表头为null
            if (head == null) {
                log.warn("表头列表第{}个元素为null，使用默认表头", i);
                excelHeads.add(Collections.singletonList("未命名列_" + i));
                continue;
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
                Object cellData;

                // 核心逻辑：空值处理（null/空字符串/"null" 统一替换为"-"，其他保留原始值）
                if (originalValue == null) {
                    cellData = "-";
                } else {
                    // 如果是数字字符串，尝试转换为 Double，让 Excel 识别为数值类型
                    cellData = convertToNumberIfNeeded(originalValue.toString());
                    // 清除 Excel 解析异常的特殊字符（换行/制表符，不修改原始业务内容）
                    if (cellData instanceof String) {
                        cellData = ((String) cellData).replace("\r", "").replace("\n", "").replace("\t", " ");
                        // 处理空字符串或"null"（不区分大小写）
                        if (((String) cellData).trim().isEmpty() || "null".equalsIgnoreCase((String) cellData)) {
                            cellData = "-";
                        }
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
    /**
     * 尝试将字符串转换为数值类型
     * @param value 待转换的字符串
     * @return 如果是有效数字则返回 Double，否则返回原字符串
     */
    private Object convertToNumberIfNeeded(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value)) {
            return value;
        }

        try {
            // 尝试去除空格
            String trimmed = value.trim();
            // 尝试转换为 Double
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            // 转换失败，返回原字符串
            return value;
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
     * 验证参数
     *
     * @param parameter
     * @return
     */
    private Result validateSpectrumParam(SpectrumParameterVo parameter) {
        // 必填字段校验
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getLuminosity())) {
            return Result.fail("00B0200A");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getStartWaveLength())) {
            return Result.fail("00B0200B");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getEndWaveLength())) {
            return Result.fail("00B0200C");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getWaveLengthInterval())) {
            return Result.fail("00B0200D");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getTimeInterval())) {
            return Result.fail("00B0200E");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getScanSpeed())) {
            return Result.fail("00B0200F");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getSpectralBandwidth())) {
            return Result.fail("00B02010");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getChangeLampWaveLength())) {
            return Result.fail("00B02011");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getEnergyLamp())) {
            return Result.fail("00B02012");
        }
        if (parameter.getGain() == null) {
            return Result.fail("00B02013");
        }

        return Result.ok();
    }

    /**
     * 设置方法参数
     *
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

    @Override
    public Result stopMeasure(String deviceSerialNumber) {
        // 1. 校验参数
        if (!StringUtils.hasText(deviceSerialNumber)) {
            return Result.fail("00000001");
        }
        // 2. 将测量停止状态设置为false
        Constants.measureStopStatus.put(deviceSerialNumber, false);
        // 3. 仪器下发停止命令
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.Stop.getType());
        R<OperationResultEntity> r = devOperationUtilService.operation(inputParam);
        if (r.getSuccess()) {
            return Result.ok();
        } else {
            return Result.fail(r.getMessage());
        }
    }

    @Override
    public Result updateMethod(TMeasureMethod methodDto) {
        LambdaQueryWrapper<TMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TMeasureTask::getMethodKey, methodDto.getId())
                .eq(TMeasureTask::getIsDelete, 0);
        long count = tMeasureTaskService.count(wrapper);
        if (count > 0) {
            return Result.ok("00B02014");
        }

        boolean b = tMeasureMethodService.updateById(methodDto);
        return b?Result.ok():Result.fail();
    }

    /**
     * 比较参数
     *
     * @param method
     * @param newParam
     * @return
     */
    private boolean compareParam(SpectrumParameter method, SpectrumParameter newParam) {
        String dbJson = JSON.toJSONString(method, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newParam, SerializerFeature.SortField);

        return dbJson.equals(frontJson);
    }

    /**
     * 创建新的方法
     */
    private TMeasureMethod createNewMethod(SpectralMethodParameterDto paramDto, Boolean isCopy) {
        TMeasureMethod fMeasureMethod = new TMeasureMethod();
        if (isCopy) {
            fMeasureMethod.setMethodName(paramDto.getMethodName());
        } else {
            fMeasureMethod.setMethodName(paramDto.getMethodName() + "- 副本");
        }

        SpectrumParameter parameter = new SpectrumParameter();
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
    private TMeasureTask createNewTask(SpectralMethodParameterDto paramDto, String methodId, TMeasureTask task, String userId) {
        TMeasureTask newTask = new TMeasureTask();
        newTask.setUserId(userId);
        if (task.getTaskName().equals(paramDto.getTaskName()) || paramDto.getTaskName() == null || paramDto.getTaskName().isEmpty()) {
            newTask.setTaskName(task.getTaskName() + "- 副本");
        } else {
            newTask.setTaskName(paramDto.getTaskName());
        }
        newTask.setCreateTime(LocalDateTime.now());
        newTask.setStartTime(LocalDateTime.now());
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
     * 获取样品的原始数据
     * 该方法与 getSampleData 方法逻辑相同，但不会进行小数位数限制
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getOriginalSampleData(TMeasureAnalysisData measureData,
                                                           int digit) {
        // 解析分析结果、峰、谷、标记点
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<FPeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), FPeakValleyMarking.class);
        List<FPeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), FPeakValleyMarking.class);
        List<FPeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), FPeakValleyMarking.class);

        // 存放数据的结果列表
        List<Map<String, Object>> resultList = new ArrayList<>();

        // 遍历每个分析数据
        for (String s : list) {
            String[] split = s.split(","); // 以逗号分割
            Map<String, Object> map = new HashMap<>();

            // 处理原始数据，直接使用原始的值
            map.put("x", split[0]);
            map.put("y", split[1]);  // 无小数位限制，直接使用原始值
            Double y = new Double(split[1]);
            Double value = DataCountUtil.formatValue(digit, y);
            String valueY = DataCountUtil.addZero(value, digit);
            map.put("astrictY", valueY);

            // 标记点数据处理
            if (gauges != null && !gauges.isEmpty()) {
                for (FPeakValleyMarking gauge : gauges) {
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
                for (FPeakValleyMarking peak : peaks) {
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
                for (FPeakValleyMarking grade : grades) {
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
     * 处理重复模式下的删除逻辑
     * 删除所有相关的测量数据和分析数据
     */
    private void handleDeleteForRepeatMode(String sampleId, String taskId) {
        // 查询所有未删除的测量数据
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getSampleKey, sampleId)
                .eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getIsDelete, 0);
        List<TMeasureData> measureDataList = tMeasureDataService.list(queryWrapper);

        if (measureDataList != null && !measureDataList.isEmpty()) {
            for (TMeasureData measureData : measureDataList) {
                // 标记测量数据为删除
                measureData.setIsDelete(deleteType);
                tMeasureDataService.updateById(measureData);

                // 查询并标记相关的分析数据为删除
                LambdaQueryWrapper<TMeasureAnalysisData> analysisQueryWrapper = new LambdaQueryWrapper<>();
                analysisQueryWrapper.eq(TMeasureAnalysisData::getSampleKey, sampleId)
                        .eq(TMeasureAnalysisData::getTaskId, taskId)
                        .eq(TMeasureAnalysisData::getMeasureDataId, measureData.getId())
                        .eq(TMeasureAnalysisData::getIsDelete, 0);
                List<TMeasureAnalysisData> analysisDataList = tMeasureAnalysisDataService.list(analysisQueryWrapper);

                if (analysisDataList != null && !analysisDataList.isEmpty()) {
                    for (TMeasureAnalysisData analysisData : analysisDataList) {
                        analysisData.setIsDelete(deleteType);
                        tMeasureAnalysisDataService.updateById(analysisData);
                    }
                }
            }
        }
    }

    /**
     * 处理不重复模式下的删除逻辑
     * 只删除最新的测量数据
     */
    private void handleDeleteForNonRepeatMode(String sampleId, String taskId) {
        // 查询最新的未删除的测量数据
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getSampleKey, sampleId)
                .eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getIsDelete, 0)
                .last("LIMIT 1");

        // 获取最新的一条
        List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            TMeasureData latestData = list.get(0);

            // 标记测量数据为删除
            latestData.setIsDelete(deleteType);
            tMeasureDataService.updateById(latestData);

            // 查询并标记相关的分析数据为删除
            LambdaQueryWrapper<TMeasureAnalysisData> analysisQueryWrapper = new LambdaQueryWrapper<>();
            analysisQueryWrapper.eq(TMeasureAnalysisData::getSampleKey, sampleId)
                    .eq(TMeasureAnalysisData::getTaskId, taskId)
                    .eq(TMeasureAnalysisData::getMeasureDataId, latestData.getId())
                    .eq(TMeasureAnalysisData::getIsDelete, 0);
            List<TMeasureAnalysisData> analysisDataList = tMeasureAnalysisDataService.list(analysisQueryWrapper);

            if (analysisDataList != null && !analysisDataList.isEmpty()) {
                for (TMeasureAnalysisData analysisData : analysisDataList) {
                    analysisData.setIsDelete(deleteType);
                    tMeasureAnalysisDataService.updateById(analysisData);
                }
            }
        }
    }

    /**
     * 调用Python服务
     *
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
                throw new RuntimeException("00000032"); // "算法服务返回异常"
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
     * 校验光谱扫描参数
     * @param parameter 光谱扫描参数
     * @return 校验结果
     */
    private R<Boolean> checkParam(SpectrumParameter parameter,String deviceSerialNumber) {
        // 1. 根对象非空校验
        if (parameter == null) {
            return R.fail("00102003");
        }

        // 2. 光度模式校验
        String luminosity = parameter.getLuminosity();
        if (!StringUtils.hasText(luminosity)) {
            return R.fail("00B03007");
        }



        List<DeviceConfParam> defaultValue = (List<DeviceConfParam>) MappingParamEnum.LUMINOSITY.getDefaultValue();
        // 使用steam流将defaultValue的value值取出来作为一个List<String>
        List<String> validLuminosityModes = defaultValue.stream()
                .map(DeviceConfParam::getParamValue)
                .collect(Collectors.toList());
        if (!validLuminosityModes.contains(luminosity)) {
            return R.fail("00B03008", luminosity);
        }

        // 3. 波长参数校验
        String startWaveStr = parameter.getStartWaveLength();
        String endWaveStr = parameter.getEndWaveLength();
        String intervalStr = parameter.getWaveLengthInterval();

        if (!StringUtils.hasText(startWaveStr)) {
            return R.fail("00B02018");
        }
        if (!StringUtils.hasText(endWaveStr)) {
            return R.fail("00B02019");
        }
        if (!StringUtils.hasText(intervalStr)) {
            return R.fail("00B0201A");
        }

        // 波长默认范围 185-1100
        double minWaveLength = Double.parseDouble(MappingParamEnum.MIN_WAVELENGTH.getDefaultValue().toString());
        double maxWaveLength = Double.parseDouble(MappingParamEnum.MAX_WAVELENGTH.getDefaultValue().toString());
        double minLampWaveLength = Double.parseDouble(MappingParamEnum.MIN_LAMP_WAVELENGTH.getDefaultValue().toString());
        double maxLampWaveLength = Double.parseDouble(MappingParamEnum.MAX_LAMP_WAVELENGTH.getDefaultValue().toString());

        if (StringUtils.hasText(deviceSerialNumber)) {
            R<DeviceMeasureVO> typeInfo = deviceManageService.queryDeviceTypeInfo(deviceSerialNumber);
            if (typeInfo.getSuccess()) {
                DeviceMeasureVO vo = typeInfo.getData();
                if (vo != null) {
                    minWaveLength = StringUtils.hasText(vo.getMinWavelength()) ? Double.parseDouble(vo.getMinWavelength()) : minWaveLength;
                    maxWaveLength = StringUtils.hasText(vo.getMaxWavelength()) ? Double.parseDouble(vo.getMaxWavelength()) : maxWaveLength;
                    minLampWaveLength = StringUtils.hasText(vo.getMinLampWavelength()) ? Double.parseDouble(vo.getMinLampWavelength()) : minLampWaveLength;
                    maxLampWaveLength = StringUtils.hasText(vo.getMaxLampWavelength()) ? Double.parseDouble(vo.getMaxLampWavelength()) : maxLampWaveLength;
                }

            }
        }


        double startWave = 0;
        double endWave = 0;

        try {
            startWave = Double.parseDouble(startWaveStr);
            if (startWave < minWaveLength || startWave > maxWaveLength) {
                return R.fail("00102006",1, startWave, minWaveLength, maxWaveLength);
            }
            if (startWaveStr.contains(".") && startWaveStr.split("\\.")[1].length() > 1) {
                return R.fail("00105005",startWaveStr);
            }

            endWave = Double.parseDouble(endWaveStr);
            if (endWave < minWaveLength || endWave > maxWaveLength) {
                return R.fail("00102006",2, endWave, minWaveLength, maxWaveLength);
            }
            if (endWaveStr.contains(".") && endWaveStr.split("\\.")[1].length() > 1) {
                return R.fail("00105005",endWaveStr);
            }

            if (startWave < endWave) {
                return R.fail("00B0201B", startWave, endWave);
            }

            double interval = Double.parseDouble(intervalStr);
            if (interval <= 0) {
                return R.fail("00B0201C", interval);
            }
            if (intervalStr.contains(".") && intervalStr.split("\\.")[1].length() > 2) {
                return R.fail("00B0201D", intervalStr);
            }

        } catch (NumberFormatException e) {
            return R.fail("00B0201E");
        }

        // 4. 扫描速度校验
        String scanSpeed = parameter.getScanSpeed();
        if (!StringUtils.hasText(scanSpeed)) {
            return R.fail("00B0201F");
        }

        // 5. 重复模式校验
        String repetitionPattern = parameter.getRepetitionPattern();
        if (!StringUtils.hasText(repetitionPattern)) {
            return R.fail("00B02020");
        }
        List<String> validPatterns = Arrays.asList("不重复", "单池重复"); // 0.不重复 1.单池重复
        if (!validPatterns.contains(repetitionPattern)) {
            return R.fail("00B02021", repetitionPattern);
        }

        String changeLampWave = parameter.getChangeLampWaveLength();
        if (!StringUtils.hasText(changeLampWave)) {
            return R.fail("00105003");
        }
        try {
            double waveVal = Double.parseDouble(changeLampWave);
            if (waveVal < minLampWaveLength || waveVal > maxLampWaveLength) {
                return R.fail("0010202A",waveVal);
            }
        } catch (NumberFormatException e) {
            return R.fail("00B0301B");
        }


        // 单池重复模式的详细校验
        if ("1".equals(repetitionPattern)) {
            String repetitionTimeStr = parameter.getRepetitionTime();
            if (!StringUtils.hasText(repetitionTimeStr)) {
                return R.fail("00B02022");
            }
            try {
                int repetitionTime = Integer.parseInt(repetitionTimeStr);
                if (repetitionTime < 2 || repetitionTime > 10) {
                    return R.fail("00102018",repetitionTime);
                }
            } catch (NumberFormatException e) {
                return R.fail("00102019",repetitionTimeStr);
            }

            String timeIntervalStr = parameter.getTimeInterval();
            if (!StringUtils.hasText(timeIntervalStr)) {
                return R.fail("00B02023");
            }
            try {
                double timeInterval = Double.parseDouble(timeIntervalStr);
                if (timeInterval < 0 || timeInterval > 60) {
                    return R.fail("0010201B",timeInterval);
                }
                if (timeIntervalStr.contains(".") && timeIntervalStr.split("\\.")[1].length() > 1) {
                    return R.fail("0010201C",timeIntervalStr);
                }
            } catch (NumberFormatException e) {
                return R.fail("0010201D",timeIntervalStr);
            }
        }

        // 6. 阶梯校正校验（Boolean类型无需复杂校验，只需确保有值？但可为null代表不启用）
        // 这里不做强制要求，Boolean允许为null

        // 7. 消除尖刺校验（新增）
        Boolean spikeRemoval = parameter.getSpikeRemoval();
        if (spikeRemoval != null && spikeRemoval) {
            // 校验光度模式是否为Abs(1)或T%(2)
            if (!"1".equals(luminosity) && !"2".equals(luminosity)) {
                return R.fail("00B02024", getLuminosityDesc(luminosity));
            }

            List<String> spikeArray = parameter.getSpikeArray();
            if (spikeArray == null || spikeArray.isEmpty()) {
                return R.fail("00B02025");
            }

            try {
                for (int i = 0; i < spikeArray.size(); i++) {
                    String spikeStr = spikeArray.get(i);
                    if (!StringUtils.hasText(spikeStr)) {
                        return R.fail("00B02026", i + 1);
                    }

                    double spikeValue = Double.parseDouble(spikeStr.trim());

                    // 校验尖刺点是否在波长范围内
                    if (spikeValue > startWave || spikeValue < endWave) {
                        return R.fail("00B02027", spikeStr);
                    }

                    // 校验小数点后最多1位（根据业务需求调整）
                    if (spikeStr.contains(".") && spikeStr.split("\\.")[1].length() > 1) {
                        return R.fail("00B02028", spikeStr);
                    }
                }


                Set<String> uniqueSpikes = new HashSet<>(spikeArray);
                if (uniqueSpikes.size() < spikeArray.size()) {
                    return R.fail("00B02029");
                }
            } catch (NumberFormatException e) {
                return R.fail("00B0202A", spikeArray);
            }
        }

        // 8. 父类 MeasurePublicParameters 字段校验
        // 8.1 光谱带宽校验
        if (!StringUtils.hasText(parameter.getSpectralBandwidth())) {
            return R.fail("00B03018");
        }
        try {
            double bandwidth = Double.parseDouble(parameter.getSpectralBandwidth());
            if (bandwidth <= 0) {
                return R.fail("00B03019", bandwidth);
            }
        } catch (NumberFormatException e) {
            return R.fail("00B0301A");
        }

        // 8.2 响应时间校验（如果有业务规则）
        // 目前没有明确规则，如果有需要可补充

        // 8.4 能量灯校验
        String energyLamp = parameter.getEnergyLamp();
        if (!StringUtils.hasText(energyLamp)) {
            return R.fail("00B0301C");
        }
        if (!energyLamp.matches("^[0-2]$")) {
            return R.fail("00B0301D", energyLamp);
        }

        // 8.5 增益校验
        if (parameter.getGain() != null && parameter.getGain() < 0) {
            return R.fail("00B0301E", parameter.getGain());
        }

        // 8.6 参考标准名称校验（长度限制等）
        if (parameter.getReferenceStandardName() != null &&
                parameter.getReferenceStandardName().length() > 100) {
            return R.fail("00B0301F");
        }

        return R.ok(true);
    }

    /**
     * 获取光度模式描述
     * @param luminosity 光度模式编码
     * @return 光度模式描述
     */
    private String getLuminosityDesc(String luminosity) {
        switch (luminosity) {
            case "1":
                return "Abs";
            case "2":
                return "T%";
            case "3":
                return "Es";
            case "4":
                return "Er";
            default:
                return "未知";
        }
    }

    /**
     * 检查光谱扫描参数是否改变
     * @param oldParam 旧参数
     * @param newParam 新参数
     * @return true--未改变，false--改变了
     */
    private boolean checkSpectrumParam(SpectrumParameter oldParam, SpectrumParameter newParam) {
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
                Objects.equals(oldParam.getStartWaveLength(), newParam.getStartWaveLength()) &&
                Objects.equals(oldParam.getEndWaveLength(), newParam.getEndWaveLength()) &&
                Objects.equals(oldParam.getWaveLengthInterval(), newParam.getWaveLengthInterval()) &&
                Objects.equals(oldParam.getRepetitionPattern(), newParam.getRepetitionPattern()) &&
                Objects.equals(oldParam.getRepetitionTime(), newParam.getRepetitionTime()) &&
                Objects.equals(oldParam.getTimeInterval(), newParam.getTimeInterval()) &&
                Objects.equals(oldParam.getLadderCorrection(), newParam.getLadderCorrection()) &&
                Objects.equals(oldParam.getScanSpeed(), newParam.getScanSpeed())&&
                Objects.equals(oldParam.getSpikeRemoval(), newParam.getSpikeRemoval())&&
                Objects.equals(oldParam.getSpikeArray(), newParam.getSpikeArray());
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
            return Result.fail("00B02015");
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
                return Result.fail("00B02016", dataPoint);
            }
        }

        // 验证每个标记点
        for (String xStr : gaugePoints) {
            try {
                double x = Double.parseDouble(xStr);
                // 允许有0.01的浮点数误差
                if (x < minWave - 0.01 || x > maxWave + 0.01) {
                    String message = String.format("标记点 %.2f 超出波长范围 [%.2f, %.2f]",
                            x, minWave, maxWave);
                    return Result.fail(message);
                }
            } catch (NumberFormatException e) {
                return Result.fail("00B02017", xStr);
            }
        }

        return null; // 校验通过
    }
    /**
     * 下发给仪器的参数校验
     * @param param
     * @param deviceSerialNumber
     * @return
     */
    private R IssueCheckParam(SpectrumParameter param, String deviceSerialNumber) {
        Boolean result = true;
        List<String> errorMessages = new ArrayList<>();
        R<DeviceMeasureVO> deviceMeasureVOR = deviceManageService.queryDeviceTypeInfo(deviceSerialNumber);
        if (!deviceMeasureVOR.getSuccess()) {
            return R.fail(deviceMeasureVOR.getMessage());
        }
        DeviceMeasureVO deviceMeasureVO = deviceMeasureVOR.getData();
        // 1. 光度模式是否存在
        boolean a = deviceMeasureVO.getLuminosity().stream()
                .anyMatch(obj -> obj.getParamValue().equals(param.getLuminosity()));
        if (!a) {
            result = result ? false : result;
            errorMessages.add("当前仪器不支持该光度模式");
        }
        // 2. 波长范围是否超出限制
        Double startWaveLength = Double.parseDouble(param.getStartWaveLength());
        Double endWaveLength = Double.parseDouble(param.getEndWaveLength());
        Double maxWavelength =  Double.parseDouble(deviceMeasureVO.getMaxWavelength());
        Double minWavelength =  Double.parseDouble(deviceMeasureVO.getMinWavelength());
        if (startWaveLength > maxWavelength || endWaveLength < minWavelength) {
            result = result ? false : result;
            errorMessages.add("波长范围超出限制, 最大值: "+maxWavelength+"  最小值: "+minWavelength);
        }
        // 3. 波长间隔是否存在
        if (!deviceMeasureVO.getScanInterval().contains(param.getWaveLengthInterval())) {
            result = result ? false : result;
            errorMessages.add("当前仪器不支持该波长间隔");
        }
        // 4. 光谱带宽是否存在
        if (!deviceMeasureVO.getBandWidth().contains(param.getSpectralBandwidth())) {
            result = result ? false : result;
            errorMessages.add("当前仪器不支持该光谱带宽");
        }
        // 5. 扫描速度是否存在
        boolean anyMatch = deviceMeasureVO.getScanSpeed().stream()
                .anyMatch(obj -> obj.getParamName().equals(param.getScanSpeed()));
        if (!anyMatch) {
            result = result ? false : result;
            errorMessages.add("当前仪器不支持该扫描速度");
        }
        // 6. 换灯波长是否超出限制
        Double lampWaveLength = Double.parseDouble(param.getChangeLampWaveLength());
        Double maxLampWavelength =  Double.parseDouble(deviceMeasureVO.getMaxLampWavelength());
        Double minLampWavelength =  Double.parseDouble(deviceMeasureVO.getMinLampWavelength());
        if (lampWaveLength > maxLampWavelength || lampWaveLength < minLampWavelength) {
            result = result ? false : result;
            errorMessages.add("换灯波长超出限制, 最大值: "+maxLampWavelength+"  最小值: "+minLampWavelength);
        }
        // 7. 在光度模式为Es/Er模式下的情况
        if (param.getLuminosity().equals(Constants.PARAM_THREE)|| param.getLuminosity().equals(Constants.PARAM_FOUR)) {
            // 7.1 能量灯是否存在
            boolean b = deviceMeasureVO.getLampType().stream()
                    .anyMatch(obj -> obj.getParamValue().equals(param.getEnergyLamp()));
            if (!b) {
                result = result ? false : result;
                errorMessages.add("当前仪器不支持该能量灯");
            }

            // 7.2 增益是否存在
            if (!deviceMeasureVO.getGain().contains(param.getGain())) {
                result = result ? false : result;
                errorMessages.add("当前仪器不支持该增益数值");
            }
        }

        if (result){
            return R.ok(true);
        }else {
            return R.fail(errorMessages);
        }

    }
}
