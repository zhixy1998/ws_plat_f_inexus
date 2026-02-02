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
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.guide.GuideCreateSampleDTO;
import com.persee.workstation.uv_inexus.dto.guide.SampleCreateHeadConf;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemBusinessParamService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSampleVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.InputParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.PreCountResultParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("rationService")
public class RationServiceImpl implements RationService {

    @Autowired
    SysUserMapper userManageDAO;
    @Autowired
    TMeasureTaskMapper tMeasureTaskMapper;
    @Autowired
    TMeasureProjectService tMeasureProjectService;
    @Autowired
    TMeasureSampleService tMeasureSampleService;
    @Autowired
    TMeasureTaskService tMeasureTaskService;
    @Autowired
    TMeasureMethodService tMeasureMethodService;

    @Autowired
    TMeasureDataService tMeasureDataService;

    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    TMeasureTaskAttachmentService taskAttachmentService;

    @Autowired
    SamplePoolConfigService tSamplePoolConfigService;

    @Autowired
    PublicUtilService publicUtilService;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    RestUtil restUtil;

    @Autowired
    GuidePublicUtilService guidePublicUtilService;

    @Autowired
    DevOperationUtilService devOperationUtilService;

    @Autowired
    SystemBusinessParamService businessParamService;

    @Value("default")
    String defaultMethodProjectName;

    @Value("文件数据")
    String newTaskName;

    @Value("默认方法")
    String defaultMethodName;

    @Value("1")
    private String deleteType;

    @Value("0")
    private String createType;

    @Value("0")
    private String stand;

    @Value("1")
    private String unknown;

    @Value("0")
    String noTested;

    @Value("1")
    String tested;

    private static final DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    @Override
    public Result updateMethodParameter(MethodParamsDto methodDto) {
        if (!StringUtils.hasText(methodDto.getCurrentMethodId())){
            //方法id不能为空,请创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000021"));
        }
        // 通用参数：方法名称
        if (methodDto.getMethodName() != null && methodDto.getMethodName().length() > 256) {
            //方法名格式错误
            return Result.fail(YamlMessageSource.getMessageStatic("00300002"));
        }
        // 通用参数：参考标准方法长度（≤256）
        if (methodDto.getReferenceStandardName() != null && methodDto.getReferenceStandardName().length() > 256) {
            //参考标准方法长度超限
            return Result.fail(YamlMessageSource.getMessageStatic("00303001"));
        }
        // 通用参数：前处理参数 每个成员≤500字符，最多10个
        if (methodDto.getPreProcessParameters() != null) {
            if (methodDto.getPreProcessParameters().size() > 10) {
                //前处理参数个数超限
                return Result.fail(YamlMessageSource.getMessageStatic("00303002"));
            }
            for (String p : methodDto.getPreProcessParameters()) {
                if (p != null && p.length() > 500) {
                    //前处理参数长度超限
                    return Result.fail(YamlMessageSource.getMessageStatic("00303003"));
                }
            }
        }

        R<Boolean> r = checkParam(methodDto);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }

        // 测量类参数（定量测定）
        // 波长值：1-3个成员，数值190-900，保留1位小数
//        if (methodDto.getWaveLength() != null) {
//            if (methodDto.getWaveLength().isEmpty() || methodDto.getWaveLength().size() > 3) {
//                //波长个数需在1-3之间
//                return Result.fail(YamlMessageSource.getMessageStatic("00303004"));
//            }
//            for (String wl : methodDto.getWaveLength()) {
//                //波长超出范围(190-900)
//                try { double v = Double.parseDouble(wl); if (v < 190.0 || v > 900.0) return Result.fail(YamlMessageSource.getMessageStatic("00303005"));
//                    String s = String.format(java.util.Locale.ROOT, "%.1f", v);
//                    //波长需保留1位小数
//                    if (!s.equals(String.valueOf(v)) && !String.valueOf(v).matches("^\\d+\\.\\d$")) return Result.fail(YamlMessageSource.getMessageStatic("00303006"));
//                } catch (Exception e) {
//                    //波长格式错误
//                    return Result.fail(YamlMessageSource.getMessageStatic("00303007")); }
//            }
//        }
//        // 波长系数：1-3个成员
//        if (methodDto.getCoefficientMode().equals(RationCurveParamEnums.direct_coefficient.getIdentity())) {
//            if (methodDto.getCoefficientParams() != null) {
//                if (methodDto.getCoefficientParams().isEmpty() || methodDto.getCoefficientParams().size() > 3) {
//                    //波长系数个数需在1-3之间
//                    return Result.fail(YamlMessageSource.getMessageStatic("00303018"));
//                }
//            }
//            // 曲线次数：1-4
//            if (org.springframework.util.StringUtils.hasText(methodDto.getCurveType())) {
//                //曲线次数范围应为1-4
//                try { int n = Integer.parseInt(methodDto.getCurveType()); if (n < 1 || n > 4) return Result.fail(YamlMessageSource.getMessageStatic("00303008"));
//                } catch (Exception ignored) {}
//            }
//            // 方程类型：枚举
////        if (StringUtils.hasText(methodDto.getEquationType())) {
////          Set<String> allow = new HashSet<>(Arrays.asList("qtCFA","qtAFC"));
////            if (!allow.contains(methodDto.getEquationType()))
////                return Result.fail("方程类型不合法");
////        }
////        // 零点方式：字节，默认0 - 此处放宽，若提供需为0-255的整数
////        // 曲线系数方式：枚举
////        if (org.springframework.util.StringUtils.hasText(methodDto.getCoefficientMode())) {
////            Set<String> allow = new HashSet<>(Arrays.asList("ctInput","ctCurve","0","1"));
////            if (!allow.contains(methodDto.getCoefficientMode())) return Result.fail("曲线系数方式不合法");
////        }
//            // 曲线系数数据：1-10 个成员
//            if (methodDto.getCoefficientParams() == null || methodDto.getCoefficientParams().isEmpty() || methodDto.getCoefficientParams().size() > 10) {
//                //曲线系数数据个数需在1-10之间
//                return Result.fail(YamlMessageSource.getMessageStatic("00303009"));
//            }
//        }
//
//
//        // 拟合结果单位：1-10字符
//        // 该字段在定量分析结果中，若存在则校验
//        if (methodDto.getResultParams() != null) {
//            for (PreCountResultParam r : methodDto.getResultParams()) {
//                System.out.println(r.getUnit().length());
//                if (r.getUnit() == null || r.getUnit().isEmpty() || r.getUnit().length() > 10) {
//                    //结果单位长度应为1-10
//                    return Result.fail(YamlMessageSource.getMessageStatic("0030300A"));
//                }
//            }
//        }

        // 根据方法id查询当前方法
        TMeasureMethod method = tMeasureMethodService.getById(methodDto.getCurrentMethodId());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        RationParameter parameter = new RationParameter();
        BeanUtils.copyProperties(methodDto, parameter);
        parameter.setEnergyLampName(EnergyLampEnums.getName(parameter.getEnergyLamp()));
        parameter.setCurveTypeName(RationCurveParamEnums.getName(parameter.getCurveType()));
        parameter.setCoefficientModeName(RationCurveParamEnums.getName(parameter.getCoefficientMode()));
        parameter.setEquationTypeName(RationCurveParamEnums.getName(parameter.getEquationType()));
        parameter.setDifferentialModeName(RationCurveParamEnums.getName(parameter.getDifferentialMode()));
        if (parameter.getZeroMethod() != null && !parameter.getZeroMethod().isEmpty()) {
            StringBuilder zeroName = new StringBuilder();
            for (String s : parameter.getZeroMethod()) {
                zeroName.append(RationCurveParamEnums.getName(s)).append(",");
            }
            zeroName.deleteCharAt(zeroName.length() - 1);
            parameter.setZeroMethodName(zeroName.toString());
        }
        /*
         *TODO 待计算结果，暂时加标识，后续需要修改
         */
        for (PreCountResultParam resultParam : parameter.getResultParams()) {
            resultParam.setParam(resultParam.getResultIdentity() + "-" + resultParam.getFormulaText());
        }

        String param = JSON.toJSONString(parameter);
        // 更新方法参数
        method.setMeasurementParamInfo(param);
        // 更新方法
        boolean update = tMeasureMethodService.updateById(method);
        return Result.ok(update);
    }


    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, RationParameter rationParameter) {
        if (rationParameter.getWaveLength() == null || rationParameter.getWaveLength().isEmpty()) {
            //请填写波长参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000022"));
        }

        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(rationParameter));
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

    @Override
    public Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<TMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TMeasureTask::getUserId, id)
                .eq(TMeasureTask::getMeasureType, measureType)
                .eq(TMeasureTask::getIsDelete, createType)
                .orderByDesc(TMeasureTask::getCreateTime);
        List<TMeasureTask> taskList = tMeasureTaskService.list(wrapper);
        if (taskList != null && !taskList.isEmpty()){
            TMeasureTask task = taskList.get(0);
            String projectItemKey = task.getProjectItemKey();
            String methodKey = task.getMethodKey();

            TMeasureProject project = tMeasureProjectService.getById(projectItemKey);
            TMeasureMethod method = tMeasureMethodService.getById(methodKey);
            MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
            return Result.ok(vo);
        }

        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();

        LambdaQueryWrapper<TMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureMethod::getMethodCreator, updateBy)
                .eq(TMeasureMethod::getMeasurementType, measureType)
                .orderByDesc(TMeasureMethod::getMethodCreateTime);
        List<TMeasureMethod> list = tMeasureMethodService.list(queryWrapper);
        if (list == null|| list.isEmpty()){
            LambdaQueryWrapper<TMeasureProject> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(TMeasureProject::getCreatorKey, updateBy)
                    .orderByDesc(TMeasureProject::getCreateTime);
            List<TMeasureProject> projectList = tMeasureProjectService.list(projectWrapper);
            if (projectList == null || projectList.isEmpty()){
                return Result.ok(vo);
            }
            vo.setProjectName(projectList.get(0).getProjectName());
            vo.setProjectId(projectList.get(0).getId());

        } else {
            TMeasureMethod tMeasureMethod = list.get(0);
            TMeasureProject tMeasureProject = tMeasureProjectService.getById(tMeasureMethod.getProjectItemKey());

            vo.setMethodId(tMeasureMethod.getId());
            vo.setMethodName(tMeasureMethod.getMethodName());
            vo.setProjectId(tMeasureProject.getId());
            vo.setProjectName(tMeasureProject.getProjectName());
        }
        return Result.ok(vo);
    }

    @Override
    public Result<MeasurementCurrentInfoVO> updateMethodParam(MethodParamsDto methodDto) {

        RationParameter parameter = new RationParameter();
        BeanUtils.copyProperties(methodDto, parameter);
        MeasurementCurrentInfoVO vo = new MeasurementCurrentInfoVO();
        String clearDataRange = methodDto.getClearDataRange();
        if (StringUtils.hasText(clearDataRange) && RationChangeMethodTypeEnums.noChange.getIdentity().equals(clearDataRange)) {
            //设置参数给仪器。。。待定
            vo.setTaskId(methodDto.getTaskId());
            vo.setMethodId(methodDto.getCurrentMethodId());
            return Result.ok(vo);
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodDto.getCurrentMethodId());
        TMeasureMethod newMethod = new TMeasureMethod();
        newMethod.setMethodName(StringUtils.hasText(methodDto.getMethodName())? methodDto.getMethodName() : method.getMethodName() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        newMethod.setMethodCreator(methodDto.getUpdateBy());
        newMethod.setMethodCreateTime(LocalDateTime.now());
        newMethod.setIsDelete(createType);
        newMethod.setProjectItemKey(method.getProjectItemKey());
        newMethod.setMeasurementType(method.getMeasurementType());
        newMethod.setMeasurementParamInfo(JSON.toJSONString(parameter));
        newMethod.setPreprocessKey(methodDto.getPreprocessKey());
        boolean save = tMeasureMethodService.save(newMethod);
        if (!save) {
            //创建失败
            return Result.fail(YamlMessageSource.getMessageStatic("方法修改失败"));
        }

        vo.setMethodId(newMethod.getId());
        vo.setMethodName(newMethod.getMethodName());
        vo.setProjectId(newMethod.getProjectItemKey());
        vo.setProjectName(tMeasureProjectService.getById(newMethod.getProjectItemKey()).getProjectName());

        newMethod = tMeasureMethodService.getById(newMethod.getId());

        if (!StringUtils.hasText(methodDto.getTaskId())) {
            return Result.ok(vo);
        }

        TMeasureTask task = tMeasureTaskService.getById(methodDto.getTaskId());
        if (task == null || task.getTaskStatus() == null) {

            return Result.ok(vo);
        }
        if (TaskStatusEnum.UNTESTED.getCode().equals(task.getTaskStatus())) {
            //任务是新建状态
            task.setMethodKey(newMethod.getId());
            task.setCreateTime(LocalDateTime.now());
            if (StringUtils.hasText(methodDto.getNewTaskName())) {
                task.setTaskName(methodDto.getNewTaskName());
            }
            tMeasureTaskService.updateById(task);
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            return Result.ok(vo);
        }

        if (TaskStatusEnum.TESTED.getCode().equals(task.getTaskStatus())) {
            //任务已测

            if (StringUtils.hasText(clearDataRange)) {

                int digit = Constants.DEFAULT_DECIMAL_NUM;
                R<TMeasureTaskAttachment> attachmentR = taskAttachmentService.queryByTaskId(task.getId());
                if (attachmentR.getSuccess()) {
                    TMeasureTaskAttachment attachment = attachmentR.getData();
                    if (StringUtils.hasText(attachment.getDataParam())) {
                        FQueryDataDTO queryDataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
                        if (queryDataDTO.getDigit() != null) {
                            digit = queryDataDTO.getDigit();
                        }
                    }
                }

                if (RationChangeMethodTypeEnums.analyze.getIdentity().equals(clearDataRange) && methodDto.getIsApplyToCurrent()) {
                    task.setMethodKey(newMethod.getId());
                    task.setCreateTime(LocalDateTime.now());
                    tMeasureTaskService.updateById(task);
                    generateTableInfo(task.getId(), task.getMethodKey(), false);
                    task = tMeasureTaskService.getById(task.getId());

                    changeStandSampleDataDigit(task.getId(), digit);
                    changeUnknowSampleDigit(task.getId(), digit);
                    //TODO 公式重算
                    vo.setTaskId(task.getId());
                    vo.setTaskName(task.getTaskName());
                    return Result.ok(vo);
                }

                if ( RationChangeMethodTypeEnums.curve.getIdentity().equals(clearDataRange) && methodDto.getIsApplyToCurrent()) {
                    task.setMethodKey(newMethod.getId());
                    task.setCreateTime(LocalDateTime.now());
                    tMeasureTaskService.updateById(task);
                    generateTableInfo(task.getId(), task.getMethodKey(), false);
                    task = tMeasureTaskService.getById(task.getId());

                    FittingReceiveInfo curveInfo = createFittingInfo(newMethod.getId());

                    String fittingInfo = null;
                    if (curveInfo != null) {
                        fittingInfo = JSONObject.toJSONString(curveInfo);
                    }

                    if (StringUtils.hasText(fittingInfo)){
                        task.setFittingParam(fittingInfo);
                        tMeasureTaskService.updateById(task);
                        changeUnknowSampleDigit(task.getId(), digit);

                    } else {
                        task.setFittingParam(fittingInfo);
                        tMeasureTaskService.updateById(task);
                        changeStandSampleDataDigit(task.getId(), digit);
                        changeUnknowSampleDigit(task.getId(), digit);
                    }
                    vo.setTaskId(task.getId());
                    vo.setTaskName(task.getTaskName());
                    return Result.ok(vo);
                }
            }
        }

        TMeasureTask newTask = new TMeasureTask();
        newTask.setMethodKey(newMethod.getId());
        newTask.setCreateTime(LocalDateTime.now());
        newTask.setFittingParam(task.getFittingParam());
        SysUserPO sysUserPO = userManageDAO.selectByUsername(methodDto.getUpdateBy());
        newTask.setUserId(sysUserPO.getId());

        String taskName = task.getTaskName() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        if (StringUtils.hasText(methodDto.getNewTaskName())) {
            taskName = methodDto.getNewTaskName();
        }

        newTask.setTaskName(taskName);
        newTask.setProjectItemKey(newMethod.getProjectItemKey());
        newTask.setTaskStatus("0");
        newTask.setMeasureType(newMethod.getMeasurementType());
        newTask.setTableInfo(StringUtils.hasText(task.getTableInfo())?task.getTableInfo() : null);
        tMeasureTaskService.save(newTask);
        generateTableInfo(newTask.getId(), newMethod.getId(), true);
        vo.setTaskId(newTask.getId());
        vo.setTaskName(newTask.getTaskName());
        return Result.ok(vo);
    }

    /**
     *  生成表头和映射信息
     */
    private R<Boolean> generateTableInfo(String taskId, String methodId, Boolean clearOld) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null || !StringUtils.hasText(method.getMeasurementParamInfo())) {
            //方法不存在
            return R.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //任务不存在
            return R.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        if (parameter == null || parameter.getSampleInputParams() == null || parameter.getSampleInputParams().size() <= parameter.getWaveLength().size()) {
            log.info("参数信息不存在");
            return R.ok();
        }
        List<InputParam> inputParams = parameter.getSampleInputParams();

        RationTaskHeadSnapShot snapShot = new RationTaskHeadSnapShot();

        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();
        if (StringUtils.hasText(task.getTableInfo()) && !clearOld) {
            snapShot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
            if (snapShot == null) {
                snapShot = new RationTaskHeadSnapShot();
                taskHeadSnapshot = new TaskHeadSnapshot();
            }
        }
        List<HeadVO> heads = taskHeadSnapshot.getSampleHeads();
        if (heads == null || heads.isEmpty()) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_unknown.getIdentity());
            heads = new ArrayList<>(preHead);
        }

        Set<String> allHeadIdentity = new HashSet<>();

        for (HeadVO head : heads) {
            allHeadIdentity.add(head.getIdentity());
        }

        List<InputParamMapTableHead> mapRelation = taskHeadSnapshot.getMapRelation();
        Map<String, String> map = new HashMap<>();
        for (InputParamMapTableHead relation : mapRelation) {
            map.put(relation.getParamIdentity(), relation.getHeadIdentity());
        }

        String luminosity = WaveLengthEnum.abs.getCode();
        List<String> waveLength = parameter.getWaveLength();

        for (InputParam param : inputParams) {
            String resultIdentity = param.getResultIdentity();
            if (StringUtils.hasText(map.get(resultIdentity))) {
                log.info("当前输入参数{}已存在", resultIdentity);
                if (allHeadIdentity.contains(map.get(resultIdentity))) {
                    log.info("当前输入参数{}绑定的表头{}存在", resultIdentity, map.get(resultIdentity));
                    continue;
                }
            }

            if (resultIdentity.matches("[A-Z]")) {
                log.info("大写字母，只绑定，不添加");
                int letterIndex = resultIdentity.charAt(0) - 'A' + 1;

                if (letterIndex <= waveLength.size()) {
                    InputParamMapTableHead relation = new InputParamMapTableHead();
                    relation.setHeadName(param.getParamName());
                    relation.setHeadIdentity(waveLength.get(letterIndex - 1) + "-" + luminosity);
                    relation.setParamName(param.getParamName());
                    relation.setParamIdentity(resultIdentity);
                    mapRelation.add(relation);
                    continue;
                }
            }

            String newHeadIdentity = randomString(8);
            while (allHeadIdentity.contains(newHeadIdentity)) {
                newHeadIdentity = randomString(8);
            }

            //创建表头
            HeadVO newHead = new HeadVO();
            newHead.setName(param.getParamName());
            newHead.setPrintTitle(param.getParamName());
            newHead.setIdentity(newHeadIdentity);
            newHead.setLocate(TableHeadILocateEnums.sample.getIdentity());

            heads.add(newHead);
            InputParamMapTableHead relation = new InputParamMapTableHead();
            relation.setHeadName(param.getParamName());
            relation.setHeadIdentity(newHeadIdentity);
            relation.setParamName(param.getParamName());
            relation.setParamIdentity(resultIdentity);
            mapRelation.add(relation);
            allHeadIdentity.add(newHeadIdentity);
        }
        taskHeadSnapshot.setSampleHeads(heads);
        taskHeadSnapshot.setMapRelation(mapRelation);

        List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), SampleTypeEnum.kind_stand.getIdentity());
        TaskHeadSnapshot standHead = new TaskHeadSnapshot();
        standHead.setSampleHeads(preHead);
        standHead.setMapRelation(new ArrayList<>());

        snapShot.setStandHead(standHead);
        snapShot.setUnknownHead(taskHeadSnapshot);

        boolean update = tMeasureTaskService.updateById(task);

        return update ? R.ok(true) : R.fail(YamlMessageSource.getMessageStatic("00000010"));


    }

    private String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }


    @Override
    public Result<MethodChangeCheckVO> checkMethodParam(MethodParamsDto checkDTO) {

        if (!StringUtils.hasText(checkDTO.getCurrentMethodId())) {
            //请选择当前方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000023"));
        }

        RationParameter newMethodParam = new RationParameter();
        BeanUtils.copyProperties(checkDTO, newMethodParam);

        R<Boolean> r = checkParam(newMethodParam);
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }

        double minWaveLength = 190.0;
        double maxWaveLength = 1100.0;

        if (StringUtils.hasText(checkDTO.getDeviceSerialNumber())) {
            R<DeviceMeasureVO> typeInfo = deviceInfoService.queryDeviceTypeInfo(checkDTO.getDeviceSerialNumber());
            if (typeInfo.getSuccess()) {
                DeviceMeasureVO vo = typeInfo.getData();
                if (vo != null) {
                    minWaveLength = StringUtils.hasText(vo.getMinWavelength()) ? Double.parseDouble(vo.getMinWavelength()) : minWaveLength;
                    maxWaveLength = StringUtils.hasText(vo.getMaxWavelength()) ? Double.parseDouble(vo.getMaxWavelength()) : maxWaveLength;
                }
            }
        }
        List<String> waveLengths = newMethodParam.getWaveLength();

        for (int i = 0; i < waveLengths.size(); i++) {
            String waveStr = waveLengths.get(i);
            if (isBlank(waveStr)) {
                //"第" + (i + 1) + "个波长值不能为空字符串"
                return Result.fail(YamlMessageSource.getMessageStatic("00102005", (i + 1)));
            }
            try {
                double waveVal = Double.parseDouble(waveStr);
                if (waveVal < minWaveLength || waveVal > maxWaveLength) {
                    //"第" + (i + 1) + "个波长值超出范围（190-900），当前值：" + waveVal
                    return Result.fail(YamlMessageSource.getMessageStatic("00102006", (i + 1), waveVal, minWaveLength, maxWaveLength));
                }
                // 校验小数点后最多1位
                String[] parts = waveStr.split("\\.");
                if (parts.length > 1 && parts[1].length() > 1) {
                    //"波长值" + waveStr + "小数点后最多保留1位"
                    return Result.fail(YamlMessageSource.getMessageStatic("00105005", waveStr));
                }
            } catch (NumberFormatException e) {
                return Result.fail(YamlMessageSource.getMessageStatic("00102007", (i + 1), waveStr));
            }
        }

        MethodChangeCheckVO vo = new MethodChangeCheckVO();
        vo.setCurrentMethodId(checkDTO.getCurrentMethodId());
        vo.setParamChange(false);
        vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
        vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());


        TMeasureMethod currentMethod = tMeasureMethodService.getById(checkDTO.getCurrentMethodId());
        if (StringUtils.hasText(currentMethod.getProjectItemKey()) && currentMethod.getProjectItemKey().equals(defaultMethodProjectName)) {
            log.info("生成新方法");
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.newMethod.getMessage());
            vo.setClearDataRange(RationChangeMethodTypeEnums.newMethod.getIdentity());
            return Result.ok(vo);
        }

        RationParameter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), RationParameter.class);

        String dbJson = JSON.toJSONString(oldParam, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newMethodParam, SerializerFeature.SortField);

        if (dbJson.equals(frontJson)) {
            vo.setParamChange(false);
            vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
            vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());
            return Result.ok(vo);
        }
        //检查测量数据
        boolean b = checkMeasureParam(oldParam, newMethodParam);
        System.out.println(b);
        if (!b) {
            log.info("测量参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.measure.getMessage());
            return Result.ok(vo);
        }

        boolean curveParam = checkCurveParam(oldParam, newMethodParam);
        if (!curveParam) {
            log.info("曲线参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.curve.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.curve.getMessage());
        }

        //检查分析参数
        boolean analyze = checkAnalyzeParam(oldParam, newMethodParam);
        if (!analyze) {
            log.info("分析参数有改变");
            vo.setClearDataRange(RationChangeMethodTypeEnums.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(RationChangeMethodTypeEnums.analyze.getMessage());
        }
        return Result.ok(vo);
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkMeasureParam(RationParameter oldParam, RationParameter newParam) {

        List<String> oldWave = oldParam.getWaveLength();
        List<String> newWave = newParam.getWaveLength();
        if (!Objects.equals(oldWave, newWave)) {
            return false;
        }

        return Objects.equals( oldParam.getSpectralBandwidth(), newParam.getSpectralBandwidth())&&
                Objects.equals(oldParam.getResponseTime(), newParam.getResponseTime()) &&
                Objects.equals(oldParam.getChangeLampWaveLength(),newParam.getChangeLampWaveLength()) &&
                Objects.equals(oldParam.getTimeInterval(),newParam.getTimeInterval())&&
                Objects.equals(oldParam.getRepeatMode(), newParam.getRepeatMode()) &&
                Objects.equals(oldParam.getRepeatCount(), newParam.getRepeatCount()) ;
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkAnalyzeParam(RationParameter oldParam, RationParameter newParam) {

        return Objects.equals(oldParam.getSampleInputParams(),newParam.getSampleInputParams()) &&
                Objects.equals(oldParam.getQcParams(), newParam.getQcParams()) &&
                Objects.equals(oldParam.getCurveCoefficient(), newParam.getCurveCoefficient()) &&
                Objects.equals(oldParam.getUnit(), newParam.getUnit()) &&
                Objects.equals(oldParam.getResultParams(), newParam.getResultParams()) &&
                Objects.equals(oldParam.getApproxParams(), newParam.getApproxParams());

    }

    /*
     * 检查曲线参数是否改变，true--未改变，false--改变了
     */
    private boolean checkCurveParam(RationParameter oldParam, RationParameter newParam) {
        List<String> oldLengthParam = oldParam.getWaveLengthParams();
        List<String> newLengthParam = newParam.getWaveLengthParams();
        if (!Objects.equals(oldLengthParam, newLengthParam)) {
            return false;
        }

        List<String> oldZero = oldParam.getZeroMethod();
        List<String> newZero = newParam.getZeroMethod();
        if (!Objects.equals(oldZero, newZero)) {
            return false;
        }
        if (!oldParam.getCoefficientMode().equals(newParam.getCoefficientMode())) {
            return false;
        }

        List<String> oldCoefficient = oldParam.getCoefficientParams();
        List<String> newCoefficient = newParam.getCoefficientParams();
        if (!Objects.equals(oldCoefficient, newCoefficient)) {
            return false;
        }


        return oldParam.getCurveType().equals(newParam.getCurveType()) &&
                oldParam.getEquationType().equals(newParam.getEquationType());

    }

    private R<Boolean> checkParam(RationParameter parameter) {
        // 1. 根对象非空校验
        if (parameter == null) {
            //定量测量参数对象不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00102003"));
        }

        // 2. 波长相关参数校验
        // 2.1 波长值校验（1-3个，数值范围190-900）
        List<String> waveLengths = parameter.getWaveLength();
        if (waveLengths == null || waveLengths.isEmpty()) {
            //波长值不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00105003"));
        }
        if (waveLengths.size() > 3) {
            //"波长值最多支持3个，当前数量：" + waveLengths.size()
            return R.fail(YamlMessageSource.getMessageStatic("00102004", waveLengths.size()));
        }
        if(!StringUtils.hasText(parameter.getChangeLampWaveLength())){
            return R.fail("换灯波长不能为空");
        }

        if (StringUtils.hasText(parameter.getChangeLampWaveLength())) {
            double v = Double.parseDouble(parameter.getChangeLampWaveLength());
            if (v < 330 || v > 390) {
                //"换灯波长要在（330-390）之间，当前值：" + v
                return R.fail(YamlMessageSource.getMessageStatic("0010202A",v));
            }

            if (waveLengths.contains(parameter.getChangeLampWaveLength())) {
                //换灯波长不能和测量波长一样。
                return R.fail(YamlMessageSource.getMessageStatic("0010202B"));
            }
        }

        // 2.2 波长参数校验（与波长值数量一致）
        List<String> waveLengthParams = parameter.getWaveLengthParams();
        if (waveLengthParams == null) {
            //波长参数不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00102008"));
        }
        if (waveLengthParams.size() != waveLengths.size()) {
            //"波长参数数量需与波长值一致（当前参数数：" + waveLengthParams.size() + "，波长数：" + waveLengths.size() + "）"
            return R.fail(YamlMessageSource.getMessageStatic("00102009",waveLengthParams.size(), waveLengths.size()));
        }
        for (int i = 0; i < waveLengthParams.size(); i++) {
            if (isBlank(waveLengthParams.get(i))) {
                return R.fail(YamlMessageSource.getMessageStatic("0010200A" , (i + 1)));
            }
        }

        // 2.3 微分方式校验（枚举值）
//        String differentialMode = parameter.getDifferentialMode();
//        if (isBlank(differentialMode)) {
//            return R.fail("微分方式不能为空");
//        }
//        List<String> validDiffModes = Arrays.asList(
//                "single", "1st-der", "2nd-der", "3rd-der", "4th-der",
//                "peak", "valley", "max", "min"
//        );
//        if (!validDiffModes.contains(differentialMode)) {
//            return R.fail("微分方式不合法（可选值：" + String.join("、", validDiffModes) + "），当前值：" + differentialMode);
//        }

        // 3. 曲线与系数相关校验
        // 3.1 曲线类型校验
        if (isBlank(parameter.getCurveType())) {
            //曲线类型不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010200B"));
        }

        // 3.2 方程类型校验（枚举值：qtCFA/qtAFC）
        String equationType = parameter.getEquationType();
        if (isBlank(equationType)) {
            //方程类型不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010200C"));
        }
//        if (!Arrays.asList("qtCFA", "qtAFC").contains(equationType)) {
//            return R.fail("方程类型不合法（可选值：qtCFA、qtAFC），当前值：" + equationType);
//        }

        // 3.3 系数方式校验（枚举值：0/1）
        String coefficientMode = parameter.getCoefficientMode();
        if (isBlank(coefficientMode)) {
            //系数方式不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010200D"));
        }
        if (RationCurveParamEnums.getName(coefficientMode) == null) {
            //系数方式不合法（可选值：直接系数法、标准曲线法）
            return R.fail(YamlMessageSource.getMessageStatic("0010200E"));
        }
        if (RationCurveParamEnums.direct_coefficient.getIdentity().equals(coefficientMode)) {
            // 3.4 系数参数校验（1-10个，数字且最多3位小数）
            List<String> coefficientParams = parameter.getCoefficientParams();
            if (coefficientParams == null || coefficientParams.isEmpty()) {
                //系数参数不能为空
                return R.fail(YamlMessageSource.getMessageStatic("0010200F"));
            }
            if (coefficientParams.size() > 10) {
                //"系数参数最多支持10个，当前数量：" + coefficientParams.size()
                return R.fail(YamlMessageSource.getMessageStatic("00102010",coefficientParams.size()));
            }

            int curveType = Integer.parseInt(parameter.getCurveType());
            if (coefficientParams.size() < curveType + 1) {
                //"系数参数数量不足，当前数量：" + coefficientParams.size() + "，所需数量：" + (curveType + 1)
                return R.fail(YamlMessageSource.getMessageStatic("00102011",coefficientParams.size(),(curveType + 1)));
            }

            for (int i = 0; i < coefficientParams.size(); i++) {
                String coeffStr = coefficientParams.get(i);
                if (isBlank(coeffStr)) {
                    //"第" + (i + 1) + "个系数参数不能为空字符串"
                    return R.fail(YamlMessageSource.getMessageStatic("00102012",(i + 1)));
                }
                try {
                    Double.parseDouble(coeffStr);
                    if (coeffStr.contains(".") && coeffStr.split("\\.")[1].length() > 10) {
                        //"第" + (i + 1) + "个系数参数小数位数不能超过10位，当前值：" + coeffStr
                        return R.fail(YamlMessageSource.getMessageStatic("00102013",(i + 1),coeffStr));
                    }
                } catch (NumberFormatException e) {
                    //"第" + (i + 1) + "个系数参数格式错误（需为数字），当前值：" + coeffStr
                    return R.fail(YamlMessageSource.getMessageStatic("00102014",(i + 1),coeffStr));
                }
            }
        }

        // 3.5 零点方式校验
        List<String> zeroMethod = parameter.getZeroMethod();
//        if (zeroMethod == null || zeroMethod.isEmpty()) {
//            return R.fail("零点方式不能为空");
//        }
        if (zeroMethod != null && !zeroMethod.isEmpty()) {
            for (String zm : zeroMethod) {
                if (RationCurveParamEnums.getName(zm) == null) {
                    //零点方式不合法（可选值：过零点、插入零点）
                    return R.fail(YamlMessageSource.getMessageStatic("00102015"));
                }
            }
        }


        // 4. 重复测量相关校验
        // 4.1 重复模式校验（枚举值：rmNone/rmCR）
        String repeatMode = parameter.getRepeatMode();
        if (isBlank(repeatMode)) {
            //重复模式不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00103007"));
        }

        if (RationCurveParamEnums.getName(repeatMode) == null) {
            //重复模式不合法
            return R.fail(YamlMessageSource.getMessageStatic("00102016" ));
        }

        // 4.2 单池重复（rmCR）时的关联校验
        if ("单池重复".equals(repeatMode)) {
            // 重复次数：2-10的整数
            String repeatCountStr = parameter.getRepeatCount();
            if (isBlank(repeatCountStr)) {
                //单池重复模式下，重复次数不能为空
                return R.fail(YamlMessageSource.getMessageStatic("00102017"));
            }
            try {
                int repeatCount = Integer.parseInt(repeatCountStr);
                if (repeatCount < 2 || repeatCount > 10) {
                    //重复次数需在2-10范围内，当前值：repeatCount
                    return R.fail(YamlMessageSource.getMessageStatic("00102018" ,repeatCount));
                }
            } catch (NumberFormatException e) {
                //"重复次数格式错误（需为整数），当前值：" + repeatCountStr
                return R.fail(YamlMessageSource.getMessageStatic("00102019",repeatCountStr));
            }

            // 时间间隔：0-60秒，保留1位小数
            String timeIntervalStr = parameter.getTimeInterval();
            if (isBlank(timeIntervalStr)) {
                //单池重复模式下，时间间隔不能为空
                return R.fail(YamlMessageSource.getMessageStatic("0010201A"));
            }
            try {
                double timeInterval = Double.parseDouble(timeIntervalStr);
                if (timeInterval < 0 || timeInterval > 60) {
                    //"时间间隔需在0-60秒范围内，当前值：" + timeInterval
                    return R.fail(YamlMessageSource.getMessageStatic("0010201B",timeInterval));
                }
                if (timeIntervalStr.contains(".") && timeIntervalStr.split("\\.")[1].length() > 1) {
                    //"时间间隔小数位数不能超过1位，当前值：" + timeIntervalStr
                    return R.fail(YamlMessageSource.getMessageStatic("0010201C",timeIntervalStr));
                }
            } catch (NumberFormatException e) {
                //"时间间隔格式错误（需为数字），当前值：" + timeIntervalStr
                return R.fail(YamlMessageSource.getMessageStatic("0010201D",timeIntervalStr));
            }
        }


        // 6. 复杂结构参数校验
        // 6.1 样品输入参数校验（最多10个）
        List<InputParam> sampleInputParams = parameter.getSampleInputParams();
        if (sampleInputParams != null && !sampleInputParams.isEmpty()) {
            if (sampleInputParams.size() > 10) {
                //"样品输入参数最多支持10个，当前数量：" + sampleInputParams.size()
                return R.fail(YamlMessageSource.getMessageStatic("0010201E",sampleInputParams.size()));
            }
            for (int i = 0; i < sampleInputParams.size(); i++) {
                InputParam inputParam = sampleInputParams.get(i);
                if (inputParam == null || isBlank(inputParam.getParamName())) {
                    //"第" + (i + 1) + "个样品输入参数标题不能为空"
                    return R.fail(YamlMessageSource.getMessageStatic("0010201F",(i+1)));
                }
            }
        }

        // 6.2 待计算结果参数校验（最多5个）
        List<PreCountResultParam> resultParams = parameter.getResultParams();
        if (resultParams != null && !resultParams.isEmpty()) {
            if (resultParams.size() > 5) {
                return R.fail(YamlMessageSource.getMessageStatic("00102020",resultParams.size()));
            }
            for (int i = 0; i < resultParams.size(); i++) {
                PreCountResultParam resultParam = resultParams.get(i);
                if (resultParam == null || isBlank(resultParam.getResultName()) || isBlank(resultParam.getFormulaText())) {
                    //"第" + (i + 1) + "个待计算结果参数标题或公式不能为空"
                    return R.fail(YamlMessageSource.getMessageStatic("00102021",(i + 1)));
                }
            }
        }

        // 6.3 修约设置校验（最多10个）
        List<RoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams != null && !approxParams.isEmpty()) {

            if (approxParams.size() > 10) {
                //"最多支持修约10个变量" + approxParams.size()
                return R.fail(YamlMessageSource.getMessageStatic("00103010",approxParams.size()));
            }
            for (RoundingVO approxParam : approxParams) {

                if (approxParam.getRoundingParams() == null || approxParam.getRoundingParams().isEmpty()) {
                    //"修约设置" + approxParam.getResultIdentity() + "的修约条件不能为空"
                    return R.fail(YamlMessageSource.getMessageStatic("00103011",approxParam.getResultIdentity()));
                }
                if (approxParam.getRoundingParams().size() > 10) {
                    //"修约设置" + approxParam.getResultIdentity() + "的修约条件数量不能超过10个"
                    return R.fail(YamlMessageSource.getMessageStatic("00103012",approxParam.getResultIdentity()));
                }

                R<Boolean> r = guidePublicUtilService.checkApproximateRange(approxParam);
                if (!r.getSuccess()) {
                    return r;
                }
            }
        }


        // 6.4 质控设置校验（最多10个，类型合法）
        List<QualityConfVO> qcParams = parameter.getQcParams();
        if (qcParams != null && !qcParams.isEmpty() && qcParams.size() > 10) {
            //"质控设置最多支持10个规则，当前数量：" + qcParams.size()
            return R.fail(YamlMessageSource.getMessageStatic("00102022",qcParams.size()));
        }

        // 7. 继承参数校验（方法名称等长度限制）
        if (parameter.getEquationTypeName() != null && parameter.getEquationTypeName().length() > 256) {
            //方法名称长度不能超过256字符
            return R.fail(YamlMessageSource.getMessageStatic("00102023"));
        }

        return R.ok(true);
    }


    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    @Override
    public Result<List<JSONObject>> querySampleList(String taskId, String sampleKind) {
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
                if (!StringUtils.hasText(measureSample.getSampleKind()) || !measureSample.getSampleKind().equals(sampleKind)) {
                    continue;
                }
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

    @Override
    public Result<List<HeadVO>> querySampleHeads(String taskId, String sampleKind) {

        if (!StringUtils.hasText(sampleKind)) {
            log.info("缺少样品种类");
            //缺少样品种类
            return Result.fail(YamlMessageSource.getMessageStatic("00000023"));
        }

        if (!StringUtils.hasText(taskId)) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
            return Result.ok(preHead);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
            return Result.ok(preHead);
        } else {
            RationTaskHeadSnapShot snapShot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
            if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
                TaskHeadSnapshot standHead = snapShot.getStandHead();
                if (standHead != null && standHead.getSampleHeads()!=null && !standHead.getSampleHeads().isEmpty()) {
                    return Result.ok(standHead.getSampleHeads());
                } else {
                    List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
                    return Result.ok(preHead);
                }
            } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                TaskHeadSnapshot unknownHead = snapShot.getUnknownHead();
                if (unknownHead != null && unknownHead.getSampleHeads()!=null && !unknownHead.getSampleHeads().isEmpty()) {
                    return Result.ok(unknownHead.getSampleHeads());
                } else {
                    List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription(), sampleKind);
                    return Result.ok(preHead);
                }
            } else {
                //样品种类出错
                return Result.fail(YamlMessageSource.getMessageStatic("00000024"));
            }
        }

    }


    @Override
    @Transactional
    public Result addOrDeleteSample(RationAddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            //没有方法！
            return Result.fail(YamlMessageSource.getMessageStatic("00000025"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(dto.getMethodId());
        if (method == null) {
            //没有方法！
            return Result.fail(YamlMessageSource.getMessageStatic("00000025"));
        }
        
        if (dto.getStandHeads().size() > 10) {
            //标准样品表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000028"));
        }

        if (dto.getUnknowHeads().size() > 10) {
            //未知样品表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000028"));
        }

        RationParameter rationParameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        boolean standHas = false;
        if (StringUtils.hasText(rationParameter.getCoefficientMode()) && rationParameter.getCoefficientMode().equals(RationCurveParamEnums.standard_curve.getIdentity())) {
            standHas = true;
            if (dto.getStandHeads() == null || dto.getStandHeads().isEmpty()) {
                //标准样品表头为空
                return Result.fail(YamlMessageSource.getMessageStatic("00000026"));
            }

            if (dto.getStandHeads().size() > 10) {
                //标准样品表格新增列数不能超过10列
                return Result.fail(YamlMessageSource.getMessageStatic("00000028"));
            }
            /// 校验是否有重复的样品编号
            // 取出所有样品编号
            List<String> sampleNos = dto.getNewStandSamples().stream()
                    .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleNumber()) // 取出 sampleNo
//                    .filter(Objects::nonNull)    // 过滤掉空值
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());

            if (sampleNos.size() < dto.getNewStandSamples().size()) {
                return Result.fail(YamlMessageSource.getMessageStatic("0000004E"));
            }

            // 查重：找到重复的样品编号
            Set<String> duplicates = sampleNos.stream()
                    .filter(no -> Collections.frequency(sampleNos, no) > 1)
                    .collect(Collectors.toSet());
            // 判断如果isTested为true，
            if (!duplicates.isEmpty()) {
                //标准样品编号重复
                return Result.fail(YamlMessageSource.getMessageStatic("00102001"));
            }
        }

        List<String> unknownSampleNos = dto.getNewUnknowSamples().stream()
                .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleNumber()) // 取出 sampleNo
//                .filter(Objects::nonNull)    // 过滤掉空值
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        if (unknownSampleNos.size() < dto.getNewUnknowSamples().size()) {
            return Result.fail(YamlMessageSource.getMessageStatic("0000004E"));
        }
        // 查重：找到重复的样品编号
        Set<String> isDuplicates = unknownSampleNos.stream()
                .filter(no -> Collections.frequency(unknownSampleNos, no) > 1)
                .collect(Collectors.toSet());
        // 判断如果isTested为true，
        if (!isDuplicates.isEmpty()) {
            //未知样品编号重复
            return Result.fail(YamlMessageSource.getMessageStatic("00102002"));
        }
        

        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, dto.getUpdateBy()));
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
            dto.setTaskId(newTask.getId());
        }

        TMeasureTask task = tMeasureTaskService.getById(dto.getTaskId());

        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        List<TaskSpecimen> taskSpecimen = new ArrayList<>();


        if (StringUtils.hasText(task.getSampleKey())) {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        }

        if (taskSpecimen == null) {

            taskSpecimen = new ArrayList<>();
        }
        Map<String, TaskSpecimen> oldMap = new HashMap<>();

        for (TaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId()).orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData one = tMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    one.setIsSave(createType);
                    tMeasureDataService.updateById(one);
                }
            }
            oldMap.put(specimen.getSpecimenKey(), specimen);
        }

        RationTaskHeadSnapShot taskHeadSnapShot = new RationTaskHeadSnapShot();
        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapShot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
        }

        /*
        处理表头
         */

        if (standHas) {
            if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
                TaskHeadSnapshot standHead = new TaskHeadSnapshot();
                if (taskHeadSnapShot.getStandHead() != null) {
                    BeanUtils.copyProperties(taskHeadSnapShot.getStandHead(), standHead);
                }
                standHead.setTaskId(task.getId());
                standHead.setSampleHeads(dto.getStandHeads());
                taskHeadSnapShot.setStandHead(standHead);
            }
        }



        if (dto.getUnknowHeads() != null && !dto.getUnknowHeads().isEmpty()) {
            TaskHeadSnapshot unknownHead = new TaskHeadSnapshot();
            if (taskHeadSnapShot.getUnknownHead() != null) {
                BeanUtils.copyProperties(taskHeadSnapShot.getUnknownHead(), unknownHead);
            }
            unknownHead.setTaskId(task.getId());
            unknownHead.setSampleHeads(dto.getUnknowHeads());
            taskHeadSnapShot.setUnknownHead(unknownHead);
        }
        task.setTableInfo(JSON.toJSONString(taskHeadSnapShot));
        tMeasureTaskService.updateById(task);
        task = tMeasureTaskService.getById(task.getId());
        Integer digit = taskAttachmentService.queryLastDigit(task.getId());
        digit = digit == null ? Constants.DEFAULT_DECIMAL_NUM : digit;

        if (standHas) {
            for (Map<String, String> map : dto.getNewStandSamples()) {

                String jsonString = JSON.toJSONString(map);
                SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);

                TMeasureSample newSample = new TMeasureSample();
                BeanUtils.copyProperties(sample, newSample);
                newSample.setId(sample.getSampleId());
                newSample.setSampleKind(stand);
                if (dto.getStandHeads() != null && !dto.getStandHeads().isEmpty()) {
                    JSONObject newColData = new JSONObject();

                    for (HeadVO head : dto.getStandHeads()) {
                        if (head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                            continue;
                        }
                        String s = map.get(head.getIdentity());
                        newColData.put(head.getIdentity(), s);
                    }

                    newSample.setHeads(JSON.toJSONString(dto.getStandHeads()));
                    newSample.setNewColData(JSON.toJSONString(newColData));
                }

                if (StringUtils.hasText(newSample.getId())) {
                    tMeasureSampleService.updateById(newSample);
                    TaskSpecimen specimen = oldMap.get(newSample.getId());
                    specimen.setSampleKind(stand);
                    specimen.setSampleType(newSample.getSampleType());
                    oldMap.put(newSample.getId(), specimen);
                } else {
                    TaskSpecimen specimen = new TaskSpecimen();
                    specimen.setDeleteType(createType);
                    specimen.setIsTested(noTested);
                    specimen.setSampleType(newSample.getSampleType());
                    specimen.setSampleKind(stand);
                    log.info("新增的标准样品：{}", newSample);
                    tMeasureSampleService.save(newSample);
                    specimen.setSpecimenKey(newSample.getId());
                    oldMap.put(newSample.getId(), specimen);
                    taskSpecimen.add(specimen);
                }
            }
        }


        for (Map<String, String> map : dto.getNewUnknowSamples()) {

            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);

            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());
            newSample.setSampleKind(unknown);

            if (dto.getUnknowHeads() != null && !dto.getUnknowHeads().isEmpty()) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getUnknowHeads()) {
                    if (head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                        continue;
                    }

                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                newSample.setHeads(JSON.toJSONString(dto.getUnknowHeads()));
                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);

                TaskSpecimen specimen = oldMap.get(newSample.getId());
                specimen.setSampleKind(unknown);
                specimen.setSampleType(newSample.getSampleType());
                oldMap.put(newSample.getId(), specimen);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleKind(unknown);
                specimen.setSampleType(newSample.getSampleType());
                log.info("新增的未知样品：{}", newSample);
                tMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                oldMap.put(newSample.getId(), specimen);
                taskSpecimen.add(specimen);
            }
        }

        for (TaskSpecimen specimen : taskSpecimen) {
            specimen = oldMap.get(specimen.getSpecimenKey()) == null ? specimen : oldMap.get(specimen.getSpecimenKey());
        }

        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);
        boolean b = tMeasureTaskService.updateById(task);

        if (!b) {
            //任务保存样品信息出错
            return Result.fail(YamlMessageSource.getMessageStatic("00100003"));
        }
        //重新分析
        if (standHas) {
            changeStandSampleDataDigit(task.getId(), digit);
        }
        changeUnknowSampleDigit(task.getId(), digit);

        if (StringUtils.hasText(dto.getDeviceSerialNumber())) {
            R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(dto.getDeviceSerialNumber());
            if (!poolInfo.getSuccess()) {
                return Result.ok();
            }
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            Boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool();
            if (isAutoChangePool) {
                List<Integer> usefulLocation = tSamplePoolConfigService.getUsefulLocation(dto.getDeviceSerialNumber());
                guidePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
            }
        }

        return Result.ok(tMeasureTaskService.updateById(task));
    }

    @Override
    public Result<Boolean> deleteSample(String taskId, String sampleId) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(sampleId)) {
            //请选择需要删除的任务和样品
            return Result.fail(YamlMessageSource.getMessageStatic("00000015"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            //任务无样品
            return Result.fail(YamlMessageSource.getMessageStatic("00000016"));
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (sampleId.equals(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData one = tMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        tMeasureTaskService.updateById(task);

        boolean b = tMeasureTaskService.updateById(task);
        if (!b) {
            //删除失败
            return Result.fail(YamlMessageSource.getMessageStatic("00100008"));
        }

        TMeasureSample measureSample = tMeasureSampleService.getById(sampleId);

        Integer i = taskAttachmentService.queryLastDigit(taskId);
        Integer digit = i == null ? Constants.DEFAULT_DECIMAL_NUM : i;

        if (measureSample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            changeStandSampleDataDigit(taskId, digit);
            changeUnknowSampleDigit(taskId, digit);
        } else {
            changeUnknowSampleDigit(taskId, digit);
        }

        return Result.ok(true);
    }

    @Override
    public Result revokeDel(String taskId, String sampleId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        String sampleKey = task.getSampleKey();

        RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, TaskSpecimen.class);
        TMeasureSample newSample = tMeasureSampleService.getById(sampleId);
        Boolean b = revokeDelSampleNumRepeat(taskSpecimen, newSample);
        if (!b) {
            //要恢复的样品编号重复，请处理重复编号后再恢复！
            return Result.fail(YamlMessageSource.getMessageStatic("00100004"));
        }


        for (TaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);

                if (shot != null && specimen.getSampleKind() != null) {

                    if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                        if (shot.getUnknownHead() != null && shot.getUnknownHead().getSampleHeads() != null) {
                            newSample.setHeads(JSON.toJSONString(shot.getUnknownHead().getSampleHeads()));
                        }
                    } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                        if (shot.getStandHead() != null && shot.getStandHead().getSampleHeads() != null) {
                            newSample.setHeads(JSON.toJSONString(shot.getStandHead().getSampleHeads()));
                        }
                    }
                }

                tMeasureSampleService.updateById(newSample);

                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData one = tMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(createType);
                    tMeasureDataService.updateById(one);
                }
            }
        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        tMeasureTaskService.updateById(task);

        TMeasureSample measureSample = tMeasureSampleService.getById(sampleId);

        Integer i = taskAttachmentService.queryLastDigit(taskId);
        Integer digit = i == null ? Constants.DEFAULT_DECIMAL_NUM : i;

        if (measureSample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            changeStandSampleDataDigit(taskId, digit);
            changeUnknowSampleDigit(taskId, digit);
        } else {
            changeUnknowSampleDigit(taskId, digit);
        }

        return Result.ok();

    }

    private Boolean revokeDelSampleNumRepeat(List<TaskSpecimen> taskSpecimen, TMeasureSample sample) {

        String sampleKind = sample.getSampleKind();
        List<String> sampleNumList = new ArrayList<>();

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!specimen.getSampleKind().equals(sampleKind)) {
                continue;
            }
            if (specimen.getDeleteType().equals(deleteType) || specimen.getSpecimenKey().equals(sample.getId())) {
                continue;
            }
            TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
            if (measureSample != null && StringUtils.hasText(measureSample.getSampleNumber())){
                sampleNumList.add(measureSample.getSampleNumber());
            }

        }
        if (sampleNumList.contains(sample.getSampleNumber())) {
            return false;
        }
        return true;

    }

    @Override
    public Result<RationMethodParam> queryMethodParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            //方法id不能为空,请创建方法或选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000021"));
        }
        if(methodId.length() != 32){
            //方法ID格式应为32位
            return Result.fail(YamlMessageSource.getMessageStatic("0030300B"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            //未查询到该方法参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000017"));
        }
        RationParameter rationParameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        RationMethodParam param = new RationMethodParam();
        BeanUtils.copyProperties(rationParameter, param);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        param.setCoefficientModeName(RationCurveParamEnums.getName(param.getCoefficientMode()));
        return Result.ok(param);
    }

    //标准样品表头
    @Override
    public Result<List<HeadVO>> getStandSampleTableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber) {

        if (!StringUtils.hasText(taskId)) {
            //请选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000006"));
        }
        if (!StringUtils.hasText(methodId)){
            //请选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000002"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        //根据任务中记录的样品id判断是否存在空白样品
        TMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
            if (shot != null && shot.getStandHead() != null) {
                if (shot.getStandHead().getSampleHeads() != null) {
                    ans.addAll(shot.getStandHead().getSampleHeads());
                }
                if (shot.getStandHead().getTaskHeads() != null) {
                    ans.addAll(shot.getStandHead().getTaskHeads());
                }
            }
        } else {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
        }

        boolean empty = false;
        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {

                if (specimen.getDeleteType().equals(deleteType) || !specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                    continue;
                }

                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

                if (StringUtils.hasText(sample.getSampleType())) {
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())){
                        empty = true;
                    }
                }
                if (StringUtils.hasText(sample.getHeads())){
                    List<HeadVO> heads = JSON.parseArray(sample.getHeads(), HeadVO.class);
                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }
        }

        List<HeadVO> result = dealHeads(new ArrayList<>(ans), parameter, SampleTypeEnum.kind_stand.getIdentity(), allDisplay, empty, deviceSerialNumber);

        return Result.ok(result);

    }

    private List<HeadVO> dealHeads(List<HeadVO> heads, RationParameter parameter, String sampleKind, Boolean allDisplay, Boolean empty, String deviceSerialNumber) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, HeadVO> map = heads.stream().collect(Collectors.toMap(HeadVO::getIdentity, head -> head));

        Map<String, List<HeadVO>> headsMap = new HashMap<>();

        Map<String, HeadVO> analyzeMap = new HashMap<>();

        for (HeadVO head : heads) {
            if (head.getIdentity().equals("remark")) {
                head.setLocate(TableHeadILocateEnums.remark.getIdentity());
            }

            headsMap.computeIfAbsent(head.getLocate(), k -> new ArrayList<>()).add(head);
            if (head.getLocate().equals(TableHeadILocateEnums.measure.getIdentity())) {
                map.put(head.getIdentity(), head);
            }
            if (head.getLocate().equals(TableHeadILocateEnums.analyze.getIdentity())) {
                analyzeMap.put(head.getIdentity(), head);
            }
        }

        List<HeadVO> allMeasureHeads = new ArrayList<>();

        for (String s : waveLength) {
            String absIdentity = s + "-"+ WaveLengthEnum.abs.getCode();
            String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
            String absName = WaveLengthEnum.abs.getName() +  "[" + s + "nm]";
            String tPercentName = WaveLengthEnum.tPercent.getName() +  "[" + s + "nm]";

            HeadVO absHead = new HeadVO(absName, absIdentity, TableHeadILocateEnums.measure.getIdentity(), absName);
            HeadVO tPercentHead = new HeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);

            if (map.containsKey(absIdentity)) {
                absHead = map.get(absIdentity);
            }
            allMeasureHeads.add(absHead);

            if (allDisplay) {
                if (map.containsKey(tPercentIdentity)) {
                    tPercentHead = map.get(tPercentIdentity);
                }
                allMeasureHeads.add(tPercentHead);
            }
        }

        String sumAbsIdentity = SampleTableEnum.SUM_Abs.getIdentity();
        String absIdentity = SampleTableEnum.Abs.getIdentity();

        HeadVO absHead = new HeadVO(SampleTableEnum.Abs.getName(), absIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.Abs.getName());
        HeadVO sumAbsHead = new HeadVO(SampleTableEnum.SUM_Abs.getName(), sumAbsIdentity, TableHeadILocateEnums.measure.getIdentity(), SampleTableEnum.SUM_Abs.getName());

        if (map.containsKey(absIdentity)) {
            absHead = map.get(absIdentity);
        }

        allMeasureHeads.add(absHead);
        if (empty) {
            if (map.containsKey(sumAbsIdentity)) {
                sumAbsHead = map.get(sumAbsIdentity);
            }
            allMeasureHeads.add(sumAbsHead);
        }
        List<HeadVO> ans = new ArrayList<>();
        List<HeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads !=null) ans.addAll(systemHeads);
        for (HeadVO an : ans) {
            if (an.getIdentity().equals(SampleTableEnum.SAMPLE_LOCATION.getIdentity())) {
                ans.remove(an);
                break;
            }

            if (an.getIdentity().equals(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity()) && StringUtils.hasText(parameter.getUnit())) {
                an.setName(an.getName() + "(" + parameter.getUnit() + ")");
                break;
            }
        }

        if (StringUtils.hasText(deviceSerialNumber)) {

            R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess()) {
                FSamplePoolInfoVO data = poolInfo.getData();
                if (data.getIsAutoChangePool() ) {
                    String sampleLocationName = SampleTableEnum.SAMPLE_LOCATION.getName();
                    String sampleLocationIdentity = SampleTableEnum.SAMPLE_LOCATION.getIdentity();
                    HeadVO sampleLocation = new HeadVO(sampleLocationName, sampleLocationIdentity, TableHeadILocateEnums.location.getIdentity(), sampleLocationName);
                    ans.add(sampleLocation);
                }
            }
        }

        List<HeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads !=null) ans.addAll(sampleHeads);
        ans.addAll(allMeasureHeads);
        List<HeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads !=null) ans.addAll(taskHeads);

        if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {

            String fitConcentrationName = SampleTableEnum.FIT_CONCENTRATION.getName();
            String fitConcentrationIdentity = SampleTableEnum.FIT_CONCENTRATION.getIdentity();

            if (StringUtils.hasText(parameter.getUnit())) {
                fitConcentrationName = fitConcentrationName + "(" + parameter.getUnit() + ")";
            }

            HeadVO fitHead = new HeadVO(fitConcentrationName, SampleTableEnum.FIT_CONCENTRATION.getIdentity(), TableHeadILocateEnums.analyze.getIdentity(), fitConcentrationName);
            if (analyzeMap.containsKey(fitConcentrationIdentity)) {

                HeadVO fHeadVO = analyzeMap.get(fitConcentrationIdentity);
                fitHead.setPrintTitle(fHeadVO.getPrintTitle());
            }
            ans.add(fitHead);

            List<HeadVO> headAnalyze = new ArrayList<>();

            List<PreCountResultParam> resultParams = parameter.getResultParams();
            if (resultParams == null) {
                resultParams = new ArrayList<>();
            }

            for (PreCountResultParam results : resultParams) {
                //TODO 方法参数标识待定，后续再改动
                if (!StringUtils.hasText(results.getParam())) {
                    results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
                }

                String name = results.getResultName() + "(" + results.getUnit() + ")";
                HeadVO analyzeHead = new HeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);

                if (analyzeMap.containsKey(results.getParam())) {
                    analyzeHead = analyzeMap.get(results.getParam());
                }
                headAnalyze.add(analyzeHead);
            }
            ans.addAll(headAnalyze);
        }
        List<HeadVO> other = headsMap.get(TableHeadILocateEnums.other.getIdentity());
        if (other == null || other.isEmpty()) {
            other = new ArrayList<>();
            HeadVO samplingTimeHead = new HeadVO(SampleTableEnum.SAMPLING_TIME.getName(), SampleTableEnum.SAMPLING_TIME.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.SAMPLING_TIME.getName());
            other.add(samplingTimeHead);
        }
        ans.addAll(other);
        List<HeadVO> remarks = headsMap.get(TableHeadILocateEnums.remark.getIdentity());
        if (remarks == null || remarks.isEmpty()) {
            remarks = new ArrayList<>();
            HeadVO remark = new HeadVO(SampleTableEnum.REMARK.getName(), SampleTableEnum.REMARK.getIdentity(), TableHeadILocateEnums.remark.getIdentity(), SampleTableEnum.REMARK.getName());
            remarks.add(remark);
        }
        ans.addAll(remarks);

        return ans;
    }

    //未知样品表头
    @Override
    public Result<List<HeadVO>> getUnknownSampleTableHead(String taskId, String methodId, Boolean allDisplay,String deviceSerialNumber) {

        if (!StringUtils.hasText(taskId)) {
            //请选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000006"));
        }
        if ( !StringUtils.hasText(methodId)){
            //请选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000002"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        LinkedHashSet<HeadVO> ans = new LinkedHashSet<>();
        //根据任务中记录的样品id判断是否存在空白样品

        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        TMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_unknown.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
            if (shot != null && shot.getUnknownHead() != null) {

                if (shot.getUnknownHead().getTaskHeads() != null) {
                    ans.addAll(shot.getUnknownHead().getTaskHeads());
                }
                if (shot.getUnknownHead().getSampleHeads() != null) {
                    ans.addAll(shot.getUnknownHead().getSampleHeads());
                }
            }
        }else {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_unknown.getIdentity());
            ans.addAll(preHead);
        }

        boolean empty = false;
        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

                if(sample == null || !SampleTypeEnum.kind_unknown.getIdentity().equals(sample.getSampleKind())) {
                    log.info("跳过该样品");
                    continue;
                }
                if (StringUtils.hasText(sample.getSampleType())) {
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())){
                        empty = true;
                    }
                }
                if (StringUtils.hasText(sample.getHeads())){
                    List<HeadVO> heads = JSON.parseArray(sample.getHeads(), HeadVO.class);
                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }
        }

        List<HeadVO> m = new ArrayList<>();
        for (HeadVO an : ans) {
            if (!an.getIdentity().equals(SampleTableEnum.FIT_CONCENTRATION.getIdentity())){
                m.add(an);
            }
        }
        List<HeadVO> result = dealHeads(new ArrayList<>(m), parameter, SampleTypeEnum.kind_unknown.getIdentity(), allDisplay, empty, deviceSerialNumber);
        return Result.ok(result);
    }

    @Override
    public R<String> saveRationSampleMeasureData(RationDataVO dataInfo) {

        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();

        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber)
                .last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);

        TMeasureData tMeasureData = new TMeasureData();
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        tMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        tMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (data != null) {
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(0);
        }
        tMeasureData.setIsDelete(createType);
        tMeasureDataService.save(tMeasureData);

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null && analysisData.getMeasureDataId() == null) {

            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.updateById(analysisData);

        } else {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setDataVersion(0);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }


        return R.ok(tMeasureData.getId());

    }

    @Override
    public R<RationSaveCountDataInfo> saveSampleMeasureData(RationDataVO dataInfo) {
        String sampleId = dataInfo.getSampleId();
        String taskId = dataInfo.getTaskId();

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        task.setTaskStatus(TaskStatusEnum.TESTED.getCode());
        tMeasureTaskService.updateById(task);


        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber)
                .last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);

        TMeasureData tMeasureData = new TMeasureData();
        tMeasureData.setTaskId(taskId);
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(dataInfo.getMachineData()));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(dataInfo.getWavelengthData()));
        tMeasureData.setMeasurementStartTime(dataInfo.getStartTime());
        tMeasureData.setMeasurementEndTime(dataInfo.getEndTime());
        tMeasureData.setSamplingTime(dataInfo.getSampleTime());

        if (data != null) {
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(0);
        }
        tMeasureData.setIsDelete(createType);
        tMeasureDataService.save(tMeasureData);

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null && analysisData.getMeasureDataId() == null) {

            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.updateById(analysisData);

        } else {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(tMeasureData.getId());
            analysisData.setDataVersion(0);
            analysisData.setAnalysisResult(JSON.toJSONString(dataInfo.getCountData()));
            analysisData.setIsDelete(createType);
            tMeasureAnalysisDataService.save(analysisData);
        }

        RationSaveCountDataInfo countDataInfo = new RationSaveCountDataInfo();
        countDataInfo.setTaskId(taskId);
        countDataInfo.setSampleId(sampleId);
        countDataInfo.setMeasureDataId(tMeasureData.getId());
        countDataInfo.setAnalyzeDataId(analysisData.getId());
        countDataInfo.setCountData(dataInfo.getCountData());

        return R.ok(countDataInfo);
    }

    @Override
    public R<Boolean> saveAnalyzeData(RationSaveCountDataInfo countData) {
        String taskId = countData.getTaskId();
        String sampleId = countData.getSampleId();
        String measureDataId = countData.getMeasureDataId();
        TMeasureAnalysisData analysisData;
        if (StringUtils.hasText(countData.getAnalyzeDataId())) {
            analysisData = tMeasureAnalysisDataService.getById(countData.getAnalyzeDataId());
        } else {
            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                    .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                    .eq(TMeasureAnalysisData::getMeasureDataId, measureDataId)
                    .orderByDesc(TMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        }

        if (analysisData == null) {
            analysisData = new TMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setMeasureDataId(measureDataId);
            analysisData.setDataVersion(0);
            analysisData.setIsDelete(createType);
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            tMeasureAnalysisDataService.save(analysisData);
        } else {
            if (!analysisData.getMeasureDataId().equals(measureDataId)) {
                analysisData.setMeasureDataId(measureDataId);
            }
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            tMeasureAnalysisDataService.updateById(analysisData);
        }

        return R.ok(true);

    }

    @Override
    public void saveEmptySampleData(List<RationSaveCountDataInfo> emptySampleList) {
        for (RationSaveCountDataInfo emptyData : emptySampleList) {
            LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureAnalysisData::getTaskId, emptyData.getTaskId())
                    .eq(TMeasureAnalysisData::getSampleKey, emptyData.getSampleId())
                    .eq(TMeasureAnalysisData::getMeasureDataId, emptyData.getMeasureDataId())
                    .orderByDesc(TMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(queryWrapper);
            if (analysisData != null) {
                analysisData.setAnalysisResult(JSON.toJSONString(emptyData.getCountData()));
                tMeasureAnalysisDataService.updateById(analysisData);
            } else {
                analysisData = new TMeasureAnalysisData();
                analysisData.setTaskId(emptyData.getTaskId());
                analysisData.setSampleKey(emptyData.getSampleId());
                analysisData.setMeasureDataId(emptyData.getMeasureDataId());
                analysisData.setDataVersion(0);
                analysisData.setAnalysisResult(JSON.toJSONString(emptyData.getCountData()));
                analysisData.setIsDelete(createType);
                tMeasureAnalysisDataService.save(analysisData);
            }
        }
    }

    @Override
    public R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter, Integer  digit) {

        List<String> waveLength = parameter.getWaveLength();
        List<String> lengthParams = parameter.getWaveLengthParams();
        double ans = 0;

        for (int i = 0; i < waveLength.size(); i++) {
            double value = Double.MIN_VALUE;
            for (RationWavelengthDataInfo dataInfo : originData) {
                if (waveLength.get(i).equals(dataInfo.getWavelength()) && dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                    value = DataCountUtil.formatValue(digit, Double.parseDouble(dataInfo.getAbsValue()));
                }
            }
            if (value == Double.MIN_VALUE) {
                //未找到对应波长数据
                return R.fail(YamlMessageSource.getMessageStatic("0030300E"));
            }
            if (i < lengthParams.size()) {
                value = value * Double.parseDouble(lengthParams.get(i));
            } else {
                value = value * 1;
            }
            ans += value;
        }
        RationCountDataInfo countDataInfo = new RationCountDataInfo();
        countDataInfo.setColValue(DataCountUtil.formatValueWithBigDecimal(digit, String.valueOf(ans)));
        countDataInfo.setIdentity(SampleTableEnum.Abs.getIdentity());
        countDataInfo.setHeadName(SampleTableEnum.Abs.getName());

        return R.ok(countDataInfo);
    }

    @Override
    public R<RationCountDataInfo> countAbsValue(List<RationWavelengthDataInfo> originData, RationParameter parameter) {

        List<String> waveLength = parameter.getWaveLength();
        List<String> lengthParams = parameter.getWaveLengthParams();
        double ans = 0;

        for (int i = 0; i < waveLength.size(); i++) {
            double value = Double.MIN_VALUE;
            for (RationWavelengthDataInfo dataInfo : originData) {
                if (waveLength.get(i).equals(dataInfo.getWavelength())) {
                    value = Double.parseDouble(dataInfo.getAbsValue());
                }
            }
            if (value == Double.MIN_VALUE) {
                //未找到对应波长数据
                return R.fail(YamlMessageSource.getMessageStatic("0030300E"));
            }
            if (i < lengthParams.size()) {
                value = value * Double.parseDouble(lengthParams.get(i));
            } else {
                value = value * 1;
            }
            ans += value;
        }
        RationCountDataInfo countDataInfo = new RationCountDataInfo();
        countDataInfo.setColValue(String.valueOf(ans));
        countDataInfo.setIdentity(SampleTableEnum.Abs.getIdentity());
        countDataInfo.setHeadName(SampleTableEnum.Abs.getName());

        return R.ok(countDataInfo);
    }

    @Override
    public Result<List<Map<String, Object>>> queryStandSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        List<Map<String, Object>> ans = new ArrayList<>();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未选择样品");
            return Result.ok(ans);
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
            if (sample == null || !sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                log.info("不是标准样品，跳过");
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            //样品信息
            map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
            map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.SAMPLE_KIND.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+ sample.getSampleLocation());
            map.put(SampleTableEnum.DILUTION_RATIO.getIdentity(), sample.getDilutionRatio());
            map.put(SampleTableEnum.AMOUNT.getIdentity(), sample.getSampleVolume());

            if (StringUtils.hasText(sample.getNewColData())){
                Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {});

                map.putAll(object);
            }

            //数据信息,repeat：false-不重复，true-重复
            if (!repeat) {
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }

                    }

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            } else {
                //重复模式，获取数据方式待定----暂时先以不重复做回显
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        Map<String, Map<String, String>> childrenList = new HashMap<>();
                        List<Map<String, String>> middle = new ArrayList<>();
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            Map<String, String> children = new HashMap<>();

                            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                            } else {
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                                childrenList.computeIfAbsent(dataInfo.getIdentity(), k -> new HashMap<>()).putAll(children);
                            }
                        }
                        childrenList.forEach((key, value) -> middle.add(value));
                        map.put("children", middle);
                    }

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            }
            ans.add(map);
        }
        return Result.ok(ans);
    }

    @Override
    public Result<RationSampleDataVO> querySampleData(QueryDataDTO queryDataDTO) {

        String taskId = queryDataDTO.getTaskId();
        Boolean allDisplay = queryDataDTO.getAllDisplay();
        Integer digit = queryDataDTO.getDigit();
        Integer otherDigit = queryDataDTO.getOtherDigit();
        List<String> option = queryDataDTO.getOption();

        if (!StringUtils.hasText(taskId)) {
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            log.info("未查询到任务信息");
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (digit == null || digit < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
            queryDataDTO.setDigit(digit);
        }

        if(queryDataDTO.getAllDisplay() == null) {
            allDisplay = false;
        }
        queryDataDTO.setAllDisplay(allDisplay);
        TMeasureTaskAttachment attachment;
        boolean recount = false;

        String deviceSerialNumber = queryDataDTO.getDeviceSerialNumber();

        if (StringUtils.hasText(deviceSerialNumber)) {
            R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess()) {
                FSamplePoolInfoVO data = poolInfo.getData();

                if (data.getIsAutoChangePool()) {
                    List<Integer> usefulLocation = tSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
                    publicUtilService.changeSampleLocation(task.getId(), usefulLocation);
                }
            }
        }

        R<TMeasureTaskAttachment> r = taskAttachmentService.queryByTaskId(taskId);
        if (!r.getSuccess()) {
            log.info("未查询到任务附件信息");
            attachment = new TMeasureTaskAttachment();
            attachment.setTaskId(taskId);
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            taskAttachmentService.save(attachment);
            recount = true;
        } else {
            attachment = r.getData();

            if (!StringUtils.hasText(attachment.getDataParam())) {
                attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                taskAttachmentService.saveOrUpdate(attachment);
                recount = true;
            } else {
                QueryDataDTO dataDTO = JSON.parseObject(attachment.getDataParam(), QueryDataDTO.class);
                if (dataDTO.getDigit() == null || !dataDTO.getDigit().equals(digit)) {
                    recount = true;

                    attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                    taskAttachmentService.saveOrUpdate(attachment);
                } else if (dataDTO.getOtherDigit() == null || !dataDTO.getOtherDigit().equals(otherDigit)) {
                    recount = true;

                    attachment.setDataParam(JSON.toJSONString(queryDataDTO));
                    taskAttachmentService.saveOrUpdate(attachment);
                }
            }
        }

        if (recount) {
            changeStandSampleDataDigit(taskId, digit);
            changeUnknowSampleDigit(taskId, digit);
        }
        List<Integer> usefulLocation = new ArrayList<>();
        if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {
            usefulLocation = tSamplePoolConfigService.getUsefulLocation(queryDataDTO.getDeviceSerialNumber());
        }
        /*
        查询数据
        */
        RationSampleDataVO dataVO = new RationSampleDataVO();

        List<Map<String, Object>> standSampleData = new ArrayList<>();
        List<Map<String, Object>> unknowSampleData = new ArrayList<>();
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            dataVO.setStandSampleData(standSampleData);
            dataVO.setUnknowSampleData(unknowSampleData);
            return Result.ok(dataVO);
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && "1".equals(specimen.getDeleteType())) {
                continue;
            }
            if (specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                Map<String, Object> singleSampleData = getSingleSampleData(task, sample, repeat, option, digit, otherDigit);
                singleSampleData.put("deleteType", specimen.getDeleteType());

                if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {

                    if (specimen.getIsTested().equals(noTested) && !usefulLocation.isEmpty()) {
                        R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(queryDataDTO.getDeviceSerialNumber());
                        if (poolInfo.getSuccess()) {
                            FSamplePoolInfoVO data = poolInfo.getData();
                            if (data.getIsAutoChangePool()) {
                                singleSampleData.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+sample.getSampleLocation());
                            }
                        }
                    }
                }

                standSampleData.add(singleSampleData);
            } else if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

                Map<String, Object> sampleData = getSingleSampleData(task, sample, repeat, option, digit, otherDigit);
                sampleData.put("deleteType", specimen.getDeleteType());
                if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {

                    if (specimen.getIsTested().equals(noTested) && !usefulLocation.isEmpty()) {
                        R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(queryDataDTO.getDeviceSerialNumber());
                        if (poolInfo.getSuccess()) {
                            FSamplePoolInfoVO data = poolInfo.getData();
                            if (data.getIsAutoChangePool()) {
                                sampleData.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+sample.getSampleLocation());
                            }
                        }
                    }
                }

                unknowSampleData.add(sampleData);


            } else {
                log.info("类型出错");
            }
        }
        dataVO.setUnknowSampleData(unknowSampleData);
        dataVO.setStandSampleData(standSampleData);
        return Result.ok(dataVO);

    }

    private Map<String, Map<String, String>> extracted(List<String> option, RationWavelengthDataInfo wlData, TMeasureSample sample) {

        Map<String, Map<String, String>> ans = new HashMap<>();

        RepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        RepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());
        String pre = StringUtils.hasText(sample.getSampleNumber()) ? sample.getSampleNumber() + "-" : "";


        if (option != null && !option.isEmpty()) {
            for (String s : option) {
                Map<String, String> children = new HashMap<>();
                if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.avg.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), wlData.getAbsValue());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(),wlData.getTPercentValue());
                    ans.putIfAbsent(pre + DataIdentityEnums.avg.getIdentity(), children);
                }
                if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.sd.getName());

                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getSd());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getSd());
                    ans.putIfAbsent(pre + DataIdentityEnums.sd.getIdentity(), children);
                }
                if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                    children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + DataIdentityEnums.rsd.getName());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getRsd());
                    children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getRsd());
                    ans.putIfAbsent(pre + DataIdentityEnums.rsd.getIdentity(), children);
                }
            }
        }
        return ans;
    }

    public Map<String, Object> getSingleSampleData(TMeasureTask task, TMeasureSample sample, Boolean repeat, List<String> option, Integer digit, Integer otherDigit) {

        String sampleId = sample.getId();
        String taskId = task.getId();
        if (otherDigit == null) {
            otherDigit = Constants.DEFAULT_DECIMAL_NUM;
        }

        Map<String, Object> map = new HashMap<>();
        //样品信息
        map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
        map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
        map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
        map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
        map.put(SampleTableEnum.SAMPLE_KIND.getIdentity(), sample.getSampleKind());

        SystemBusinessParamVO businessParamVO = businessParamService.queryBusinessParam();
        Integer concentrationDigits = businessParamVO.getConcentrationDigits();

        if (sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            String s = DataCountUtil.addZero(Double.valueOf(sample.getNormalConcentration()), concentrationDigits);
            map.put(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity(), s);
        }

        if (StringUtils.hasText(sample.getNewColData())) {
            Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {
            });

            map.putAll(object);
        }

        //数据信息,repeat：false-不重复，true-重复
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);
        if (!repeat) {
            if (data != null) {

                if (StringUtils.hasText(data.getOriginalCalculationData())) {
                    List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                    for (RationWavelengthDataInfo dataInfo : list) {
                        Double v = DataCountUtil.formatValue(digit, Double.valueOf(dataInfo.getAbsValue()));
                        String s = DataCountUtil.addZero(v, digit);

                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), s);

                        Double t = DataCountUtil.formatValue(otherDigit, Double.valueOf(dataInfo.getTPercentValue()));
                        String tP = DataCountUtil.addZero(t, otherDigit);

                        map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tP);
                    }
                }

                map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());

                LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                        .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                        .eq(TMeasureAnalysisData::getMeasureDataId, data.getId())
                        .orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");

                TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null && one.getAnalysisResult() != null) {
                    List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                    for (RationCountDataInfo countData : countDataList) {

                        if (countData.getColValue() != null) {
                            map.put(countData.getIdentity(), countData.getColValue());
                        }
                    }
                }

            }
        } else {
            //重复模式，获取数据方式待定----暂时先以不重复做回显
            if (data != null) {
                map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                if (StringUtils.hasText(data.getOriginalCalculationData())) {

                    Map<String, Map<String, String>> childrenList = new TreeMap<>((key1, key2) -> {
                        // 分割键，格式如 "default-1-1"
                        String[] parts1 = key1.split("-");
                        String[] parts2 = key2.split("-");

                        // 按照各部分进行比较
                        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {

                            // 如果都是数字，则按数值比较
                            if (isNumeric(parts1[i]) && isNumeric(parts2[i])) {
                                int num1 = Integer.parseInt(parts1[i]);
                                int num2 = Integer.parseInt(parts2[i]);
                                int compare = Integer.compare(num1, num2);
                                if (compare != 0) {
                                    return compare;
                                }
                            } else {
                                // 否则按字符串比较
                                int compare = parts1[i].compareTo(parts2[i]);
                                if (compare != 0) {
                                    return compare;
                                }
                            }
                        }

                        // 如果前缀都相同，长度短的排在前面
                        return Integer.compare(parts1.length, parts2.length);
                    });

                    Map<String, Map<String, String>> resultChildrenList = new TreeMap<>();
                    List<Map<String, String>> middle = new ArrayList<>();
                    List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                    for (RationWavelengthDataInfo dataInfo : list) {
                        Map<String, String> children = new HashMap<>();

                        Double v = DataCountUtil.formatValue(digit, Double.valueOf(dataInfo.getAbsValue()));
                        String s = DataCountUtil.addZero(v, digit);

                        if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), s);

                            Double t = DataCountUtil.formatValue(otherDigit, Double.valueOf(dataInfo.getTPercentValue()));
                            String tP = DataCountUtil.addZero(t, otherDigit);

                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tP);

//                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getAbsValue()));
//                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit, dataInfo.getTPercentValue()));

                            Map<String, Map<String, String>> ch = extracted(option, dataInfo, sample);
                            if (!ch.isEmpty()) {
                                for (Map.Entry<String, Map<String, String>> entry : ch.entrySet()) {
                                    String key = entry.getKey();
                                    Map<String, String> value = entry.getValue();

                                    if (resultChildrenList.containsKey(key)) {
                                        // 如果键已存在，将新值中的键值对合并到现有Map中
                                        Map<String, String> existingMap = resultChildrenList.get(key);
                                        // 将value中的所有键值对放入existingMap中（相同键会覆盖，新键会添加）
                                        existingMap.putAll(value);
                                    } else {
                                        // 如果键不存在，直接添加（创建副本以避免引用问题）
                                        resultChildrenList.put(key, new HashMap<>(value));
                                    }
                                }
                            }

                        } else {

                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), s);

                            Double t = DataCountUtil.formatValue(otherDigit, Double.valueOf(dataInfo.getTPercentValue()));
                            String tP = DataCountUtil.addZero(t, otherDigit);

                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tP);

//                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getAbsValue()));
//                            children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), DataCountUtil.formatValueWithBigDecimal(digit,dataInfo.getTPercentValue()));
                            String[] split = dataInfo.getIdentity().split("-");
                            String pre = StringUtils.hasText(sample.getSampleNumber()) ? sample.getSampleNumber() + "-" : "";
                            children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), pre + split[split.length - 1]);
                            String time = dataInfo.getMeasureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            children.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), time);
                            childrenList.computeIfAbsent(dataInfo.getIdentity(), k -> new HashMap<>()).putAll(children);
                        }
                    }
                    childrenList.forEach((key, value) -> middle.add(value));
                    resultChildrenList.forEach((key, value) -> middle.add(value));
                    map.put("children", middle);
                }
            }

        }

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId);
        if (data != null) {
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getMeasureDataId, data.getId());
        }
        analysisDataQueryWrapper.orderByDesc(TMeasureAnalysisData::getDataVersion).last("limit 1");

        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null && one.getAnalysisResult() != null) {
            List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
            for (RationCountDataInfo countData : countDataList) {
                if (StringUtils.hasText(countData.getColValue())) {
                    map.put(countData.getIdentity(), DataCountUtil.addZero(Double.valueOf(countData.getColValue()), digit));
                }
            }
        }
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            Map<String, String> jsonObject;
            jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>(){});

            map.putAll(jsonObject);
        }
        return map;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches("-?\\d+");
    }



    @Override
    public Result<List<Map<String, Object>>> queryUnknowSampleData(String taskId, Boolean allDisplay) {
        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        List<Map<String, Object>> ans = new ArrayList<>();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        boolean repeat = false;
        if (StringUtils.hasText(task.getMethodKey())) {
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
                if (StringUtils.hasText(parameter.getRepeatMode()) && parameter.getRepeatMode().equals("单池重复")) {
                    repeat = true;
                }
            }
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未选择样品");
            return Result.ok(ans);
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        for (TaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }
            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
            if (sample == null || !sample.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                log.info("不是未知样品，跳过");
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            //样品信息
            map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
            map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.DILUTION_RATIO.getIdentity(), sample.getDilutionRatio());
            map.put(SampleTableEnum.AMOUNT.getIdentity(), sample.getSampleVolume());
            map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put(SampleTableEnum.SAMPLE_KIND.getIdentity(), sample.getSampleKind());
            map.put(SampleTableEnum.NORMAL_CONCENTRATION.getIdentity(), sample.getNormalConcentration());
            map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池"+sample.getSampleLocation());

            if (StringUtils.hasText(sample.getNewColData())){
                Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {});

                map.putAll(object);
            }

            //数据信息,repeat：false-不重复，true-重复
            if (!repeat) {
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {

                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                            map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                        }
                    }

                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            } else {
                //重复模式，获取数据方式待定----暂时先以不重复做回显
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getTaskId, taskId)
                        .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
                TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                if (data != null) {
                    map.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), data.getSamplingTime());
                    map.put(SampleTableEnum.REMARK.getIdentity(), data.getRemark());
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        Map<String, Map<String, String>> childrenList = new HashMap<>();
                        List<Map<String, String>> middle = new ArrayList<>();
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        for (RationWavelengthDataInfo dataInfo : list) {
                            Map<String, String> children = new HashMap<>();

                            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                map.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                            } else {
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue());
                                children.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue());
                                childrenList.computeIfAbsent(dataInfo.getIdentity(), k -> new HashMap<>()).putAll(children);
                            }
                        }
                        childrenList.forEach((key, value) -> middle.add(value));
                        map.put("children", middle);
                    }

                    LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                    analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                            .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureAnalysisData::getAnalysisName, data.getId() + "-" + data.getSerialNumber());

                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                    if (one != null && one.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataList = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo countData : countDataList) {
                            map.put(countData.getIdentity(), String.valueOf(countData.getColValue()));
                        }
                    }

                }
            }
            ans.add(map);
        }
        return Result.ok(ans);
    }


    /**
     * 标样需要校验是否全测完了，为了前端弹窗提示输入浓度
     */
    @Override
    public R<Boolean> checkStandAllSampleTest(String taskId) {

         TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            return R.ok(true);
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        if (taskSpecimen == null || taskSpecimen.isEmpty()) {
            return R.ok(true);
        }
        boolean allTested = true;
        for (TaskSpecimen specimen : taskSpecimen) {
            if(specimen.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                if (specimen.getIsTested().equals(noTested) && specimen.getDeleteType().equals(createType)) {
                    allTested = false;
                    break;
                }
            }
        }

        return R.ok(allTested);
    }

    /**
     * 创建样品
     */
    @Override
    public R<Boolean> createSample(GuideCreateSampleDTO dto) {

        String taskId = dto.getTaskId();
        String sampleKind = dto.getSampleKind();
        String normalConcentration = dto.getNormalConcentration();
        String updateBy = dto.getUpdateBy();
        String deviceSerialNumber = dto.getDeviceSerialNumber();

         TMeasureTask task = tMeasureTaskService.getById(taskId);

        if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {

            if (!StringUtils.hasText(normalConcentration)) {
                //请输入标准浓度
                return R.fail(YamlMessageSource.getMessageStatic("00102024"));
            }
            try {
                double v = Double.parseDouble(normalConcentration);

                if (v < 0) {
                    // 输入提示：标准浓度必须是非负数
                    return R.fail(YamlMessageSource.getMessageStatic("0010202D"));
                }
            } catch (Exception e) {
                //请输入正确的标准浓度
                return R.fail(YamlMessageSource.getMessageStatic("0010202D"));
            }
        }

        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }
        Result<List<HeadVO>> listResult = querySampleHeads(taskId, sampleKind);
        if (!listResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())){
            return R.fail(listResult.getMessage());
        }
        Integer sampleAutoNum = task.getSampleAutoNum();
        if (sampleAutoNum == null) {
            sampleAutoNum = 0;
        }

        SystemBusinessParamVO businessParamVO = businessParamService.queryBusinessParam();
        Integer concentrationDigit = businessParamVO.getConcentrationDigits();

        List<HeadVO> heads = listResult.getData();
        //动态表头
        List<HeadVO> dynamicHeads = new ArrayList<>();
        heads.forEach(head -> {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity())){
                dynamicHeads.add(head);
            }
        });
        boolean isAutoChangePool = false;

        R<FSamplePoolInfoVO> poolInfo = tSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
        if (poolInfo.getSuccess()) {
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            isAutoChangePool = poolInfoVO.getIsAutoChangePool();

        }

        if (taskSpecimen.isEmpty()) {
            TMeasureSample newSample = new  TMeasureSample();

            sampleAutoNum = 1;
            String namePrefix = "样品";
            int startNum = 1;

            if (dto.getSampleNameConfig() != null) {
                SampleCreateHeadConf nameConfig = dto.getSampleNameConfig();
                if (StringUtils.hasText(nameConfig.getPreName())) {
                    namePrefix = nameConfig.getPreName();
                }
                if (nameConfig.getStartNum() != null) {
                    startNum = nameConfig.getStartNum();
                }
            }

            String sampleNumPrefix = "default-";
            int numStartNum = 1;
            if (dto.getSampleNumConfig() != null) {
                SampleCreateHeadConf numConfig = dto.getSampleNumConfig();
                if (StringUtils.hasText(numConfig.getPreName())) {
                    sampleNumPrefix = numConfig.getPreName();
                }
                if (numConfig.getStartNum() != null) {
                    numStartNum = numConfig.getStartNum();
                }
            }

            newSample.setSampleName(namePrefix + startNum);

            newSample.setSampleNumber(sampleNumPrefix + numStartNum);

            if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
            } else {
                newSample.setSampleType(SampleTypeEnum.stand.getIdentity());

                String newNormalConcentration = DataCountUtil.addZero(Double.parseDouble(normalConcentration), concentrationDigit);

                newSample.setNormalConcentration(newNormalConcentration);
            }

            newSample.setSampleKind(sampleKind);
            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }
            boolean save = tMeasureSampleService.save(newSample);
            if (!save) {
                //保存失败
                return R.fail(YamlMessageSource.getMessageStatic("00300008"));
            }

            taskSpecimen.add(new TaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), sampleKind));

        } else {
            boolean hasNoTested = false;
            List<String> sampleNumList = new ArrayList<>();
            List<String> sampleNameList = new ArrayList<>();

            double cou = 0;
            double last = 0;

            for (TaskSpecimen specimen : taskSpecimen) {
                if (!specimen.getSampleKind().equals(sampleKind) || specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                if (specimen.getIsTested().equals(noTested)) {
                    hasNoTested = true;
                    break;
                }
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (measureSample != null && StringUtils.hasText(measureSample.getSampleName())){
                    sampleNumList.add(measureSample.getSampleNumber());
                    sampleNameList.add(measureSample.getSampleName());
                    if (StringUtils.hasText(measureSample.getNormalConcentration())) {
                        cou =  Double.parseDouble(measureSample.getNormalConcentration()) - last;
                        last = Double.parseDouble(measureSample.getNormalConcentration());
                    }
                }
            }
            if (hasNoTested) {
                return R.ok(true);
            }

             TMeasureSample newSample = new  TMeasureSample();
            if (cou == 0) {
                cou = 1;
            }

            String namePrefix = "样品";
            int nameStartNum = 1;
            int nameSuffix = 1;

            if (dto.getSampleNameConfig() != null) {
                SampleCreateHeadConf nameConfig = dto.getSampleNameConfig();
                if (StringUtils.hasText(nameConfig.getPreName())) {
                    namePrefix = nameConfig.getPreName();
                }
                if (nameConfig.getStartNum() != null) {
                    nameStartNum = nameConfig.getStartNum();
                }
                if (nameConfig.getStep() != null) {
                    nameSuffix = nameConfig.getStep();
                }
            }

            String sampleNumPrefix = "default-";
            int numStartNum = 1;
            int numSuffix = 1;
            if (dto.getSampleNumConfig() != null) {
                SampleCreateHeadConf numConfig = dto.getSampleNumConfig();
                if (StringUtils.hasText(numConfig.getPreName())) {
                    sampleNumPrefix = numConfig.getPreName();
                }
                if (numConfig.getStartNum() != null) {
                    numStartNum = numConfig.getStartNum();
                }
                if (numConfig.getStep() != null) {
                    numSuffix = numConfig.getStep();
                }
            }

            sampleAutoNum = numStartNum;
            while (sampleNumList.contains(sampleNumPrefix+ sampleAutoNum)) {
                sampleAutoNum += numSuffix;
            }

            int nameNum = nameStartNum;
            while (sampleNameList.contains(namePrefix+ nameNum)) {
                nameNum += nameSuffix;
            }

            int num = sampleAutoNum;
            int numName = nameNum;

            sampleNumList.add(sampleNumPrefix + num);
            sampleNameList.add(namePrefix+ numName);
            newSample.setSampleNumber(sampleNumPrefix + num);
            newSample.setSampleName(namePrefix + numName);

            List<Integer> usefulLocation = tSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            int all = usefulLocation.size();

            if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                newSample.setSampleType(SampleTypeEnum.unKnown.getIdentity());
            } else {
                newSample.setSampleType(SampleTypeEnum.stand.getIdentity());

                String newNormalConcentration = DataCountUtil.addZero(Double.parseDouble(normalConcentration), concentrationDigit);
                newSample.setNormalConcentration(newNormalConcentration);
            }
            newSample.setSampleKind(sampleKind);
            if (!dynamicHeads.isEmpty()) {
                newSample.setHeads(JSON.toJSONString(dynamicHeads));
            }
            boolean save = tMeasureSampleService.save(newSample);
            if (!save) {
                //保存失败
                return R.fail(YamlMessageSource.getMessageStatic("00300008"));
            }
            taskSpecimen.add(new TaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), sampleKind));


            if (isAutoChangePool) {
                for (int i = 1; i < all; i++) {
                    newSample.setId(null);
                    while (sampleNumList.contains(sampleNumPrefix+ sampleAutoNum)) {
                        sampleAutoNum += numSuffix;
                    }

                    while (sampleNameList.contains(namePrefix+ nameNum)) {
                        nameNum += nameSuffix;
                    }

                    num = sampleAutoNum;
                    numName = nameNum;

                    sampleNumList.add(sampleNumPrefix + num);
                    sampleNameList.add(namePrefix+ numName);
                    newSample.setSampleNumber(sampleNumPrefix + num);
                    newSample.setSampleName(namePrefix + numName);

                    if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
                        normalConcentration = DataCountUtil.addZero((Double.parseDouble(normalConcentration) + cou), concentrationDigit);

                        newSample.setNormalConcentration(normalConcentration);
                    }
                    save = tMeasureSampleService.save(newSample);
                    if (!save) {
                        //保存失败
                        return R.fail(YamlMessageSource.getMessageStatic("00300008"));
                    }
                    taskSpecimen.add(new TaskSpecimen(newSample.getId(), createType, noTested, newSample.getSampleType(), sampleKind));
                }
            }
        }
        task.setSampleAutoNum(sampleAutoNum);
        task.setSampleKey(JSON.toJSONString(taskSpecimen));

        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            //更新失败
            return R.fail(YamlMessageSource.getMessageStatic("00000048"));
        }

        if (isAutoChangePool) {
            List<Integer> usefulLocation = tSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            guidePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
        }

        return R.ok(true);
    }



    @Override
    public R<FittingReceiveInfo> fittingCurve(List<RationCountDataInfo> countData, Boolean empty, String methodId, List<String> inputParam) {
        
        List<Double> data = new ArrayList<>();
        FittingParam param = new FittingParam();

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        if (!StringUtils.hasText(parameter.getCurveType())){
            //请选择曲线类型
            return R.fail(YamlMessageSource.getMessageStatic("00102025"));
        }
        int minPointNum = getMinPointNum(parameter);

        if (inputParam.size() < minPointNum) {
            log.info("数据点数不足，不进行曲线拟合");
            //数据点数不足，不进行曲线拟合
            return R.fail(YamlMessageSource.getMessageStatic("00102026"));
        }

        if (empty) {
            for (RationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                    data.add(Double.parseDouble(dataInfo.getColValue()));

                }
            }
        } else {
            for (RationCountDataInfo dataInfo : countData) {
                if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                    data.add(Double.parseDouble(dataInfo.getColValue()));
                }
            }
        }

        List<String> zeroMethod = parameter.getZeroMethod();

        param.setForce_zero(0L);

        if (zeroMethod != null && !zeroMethod.isEmpty()) {
            for (String zero: zeroMethod) {
                if (zero.equals(RationCurveParamEnums.insert_zero.getIdentity())) {
                    data.add(0.00);
                    inputParam.add("0.0");
                }

                if (zero.equals(RationCurveParamEnums.force_zero.getIdentity())) {
                    param.setForce_zero(1L);
                }
            }
        }

        param.setDegree(Long.parseLong(parameter.getCurveType()));

        if (RationCurveParamEnums.typeAbs.getIdentity().equals(parameter.getEquationType())) {
            param.setX_data(data.stream().mapToDouble(Double::doubleValue).toArray());
            param.setY_data(inputParam.stream().mapToDouble(Double::parseDouble).toArray());
        } else {
            param.setY_data(data.stream().mapToDouble(Double::doubleValue).toArray());
            param.setX_data(inputParam.stream().mapToDouble(Double::parseDouble).toArray());
        }
        String s = restUtil.dataFit(param);
        FittingReceiveInfo info = JSON.parseObject(s, FittingReceiveInfo.class);
        if (info == null) {
            log.info("数据拟合失败");
            return R.fail();
        }

        if (!ResultCodeEnum.SUCCESS.getCode().equals(info.getCode())) {
            log.info("数据拟合失败");
            return R.fail(info.getMessage());
        }

        SystemBusinessParamVO businessParamVO = businessParamService.queryBusinessParam();
        Integer kDigits = businessParamVO.getKDigits();
        Integer rSquareDigits = businessParamVO.getRSquareDigits();
        List<Double> kList = new ArrayList<>();

        if (info.getCoefficients() != null && !info.getCoefficients().isEmpty()) {
            for (Double coefficient : info.getCoefficients()) {
                Double v = DataCountUtil.formatValue(kDigits, coefficient);
                kList.add(v);
            }
            info.setRoundCoefficient(kList);
        }

        if (info.getR() != null){

            Double r = Double.parseDouble(info.getR());
            if (!r.isNaN() && !r.isInfinite()) {
                Double value = DataCountUtil.formatValue(rSquareDigits, r);
                info.setRoundR(DataCountUtil.addZero(value, rSquareDigits));
            } else {
                info.setRoundR(String.valueOf(Double.NaN));
            }
        }

        if (info.getR2() != null){

            Double r = Double.parseDouble(info.getR2());
            if (!r.isNaN() && !r.isInfinite()) {
                Double value = DataCountUtil.formatValue(rSquareDigits, r);
                info.setRoundR2(DataCountUtil.addZero(value, rSquareDigits));
            } else {
                info.setRoundR2(String.valueOf(Double.NaN));
            }
        }

        String equation = "";

        if (RationCurveParamEnums.typeAbs.getIdentity().equals(parameter.getEquationType())) {

            equation = rebuildFormula(kList, kDigits, info.getDegree(), "Abs", "C");
            info.setEquation(equation);
        } else {
            equation = rebuildFormula(kList,kDigits, info.getDegree(), "C", "Abs");
            info.setEquation(equation);
        }
        Arrays.stream(param.getX_data()).min().ifPresent(info::setMinX);
        Arrays.stream(param.getX_data()).max().ifPresent(info::setMaxX);

        return R.ok(info);
    }

    private static int getMinPointNum(RationParameter parameter) {
        int minPointNum = Integer.MAX_VALUE;

        switch (parameter.getCurveType()) {
            case "1":
                minPointNum = 2;
                break;
            case "2":
                minPointNum = 3;
                break;

            case "3":
                minPointNum = 4;
                break;

            case "4":
                minPointNum = 5;
                break;
        }

        if (parameter.getZeroMethod() != null && parameter.getZeroMethod().contains(RationCurveParamEnums.insert_zero.getIdentity())) {
            minPointNum = minPointNum - 1;
        }
        return minPointNum;
    }

    public String rebuildFormula(List<Double> coefficientList, Integer kDigits, Integer degree, String x, String y) {


        while (coefficientList.size() < degree + 1) {
            coefficientList.add(0.0);
        }

        StringBuilder formula = new StringBuilder();
        formula.append(y).append(" = ");
        int index = 0;

        for (int i = 0; i <= degree; i++) {

            // 设置系数字段（根据degree动态设置）
            if(coefficientList.get(degree - i) != 0.0){
                double cuff = coefficientList.get(degree - i);
                if (index == 0){
                    if (cuff < 0) {
                        formula.append(" - ");
                    }
                }
                if (index > 0) {
                    if (cuff < 0) {
                        formula.append(" - ");
                    } else {
                        formula.append(" + ");
                    }
                }
                // 添加系数绝对值，如果是负数则取绝对值
                double value = Math.abs(cuff);
                String s = DataCountUtil.formatValueWithBigDecimal(kDigits, String.valueOf(value));
                s = DataCountUtil.addZero(Double.valueOf(s), kDigits);

                formula.append(s);
                if (degree - i > 0) {
                    formula.append("*").append(x);
                    if (degree - i > 1) {
                        formula.append("^").append(degree - i);
                    }
                }
                index++;
            }
        }
        return formula.toString();
    }

    @Override
    public Result<RationFitCurveVO> queryCurveInfo(String taskId, String methodId){

        if (!StringUtils.hasText(taskId)) {
            log.info("缺少任务id");
            //未选择任务
            return Result.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        if (!StringUtils.hasText(methodId)) {
            log.info("缺少方法id");
            //未选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            log.info("任务不存在");
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        int digit = Constants.DEFAULT_DECIMAL_NUM;
        SystemBusinessParamVO businessParamVO = businessParamService.queryBusinessParam();
        int rSquareDigits = businessParamVO.getRSquareDigits();

//        R<TMeasureTaskAttachment> attachmentR = taskAttachmentService.queryByTaskId(taskId);
//        if (attachmentR.getSuccess()) {
//            TMeasureTaskAttachment data = attachmentR.getData();
//            if (data != null && data.getDataParam() != null) {
//                FQueryDataDTO dataDTO = JSON.parseObject(data.getDataParam(), FQueryDataDTO.class);
//                if (dataDTO!=null && dataDTO.getDigit() != null) {
//                    digit = dataDTO.getDigit();
//                }
//            }
//        }

        RationFitCurveVO resultVo = new RationFitCurveVO();


        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);

        if (StringUtils.hasText(task.getSampleKey())) {
            RationCurveDataInfo curveData = getCurveData(task, parameter, digit);

            if (StringUtils.hasText(parameter.getCoefficientMode()) && parameter.getCoefficientMode().equals(RationCurveParamEnums.direct_coefficient.getIdentity())) {
                FittingReceiveInfo fittingInfo = createFittingInfo(methodId);

                if (fittingInfo != null) {
                    fittingInfo.setMaxX(curveData.getMaxX());
                    fittingInfo.setMinX(curveData.getMinX());

                    resultVo = curveData(fittingInfo);
                    resultVo.setUnknowPoints(curveData.getUnknowPoints());
                    resultVo.setTaskId(taskId);
                    resultVo.setEquationType(RationCurveParamEnums.getName(parameter.getEquationType()));
                    resultVo.setCurveType(RationCurveParamEnums.getName(parameter.getCurveType()));
                    return Result.ok(resultVo);
                }
            }

            if (StringUtils.hasText(task.getFittingParam()) && !task.getFittingParam().equals("{}") && !task.getFittingParam().equals("null")){
                FittingReceiveInfo fittingParam = JSONObject.parseObject(task.getFittingParam(), FittingReceiveInfo.class);


                fittingParam.setMaxX(curveData.getMaxX());
                fittingParam.setMinX(curveData.getMinX());
                resultVo = curveData(fittingParam);
                resultVo.setStandPoints(curveData.getStandPoints());
                resultVo.setUnknowPoints(curveData.getUnknowPoints());

                if (StringUtils.hasText(fittingParam.getRoundR())) {
                    Double r = null;
                    try  {
                        r = Double.valueOf(fittingParam.getRoundR());
                    } catch (Exception ignored) {
                    }
                    if (r != null) {
                        if (!r.isNaN() && !r.isInfinite()) {
                            resultVo.setR(DataCountUtil.addZero(r, rSquareDigits));
                        } else {
                            resultVo.setR(String.valueOf(r));
                        }

                    }
                }
                if (StringUtils.hasText(fittingParam.getRoundR2())) {

                    Double r2 = null;
                    try  {
                        r2 = Double.valueOf(fittingParam.getRoundR2());
                    } catch (Exception ignored) {
                    }
                    if (r2 != null) {
                        if (!r2.isNaN() && !r2.isInfinite()) {
                            resultVo.setR2(DataCountUtil.addZero(r2, rSquareDigits));
                        } else {
                            resultVo.setR2(String.valueOf(r2));
                        }
                    }
                }
            }
        }
        resultVo.setTaskId(taskId);
        resultVo.setEquationType(RationCurveParamEnums.getName(parameter.getEquationType()));
        resultVo.setCurveType(RationCurveParamEnums.getName(parameter.getCurveType()));

        return Result.ok(resultVo);

    }

    private FittingReceiveInfo createFittingInfo(String methodId) {
        FittingReceiveInfo fittingReceiveInfo = new FittingReceiveInfo();

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (StringUtils.hasText(method.getMeasurementParamInfo())) {
            RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
            if (parameter.getCoefficientMode().equals(RationCurveParamEnums.direct_coefficient.getIdentity())) {
                int degree = Integer.parseInt(parameter.getCurveType());
                fittingReceiveInfo.setDegree(degree);

                String variable = parameter.getEquationType().equals(RationCurveParamEnums.typeAbs.getIdentity())? "Abs":"C";
                String Y = parameter.getEquationType().equals(RationCurveParamEnums.typeAbs.getIdentity())? "C":"Abs";
                StringBuilder formula = new StringBuilder();
                formula.append(Y).append(" = ");
                List<String> coefficientList = parameter.getCoefficientParams();
                List<Double> paramList = new ArrayList<>();
                while (coefficientList.size() < degree + 1) {
                    coefficientList.add("0");
                }
                for (int i = 0; i <= degree; i++) {

                    // 系数索引：从高次到低次（第一个系数是最高次项）
                    int coeffIndex = degree - i;
                    double coeffValue = 0.0;
                    paramList.add(Double.valueOf(coefficientList.get(i)));
                    // 解析系数值
                    if (coeffIndex < coefficientList.size()) {
                        try {
                            coeffValue = Double.parseDouble(coefficientList.get(coeffIndex));

                        } catch (NumberFormatException ignored) {
                            // 保持默认值0.0
                        }
                    }
                    // 设置系数字段（根据degree动态设置）
                    switch (degree - i) {
                        case 0: fittingReceiveInfo.setK0(coeffValue); break;
                        case 1: fittingReceiveInfo.setK1(coeffValue); break;
                        case 2: fittingReceiveInfo.setK2(coeffValue); break;
                        case 3: fittingReceiveInfo.setK3(coeffValue); break;
                        case 4: fittingReceiveInfo.setK4(coeffValue); break;
                    }
                    if(!coefficientList.get(degree - i).equals("0")){
                        double cuff = Double.parseDouble(coefficientList.get(degree - i));
                        if (i==0){
                            if (cuff < 0) {
                                formula.append(" - ");
                            }
                        }
                        if (i > 0) {
                            if (cuff < 0) {
                                formula.append(" - ");
                            } else {
                                formula.append(" + ");
                            }
                        }
                        System.out.println(formula);
                        System.out.println(cuff);
                        // 添加系数绝对值，如果是负数则取绝对值
                        formula.append(Math.abs(cuff));
                        if (degree - i > 0) {
                            formula.append("*").append(variable);
                            if (degree - i > 1) {
                                formula.append("^").append(degree - i);
                            }
                        }
                    }
                }
                fittingReceiveInfo.setEquation(formula.toString());
                fittingReceiveInfo.setRoundCoefficient(paramList);
                return fittingReceiveInfo;

            }

        }
        return null;
    }


    private RationCurveDataInfo getCurveData(TMeasureTask task, RationParameter parameter, Integer digit) {

        //系数方式：直接系数法或其他
        String coefficientMode = parameter.getCoefficientMode();
        boolean isDirectCoefficient = coefficientMode.equals(RationCurveParamEnums.direct_coefficient.getIdentity());
        String equationType = parameter.getEquationType();
        String taskId = task.getId();

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        boolean standEmpty = false;
        boolean unKnowEmpty = false;

        List<RationCurvePointsValue> standData = new ArrayList<>();
        List<RationCurvePointsValue> unKnowData = new ArrayList<>();

        for (TaskSpecimen specimen : taskSpecimen) {
            if (specimen.getDeleteType().equals(deleteType)) {
                continue;
            }

            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                if (specimen.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                    unKnowEmpty = true;
                } else {
                    standEmpty = true;
                }
                continue;
            }

            RationCurvePointsValue point = new RationCurvePointsValue();

            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, sample.getId())
                    .orderByDesc(TMeasureData::getSerialNumber)
                    .last("limit 1");
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (data == null) {
                continue;
            }

            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                    .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                    .eq(TMeasureAnalysisData::getMeasureDataId, data.getId())
                    .orderByDesc(TMeasureAnalysisData::getDataVersion)
                    .last("limit 1");
            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
            if (analysisData == null) {
                continue;
            }

            if (StringUtils.hasText(analysisData.getAnalysisResult())) {
                List<RationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), RationCountDataInfo.class);
                for (RationCountDataInfo dataInfo : countDataInfoList) {
                    if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                        point.setAbs(DataCountUtil.addZero(Double.valueOf(dataInfo.getColValue()), digit));
                    }
                    if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                        point.setSumAbs(DataCountUtil.addZero(Double.valueOf(dataInfo.getColValue()), digit));

                    }
                    if (dataInfo.getIdentity().equals(SampleTableEnum.FIT_CONCENTRATION.getIdentity())) {
                        point.setConcentration(DataCountUtil.addZero(Double.valueOf(dataInfo.getColValue()), digit));
                    }
                }
            }

            if (sample.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
                point.setConcentration(sample.getNormalConcentration());
                standData.add(point);
            } else {
                unKnowData.add(point);
            }
        }

        RationCurveDataInfo result = new RationCurveDataInfo();
        result.setMaxX(Double.MIN_VALUE);
        result.setMinX(Double.MAX_VALUE);

        if (!isDirectCoefficient && !standData.isEmpty()) {
            List<JSONObject> standPoints = processPoints(standData, standEmpty, equationType, result);
            result.setStandPoints(standPoints);
        }
        List<JSONObject> unknownPoints = processPoints(unKnowData, unKnowEmpty, equationType, result);
        result.setUnknowPoints(unknownPoints);


        if (result.getMaxX().equals(result.getMinX())) {
            if (result.getMaxX() == 0.0) {
                result.setMaxX(1.0);
                result.setMinX(0.0);
            } else {
                double v = result.getMaxX() / 20;
                result.setMaxX(result.getMaxX() + v);
                result.setMinX(result.getMinX() - v);
            }
        } else if (result.getMaxX()==Double.MIN_VALUE || result.getMinX()==Double.MAX_VALUE) {
                result.setMaxX(1.0);
                result.setMinX(0.0);
        }



        return result;
    }

    private List<JSONObject> processPoints(List<RationCurvePointsValue> dataList, boolean useSumAbs,
                                           String curveType, RationCurveDataInfo result) {
        List<JSONObject> points = new ArrayList<>();

        for (RationCurvePointsValue data : dataList) {
            JSONObject point = new JSONObject();

            if (RationCurveParamEnums.typeAbs.getIdentity().equals(curveType)) {
                // 吸光度为X轴
                String xValue = useSumAbs ? data.getSumAbs() : data.getAbs();
                point.put("x", xValue);
                updateMinMax(result, xValue);
                point.put("y", data.getConcentration());
            } else {
                // 浓度为X轴
                String xValue = data.getConcentration();
                point.put("x", xValue);
                updateMinMax(result, xValue);
                point.put("y", useSumAbs ? data.getSumAbs() : data.getAbs());
            }
            points.add(point);
        }

        return points;
    }

    // 提取更新最小最大值的逻辑
    private void updateMinMax(RationCurveDataInfo result, String value) {
        if (StringUtils.hasText(value)) {
            try {
                double v = Double.parseDouble(value);
                if (v > result.getMaxX()) {
                    result.setMaxX(v);
                }
                if (v < result.getMinX()) {
                    result.setMinX(v);
                }
            } catch (NumberFormatException e) {
                // 处理数值转换异常，根据实际需求决定是否记录日志
            }
        }
    }


    @Override
    public RationFitCurveVO curveData(FittingReceiveInfo info) {
        List<Double> list = info.getRoundCoefficient().stream()
                .map(d -> BigDecimal.valueOf(d)
                        .setScale(3, RoundingMode.HALF_UP)
                        .doubleValue())
                .collect(Collectors.toList());

        List<JSONObject> dataList = new ArrayList<>();
        List<Double> coefficients = info.getRoundCoefficient();
        for (int i = 0; i < 5; i++) {
            if (i >= coefficients.size()) {
                coefficients.add(0.0);
            }
        }

        double k0 = coefficients.get(0);
        double k1 = coefficients.get(1);
        double k2 = coefficients.get(2);
        double k3 = coefficients.get(3);
        double k4 = coefficients.get(4);

        double step = 0.01;
        int dataDigit = 8;
        //采用3位小数来计算
        double minX = info.getMinX() == null ? -1 : info.getMinX();
        double maxX = info.getMaxX() == null ? 1 : info.getMaxX();
        if (minX == maxX) {
            minX = minX - (minX / 2);
            maxX = maxX + (maxX / 2);
            if (minX == 0) {
                minX = -1;
                maxX = 1;
            }
        }
        if (minX > maxX) {
            double temp = minX;
            minX = maxX;
            maxX = temp;
        }

        step = (maxX - minX) / 100;

        double st = step * 3;

        switch (info.getDegree()) {
            case 1:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;
            case 2:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i + k2 * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;
            case 3:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);
                }
                break;

            case 4:
                for (double i = minX - st; i < maxX + st; i += step) {
                    double y = k0 + k1 * i + k2 * i * i + k3 * i * i * i + k4 * i * i * i * i;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("x", new BigDecimal(i).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    jsonObject.put("y", new BigDecimal(y).setScale(dataDigit, RoundingMode.HALF_UP).doubleValue());
                    dataList.add(jsonObject);

                }
                break;
            default:
                break;
        }
        RationFitCurveVO vo = new RationFitCurveVO();
        vo.setCoefficientList(list);
        vo.setDataList(dataList);
        vo.setEquation(info.getEquation());
        vo.setCurveType(RationCurveParamEnums.getName(String.valueOf(info.getDegree())));

        if (StringUtils.hasText(info.getRoundR())) {
            Double r = null;
            try  {
                r = Double.valueOf(info.getRoundR());
            } catch (Exception ignored) {
            }
            if (r != null) {
                if (!r.isNaN() && !r.isInfinite()) {
                    vo.setR(DataCountUtil.addZero(r, dataDigit));
                } else {
                    vo.setR(String.valueOf(r));
                }
            }
        }
        if (StringUtils.hasText(info.getRoundR2())) {
            Double r2 = null;
            try  {
                r2 = Double.valueOf(info.getRoundR2());
            } catch (Exception ignored) {
            }

            if (r2 != null) {
                if (!r2.isNaN() && !r2.isInfinite()) {
                    vo.setR2(DataCountUtil.addZero(r2, dataDigit));
                } else {
                    vo.setR2(String.valueOf(r2));
                }
            }
        }
        return vo;
    }

    /**
     * 值计算
     * @param info 曲线信息
     * @param value 待计算的值
     * @param equationType 曲线类型
     * @param digit 小数位数
     * @return
     */
    @Override
    public String valueCalculate(FittingReceiveInfo info, Double value, String equationType, Integer digit) {

        String s = "";
        String ans = null;

        if (equationType.equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getRoundCoefficient(), value, 1);
        } else if (equationType.equals(RationCurveParamEnums.typeCurve.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getRoundCoefficient(), value, 0);
        }
        if (StringUtils.hasText(s)) {
            JSONObject jsonObject = JSON.parseObject(s);

            String mid = jsonObject.get("value").toString();
            ans = DataCountUtil.formatValueWithBigDecimal(digit, mid);
        }
        return ans;
    }

    @Override
    public String valueCalculate(FittingReceiveInfo info, Double value, String equationType) {

        String s = "";
        String ans = null;

        if (equationType.equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getRoundCoefficient(), value, 1);
        } else if (equationType.equals(RationCurveParamEnums.typeCurve.getIdentity())) {
            s = restUtil.concentrationCalculate(info.getRoundCoefficient(), value, 0);
        }
        if (StringUtils.hasText(s)) {
            JSONObject jsonObject = JSON.parseObject(s);

            ans = jsonObject.get("value").toString();
        }
        return ans;
    }

    @Override
    public List<RationCountDataInfo> dataRounding(List<RationCountDataInfo> dataVO, RationParameter parameter) {
        List<RoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams == null || approxParams.isEmpty()) {
            log.info("没有设置修约参数");
            return dataVO;
        }
        List<PreCountResultParam> resultParams = parameter.getResultParams();
        if (resultParams == null || resultParams.isEmpty()) {
            log.info("没有设置待计算结果参数，不做修约处理");
            return dataVO;
        }

        if (dataVO == null || dataVO.isEmpty()) {
            log.info("没有待修约的数据");
            return dataVO;
        }
        Map<String, String> map = new HashMap<>();
        for (PreCountResultParam param : resultParams) {
            map.put(param.getResultIdentity(), param.getResultIdentity() + "-" + param.getFormulaText());
        }

        map.put(SampleTableEnum.FIT_CONCENTRATION.getIdentity(), SampleTableEnum.FIT_CONCENTRATION.getIdentity());
        Map<String, List<RoundingParamVO>> roundingInfo = new HashMap<>();

        for (RoundingVO param : approxParams) {
            String identity = param.getResultIdentity();
            if (map.containsKey(identity)) {
                String key = map.get(identity);
                roundingInfo.put(key, param.getRoundingParams());
            }
        }
        for (RationCountDataInfo count : dataVO) {

            String resultIdentity = count.getIdentity();

            List<RoundingParamVO> list = roundingInfo.get(resultIdentity);
            if (roundingInfo.containsKey(resultIdentity) && list != null) {
                String s = roundData(count.getColValue(), list);
                count.setColValue(s);
            }
        }

        return dataVO;
    }

    @Override
    public String fitDataRounding(String fitConcentration, RationParameter parameter) {

        BigDecimal bd = new BigDecimal(fitConcentration);

        SystemBusinessParamVO vo = businessParamService.queryBusinessParam();
        if (vo.getIsNegativeShow() && bd.signum() < 0) {
            return "未检出";
        }

        List<RoundingVO> approxParams = parameter.getApproxParams();
        if (approxParams == null || approxParams.isEmpty()) {
            log.info("无需修约");
            return fitConcentration;
        }

        if (!StringUtils.hasText(fitConcentration)) {
            log.info("数据为空");
            return fitConcentration;
        }

        List<RoundingParamVO> roundingInfo = new ArrayList<>();

        for (RoundingVO param : approxParams) {
            String identity = param.getResultIdentity();
            if (SampleTableEnum.FIT_CONCENTRATION.getIdentity().equals(identity)) {
                roundingInfo = param.getRoundingParams();
            }
        }

        if (roundingInfo.isEmpty()) {
            log.info("没有设置修约规则，无需修约");
            return fitConcentration;
        }

        return roundData(fitConcentration, roundingInfo);
    }

    private String roundData(String value, List<RoundingParamVO> roundingParams) {
        double middle = Double.parseDouble(value);
        String  ans = value;
        for (RoundingParamVO param : roundingParams) {
            Double lowerLimit = param.getLowerLimit();

            Double upperLimit = param.getUpperLimit();

            // 检查下限
            boolean lowerCheck = true;
            if (lowerLimit != null) {
                if (param.getIsLowerLimitIncluded() != null && param.getIsLowerLimitIncluded()) {
                    // 包含下限
                    lowerCheck = middle >= lowerLimit;
                } else {
                    // 不包含下限
                    lowerCheck = middle > lowerLimit;
                }
            }

            // 检查上限
            boolean upperCheck = true;
            if (upperLimit != null) {
                if (param.getIsUpperLimitIncluded() != null && param.getIsUpperLimitIncluded()) {
                    // 包含上限
                    upperCheck = middle <= upperLimit;
                } else {
                    // 不包含上限
                    upperCheck = middle < upperLimit;
                }
            }

            if (lowerCheck && upperCheck) {
                String roundingType = param.getRoundingType();

                if (roundingType == null) {
                    return value;
                }

                if (roundingType.equals(RoundingTypeEnums.SIGNIFICANT_FIGURES.getCode())) {
                    ans = DataCountUtil.formatToSignificantDigits(param.getDecimalPlaces(), BigDecimal.valueOf(middle));
                } else {
                    ans = DataCountUtil.addZero(DataCountUtil.formatValue(param.getDecimalPlaces(), middle), param.getDecimalPlaces());
                }

                return ans;
            }
        }
        return value;
    }


    @Override
    public R<List<RationCountDataInfo>> formula(List<RationWavelengthDataInfo> vo, TMeasureSample sample, TMeasureTask task, RationParameter parameter, Integer digit) {
        if (!StringUtils.hasText(task.getTableInfo())) {
            //缺少分析参数，无法计算分析
            return R.fail(YamlMessageSource.getMessageStatic("00300003"));
        }
        TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
        if (snapshot.getMapRelation() == null || snapshot.getMapRelation().isEmpty()) {
            //缺少映射关系，无法计算分析
            return R.fail(YamlMessageSource.getMessageStatic("00300004"));
        }
        String jsonString = JSON.toJSONString(sample);
        JSONObject allData = JSON.parseObject(jsonString);
        if (StringUtils.hasText(sample.getNewColData())) {
            JSONObject object = JSONObject.parseObject(sample.getNewColData());
            allData.putAll(object);
        }
        LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureAnalysisData::getTaskId, task.getId())
                .eq(TMeasureAnalysisData::getSampleKey, sample.getId())
                .orderByDesc(TMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(queryWrapper);
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            String analysisParam = one.getAnalysisParam();
            JSONObject object = JSON.parseObject(analysisParam);
            allData.putAll(object);
        }

        for (RationWavelengthDataInfo dataInfo : vo) {
            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                allData.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), dataInfo.getAbsValue()==null? null :dataInfo.getAbsValue());
                allData.put(dataInfo.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), dataInfo.getTPercentValue()==null? null :dataInfo.getTPercentValue());
            }
        }

        StringBuilder values = new StringBuilder();

        StringBuilder identity = new StringBuilder();

        for (InputParam inputParam : parameter.getSampleInputParams()) {
            boolean b = snapshot.getMapRelation().stream().anyMatch(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity()));

            Optional<InputParamMapTableHead> mapTableHead = snapshot.getMapRelation().stream().filter(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity())).findFirst();
            if (!mapTableHead.isPresent()) {
                //"缺少参数映射：" + inputParam.getParamName()
                return R.fail(YamlMessageSource.getMessageStatic("00300005",inputParam.getParamName()));
            }
            identity.append(mapTableHead.get().getParamIdentity()).append(",");
            String s = allData.getString(mapTableHead.get().getHeadIdentity());
            if (!StringUtils.hasText(s)) {
                //缺少数据！
                return R.fail(YamlMessageSource.getMessageStatic("00300006"));
            }
            s = DataCountUtil.formatValueWithBigDecimal(digit, s);
            values.append(s).append(",");
        }
        identity.deleteCharAt(identity.length()-1);
        values.deleteCharAt(values.length()-1);

        List<RationCountDataInfo> countList = new ArrayList<>();
        for (PreCountResultParam param : parameter.getResultParams()) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = restUtil.sum(text, identity.toString(), values.toString());
            RationCountDataInfo countDataInfo = new RationCountDataInfo();
            BeanUtils.copyProperties(param, countDataInfo);
            JSONObject resultJson = JSONObject.parseObject(result);

            if (!resultJson.get("code").equals("0") && !resultJson.get("code").equals(0)) {
                //"公式计算出错！" + resultJson.get("message")
                return R.fail(YamlMessageSource.getMessageStatic("00300007",resultJson.get("message")));
            }
            countDataInfo.setColValue((String) resultJson.get("result"));
            countList.add(countDataInfo);
        }

        return R.ok(countList);
    }

    @Override
    public R<Boolean> changeStandSampleDataDigit(String taskId, Integer digit) {
        /*
        数位数的影响---测量数据不影响，平均值，sd，rsd影响
        abs,sumAbs，曲线拟合，拟合浓度，公式计算都会受影响
        首先，平均值，sd,rsd计算abs
        然后，sumAbs计算
        最后，曲线拟合，
        拟合浓度，公式计算
        标准样品和未知样品，需要一起查询
       不然针对api操作，会出现小数位数不一致的问题
       那重新计算，应该针对单样品？
       光度可以针对任务，定量应该针对样品来操作
         */
        String dealSampleKind = SampleTypeEnum.kind_stand.getIdentity();

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            //没有样品
            return R.fail(YamlMessageSource.getMessageStatic("00303010"));
        }
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        boolean repeat = parameter.getRepeatMode().equals("单池重复");

        if (StringUtils.hasText(parameter.getCoefficientMode()) && parameter.getCoefficientMode().equals(RationCurveParamEnums.direct_coefficient.getIdentity())) {
            FittingReceiveInfo fittingInfo = createFittingInfo(task.getMethodKey());
            task.setFittingParam(JSON.toJSONString(fittingInfo));
            tMeasureTaskService.updateById(task);
            return R.ok();
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        boolean empty = false;
        boolean lastIsEmpty = false;
        Double minuend = 0.0;
        //存放空白样数据（计算后的值）
        List<RationSaveCountDataInfo> emptyDataList = new ArrayList<>();
        //存放空白样计算出的Abs的值
        List<RationCountDataInfo> emptyList = new ArrayList<>();

        //参与曲线拟合的数据
        List<RationCountDataInfo> fitCurveData = new ArrayList<>();
        //参与曲线拟合的参数
        List<String> fitCurveParams = new ArrayList<>();

        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSpecimenKey().equals(dealSampleKind)
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())
                );

        for (TaskSpecimen specimen : taskSpecimen) {

            if (specimen.getDeleteType().equals(deleteType)) {
                log.info("删除的样品");
                continue;
            }
            if (specimen.getIsTested().equals(noTested)) {
                log.info("没有测过的样品，跳过");
                continue;
            }

            if (!specimen.getSampleKind().equals(dealSampleKind)) {
                log.info("不是需要处理的样品，跳过");
                continue;
            }

            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureData::getSerialNumber)
                    .last("limit 1");
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (data == null) {
                log.info("没有测量数据");
                continue;
            }
            if (!StringUtils.hasText(data.getOriginalCalculationData())) {
                log.info("没有测量数据");
                continue;
            }
            List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
            if (list == null || list.isEmpty()) {
                log.info("没有测量数据");
                continue;
            }
            RationCountDataInfo ansData;
            RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
            dealData.setSampleId(specimen.getSpecimenKey());
            dealData.setTaskId(taskId);
            dealData.setMeasureDataId(data.getId());

            if (repeat) {
                List<RationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                R<RationCountDataInfo> absValue = countAbsValue(wavelengthDataInfos, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("计算abs失败");
                    RationCountDataInfo value = getAbsValue(taskId, sample.getId(), data.getId(), empty);
                    if (value == null) {
                        //数据处理出错且没有找到旧数据，处理失败
                        return R.fail(YamlMessageSource.getMessageStatic("00303011"));
                    }
                    fitCurveData.add(value);
                    fitCurveParams.add(sample.getNormalConcentration());

                    continue;
                }
                ansData = absValue.getData();

            } else {

                R<RationCountDataInfo> absValue = countAbsValue(list, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("重新计算abs失败");
                    RationCountDataInfo value = getAbsValue(taskId, sample.getId(), data.getId(), empty);
                    if (value == null) {
                        //数据处理出错且没有找到旧数据，处理失败
                        return R.fail(YamlMessageSource.getMessageStatic("00303011"));
                    }
                    fitCurveData.add(value);
                    fitCurveParams.add(sample.getNormalConcentration());

                    continue;
                }
                ansData = absValue.getData();
            }

            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                empty = true;
                RationCountDataInfo yAbs = new RationCountDataInfo();
                yAbs.setHeadName(SampleTableEnum.SUM_Abs.getName());
                yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                yAbs.setColValue(DataCountUtil.addZero(0.0, digit));

                List<RationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (RationCountDataInfo dataInfo : emptyList) {
                        sub += Double.parseDouble(dataInfo.getColValue());
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;

                    yAbs.setColValue(String.valueOf(v));
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (RationSaveCountDataInfo emptyData : emptyDataList) {
                        List<RationCountDataInfo> emList = emptyData.getCountData();
                        List<RationCountDataInfo> newList = new ArrayList<>();

                        for (RationCountDataInfo dataInfo : emList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newList.add(dataInfo);
                                double v1 = Double.parseDouble(dataInfo.getColValue()) - minuend;

                                RationCountDataInfo sumAbs = new RationCountDataInfo();
                                BeanUtils.copyProperties(yAbs, sumAbs);

                                sumAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit,v1), digit));
                                newList.add(sumAbs);
                            }
                        }
                        emptyData.setCountData(newList);
                    }
                } else {
                    emptyList.clear();
                    emptyDataList.clear();
                    emptyList.add(ansData);
                    minuend = Double.parseDouble(ansData.getColValue());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    currentList.add(yAbs);
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                }
                saveEmptySampleData(emptyDataList);
                lastIsEmpty = true;

            } else {
                List<RationCountDataInfo> currentList = new ArrayList<>();

                //测量过的数据，非空白样，添加标准浓度为了拟合曲线
                //TODO 未来需要添加质量和浓度的区分
                fitCurveParams.add(sample.getNormalConcentration());

                if (empty) {
                    RationCountDataInfo sumAbsData = new RationCountDataInfo();
                    sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                    sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    sumAbsData.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    currentList.add(sumAbsData);
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    fitCurveData.add(sumAbsData);
                } else {
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    fitCurveData.add(ansData);
                }
                saveAnalyzeData(dealData);
                lastIsEmpty = false;
            }
            specimen.setIsTested(tested);
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));

        //拟合曲线
        R<FittingReceiveInfo> infoR = fittingCurve(fitCurveData, empty, task.getMethodKey(), fitCurveParams);
        if (infoR.getSuccess()) {
            log.info("拟合曲线成功，修改任务中的拟合参数");

            String jsonString = JSON.toJSONString(infoR.getData());
            task.setFittingParam(jsonString);
        }
        tMeasureTaskService.updateById(task);
        return R.ok();
    }

    @Override
    public R<Boolean> changeUnknowSampleDigit(String taskId, Integer digit) {
        String dealSampleKind = SampleTypeEnum.kind_unknown.getIdentity();
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("没有样品");
            //没有样品
            return R.fail(YamlMessageSource.getMessageStatic("00303010"));
        }
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        boolean repeat = "单池重复".equals(parameter.getRepeatMode());

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        boolean empty = false;
        boolean lastIsEmpty = false;
        Double minuend = 0.0;
        //存放空白样数据（计算后的值）
        List<RationSaveCountDataInfo> emptyDataList = new ArrayList<>();
        //存放空白样计算出的Abs的值
        List<RationCountDataInfo> emptyList = new ArrayList<>();

        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSpecimenKey().equals(dealSampleKind)
                                && specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())&&
                                specimen.getDeleteType() != null &&
                                specimen.getDeleteType().equals(createType)
                );

        for (TaskSpecimen specimen : taskSpecimen) {

            if (specimen.getDeleteType().equals(deleteType)) {
                log.info("删除的样品");
                continue;
            }
            if (specimen.getIsTested().equals(noTested)) {
                log.info("没有测过的样品，跳过");
                continue;
            }

            if (!specimen.getSampleKind().equals(dealSampleKind)) {
                log.info("不是需要处理的样品，跳过");
                continue;
            }

            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureData::getSerialNumber)
                    .last("limit 1");
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (data == null) {
                log.info("没有测量数据");
                continue;
            }
            if (!StringUtils.hasText(data.getOriginalCalculationData())) {
                log.info("没有测量数据");
                continue;
            }
            List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
            if (list == null || list.isEmpty()) {
                log.info("没有测量数据");
                continue;
            }
            RationCountDataInfo ansData;
            RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
            dealData.setSampleId(specimen.getSpecimenKey());
            dealData.setTaskId(taskId);
            dealData.setMeasureDataId(data.getId());

            if (repeat) {
                List<RationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                R<RationCountDataInfo> absValue = countAbsValue(wavelengthDataInfos, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("当前样品处理出错，跳过");
                    continue;
                }
                ansData = absValue.getData();

            } else {
                R<RationCountDataInfo> absValue = countAbsValue(list, parameter, digit);
                if (!absValue.getSuccess()) {
                    log.info("计算abs出错，跳过");
                    continue;
                }
                ansData = absValue.getData();
            }

            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                empty = true;
                RationCountDataInfo yAbs = new RationCountDataInfo();
                yAbs.setHeadName(SampleTableEnum.SUM_Abs.getName());
                yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                yAbs.setColValue(DataCountUtil.addZero(0.0, digit));

                List<RationCountDataInfo> currentList = new ArrayList<>();

                if (lastIsEmpty) {
                    emptyList.add(ansData);

                    double sub = 0.0;
                    //计算平均值
                    for (RationCountDataInfo dataInfo : emptyList) {
                        sub += Double.parseDouble(dataInfo.getColValue());
                    }
                    minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
                    //添加到空白列表
                    currentList.add(ansData);
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit,v),digit));
                    currentList.add(yAbs);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                    for (RationSaveCountDataInfo emptyData : emptyDataList) {
                        List<RationCountDataInfo> emList = emptyData.getCountData();
                        List<RationCountDataInfo> newList = new ArrayList<>();

                        for (RationCountDataInfo dataInfo : emList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newList.add(dataInfo);
                                double v1 = Double.parseDouble(dataInfo.getColValue()) - minuend;
                                RationCountDataInfo sumAbs = new RationCountDataInfo();
                                BeanUtils.copyProperties(yAbs, sumAbs);

                                sumAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit,v1), digit));
                                newList.add(sumAbs);
                            }
                        }
                        emptyData.setCountData(newList);
                    }
                } else {
                    emptyList.clear();
                    emptyDataList.clear();
                    emptyList.add(ansData);
                    minuend = Double.parseDouble(ansData.getColValue());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    currentList.add(yAbs);
                    currentList.add(ansData);
                    dealData.setCountData(currentList);
                    emptyDataList.add(dealData);

                }
                saveEmptySampleData(emptyDataList);
                lastIsEmpty = true;
            } else {
                List<RationCountDataInfo> currentList = new ArrayList<>();
                /*
                未知样品
                计算对象：countData
                首先，有空白，就计算sumAbs值，并且赋值到countData中
                然后，根据曲线计算，公式计算
                最后保存，计算完，abs值就保存一次，但是要注意替换。
                 */
                double toCountData = 0.0;

                if (empty) {
                    RationCountDataInfo sumAbsData = new RationCountDataInfo();
                    sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                    sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                    sumAbsData.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                    toCountData = Double.parseDouble(sumAbsData.getColValue());
                    currentList.add(sumAbsData);
                } else {
                    toCountData = Double.parseDouble(ansData.getColValue());
                }
                currentList.add(ansData);
                /*
                获取拟合的值
                 */
                FittingReceiveInfo fitInfo = JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);

                if (fitInfo != null && fitInfo.getRoundCoefficient()!= null) {
                    String ans = valueCalculate(fitInfo, toCountData, parameter.getEquationType(), digit);

                    if (ans != null) {
                        RationCountDataInfo valueData = new RationCountDataInfo();
                        valueData.setOriginalValue(ans);
                        String s = fitDataRounding(ans, parameter);

                        valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                        valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                        valueData.setColValue(s);
                        currentList.add(valueData);
                    }
                }

                //TODO 公式计算，待做
                dealData.setCountData(currentList);
                saveAnalyzeData(dealData);
                lastIsEmpty = false;
            }
        }
        return R.ok();
    }


    public RationCountDataInfo getAbsValue(String taskId, String sampleId, String measureDataId, Boolean empty) {
        LambdaQueryWrapper<TMeasureAnalysisData> query = new LambdaQueryWrapper<>();
        query.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId)
                .eq(TMeasureAnalysisData::getMeasureDataId, measureDataId).orderByDesc(TMeasureAnalysisData::getDataVersion);
        List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(query);
        if (list != null && !list.isEmpty()) {
            if (empty) {
                for (TMeasureAnalysisData analysisData : list) {
                    if (analysisData.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                return dataInfo;
                            }
                        }
                    }
                }
            } else {
                for (TMeasureAnalysisData analysisData : list) {
                    if (analysisData.getAnalysisResult() != null) {
                        List<RationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), RationCountDataInfo.class);
                        for (RationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                return dataInfo;
                            }
                        }
                    }
                }

            }

        }
        return null;

    }
    public List<RationWavelengthDataInfo> countRepeatData(List<RationWavelengthDataInfo> originData, Integer digit) {

        List<RationWavelengthDataInfo> ans = new ArrayList<>();

        Map<String, List<Double>> absValue = new HashMap<>();
        Map<String, List<Double>> tPercentValue = new HashMap<>();
        Map<String, RationWavelengthDataInfo> dataMap = new HashMap<>();
        Set<String> wavelengthSet = new HashSet<>();

        for (RationWavelengthDataInfo data : originData) {
            if (data.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                dataMap.putIfAbsent(data.getWavelength(), data);
                continue;
            }
            ans.add(data);
            double abs = Double.parseDouble(data.getAbsValue());
            double tPercent = Double.parseDouble(data.getTPercentValue());

            absValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(abs);
            tPercentValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(tPercent);
            wavelengthSet.add(data.getWavelength());
        }

        for (String wavelength : wavelengthSet) {
            RationWavelengthDataInfo dataInfo = dataMap.get(wavelength);
            RepeatMeasureData repeatData;
            Map<String, RepeatMeasureData> otherData = new HashMap<>();

            List<Double> allAbs = absValue.get(wavelength);
            double mean = DataCountUtil.calculateMean(allAbs, digit);
            Double absSD = DataCountUtil.calculateSD(allAbs, digit);
            Double absRsd = DataCountUtil.calculateRSD(allAbs, digit);
            repeatData = new RepeatMeasureData(absSD == null ? "未检出" :String.valueOf(absSD), absRsd == null ? "未检出" :String.valueOf(absRsd));
            otherData.put(wavelength + "-" + WaveLengthEnum.abs.getCode(), repeatData);

            List<Double> allPercent = tPercentValue.get(wavelength);
            double meanT = DataCountUtil.calculateMean(allPercent, digit);
            Double percentSD = DataCountUtil.calculateSD(allPercent, digit);
            Double percentRsd = DataCountUtil.calculateRSD(allPercent, digit);
            repeatData = new RepeatMeasureData(percentSD == null ? "未检出" :String.valueOf(percentSD), percentRsd == null ? "未检出" :String.valueOf(percentRsd));
            otherData.put(wavelength + "-" + WaveLengthEnum.abs.getCode(), repeatData);

            dataInfo.setAbsValue(String.valueOf(mean));
            dataInfo.setTPercentValue(String.valueOf(meanT));
            dataInfo.setOtherData(otherData);

            ans.add(dataInfo);
        }
        return ans;
    }

    @Override
    public Result zero(String deviceSerialNumber, String methodId, String taskId, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            //未连接仪器
            return Result.fail(YamlMessageSource.getMessageStatic("0000001D"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
            newTask.setUserId(po.getId());
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
            taskId = newTask.getId();
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            //缺少波长
            return Result.fail(YamlMessageSource.getMessageStatic("0000001E"));
        }
        List<String> waveLength = parameter.getWaveLength();

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();

        param.setPhotoMode(Integer.valueOf(WaveLengthEnum.abs.getCode()));
        param.setResponseTime(Double.valueOf(parameter.getResponseTime()));

        if (parameter.getGain() != null) {
            param.setGain(parameter.getGain());
        }
        if (StringUtils.hasText(parameter.getEnergyLamp())) {
            param.setLampPos(Integer.valueOf(parameter.getEnergyLamp()));
        }
        Boolean needSetSit = deviceInfoService.isNeedSetSit(deviceSerialNumber);
        if (needSetSit) {
            param.setSlit1(Double.valueOf(parameter.getSpectralBandwidth()));
        }

        inputParam.setParam(param);

        R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return Result.fail(operation.getMessage());
        }

        inputParam.setOperationType(OperationTypeEnums.Zero.getType());
        inputParam.setWavelength(waveLength);
        R<OperationResultEntity> zero = devOperationUtilService.operation(inputParam);
        if (!zero.getSuccess()) {
            return Result.fail(zero.getMessage());
        }
        OperationResultEntity data = zero.getData();
        List<ChangeParam> zeroResult = data.getZeroResult();

        task.setZeroParam(JSON.toJSONString(zeroResult));

        boolean update = tMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
    }

    @Override
    public Result zero2(String deviceSerialNumber, String methodId, String taskId, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            //未连接仪器
            return Result.fail(YamlMessageSource.getMessageStatic("0000001D"));
        }

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
            newTask.setUserId(po.getId());
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
            taskId = newTask.getId();
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            //缺少波长
            return Result.fail(YamlMessageSource.getMessageStatic("0000001E"));
        }
        List<String> waveLength = parameter.getWaveLength();
        // 通过序列号获取ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        if (RationCurveParamEnums.no_differential.equals(parameter.getDifferentialModeName())){
            List<ChangeParam> list = new ArrayList<>();
            for (String s : waveLength) {

                double v = Double.parseDouble(s);
                Command command = new Command(SetParamEnum.SetWl.getComId(), new Object[]{v});
                devMessageUtil.wavelengthLocate(command, ip);

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
                ChangeParam changeParam = zero.getData();
                changeParam.setWl(s);
                list.add(changeParam);
            }
            task.setZeroParam(JSON.toJSONString(list));
        }else{
            Map<String, List<ChangeParam>> map = new HashMap<>();
            for (String s : waveLength) {
                double startWaveLength = Double.parseDouble(s)+5;
                double endWaveLength = Double.parseDouble(s)-5;
                List<Command> commands = new ArrayList<>();
                Command com1 = new Command(SetParamEnum.SetStartWl.getComId(), new Object[]{startWaveLength});
                Command com2 = new Command(SetParamEnum.SetEndWl.getComId(), new Object[]{endWaveLength});
                commands.add(com1);
                commands.add(com2);
                R<String> stringR = null;
                try {
                    stringR = devMessageUtil.spectrumScanSet(commands,ip);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!stringR.getSuccess()) {
                    return Result.fail(stringR.getMessage());
                }
                Command command = new Command(SetParamEnum.BaseLine.getComId(), new Object[]{});
                R<List<ChangeParam>> listR = devMessageUtil.spectrumScanGetBaseline(command, ip);
                if (!listR.getSuccess()) {
                    return Result.fail(listR.getMessage());
                }
                List<ChangeParam> data = listR.getData();
                map.put(s, data);
            }
            task.setZeroParam(JSON.toJSONString(map));
        }
        boolean update = tMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
    }

    @Override
    public List<QualityCheckResultVO> qualityControl(String taskId, String methodId) {
        List<QualityCheckResultVO> result = new ArrayList<>();

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            log.info("缺少参数，无法进行质控管理");
            return null;
        }
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (method == null || task == null) {
            log.info("没有找到该任务或方法");
            return null;
        }

        String paramInfo = method.getMeasurementParamInfo();
        RationParameter parameter = JSON.parseObject(paramInfo, RationParameter.class);
        if (parameter == null || parameter.getQcParams() == null || parameter.getQcParams().isEmpty()) {
            log.info("没有质控参数");
            return null;
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("任务没有样品");
            return null;
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        if (taskSpecimen == null || taskSpecimen.isEmpty()) {
            log.info("样品转换失败");
            return null;
        }
        List<QualityConfVO> qcParams = parameter.getQcParams();

        for (TaskSpecimen specimen : taskSpecimen) {
            QualityCheckResultVO vo = new QualityCheckResultVO();
            if (specimen.getDeleteType().equals(deleteType) || specimen.getIsTested().equals(noTested)) {
                log.info("该样品已删除或未测试");
                continue;
            }


            QualityConfEnums.slope_qc.getCode();





        }

        return result;

    }

    @Override
    public R<List<InputParam>> queryInputParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            //未选择方法
            return R.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        RationParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), RationParameter.class);
        List<InputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<InputParamMapTableHead>> queryInputParamMapTableHead(String taskId, String sampleKind) {

        if (!StringUtils.hasText(taskId)) {
            //未选择任务
            return R.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        if (!StringUtils.hasText(sampleKind)) {
            //未选择样品种类
            return R.fail(YamlMessageSource.getMessageStatic("00303012"));
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<InputParamMapTableHead> ans = new ArrayList<>();
        if (!StringUtils.hasText(task.getTableInfo())) {
            return R.ok(ans);
        }
        RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);

        if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
            TaskHeadSnapshot standHead = shot.getStandHead();
            if (standHead != null && standHead.getMapRelation() != null) {
                ans = standHead.getMapRelation();
            }
        } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            TaskHeadSnapshot unknownHead = shot.getUnknownHead();
            if (unknownHead != null && unknownHead.getMapRelation() != null) {
                ans = unknownHead.getMapRelation();
            }
        }
        return R.ok(ans);

    }

    @Override
    public R<Boolean> saveInputParamMapTableHead(TaskHeadSnapshotDTO snapshotDTO) {

        if (!StringUtils.hasText(snapshotDTO.getTaskId())) {
            //未选择任务
            return R.fail(YamlMessageSource.getMessageStatic("0000001B"));
        }
        if (!StringUtils.hasText(snapshotDTO.getSampleKind())) {
            //未选择样品种类
            return R.fail(YamlMessageSource.getMessageStatic("00303012"));
        }

        TMeasureTask task = tMeasureTaskService.getById(snapshotDTO.getTaskId());
        if (task == null) {
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();
        List<HeadVO> taskHeads = new ArrayList<>();
        List<HeadVO> sampleHeads = new ArrayList<>();
        for (HeadVO head : snapshotDTO.getTaskHeads()) {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity()) || head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                sampleHeads.add(head);
            } else {
                taskHeads.add(head);
            }
        }
        BeanUtils.copyProperties(snapshotDTO, taskHeadSnapshot);
        taskHeadSnapshot.setTaskHeads(taskHeads);
        taskHeadSnapshot.setSampleHeads(sampleHeads);

        RationTaskHeadSnapShot shot = JSON.parseObject(task.getTableInfo(), RationTaskHeadSnapShot.class);
        if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_stand.getIdentity())) {
            shot.setStandHead(taskHeadSnapshot);
        } else if (snapshotDTO.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            shot.setUnknownHead(taskHeadSnapshot);
        } else {
            log.info("样品种类有误");
            //样品种类有误
            return R.fail(YamlMessageSource.getMessageStatic("00303013"));
        }

        task.setTableInfo(JSON.toJSONString(shot));

        return tMeasureTaskService.updateById(task) ? R.ok(true) : R.fail(YamlMessageSource.getMessageStatic("00300008"));//保存失败
    }

    /**
     * 多格式文件导出（支持word/txt/xlsx）
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType) {
        // log.info("开始导出11111111，taskId: {}, fileType: {}", taskId, fileType);
        // 1. 基础参数校验（复用原逻辑）
        if (!StringUtils.hasText(taskId)) {
            //缺少任务id
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            // log.error("导出失败：任务不存在，taskId: {}", taskId);
            //没有找到该任务
            return R.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // 处理fileType默认值和合法性
        if (!StringUtils.hasText(fileType)) {
            fileType = "word"; // 默认导出word
        } else {
            fileType = fileType.toLowerCase();
        }
        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            //不支持的导出格式，仅支持word/txt/xlsx
            return R.fail(YamlMessageSource.getMessageStatic("00300001"));
        }
        // 处理allDisplay默认值
        if (allDisplay == null) {
            allDisplay = true;
        }

        // 2. 准备表头和数据（完全复用原downloadWord逻辑）
        String methodId = task.getMethodKey();
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //测量方法不存在
            return R.fail(YamlMessageSource.getMessageStatic("00300009"));
        }

        // 构建表头（与原逻辑一致）
        List<HeadVO> heads = new ArrayList<>();
        // 添加动态表头
        List<HeadVO> vos = getStandSampleTableHead(taskId, methodId, allDisplay,task.getDeviceSerialNumber()).getData();
        heads.addAll(vos);
        // // 最前面加序号列（所有格式统一需要）
        heads.add(0, new HeadVO("序号", "serialNumber"));

        // 查询数据（复用原逻辑）
        QueryDataDTO queryDataDTO = new QueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        Result<RationSampleDataVO> sampleDataResult = querySampleData(queryDataDTO);
        if (!sampleDataResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) { // 校验查询是否成功
            //"查询样品数据失败：" + sampleDataResult.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00303014",sampleDataResult.getMessage()));
        }
        RationSampleDataVO dataVO = sampleDataResult.getData();
        if (dataVO == null) { // 校验VO对象是否为空
            //样品数据为空
            return R.fail(YamlMessageSource.getMessageStatic("00303015"));
        }
        // 2. 仅提取标准样品数据（standSampleData）
        List<Map<String, Object>> data = dataVO.getStandSampleData();
        // 3. 校验标准样品数据是否为空
        if (data == null || data.isEmpty()) {
            //没有可导出的标准样品数据
            return R.fail(YamlMessageSource.getMessageStatic("00303016"));
        }
        // 给数据加序号（与序号列对应）
        for (int i = 0; i < data.size(); i++) {
            data.get(i).put("serialNumber", i + 1);
        }

        // 3. 按格式生成文件（核心分支）
        String fileName = task.getTaskName() + "." + getFileSuffix(fileType);
        File file = new File(fileName);
        try {
            switch (fileType) {
                case "word":
                    generateWordFile(file, task, method, heads, data, vos.size());
                    break;
                case "txt":
                    generateTxtFile(file, heads, data);
                    break;
                case "xlsx":
                    generateExcelFile(file, heads, data);
                    break;
            }
            return R.ok(file);
        } catch (Exception e) {
            log.error("文件生成失败", e);
            // 异常时删除无效文件
            if (file.exists()) {
                file.delete();
            }
            //"文件生成失败：" + e.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00302006",e.getMessage()));
        }
    }

    @Override
    public void test(String a, String b) {

        List<Double> list = new ArrayList<>();
        list.add(1.0);
        list.add(0.0);
        list.add(2.0);

        String s = rebuildFormula(list, 2,2, "Abs", "C");
        System.out.println(s);
    }

    // ---------------------- 原有downloadWord方法（可保留或删除，建议用上面的downloadFile替代） ----------------------
    public R<File> downloadWord(String taskId, Boolean allDisplay) {
        // 直接调用多格式方法，指定格式为word
        return downloadFile(taskId, allDisplay, "word");
    }

    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, TMeasureTask task, TMeasureMethod method,
                                 List<HeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        CTDocument1 document = doc.getDocument();
        CTBody body = document.getBody();

        // 页面设置（复用原逻辑）
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

        // 添加标题（复用原逻辑）
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("任务：" + task.getTaskName() + "\r");
        run.setText("方法：" + method.getMethodName() + "\r");
        run.setText("测试数据：");

        // 修复表格创建逻辑：直接创建1行N列（N=表头数量），避免列错位
        int columnCount = heads.size();
        XWPFTable table = doc.createTable(1, columnCount);
        table.setWidth("100%");

        // 填充表头（修复原逻辑的表头赋值问题 + 增加日志排查）
        XWPFTableRow headerRow = table.getRow(0);
        // 先打印headerRow是否为空，排除行对象本身的问题
        if (headerRow == null) {
            log.error("表头行对象headerRow为null！无法填充表头");
            throw new RuntimeException("表头行对象为空，无法继续生成Word文件");
        }
        // log.info("表头行headerRow创建成功，开始填充表头（总列数：{}）", columnCount);

        for (int i = 0; i < columnCount; i++) {
            // 1. 打印当前循环索引，方便定位具体哪一列出问题
            // log.info("正在处理表头第 {} 列（从0开始计数）", i);
            
            // 2. 验证单元格cell是否存在
            XWPFTableCell cell = headerRow.getCell(i);
            if (cell == null) {
                // log.error("表头第 {} 列的单元格cell为null！", i);
                // 可选：若单元格为null，主动创建一个（避免后续空指针）
                cell = headerRow.createCell();
                // log.warn("已为表头第 {} 列主动创建空单元格", i);
            }
            
            // 3. 验证heads列表在当前索引是否有值
            if (i >= heads.size()) {
                // log.error("表头索引 {} 超出heads列表长度！heads列表实际长度：{}", i, heads.size());
                cell.setText("未定义列"); // 给默认值，避免空指针
                continue;
            }
            HeadVO head = heads.get(i);
            if (head == null) {
                // log.error("表头第 {} 列的HeadVO对象为null！", i);
                cell.setText("空表头"); // 给默认值
                continue;
            }
            
            // 4. 验证表头名称name是否存在（字段缺失检查）
            String headName = head.getName();
            if (headName == null || headName.trim().isEmpty()) {
                // log.warn("表头第 {} 列的名称name为空（HeadVO.getIdentity()：{}）", i, head.getIdentity());
                headName = "未命名列（" + head.getIdentity() + "）"; // 用identity补全，方便排查
            }
            
            // 最终填充单元格
            cell.setText(headName);
            // log.info("表头第 {} 列填充完成：cell内容 = {}", i, headName);
        }

        // 填充数据行（修复原逻辑的索引越界问题）
        int rowIndex = 0;
        for (Map<String, Object> map : data) {
            XWPFTableRow row = table.createRow();
            rowIndex++;
            // 第0列：序号
            XWPFTableCell indexCell = row.getCell(0);
            if (indexCell != null) {
                indexCell.setText(String.valueOf(rowIndex));
            }

            // 填充其他列（从1开始，与表头对应）
            for (int i = 1; i < columnCount; i++) {
                // 1. 获取当前列对应的表头信息
                HeadVO head = i < heads.size() ? heads.get(i) : null;
                if (head == null) {
                    continue; // 表头不存在则跳过
                }
                
                // 2. 获取原始数据（不做默认值处理，保留null）
                String identity = head.getIdentity();
                Object originalValue = map != null ? map.get(identity) : null;
                
                // 3. 原始值直接转换为字符串（不做任何额外处理）
                String cellData;
                if (originalValue == null) {
                    cellData = ""; // null值保留空字符串
                } else {
                    cellData = originalValue.toString(); // 原值直接转换
                }

                // 4. 仅处理单元格为空的情况（避免空指针）
                XWPFTableCell cell = row.getCell(i);
                if (cell != null) {
                    cell.setText(cellData);
                }
            }
        }

        // 表格样式（复用原逻辑）
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                XWPFParagraph paragraph1 = cell.getParagraphArray(0);
                paragraph1.setAlignment(ParagraphAlignment.CENTER);
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        // 写入文件（修复原逻辑的流关闭问题）
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.write(out);
        } finally {
            doc.close(); // 关闭文档对象，释放资源
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
                            // log.warn("表头列表中存在null元素，使用默认表头名称");
                            return "未命名列";
                        }
                        // 处理表头名称为null/空的情况
                        String headName = head.getName();
                        return (headName == null || headName.trim().isEmpty()) ? "未命名列" : headName;
                    })
                    .collect(Collectors.joining("\t"));
            writer.write(headerLine);
            writer.newLine(); // 换行

            // 2. 写入数据（与表头顺序一致）—— 防护null的Map、null的字段值、枚举处理null
            for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
                Map<String, Object> map = data.get(rowIdx);
                // 防护：当前数据行的Map为null
                if (map == null) {
                    // log.warn("数据列表第{}行的Map为null，跳过该行数据", rowIdx + 1);
                    continue;
                }

                List<String> rowData = new ArrayList<>();
                for (HeadVO head : heads) {
                    // 防护：表头为null（避免后续head.getIdentity()报错）
                    String identity = head != null ? head.getIdentity() : "unknown";
                    // 从Map取值：用getOrDefault避免key不存在，同时防护value为null
                    Object value = map.getOrDefault(identity, "");
                    String cellData = value == null ? "" : String.valueOf(value);

                    // 特殊字段格式化（与Word/Excel保持一致，增加null防护）
                    if (head != null && "sampleType".equals(identity)) {
                        // 防护：cellData为null/空/字符串"null"
                        if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        } else {
                            // 防护：枚举方法返回null时用"-"替代
                            String typeName = SampleTypeEnum.getName(cellData);
                            cellData = typeName == null ? "-" : typeName;
                        }
                    }
                    if (head != null && "sampleLocation".equals(identity)) {
                        // 防护：避免拼接"样品null"或"样品"（空值）
                        if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                            cellData = "-";
                        } else {
                            cellData = "样品" + cellData;
                        }
                    }

                    // 最终统一空值处理：将空字符串/"null"转为"-"
                    if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                        cellData = "-";
                    }

                    rowData.add(cellData);
                }

                // 写入当前行数据（制表符分隔）
                writer.write(String.join("\t", rowData));
                writer.newLine();
                // log.debug("TXT文件第{}行数据写入完成", rowIdx + 1);
            }

            // log.info("TXT文件生成成功，路径：{}", file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            // log.error("TXT文件路径不存在或无法写入：{}", file.getAbsolutePath(), e);
            throw e;
        } catch (IOException e) {
            // log.error("TXT文件写入IO异常", e);
            throw e;
        } catch (Exception e) {
            // log.error("TXT文件生成未知异常", e);
            throw e;
        }
    }

    /**
     * 生成 XLSX 文件（修复空指针异常）
     */
    private void generateExcelFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 1. 校验核心参数是否为null（避免后续调用方法时报错）
        if (heads == null) {
            throw new IllegalArgumentException("表头列表heads不能为null");
        }
        if (data == null) {
            throw new IllegalArgumentException("数据列表data不能为null");
        }

        // 2. 转换表头（处理空表头和null元素）
        List<List<String>> excelHeads = new ArrayList<>();
        for (int i = 0; i < heads.size(); i++) {
            HeadVO head = heads.get(i);
            // 防护：当前表头为null
            if (head == null) {
                // log.warn("表头列表第{}个元素为null，使用默认表头", i);
                excelHeads.add(Collections.singletonList("未命名列_" + i));
            continue;
        }
        // 防护：表头名称为null/空
        String headName = head.getName();
        if (headName == null || headName.trim().isEmpty()) {
            headName = "未命名列（" + head.getIdentity() + "）";
            // log.warn("表头第{}列名称为空，使用默认名称：{}", i, headName);
        }
        excelHeads.add(Collections.singletonList(headName));
    }

    // 3. 转换数据（处理空Map和null值）
    List<List<Object>> excelData = new ArrayList<>();
    for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
        Map<String, Object> map = data.get(rowIdx);
        // 防护：当前数据行为null
        if (map == null) {
            // log.warn("数据列表第{}行的Map为null，跳过该行", rowIdx);
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

            // 提取字段值，防护null
            Object value = map.get(head.getIdentity());
            String cellData = "-"; // 默认空值

            if (value != null) {
                cellData = String.valueOf(value);
                // 特殊字段格式化（增加null防护）
                if ("sampleType".equals(head.getIdentity())) {
                    // 防护：cellData为null/空
                    if (cellData == null || cellData.trim().isEmpty() || "null".equalsIgnoreCase(cellData)) {
                        cellData = "-";
                    } else {
                        // 防护：枚举方法返回null
                        String typeName = SampleTypeEnum.getName(cellData);
                        cellData = typeName == null ? "-" : typeName;
                    }
                }
                if ("sampleLocation".equals(head.getIdentity())) {
                    // 防护：避免拼接"样品null"
                    cellData = (cellData == null || "null".equalsIgnoreCase(cellData)) ? "-" : ("样品" + cellData);
                }
                // 清除特殊字符，防护空字符串
                cellData = cellData.replace("\r", "").replace("\n", "").replace("\t", " ");
                if (cellData.isEmpty() || "null".equalsIgnoreCase(cellData)) {
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

}
