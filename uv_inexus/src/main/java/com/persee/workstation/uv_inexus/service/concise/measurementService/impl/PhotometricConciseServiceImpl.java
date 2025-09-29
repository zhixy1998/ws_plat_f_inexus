package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FAddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SaveDataDto;
import com.persee.workstation.uv_inexus.dto.concise.photometricDto.NewPhotometricMethodParamCheckDTO;
import com.persee.workstation.uv_inexus.dto.concise.rationDto.FTaskHeadSnapshotDTO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.*;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.RestUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricReportDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.*;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRepeatMeasureData;
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

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("fPhotometricService")
public class PhotometricConciseServiceImpl implements PhotometricConciseService {
    @Autowired
    FMeasureProjectService fMeasureProjectService;
    @Autowired
    FMeasureTaskService fMeasureTaskService;
    @Autowired
    FMeasureSampleService fMeasureSampleService;
    @Autowired
    SysUserMapper userManageDAO;
    @Autowired
    FMeasureTaskMapper fMeasureTaskMapper;
    @Autowired
    FMeasureMethodService fMeasureMethodService;
    @Autowired
    FMeasureDataService fMeasureDataService;

    @Autowired
    FMeasureAnalysisDataService fMeasureAnalysisDataService;

    @Autowired
    FMeasureTaskAttachmentService fMeasureTaskAttachmentService;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    RestUtil restUtil;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noSave;

    @Value("1")
    String saved;

    @Value("0")
    String noTested;

    @Value("1")
    String tested;

    @Value("11")
    String defaultProjectId;

    @Value("文件数据")
    String newTaskName;

    private R<Boolean> checkParam(FPhotometricParameter parameter) {
        // 1. 校验光度模式
        if (parameter.getLuminosity() == null || parameter.getLuminosity().trim().isEmpty()) {
            return R.fail("光度模式不能为空");
        }

        if (WaveLengthEnum.getName(parameter.getLuminosity()) == null) {
            return R.fail("光度模式选择出错");
        }
        // 2. 校验波长值
        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            return R.fail("波长值不能为空");
        }
        if (parameter.getWaveLength().size() > 26) {
            return R.fail("波长值数量必须在1-26个范围内");
        }
        for (String waveStr : parameter.getWaveLength()) {
            if (waveStr == null || waveStr.trim().isEmpty()) {
                return R.fail("波长值不能包含空值");
            }
            try {
                double waveLength = Double.parseDouble(waveStr);
                if (waveLength <= 190 || waveLength >= 900) {
                    return R.fail("波长值" + waveStr + "必须在190-900范围内");
                }
                // 校验小数点后最多1位
                String[] parts = waveStr.split("\\.");
                if (parts.length > 1 && parts[1].length() > 1) {
                    return R.fail("波长值" + waveStr + "小数点后最多保留1位");
                }
            } catch (NumberFormatException e) {
                return R.fail("波长值" + waveStr + "必须为有效的数字");
            }
        }

        // 3. 校验重复模式
        if (parameter.getRepetitionPattern() == null || parameter.getRepetitionPattern().trim().isEmpty()) {
            return R.fail("重复模式不能为空");
        }


        if (!Arrays.asList("单池重复", "不重复").contains(parameter.getRepetitionPattern())) {
            return R.fail("重复模式只能是rmNone(不重复)、rmCR(单池重复)");
        }

        // 4. 校验重复次数（单池重复模式下必校验）
        if ("单池重复".equals(parameter.getRepetitionPattern())) {
            if (parameter.getRepetitionTime() == null || parameter.getRepetitionTime().trim().isEmpty()) {
                return R.fail("单池重复模式下重复次数不能为空");
            }
            try {
                int repetitionTime = Integer.parseInt(parameter.getRepetitionTime());
                if (repetitionTime < 2 || repetitionTime > 10) {
                    return R.fail("重复次数必须在2-10范围内");
                }
            } catch (NumberFormatException e) {
                return R.fail("重复次数必须为有效的整数");
            }
        }

        // 5. 校验时间间隔（单池重复模式下必校验）
        if ("单池重复".equals(parameter.getRepetitionPattern())) {
            if (parameter.getTimeInterval() == null || parameter.getTimeInterval().trim().isEmpty()) {
                return R.fail("单池重复模式下时间间隔不能为空");
            }
            try {
                double timeInterval = Double.parseDouble(parameter.getTimeInterval());
                if (timeInterval < 0 || timeInterval > 60) {
                    return R.fail("时间间隔必须在0-60范围内（单位：秒）");
                }
                // 校验小数点后最多1位
                String[] parts = parameter.getTimeInterval().split("\\.");
                if (parts.length > 1 && parts[1].length() > 1) {
                    return R.fail("时间间隔小数点后最多保留1位");
                }
            } catch (NumberFormatException e) {
                return R.fail("时间间隔必须为有效的数字");
            }
        }

        // 8. 校验输入参数（数量限制）
        if (parameter.getSampleInputParams() != null && parameter.getSampleInputParams().size() > 10) {
            return R.fail("输入参数数量不能超过10个");
        }

        // 9. 校验待计算结果（数量限制）
        if (parameter.getResultParams() != null && parameter.getResultParams().size() > 5) {
            return R.fail("待计算结果数量不能超过5个");
        }

        // 10. 校验修约设置（数量限制）
        if (parameter.getApproxParams() != null && parameter.getApproxParams().size() > 10) {
            return R.fail("修约设置数量不能超过10个");
        }

        // 11. 校验质控设置（数量限制）
        if (parameter.getQcParams() != null && parameter.getQcParams().size() > 10) {
            return R.fail("质控设置数量不能超过10个");
        }

        // 所有校验通过
        return R.ok(true);
    }

    @Override
    public Result createMethodPort(FMethodParamRequestDto methodDto, String updateBy, FPhotometricParameter photometricParameter) {

        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
            return Result.fail("请填写波长值");
        }

        // 创建测量方法表
        FMeasureMethod fMeasureMethod = new FMeasureMethod();
        fMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        fMeasureMethod.setMethodName(methodDto.getMethodName());
        fMeasureMethod.setMeasurementType(methodDto.getMethodType());
        fMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        fMeasureMethod.setMethodCreator(updateBy);
        fMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        fMeasureMethod.setMethodUseTime(LocalDateTime.now());
        boolean save = fMeasureMethodService.save(fMeasureMethod);
        if (save) {
            return Result.ok("创建成功");
        } else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> newDraft(SaveDataDto dto) {

        String methodId = dto.getMethodId();
        String taskId = dto.getTaskId();
        String updateBy = dto.getUpdateBy();
        Boolean isSave = dto.getIsSave();

        if (!StringUtils.hasText(methodId)) {
            return Result.fail("请选择方法");
        }

        if (StringUtils.hasText(taskId) && isSave) {
            saveData(taskId);
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        SysUserPO sysUserPO = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String userId = sysUserPO.getId();

        FMeasureTask task = new FMeasureTask();
        task.setTaskName("新建光度测量草稿" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        task.setIsDelete(createType);
        task.setMeasureType(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription());
        task.setMethodKey(methodId);
        task.setUserId(userId);
        task.setProjectItemKey(defaultProjectId);
        task.setCreateTime(LocalDateTime.now());
        List<FTaskSpecimen> samples = new ArrayList<>();
        task.setSampleKey(JSON.toJSONString(samples));

        List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
        FTaskHeadSnapshot snapshot = new FTaskHeadSnapshot();
        snapshot.setSampleHeads(preHead);
        task.setTableInfo(JSON.toJSONString(snapshot));

        boolean save = fMeasureTaskService.save(task);
        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();
        vo.setMethodId(task.getMethodKey());

//        vo.setProjectId(task.getProjectItemKey());
        if (save) {
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            method.setMethodUseTime(LocalDateTime.now());
            fMeasureMethodService.updateById(method);
            return Result.ok(vo);
        } else {
            return Result.fail("新建失败");
        }
    }

    @Override
    public Result<FMethodChangeCheckVO> checkMethodParam(NewPhotometricMethodParamCheckDTO checkDTO) {

        if (checkDTO.getNewMethodParam() == null) {
            return Result.fail("没有可应用的方法参数");
        }

        R<Boolean> r = checkParam(checkDTO.getNewMethodParam());
        if (!r.getSuccess()) {
            return Result.fail(r.getMessage());
        }

        FMethodChangeCheckVO vo = new FMethodChangeCheckVO();
        vo.setCurrentMethodId(checkDTO.getCurrentMethodId());
        vo.setParamChange(false);
        vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.noChange.getIdentity());
        vo.setMessage(PhotoMetricChangeMethodTypeEnum.noChange.getMessage());

        if (!StringUtils.hasText(checkDTO.getCurrentMethodId())) {
            log.info("生成新方法");
            vo.setParamChange(true);
            vo.setMessage(PhotoMetricChangeMethodTypeEnum.newMethod.getMessage());
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.newMethod.getIdentity());
            return Result.ok(vo);
        }

        if (!StringUtils.hasText(checkDTO.getCurrentTaskId())) {
            log.info("没有任务存在，将使用该方法生成数据");
            vo.setParamChange(true);
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.noData.getIdentity());
            return Result.ok(vo);
        }

        FMeasureMethod method = fMeasureMethodService.getById(checkDTO.getCurrentMethodId());
        FPhotometricParameter oldParam = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        FPhotometricParameter methodParam = checkDTO.getNewMethodParam();

        String dbJson = JSON.toJSONString(oldParam, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(methodParam, SerializerFeature.SortField);

        if (dbJson.equals(frontJson)) {
            vo.setParamChange(false);
            vo.setClearDataRange(RationChangeMethodTypeEnums.noChange.getIdentity());
            vo.setMessage(RationChangeMethodTypeEnums.noChange.getMessage());
            return Result.ok(vo);
        }

        //检查测量数据
        boolean b = checkMeasureParam(oldParam, methodParam);
        if (!b) {
            log.info("测量参数有改变，要清理所有数据");
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.measure.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(PhotoMetricChangeMethodTypeEnum.measure.getMessage());
            return Result.ok(vo);
        }

        //检查分析参数
        boolean analyze = checkAnalyzeParam(oldParam, methodParam);
        if (!analyze) {
            log.info("分析参数有改变，要清理分析数据");
            vo.setClearDataRange(PhotoMetricChangeMethodTypeEnum.analyze.getIdentity());
            vo.setParamChange(true);
            vo.setMessage(PhotoMetricChangeMethodTypeEnum.analyze.getMessage());
        }
        return Result.ok(vo);
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkMeasureParam(FPhotometricParameter oldParam, FPhotometricParameter newParam) {

        List<String> oldWave = oldParam.getWaveLength();
        List<String> newWave = newParam.getWaveLength();
        if (!Objects.equals(oldWave, newWave)) {
            return false;
        }

        if (!Objects.equals(oldParam.getLuminosity(), newParam.getLuminosity())) {
            return false;
        }

        if (!Objects.equals(oldParam.getEnergyLamp(), newParam.getEnergyLamp())) {
            return false;
        }
        if (!Objects.equals(oldParam.getGain(), newParam.getGain())) {
            return false;
        }

        return Objects.equals(oldParam.getSpectralBandwidth(), newParam.getSpectralBandwidth()) &&
                Objects.equals(oldParam.getResponseTime(), newParam.getResponseTime()) &&
                Objects.equals(oldParam.getChangeLampWaveLength(), newParam.getChangeLampWaveLength()) &&
                Objects.equals(oldParam.getRepetitionPattern(), newParam.getRepetitionPattern()) &&
                Objects.equals(oldParam.getRepetitionTime(), newParam.getRepetitionTime()) &&
                Objects.equals(oldParam.getTimeInterval(), newParam.getTimeInterval());
    }

    //检查参数是否改变，true--未改变，false--改变了
    private boolean checkAnalyzeParam(FPhotometricParameter oldParam, FPhotometricParameter newParam) {
        List<FPreCountResultParam> oldResult = oldParam.getResultParams();
        List<FPreCountResultParam> newResult = newParam.getResultParams();

        if (oldResult == null ) {
            oldResult = new ArrayList<>();
        }
        if (newResult == null ) {
            newResult = new ArrayList<>();
        }

        if (!oldResult.equals(newResult)) {
            if (newResult.size() != oldResult.size()) {
                return false;
            }

            for (int i = 0; i < oldResult.size(); i++) {
                FPreCountResultParam oldResultParam = oldResult.get(i);
                FPreCountResultParam newResultParam = newResult.get(i);
                if (oldResultParam.equals(newResultParam)) {
                    continue;
                }
                if (!oldResultParam.getFormulaText().equals(newResultParam.getFormulaText())) {
                    return false;
                }
            }
        }
        return true;
    }



    @Override
    public Result<FMeasurementCurrentInfoVO> applyMethod(NewPhotometricMethodParamCheckDTO methodDTO) {

        if (methodDTO.getNewMethodParam() == null && !StringUtils.hasText(methodDTO.getNewMethodId())) {
            return Result.fail("没有可应用的方法或参数");
        }
        if (!StringUtils.hasText(methodDTO.getMeasureType())) {
            methodDTO.setMeasureType(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription());
        }
        String methodId = methodDTO.getNewMethodId();
        if (StringUtils.hasText(methodId)) {
            FMeasureMethod method = fMeasureMethodService.getById(methodId);
            if (method == null) {
                return Result.fail("没有该方法");
            }
        }

        FMeasurementCurrentInfoVO ans = new FMeasurementCurrentInfoVO();
        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, methodDTO.getUpdateBy()));
        String userId = po.getId();
        ans.setMethodName(methodDTO.getMethodName());

        if (!StringUtils.hasText(methodId)) {
            FMeasureMethod method = new FMeasureMethod();
            method.setMeasurementParamInfo(JSON.toJSONString(methodDTO.getNewMethodParam()));
            if (StringUtils.hasText(methodDTO.getMethodName())) {
                String[] split = methodDTO.getMethodName().split("-");
                method.setMethodName(split[0] + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            } else {
                method.setMethodName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            }
            method.setMeasurementType(methodDTO.getMeasureType());
            method.setProjectItemKey(defaultProjectId);
            method.setMethodCreator(methodDTO.getUpdateBy());
            method.setMethodCreateTime(LocalDateTime.now());
            method.setMethodUseTime(LocalDateTime.now());
            boolean save = fMeasureMethodService.save(method);

            if (!save) {
                return Result.fail("应用失败");
            }
            methodId = method.getId();
            ans.setMethodName(method.getMethodName());
        }
        ans.setMethodId(methodId);

        FMeasureTask task = new FMeasureTask();
        if (!StringUtils.hasText(methodDTO.getCurrentTaskId())) {
            //任务不存在，创建任务
            task.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            task.setMethodKey(methodId);
            task.setCreateTime(LocalDateTime.now());
            task.setUserId(userId);
            task.setProjectItemKey(defaultProjectId);
            task.setIsDelete(createType);
            task.setMeasureType(methodDTO.getMeasureType());

            List<FTaskSpecimen> samples = new ArrayList<>();
            task.setSampleKey(JSON.toJSONString(samples));

            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            FTaskHeadSnapshot snapshot = new FTaskHeadSnapshot();
            snapshot.setSampleHeads(preHead);
            task.setTableInfo(JSON.toJSONString(snapshot));
            fMeasureTaskService.save(task);

            ans.setTaskId(task.getId());
            ans.setTaskName(task.getTaskName());
            FMeasureMethod method = fMeasureMethodService.getById(methodId);
            method.setMethodUseTime(LocalDateTime.now());
            fMeasureMethodService.updateById(method);
            return Result.ok(ans);

        } else {
            task = fMeasureTaskService.getById(methodDTO.getCurrentTaskId());
            if (task == null) {
                task = new FMeasureTask();
                task.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                task.setMethodKey(methodId);
                task.setCreateTime(LocalDateTime.now());
                task.setUserId(userId);
                task.setProjectItemKey(defaultProjectId);
                task.setIsDelete(createType);
                task.setMeasureType(methodDTO.getMeasureType());

                List<FTaskSpecimen> samples = new ArrayList<>();
                task.setSampleKey(JSON.toJSONString(samples));

                List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
                FTaskHeadSnapshot snapshot = new FTaskHeadSnapshot();
                snapshot.setSampleHeads(preHead);
                task.setTableInfo(JSON.toJSONString(snapshot));
                fMeasureTaskService.save(task);

                ans.setTaskId(task.getId());
                ans.setTaskName(task.getTaskName());
                FMeasureMethod method = fMeasureMethodService.getById(methodId);
                method.setMethodUseTime(LocalDateTime.now());
                fMeasureMethodService.updateById(method);
                return Result.ok(ans);
            }

            if (!StringUtils.hasText(task.getSampleKey())) {
                task.setMethodKey(methodId);
                fMeasureTaskService.updateById(task);

                ans.setTaskId(task.getId());
                ans.setTaskName(task.getTaskName());
                FMeasureMethod method = fMeasureMethodService.getById(methodId);
                method.setMethodUseTime(LocalDateTime.now());
                fMeasureMethodService.updateById(method);
                return Result.ok(ans);
            }

            if (methodDTO.getIsSaveDraft() == null || !methodDTO.getIsSaveDraft()) {
                if (methodDTO.getClearDataRange().equals(PhotoMetricChangeMethodTypeEnum.measure.getIdentity())) {
                    LambdaQueryWrapper<FMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(FMeasureData::getTaskId, task.getId())
                            .eq(FMeasureData::getIsSave, noSave);
                    fMeasureDataService.remove(wrapper);

                    LambdaQueryWrapper<FMeasureAnalysisData> analysisWrapper = new LambdaQueryWrapper<>();
                    analysisWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                            .eq(FMeasureAnalysisData::getIsSave, noSave);
                    fMeasureAnalysisDataService.remove(analysisWrapper);

                    FMeasureTask newTask = new FMeasureTask();
                    newTask.setProjectItemKey(defaultProjectId);
                    newTask.setUserId(userId);
                    newTask.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

                    newTask.setTableInfo(task.getTableInfo());

                    newTask.setId(null);
                    newTask.setMethodKey(methodId);
                    newTask.setDeviceSerialNumber(null);
                    newTask.setIsDelete(createType);
                    newTask.setCreateTime(LocalDateTime.now());
                    newTask.setMeasureType(methodDTO.getMeasureType());
                    fMeasureTaskService.save(newTask);
                    ans.setTaskId(newTask.getId());
                    ans.setTaskName(newTask.getTaskName());
                    FMeasureMethod method = fMeasureMethodService.getById(methodId);
                    method.setMethodUseTime(LocalDateTime.now());
                    fMeasureMethodService.updateById(method);
                    return Result.ok(ans);

                } else if (methodDTO.getClearDataRange().equals(PhotoMetricChangeMethodTypeEnum.analyze.getIdentity())) {

                    task.setMethodKey(methodId);
                    fMeasureTaskService.updateById(task);


                    List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
                    if (taskSpecimen != null && !taskSpecimen.isEmpty()) {
                        for (FTaskSpecimen specimen : taskSpecimen) {

                            FMeasureMethod fMeasureMethod = fMeasureMethodService.getById(methodId);
                            FPhotometricParameter parameter = JSON.parseObject(fMeasureMethod.getMeasurementParamInfo(), FPhotometricParameter.class);
                            Boolean b = dealData(task, specimen.getSpecimenKey(), parameter);
                            if (!b) {
                                return Result.fail("重新分析出错");
                            }
                        }
                    }
                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    return Result.ok(ans);

                } else {
                    task.setMethodKey(methodId);
                    fMeasureTaskService.updateById(task);
                    ans.setTaskId(task.getId());
                    ans.setTaskName(task.getTaskName());
                    FMeasureMethod method = fMeasureMethodService.getById(methodId);
                    method.setMethodUseTime(LocalDateTime.now());
                    fMeasureMethodService.updateById(method);
                    return Result.ok(ans);

                }
            } else {
                //保存数据

                saveData(task.getId());

                FMeasureTask newTask = new FMeasureTask();
                newTask.setProjectItemKey(defaultProjectId);
                newTask.setUserId(userId);
                newTask.setTaskName(newTaskName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

                newTask.setTableInfo(task.getTableInfo());

                newTask.setId(null);
                newTask.setMethodKey(methodId);
                newTask.setDeviceSerialNumber(null);
                newTask.setIsDelete(createType);
                newTask.setCreateTime(LocalDateTime.now());
                newTask.setMeasureType(methodDTO.getMeasureType());
                fMeasureTaskService.save(newTask);

                ans.setTaskId(newTask.getId());
                ans.setTaskName(newTask.getTaskName());
                FMeasureMethod method = fMeasureMethodService.getById(methodId);
                method.setMethodUseTime(LocalDateTime.now());
                fMeasureMethodService.updateById(method);
                return Result.ok(ans);
            }
        }
    }

    private void saveData(String taskId) {
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getIsSave, noSave);
        List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
        // 批量更新优化
        if (!list.isEmpty()) {
            list.forEach(fMeasureData -> fMeasureData.setIsSave(saved));
            fMeasureDataService.updateBatchById(list);
        }

        LambdaQueryWrapper<FMeasureAnalysisData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getIsSave, noSave);
        List<FMeasureAnalysisData> anaList = fMeasureAnalysisDataService.list(wrapper);
        if (!anaList.isEmpty()) {
            anaList.forEach(fMeasureAnalysisData -> fMeasureAnalysisData.setIsSave(saved));
            fMeasureAnalysisDataService.updateBatchById(anaList);
        }
    }


    private Boolean dealData(FMeasureTask task, String sampleId, FPhotometricParameter parameter) {

        /*
        分析数据，不保存数据
        查找测量数据，公式计算
         */
        FMeasureSample sample = fMeasureSampleService.getById(sampleId);
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, task.getId())
                .eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber)
                .last("limit 1");
        FMeasureData one = fMeasureDataService.getOne(queryWrapper);
        if (one == null || !StringUtils.hasText(one.getOriginalCalculationData())) {
            log.info("没有测量数据");
            return false;
        }

        int digit = Constants.DEFAULT_DECIMAL_NUM;
        List<FPhotometricOriginDataInfo> listData = JSON.parseArray(one.getOriginalCalculationData(), FPhotometricOriginDataInfo.class);
        R<FMeasureTaskAttachment> attachmentR = fMeasureTaskAttachmentService.queryByTaskId(task.getId());
        if (attachmentR.getSuccess()) {
            FMeasureTaskAttachment attachment = attachmentR.getData();
            if (StringUtils.hasText(attachment.getDataParam())) {
                FQueryDataDTO queryDataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
                if (queryDataDTO.getDigit() != null) {
                    digit = queryDataDTO.getDigit();
                }
            }
        }

        R<List<FPhotometricCountDataInfo>> formula = formula(listData, sample, task, parameter, digit);
        if (!formula.getSuccess()) {
            return false;
        }

        List<FPhotometricCountDataInfo> data = formula.getData();

        LambdaQueryWrapper<FMeasureAnalysisData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                .eq(FMeasureAnalysisData::getSampleKey, sampleId);

        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(wrapper);
        analysisData.setAnalysisResult(JSON.toJSONString(data));
        analysisData.setIsSave(createType);
        return fMeasureAnalysisDataService.updateById(analysisData);
    }


    @Override
    public Result<FMeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType) {

        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();

        LambdaQueryWrapper<FMeasureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FMeasureTask::getUserId, id)
                .eq(FMeasureTask::getMeasureType, measureType)
                .orderByDesc(FMeasureTask::getCreateTime);

        List<FMeasureTask> taskList = fMeasureTaskService.list(wrapper);
        if (taskList != null && !taskList.isEmpty()) {
            FMeasureTask task = taskList.get(0);
            String projectItemKey = task.getProjectItemKey();
            String methodKey = task.getMethodKey();

            FMeasureMethod method = fMeasureMethodService.getById(methodKey);
            FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getTaskName());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
//            vo.setProjectId(project.getId());
//            vo.setProjectName(project.getProjectName());
            return Result.ok(vo);
        }
        FMeasurementCurrentInfoVO vo = new FMeasurementCurrentInfoVO();

        LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureMethod::getMethodCreator, updateBy)
                .eq(FMeasureMethod::getMeasurementType, measureType)
                .orderByDesc(FMeasureMethod::getMethodCreateTime);
        List<FMeasureMethod> list = fMeasureMethodService.list(queryWrapper);
        if (list == null || list.isEmpty()) {
            LambdaQueryWrapper<FMeasureProject> projectWrapper = new LambdaQueryWrapper<>();
            projectWrapper.eq(FMeasureProject::getCreatorKey, updateBy)
                    .orderByDesc(FMeasureProject::getCreateTime);
            List<FMeasureProject> projectList = fMeasureProjectService.list(projectWrapper);
            if (projectList == null || projectList.isEmpty()) {
                return Result.ok(vo);
            }
//            vo.setProjectName(projectList.get(0).getProjectName());
//            vo.setProjectId(projectList.get(0).getId());

        } else {
            FMeasureMethod fMeasureMethod = list.get(0);
            FMeasureProject fMeasureProject = fMeasureProjectService.getById(fMeasureMethod.getProjectItemKey());

            vo.setMethodId(fMeasureMethod.getId());
            vo.setMethodName(fMeasureMethod.getMethodName());
//            vo.setProjectId(fMeasureProject.getId());
//            vo.setProjectName(fMeasureProject.getProjectName());
        }
        return Result.ok(vo);

    }

    @Override
    public Result addOrDeleteSample(FAddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            return Result.fail("请先创建方法");
        }
        if (dto.getHeads() == null || dto.getHeads().isEmpty()) {
            return Result.fail("至少需要一个表头");
        }

        if (dto.getHeads().size() > 10) {
            return Result.fail("表格新增列数不能超过10列");
        }
        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            FMeasureTask newTask = new FMeasureTask();

            FMeasureMethod method = fMeasureMethodService.getById(dto.getMethodId());
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
                if (fMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                return Result.fail("系统出错");
            }
            dto.setTaskId(newTask.getId());
        }


        FMeasureTask task = fMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        if (StringUtils.hasText(task.getSampleKey())) {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        }
        if (taskSpecimen == null) {
            taskSpecimen = new ArrayList<>();
        }

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId())
                        .orderByDesc(FMeasureData::getSerialNumber);
                List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    FMeasureData one = list.get(0);
                    one.setIsDelete(deleteType);
                    fMeasureDataService.updateById(one);
                }
            }
        }

        FTaskHeadSnapshot taskHeadSnapshot = new FTaskHeadSnapshot();

        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
        }

        if (dto.getHeads() != null && !dto.getHeads().isEmpty()) {
            taskHeadSnapshot.setSampleHeads(dto.getHeads());
        }
        taskHeadSnapshot.setTaskId(task.getId());
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));


        for (Map<String, String> map : dto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            FSampleVO sample = JSON.parseObject(jsonString, FSampleVO.class);
            FMeasureSample newSample = new FMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (dto.getHeads() != null) {
                JSONObject newColData = new JSONObject();

                for (FHeadVO head : dto.getHeads()) {
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
                fMeasureSampleService.updateById(newSample);
            } else {
                FTaskSpecimen specimen = new FTaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleType(newSample.getSampleType());
                log.info("新增的样品：{}", newSample);
                fMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                taskSpecimen.add(specimen);
            }
        }

        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);
        boolean b = fMeasureTaskService.updateById(task);
        if (!b) {
            return Result.fail("任务保存样品信息出错");
        }

        if (StringUtils.hasText(dto.getDeviceSerialNumber())) {
            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(dto.getDeviceSerialNumber());
            if (!poolInfo.getSuccess()) {
                return Result.ok();
            }
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            Boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool();
            if (isAutoChangePool) {
                List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(dto.getDeviceSerialNumber());
                concisePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
            }
        }
        return Result.ok();
    }

    @Override
    public Result<Boolean> deleteSample(String taskId, String sampleId) {

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(sampleId)) {
            return Result.fail("请选择需要删除的任务和样品");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        if (!StringUtils.hasText(task.getSampleKey())) {
            return Result.fail("任务无样品");
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        for (FTaskSpecimen specimen : taskSpecimen) {
            if (sampleId.equals(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(FMeasureData::getTaskId, task.getId())
                         .orderByDesc(FMeasureData::getSerialNumber);
                List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    FMeasureData one = list.get(0);
                    one.setIsDelete(deleteType);
                    fMeasureDataService.updateById(one);
                }
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        fMeasureTaskService.updateById(task);
        return Result.ok(true);
    }

    @Override
    public Result revokeDel(String taskId, String sampleId) {
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        String sampleKey = task.getSampleKey();

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, FTaskSpecimen.class);
        for (FTaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);
            }
            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .eq(FMeasureData::getTaskId, task.getId())
                     .orderByDesc(FMeasureData::getSerialNumber);
            List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
            if (list != null && !list.isEmpty()) {
                FMeasureData one = list.get(0);
                one.setIsDelete(createType);
                fMeasureDataService.updateById(one);
            }

        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(fMeasureTaskService.updateById(task));
    }

    @Override
    public Result<List<JSONObject>> newQuerySampleInfo(String taskId) {
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                FMeasureSample measureSample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                FSampleVO vo = new FSampleVO();
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
    public Result<List<FHeadVO>> querySampleHeads(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
            return Result.ok(preHead);
        } else {
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription(), null);
                return Result.ok(preHead);
            }
        }
    }

    @Override
    public Result<FMethodParam> queryMethodParam(String methodId) {

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            return Result.fail("未查询到该方法参数");
        }
        FPhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        FMethodParam param = new FMethodParam();
        BeanUtils.copyProperties(photometricParameter, param);
        param.setMethodId(method.getId());
        param.setMethodName(method.getMethodName());
        param.setMeasurementType(method.getMeasurementType());
        param.setLuminosityName(WaveLengthEnum.getName(photometricParameter.getLuminosity()));
        param.setSamplePoolWayName(RepeatConfigEnum.getName(photometricParameter.getSamplePoolWay()));
//        param.setPhotometricParameter(photometricParameter);
        return Result.ok(param);
    }

    @Override
    public R<FPhotometricReportDataVO> getReportCount(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        if (parameter.getResultParams() == null || parameter.getResultParams().isEmpty()) {
            return R.fail("缺少结果参数");
        }

        List<FHeadVO> heads = new ArrayList<>();

        List<Map<String, Object>> data = new ArrayList<>();
        FPhotometricReportDataVO ans = new FPhotometricReportDataVO();

        heads.add(new FHeadVO("样品编号", "sampleNumber"));
        heads.add(new FHeadVO("样品名称", "sampleName"));
        heads.add(new FHeadVO("样品类型", "sampleType"));
        for (FPreCountResultParam param : parameter.getResultParams()) {
            heads.add(new FHeadVO(param.getResultName(), param.getResultIdentity() + "-" +param.getFormulaText()));
        }
        ans.setHeadList(heads);

        if (StringUtils.hasText(task.getSampleKey())) {
            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

            for (FTaskSpecimen specimen : taskSpecimen) {
                Map<String, Object> map = new HashMap<>();
                FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                map.put("sampleName", sample.getSampleName());
                map.put("sampleNumber", sample.getSampleNumber());
                map.put("sampleType", SampleTypeEnum.getName(sample.getSampleType()));

                LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                        .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey());
                List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(queryWrapper);
                if (list != null && !list.isEmpty()) {
                    FMeasureAnalysisData analysisData = list.get(0);
                    List<FPhotometricCountDataInfo> countDataInfos = JSON.parseArray(analysisData.getAnalysisResult(), FPhotometricCountDataInfo.class);

                    if (countDataInfos != null && !countDataInfos.isEmpty()) {
                        for (FPhotometricCountDataInfo count : countDataInfos) {
                            double value = Double.parseDouble(count.getCountValue());
                            map.put(count.getResultIdentity() + "-" + count.getFormulaText(), String.format("%.2f",value));
                        }
                    }
                    data.add(map);
                }
            }
        }
        ans.setDataList(data);

        return R.ok(ans);
    }

    @Override
    public Result<List<FHeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay, String deviceSerialNumber) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(methodId)) {
            return Result.fail("请选择方法或任务");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
        LinkedHashSet<FHeadVO> ans = new LinkedHashSet<>();

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
            return Result.ok(new ArrayList<>(ans));
        }

        if (StringUtils.hasText(task.getTableInfo())) {
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            if (snapshot != null) {
                if (snapshot.getTaskHeads()!= null && !snapshot.getTaskHeads().isEmpty()) {
                    ans.addAll(snapshot.getTaskHeads());
                }
                if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                    ans.addAll(snapshot.getSampleHeads());
                }
            }
        } else {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(method.getMeasurementType(), SampleTypeEnum.kind_stand.getIdentity());
            ans.addAll(preHead);
        }

        if (StringUtils.hasText(task.getSampleKey())) {

            List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen specimen : taskSpecimen) {
                FMeasureSample measureSample = fMeasureSampleService.getById(specimen.getSpecimenKey());
                if (StringUtils.hasText(measureSample.getHeads())){
                    List<FHeadVO> heads = JSON.parseArray(measureSample.getHeads(), FHeadVO.class);

                    if (heads != null && !heads.isEmpty()) {
                        ans.addAll(heads);
                    }
                }
            }

        }

        List<FHeadVO> vos = dealHeads(new ArrayList<>(ans), parameter, allDisplay, deviceSerialNumber);
        return Result.ok(new ArrayList<>(vos));
    }


    private List<FHeadVO> dealHeads(List<FHeadVO> heads, FPhotometricParameter parameter, Boolean allDisplay, String deviceSerialNumber) {
        List<String> waveLength = parameter.getWaveLength();
        Map<String, FHeadVO> map = heads.stream().collect(Collectors.toMap(FHeadVO::getIdentity, head -> head));

        Map<String, List<FHeadVO>> headsMap = new HashMap<>();

        Map<String, FHeadVO> analyzeMap = new HashMap<>();

        for (FHeadVO head : heads) {
            headsMap.computeIfAbsent(head.getLocate(), k -> new ArrayList<>()).add(head);
            if (head.getLocate().equals(TableHeadILocateEnums.measure.getIdentity())) {
                map.put(head.getIdentity(), head);
            }
            if (head.getLocate().equals(TableHeadILocateEnums.analyze.getIdentity())) {
                analyzeMap.put(head.getIdentity(), head);
            }
        }

        List<FHeadVO> allMeasureHeads = new ArrayList<>();

        String mode = parameter.getLuminosity();
        if (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode())) {
            for (String s : waveLength) {
                String identity = s + "-"+ mode;
                String name =WaveLengthEnum.getName(mode) + "[" + s + "nm]";
                FHeadVO newHead = new FHeadVO(name, identity, TableHeadILocateEnums.measure.getIdentity(), name);
                if (map.containsKey(identity)) {
                    newHead = map.get(identity);
                }
                allMeasureHeads.add(newHead);
            }
        } else if (mode.equals(WaveLengthEnum.abs.getCode()) || mode.equals(WaveLengthEnum.tPercent.getCode())) {
            for (String s : waveLength) {
                String absIdentity = s + "-"+ WaveLengthEnum.abs.getCode();
                String tPercentIdentity = s + "-"+ WaveLengthEnum.tPercent.getCode();
                String absName = WaveLengthEnum.abs.getName()+ "[" + s + "nm]";
                String tPercentName = WaveLengthEnum.tPercent.getName()+ "[" + s + "nm]";

                FHeadVO absHead = new FHeadVO(absName, absIdentity, TableHeadILocateEnums.measure.getIdentity(), absName);
                FHeadVO tPercentHead = new FHeadVO(tPercentName, tPercentIdentity, TableHeadILocateEnums.measure.getIdentity(), tPercentName);

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
        }
        List<FHeadVO> ans = new ArrayList<>();
        List<FHeadVO> systemHeads = headsMap.get(TableHeadILocateEnums.system.getIdentity());
        if (systemHeads != null) ans.addAll(systemHeads);
        for (FHeadVO an : ans) {
            if (an.getIdentity().equals(SampleTableEnum.SAMPLE_LOCATION.getIdentity())) {
                ans.remove(an);
                break;
            }
        }

        if (StringUtils.hasText(deviceSerialNumber)) {

            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (poolInfo.getSuccess()) {
                FSamplePoolInfoVO data = poolInfo.getData();
                if (data.getIsAutoChangePool() ){
                    String sampleLocationName = SampleTableEnum.SAMPLE_LOCATION.getName();
                    String sampleLocationIdentity = SampleTableEnum.SAMPLE_LOCATION.getIdentity();
                    FHeadVO sampleLocation = new FHeadVO(sampleLocationName, sampleLocationIdentity, TableHeadILocateEnums.location.getIdentity(), sampleLocationName);
                    ans.add(sampleLocation);
                }
            }
        }

        List<FHeadVO> sampleHeads = headsMap.get(TableHeadILocateEnums.sample.getIdentity());
        if (sampleHeads != null) ans.addAll(sampleHeads);
        ans.addAll(allMeasureHeads);
        List<FHeadVO> taskHeads = headsMap.get(TableHeadILocateEnums.data.getIdentity());
        if (taskHeads != null)ans.addAll(taskHeads);

        List<FPreCountResultParam> resultParams = parameter.getResultParams();
        List<FHeadVO> headAnalyze = new ArrayList<>();
        if (resultParams == null) {
            resultParams = new ArrayList<>();
        }
        for (FPreCountResultParam results : resultParams) {
            //TODO 方法参数标识待定，后续再改动
            if (!StringUtils.hasText(results.getParam())) {
                results.setParam(results.getResultIdentity() + "-" + results.getFormulaText());
            }
            String name = results.getResultName() + "(" + results.getUnit() + ")";
            FHeadVO analyzeHead = new FHeadVO(name, results.getParam(), TableHeadILocateEnums.analyze.getIdentity(), name);

            if (analyzeMap.containsKey(results.getParam())) {
                analyzeHead = analyzeMap.get(results.getParam());
            }
            headAnalyze.add(analyzeHead);
        }
        ans.addAll(headAnalyze);

        List<FHeadVO> other = headsMap.get(TableHeadILocateEnums.other.getIdentity());
        if (other == null || other.isEmpty()) {
            other = new ArrayList<>();
            FHeadVO samplingTimeHead = new FHeadVO(SampleTableEnum.SAMPLING_TIME.getName(), SampleTableEnum.SAMPLING_TIME.getIdentity(), TableHeadILocateEnums.other.getIdentity(), SampleTableEnum.SAMPLING_TIME.getName());
            FHeadVO remark = new FHeadVO(SampleTableEnum.REMARK.getName(), SampleTableEnum.REMARK.getIdentity(), TableHeadILocateEnums.data.getIdentity(), SampleTableEnum.REMARK.getName());
            other.add(samplingTimeHead);
            other.add(remark);
        }

        ans.addAll(other);

        return ans;
    }
    
    @Override
    public R<List<FPhotometricCountDataInfo>> formula(List<FPhotometricOriginDataInfo> vo, FMeasureSample sample, FMeasureTask task, FPhotometricParameter parameter, Integer digit) {

        if (!StringUtils.hasText(task.getTableInfo())) {
            return R.fail("缺少分析参数，无法计算分析");
        }
        FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
        if (snapshot.getMapRelation() == null || snapshot.getMapRelation().isEmpty()) {
            return R.fail("缺少映射关系，无法计算分析");
        }
        String jsonString = JSON.toJSONString(sample);
        JSONObject allData = JSON.parseObject(jsonString);
        if (StringUtils.hasText(sample.getNewColData())) {
            JSONObject object = JSONObject.parseObject(sample.getNewColData());
            allData.putAll(object);
        }
        LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                .eq(FMeasureAnalysisData::getSampleKey, sample.getId())
                .orderByDesc(FMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(queryWrapper);
        if (one != null && StringUtils.hasText(one.getAnalysisParam())) {
            String analysisParam = one.getAnalysisParam();
            JSONObject object = JSON.parseObject(analysisParam);
            allData.putAll(object);
        }

        for (FPhotometricOriginDataInfo dataInfo : vo) {
            if (dataInfo.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                allData.putAll(dataInfo.getData());
            }
        }

        StringBuilder values = new StringBuilder();

        StringBuilder identity = new StringBuilder();

        for (FInputParam inputParam : parameter.getSampleInputParams()) {

            Optional<FInputParamMapTableHead> mapTableHead = snapshot.getMapRelation().stream().filter(
                    mapRelation -> inputParam.getResultIdentity().equals(mapRelation.getParamIdentity())).findFirst();
            if (!mapTableHead.isPresent()) {
                return R.fail("缺少参数映射：" + inputParam.getParamName());
            }
            identity.append(mapTableHead.get().getParamIdentity()).append(",");
            String s = allData.getString(mapTableHead.get().getHeadIdentity());
            if (!StringUtils.hasText(s)) {
                return R.fail("缺少数据！");
            }
            s = DataCountUtil.formatValueWithBigDecimal(digit, s);
            values.append(s).append(",");
        }
        identity.deleteCharAt(identity.length()-1);
        values.deleteCharAt(values.length()-1);

        List<FPhotometricCountDataInfo> countList = new ArrayList<>();
        for (FPreCountResultParam param : parameter.getResultParams()) {
            String[] split = param.getFormulaText().split("=");
            String text = split[split.length - 1];
            String result = restUtil.sum(text, identity.toString(), values.toString());
            FPhotometricCountDataInfo countDataInfo = new FPhotometricCountDataInfo();
            BeanUtils.copyProperties(param, countDataInfo);
            JSONObject resultJson = JSONObject.parseObject(result);

            if (!resultJson.get("code").equals("0") && !resultJson.get("code").equals(0)) {
                return R.fail("公式计算出错！" + resultJson.get("message"));
            }
            countDataInfo.setCountValue(resultJson.get("result").toString());
            countList.add(countDataInfo);
        }

        return R.ok(countList);
    }


    @Override
    public Result<List<Map<String, Object>>> queryData(FQueryDataDTO queryDataDTO) {
        String taskId = queryDataDTO.getTaskId();
        Boolean allDisplay = queryDataDTO.getAllDisplay();
        List<String> option = queryDataDTO.getOption();

        if (!StringUtils.hasText(taskId) || allDisplay == null) {
            return Result.fail("未选择任务");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        List<Map<String, Object>> ans = new ArrayList<>();
        if (!StringUtils.hasText(task.getSampleKey())) {
            log.info("未找到样品");
            return Result.ok(ans);
        }

        Integer digit = queryDataDTO.getDigit();
        if (digit == null || digit < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
            queryDataDTO.setDigit(digit);
        }

        R<FMeasureTaskAttachment> attachmentResult = fMeasureTaskAttachmentService.queryByTaskId(taskId);
        if (!attachmentResult.getSuccess()) {
            FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
            taskAttachment.setTaskId(taskId);
            taskAttachment.setDataParam(JSON.toJSONString(queryDataDTO));
            fMeasureTaskAttachmentService.save(taskAttachment);
            attachmentResult = fMeasureTaskAttachmentService.queryByTaskId(taskId);
        }
        FMeasureTaskAttachment attachment = attachmentResult.getData();
        if (!StringUtils.hasText(attachment.getDataParam())) {
            attachment.setDataParam(JSON.toJSONString(queryDataDTO));
            fMeasureTaskAttachmentService.saveOrUpdate(attachment);
        }


        boolean repeat = false;
        FPhotometricParameter parameter = null;
        if (StringUtils.hasText(task.getMethodKey())) {
            FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
            if (method != null && StringUtils.hasText(method.getMeasurementParamInfo())) {
                parameter= JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
                if (StringUtils.hasText(parameter.getRepetitionPattern()) && parameter.getRepetitionPattern().equals("单池重复")) {
                    repeat = true;
                }
            }
        }
        List<Integer> usefulLocation = new ArrayList<>();

        if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {
            usefulLocation = fSamplePoolConfigService.getUsefulLocation(queryDataDTO.getDeviceSerialNumber());
        }

        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        for (FTaskSpecimen specimen : taskSpecimen) {
            if (!allDisplay && specimen.getDeleteType().equals("1")) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            FMeasureSample sample = fMeasureSampleService.getById(specimen.getSpecimenKey());

            //样品信息
            map.put(SampleTableEnum.SAMPLE_ID.getIdentity(), sample.getId());
            map.put(SampleTableEnum.SAMPLE_NAME.getIdentity(), sample.getSampleName());
            map.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber());
            map.put(SampleTableEnum.SAMPLE_TYPE.getIdentity(), SampleTypeEnum.getName(sample.getSampleType()));
            map.put("deleteType", specimen.getDeleteType());

            if (StringUtils.hasText(queryDataDTO.getDeviceSerialNumber())) {

                if (specimen.getIsTested().equals(noTested) && !usefulLocation.isEmpty()) {
                    R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(queryDataDTO.getDeviceSerialNumber());
                    if (poolInfo.getSuccess()) {
                        FSamplePoolInfoVO data = poolInfo.getData();
                        if (data.getIsAutoChangePool()) {
                            map.put(SampleTableEnum.SAMPLE_LOCATION.getIdentity(), "样品池" + sample.getSampleLocation());
                        }
                    }
                }
            }

            if (StringUtils.hasText(sample.getNewColData())){
                Map<String, String> object = JSONObject.parseObject(sample.getNewColData(), new TypeReference<Map<String, String>>() {});
                map.putAll(object);
            }

            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureData::getTaskId, taskId)
                    .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(FMeasureData::getSerialNumber)
                    .last("limit 1");
            FMeasureData data = fMeasureDataService.getOne(queryWrapper);
            if (!repeat) {
                if (data != null) {
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        List<FPhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), FPhotometricOriginDataInfo.class);

                        for (FPhotometricOriginDataInfo wlData : wlDataList) {
                            Map<String, String> originData = wlData.getData();
                            // 创建一个新的Map来存储格式化后的数据
                            Map<String, String> formattedData = new HashMap<>();
                            // 使用现有的工具类 DataCountUtil.formatValueWithBigDecimal 进行格式化
                            for (Map.Entry<String, String> entry : originData.entrySet()) {
//                                String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                Double v = DataCountUtil.formatValue(digit, Double.valueOf(entry.getValue()));
                                String s = DataCountUtil.addZero(v, digit);
                                formattedData.put(entry.getKey(), s);
                            }
                            map.putAll(formattedData);
                            map.put("samplingTime", data.getSamplingTime());
                        }

                        Map<String, String> countData = getCountData(task, sample, parameter, digit, wlDataList);
                        map.putAll(countData);
                    }
                }

                LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                        .eq(FMeasureAnalysisData::getSampleKey, sample.getId())
                        .orderByDesc(FMeasureAnalysisData::getAnalysisName).last("limit 1");

                FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null) {
                    if (StringUtils.hasText(one.getAnalysisParam())) {
                        Map<String, String> jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>() {
                        });
                        map.putAll(jsonObject);
                    }
                }
            } else {
                if (data != null) {
                    if (StringUtils.hasText(data.getOriginalCalculationData())){
                        Map<String, Map<String, String>> childrenList = new HashMap<>();

                        List<Map<String, String>> middle = new ArrayList<>();
                        List<FPhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), FPhotometricOriginDataInfo.class);

                        Map<String, Map<String, String>> countDataList = new HashMap<>();
                        FPhotometricOriginDataInfo dataInfo = reCountData(wlDataList, digit);
                        for (FPhotometricOriginDataInfo wlData : wlDataList) {

                            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                                wlDataList.remove(wlData);
                                BeanUtils.copyProperties(dataInfo, wlData);
                                wlDataList.add(dataInfo);
                                map.putAll(wlData.getData());
                                Map<String, Map<String, String>> ch = extracted(option, wlData, sample);
                                if (!ch.isEmpty()) {
                                    for (Map.Entry<String, Map<String, String>> entry : ch.entrySet()) {
                                        String key = entry.getKey();
                                        Map<String, String> value = entry.getValue();

                                        if (countDataList.containsKey(key)) {
                                            // 如果键已存在，将新值中的键值对合并到现有Map中
                                            Map<String, String> existingMap = countDataList.get(key);
                                            // 将value中的所有键值对放入existingMap中（相同键会覆盖，新键会添加）
                                            existingMap.putAll(value);
                                        } else {
                                            // 如果键不存在，直接添加（创建副本以避免引用问题）
                                            countDataList.put(key, new HashMap<>(value));
                                        }
                                    }
                                }
                            } else {
                                Map<String, String> originData = wlData.getData();
                                // 创建一个新的Map来存储格式化后的数据
                                Map<String, String> formattedData = new HashMap<>();
                                // 使用现有的工具类 DataCountUtil.formatValueWithBigDecimal 进行格式化
                                for (Map.Entry<String, String> entry : originData.entrySet()) {
//                                    String formattedValue = DataCountUtil.formatValueWithBigDecimal(digit, entry.getValue());
                                    Double v = DataCountUtil.formatValue(digit, Double.valueOf(entry.getValue()));
                                    String s = DataCountUtil.addZero(v, digit);
                                    formattedData.put(entry.getKey(), s);
                                }

                                Map<String, String> children = new HashMap<>(formattedData);
                                String[] split = wlData.getIdentity().split("-");
                                children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + split[split.length - 1]);
                                children.put(SampleTableEnum.SAMPLING_TIME.getIdentity(), wlData.getMeasureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                                childrenList.putIfAbsent(wlData.getIdentity(), children);
                            }
                        }
                        childrenList.putAll(countDataList);
                        map.put("samplingTime", data.getSamplingTime());

                        childrenList.forEach((key, value) -> middle.add(value));
                        map.put("children", middle);
                        Map<String, String> countData = getCountData(task, sample, parameter, digit, wlDataList);
                        map.putAll(countData);
                    }
                    map.put("remark", data.getRemark());
                }

                LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
                analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                        .eq(FMeasureAnalysisData::getSampleKey, sample.getId())
                        .orderByDesc(FMeasureAnalysisData::getAnalysisName).last("limit 1");

                FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
                if (one != null) {
                    if (StringUtils.hasText(one.getAnalysisParam())) {
                        Map<String, String> jsonObject = JSON.parseObject(one.getAnalysisParam(), new TypeReference<Map<String, String>>() {
                        });
                        map.putAll(jsonObject);
                    }
                }
            }
            ans.add(map);
        }

        return Result.ok(ans);
    }

    public Map<String,String> getCountData(FMeasureTask task, FMeasureSample sample, FPhotometricParameter parameter, Integer digit, List<FPhotometricOriginDataInfo> wlDataList) {

        Map<String, String> map = new HashMap<>();

        R<List<FPhotometricCountDataInfo>> formula = formula(wlDataList, sample, task, parameter, digit);
        if (formula.getSuccess()) {
            List<FPhotometricCountDataInfo> formulaData = formula.getData();
            for (FPhotometricCountDataInfo count : formulaData) {
                String s = DataCountUtil.formatValueWithBigDecimal(digit, count.getCountValue());
                map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);
            }
        }

        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, task.getId())
                .eq(FMeasureAnalysisData::getSampleKey, sample.getId())
                .orderByDesc(FMeasureAnalysisData::getAnalysisName).last("limit 1");

        FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (one != null) {
            List<FPhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), FPhotometricCountDataInfo.class);

            if (map.isEmpty()) {
                for (FPhotometricCountDataInfo count : countList) {

                    String s = DataCountUtil.formatValueWithBigDecimal(digit, count.getCountValue());
                    map.put(count.getResultIdentity() + "-" + count.getFormulaText(), s);

                }
            }
        }
        return map;
    }

    private FPhotometricOriginDataInfo reCountData(List<FPhotometricOriginDataInfo> wlDataList, Integer digit) {

        Map<String, List<Double>> data = new HashMap<>();
        Map<String, FRepeatMeasureData> otherData = new HashMap<>();
        Map<String, String> ans = new HashMap<>();
        FPhotometricOriginDataInfo result = new FPhotometricOriginDataInfo();

        for (FPhotometricOriginDataInfo wlData : wlDataList) {
            if (wlData.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                BeanUtils.copyProperties(wlData, result);
                continue;
            }

            wlData.getData().forEach((key, value) -> {
                data.computeIfAbsent(key, k -> new ArrayList<>()).add(Double.parseDouble(value));
            });
        }

        for (Map.Entry<String, List<Double>> entry : data.entrySet()) {
            List<Double> values = entry.getValue();
            double mean = DataCountUtil.calculateMean(values, digit);
            Double sd = DataCountUtil.calculateSD(values, digit);
            Double rsd = DataCountUtil.calculateRSD(values, digit);

            FRepeatMeasureData repeatMeasureData = new FRepeatMeasureData();
            repeatMeasureData.setSd(sd == null ? "未检出" :DataCountUtil.addZero(sd, digit));
            repeatMeasureData.setRsd(rsd == null ? "未检出" :DataCountUtil.addZero(rsd, digit));
            otherData.put(entry.getKey(), repeatMeasureData);
            ans.put(entry.getKey(), DataCountUtil.addZero(mean, digit));
        }
        result.setData(ans);
        result.setOtherData(otherData);
        return result;
    }

    private Map<String, Map<String, String>> extracted(List<String> option, FPhotometricOriginDataInfo wlData, FMeasureSample sample) {

        Map<String, String> originData = wlData.getData();
        Map<String, Map<String, String>> ans = new HashMap<>();
        String mapName = wlData.getWavelength() + "-" + wlData.getMode();

        FRepeatMeasureData absData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode());
        FRepeatMeasureData tPercentData = wlData.getOtherData().get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode());


        if (wlData.getMode().equals(WaveLengthEnum.abs.getCode()) || wlData.getMode().equals(WaveLengthEnum.tPercent.getCode())) {

            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());

                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode()));
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), originData.get(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode()));
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());

                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getSd());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getSd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.abs.getCode(), absData.getRsd());
                        children.put(wlData.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData.getRsd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        } else {
            if (!option.isEmpty()) {
                for (String s : option) {
                    Map<String, String> children = new HashMap<>();
                    if (DataIdentityEnums.avg.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());
                        children.put(mapName, originData.get(mapName));
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.avg.getIdentity(), children);
                    }
                    if (DataIdentityEnums.sd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
                        FRepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getSd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.sd.getIdentity(), children);
                    }
                    if (DataIdentityEnums.rsd.getIdentity().equals(s)) {
                        children.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
                        FRepeatMeasureData measureData = wlData.getOtherData().get(mapName);
                        children.put(mapName, measureData.getRsd());
                        ans.putIfAbsent(wlData.getWavelength() + "-" + DataIdentityEnums.rsd.getIdentity(), children);
                    }
                }
            }
        }

        return ans;
    }

    @Override
    public R<String> savePhotometricMeasureData(FPhotometricSampleAndDataVO sampleAndDataVO) {

        String taskId = sampleAndDataVO.getTaskId();
        String sampleId = sampleAndDataVO.getSampleId();
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
        FMeasureData fMeasureData = fMeasureDataService.getOne(queryWrapper);

        if (fMeasureData == null) {
            fMeasureData = new FMeasureData();
            fMeasureData.setSerialNumber(0);
        }

        if (StringUtils.hasText(fMeasureData.getOriginalCalculationData()) || StringUtils.hasText(fMeasureData.getInstrumentData())) {
            fMeasureData = new FMeasureData();
            fMeasureData.setSerialNumber(fMeasureData.getSerialNumber() + 1);
        }
        fMeasureData.setTaskId(taskId);
        fMeasureData.setSampleKey(sampleId);
        fMeasureData.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
        fMeasureData.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
        fMeasureData.setMeasurementStartTime(sampleAndDataVO.getStartTime());
        fMeasureData.setMeasurementEndTime(sampleAndDataVO.getEndTime());
        fMeasureData.setSamplingTime(sampleAndDataVO.getSampleTime());

        fMeasureData.setIsDelete(createType);

        fMeasureDataService.saveOrUpdate(fMeasureData);

        return R.ok(fMeasureData.getId());
    }

    @Override
    public R<Boolean> saveCountData(FPhotometricSampleAndDataVO countData, String dataId) {

        if (countData.getCountData() == null || countData.getCountData().isEmpty()) {
            return R.fail("没有要保存的数据");
        }
        String taskId = countData.getTaskId();
        String sampleId = countData.getSampleId();

        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null) {
            if (StringUtils.hasText(analysisData.getMeasureDataId())) {
                analysisData.setId(null);
                analysisData.setDataVersion(analysisData.getDataVersion() + 1);
            }
            analysisData.setMeasureDataId(dataId);

            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
        } else {
            analysisData = new FMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisResult(JSON.toJSONString(countData.getCountData()));
            analysisData.setDataVersion(0);
            analysisData.setMeasureDataId(dataId);
        }
        analysisData.setIsDelete(createType);
        analysisData.setIsSave(noSave);
        fMeasureAnalysisDataService.saveOrUpdate(analysisData);
        return R.ok(true);

    }

    @Override
    public Result saveDataInfo(FSaveDataVO vo) {
        if (!StringUtils.hasText(vo.getTaskId())) {
            return Result.fail("请创建任务");
        }

        if (!StringUtils.hasText(vo.getSampleId())) {
            return Result.fail("缺少样品id");
        }

        if (vo.getData() == null || vo.getData().isEmpty()) {
            return Result.ok();
        }
        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();
        LambdaQueryWrapper<FMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getDataVersion).last("limit 1");

        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(queryWrapper);
        if (analysisData != null) {
            Map<String, String> jsonObject;
            if (StringUtils.hasText(analysisData.getAnalysisParam())) {
                jsonObject = JSON.parseObject(analysisData.getAnalysisParam(), new TypeReference<Map<String, String>>(){});
            } else {
                jsonObject = vo.getData();
            }
            analysisData.setAnalysisParam(JSON.toJSONString(jsonObject));
            fMeasureAnalysisDataService.updateById(analysisData);
        } else {
            analysisData = new FMeasureAnalysisData();
            analysisData.setTaskId(taskId);
            analysisData.setSampleKey(sampleId);
            analysisData.setAnalysisParam(JSON.toJSONString(vo.getData()));
            analysisData.setDataVersion(0);
            analysisData.setIsDelete(createType);
            fMeasureAnalysisDataService.save(analysisData);
        }

        return Result.ok(true);
    }

    /**
     * 零点校零
     * @param deviceSerialNumber
     * @param methodId
     * @return
     */
    @Override
    public Result zero(String deviceSerialNumber, String methodId, String taskId,String updateBy) {

        if (!StringUtils.hasText(deviceSerialNumber)) {
            return Result.fail("未连接仪器");
        }

        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(taskId)){
            //创建任务

            //创建任务
            FMeasureTask newTask = new FMeasureTask();

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
                if (fMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            if (!save) {
                return Result.fail("系统出错");
            }
            taskId = newTask.getId();
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        FPhotometricParameter photometricParameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
        if (photometricParameter.getWaveLength() == null || photometricParameter.getWaveLength().isEmpty()) {
            return Result.fail("缺少波长");
        }
        List<String> waveLength = photometricParameter.getWaveLength();
        // 通过序列号获取ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        List<ChangeParam> list = new ArrayList<>();
        for (String s : waveLength) {

            double v = Double.parseDouble(s);
            Command command = new Command(SetParamEnum.SetWl.getComId(), new Object[]{v});
            devMessageUtil.wavelengthLocate(command, ip);

            command = new Command(SetParamEnum.Zero.getComId(), new Object[]{});
            R<ChangeParam> zero = devMessageUtil.zero(command, ip);
            if (!zero.getSuccess()) {
                return Result.fail(zero.getMessage());
            }
            ChangeParam changeParam = zero.getData();
            changeParam.setWl(s);
            list.add(changeParam);
        }
        task.setZeroParam(JSON.toJSONString(list));

        boolean update = fMeasureTaskService.updateById(task);
        return update ? Result.ok() : Result.fail();
    }

    @Override
    public Result<Boolean> saveButton(String taskId, String updateBy) {
        if (!StringUtils.hasText(taskId)) {
            return Result.fail("未创建任务，无法保存");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        SysUserPO po = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        String id = po.getId();
        task.setUserId(id);
        task.setIsDelete("1");
        fMeasureTaskService.updateById(task);
        return Result.ok(true);
    }

    @Override
    public Result<FMeasurementCurrentInfoVO> getInfoByTaskId(String taskId) {
        if (!StringUtils.hasText(taskId)){
            return Result.fail("缺少任务id");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        FMeasurementCurrentInfoVO measurementCurrentInfoVO = new FMeasurementCurrentInfoVO();
        measurementCurrentInfoVO.setTaskId(taskId);
        measurementCurrentInfoVO.setTaskName(task.getTaskName());
        measurementCurrentInfoVO.setMethodId(task.getMethodKey());
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        measurementCurrentInfoVO.setMethodName(method.getMethodName());
//        measurementCurrentInfoVO.setProjectId(task.getProjectItemKey());
        FMeasureProject project = fMeasureProjectService.getById(task.getProjectItemKey());
//        measurementCurrentInfoVO.setProjectName(project.getProjectName());

        return Result.ok(measurementCurrentInfoVO);
    }

    @Override
    public R<File> downloadWord(String taskId, Boolean allDisplay, Integer digit) {
        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }

        String fileName = task.getTaskName() + ".docx";

        String methodId = task.getMethodKey();
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        List<FHeadVO> heads = new ArrayList<>();

        heads.add(new FHeadVO("样品编号", "sampleNumber"));
        heads.add(new FHeadVO("样品名称", "sampleName"));
        heads.add(new FHeadVO("样品类型", "sampleType"));
        heads.add(new FHeadVO("稀释比", "dilutionRatio"));
        heads.add(new FHeadVO("样品体积", "sampleVolume"));
        heads.add(new FHeadVO("样品位置", "sampleLocation"));

        List<FHeadVO> vos = tableHead(taskId, methodId, allDisplay, null).getData();
        heads.addAll(vos);
        heads.add(new FHeadVO("备注", "remark"));
        heads.add(new FHeadVO("采样时间", "samplingTime"));

        FQueryDataDTO queryDataDTO = new FQueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        queryDataDTO.setDigit(digit);

        List<Map<String, Object>> data = queryData(queryDataDTO).getData();


        XWPFDocument doc = new XWPFDocument();
        CTDocument1 document = doc.getDocument();
        CTBody body = document.getBody();
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        CTSectPr section = body.getSectPr();
        if(!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        CTPageSz pageSize = section.getPgSz();
        if (vos.size() > 5) {
            //必须要设置下面两个参数，否则整个的代码是无效的
            pageSize.setW(BigInteger.valueOf(15840));
            pageSize.setH(BigInteger.valueOf(12240));
        }

        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("任务：" + task.getTaskName() + "\r" );
        run.setText("方法：" + method.getMethodName() + "\r");
        run.setText("测试数据：");
        XWPFTable table = doc.createTable();

        table.setWidth("100%");
        int i = 0;
        for (Map<String, Object> map : data) {
            XWPFTableRow headerRow = table.getRow(0);
            if(headerRow != null) {
                XWPFTableCell cell = headerRow.getCell(0);
                if(cell != null) {
                    cell.setText("序号");
                }

                XWPFTableRow row = table.createRow();
                int m = 1;
                for (FHeadVO head : heads) {

                    String cellData = String.valueOf(map.get(head.getIdentity()));

                    if (head.getIdentity().equals("sampleType")) {
                        cellData = SampleTypeEnum.getName(cellData);
                    }
                    if (head.getIdentity().equals("sampleLocation")) {
                        cellData = "样品"+cellData;
                    }
                    if (i == 0) {
                        headerRow.addNewTableCell().setText(head.getName());
                        row.addNewTableCell().setText(cellData);
                        row.getCell(0).setText(String.valueOf(1));
                    } else {
                        row.getCell(m).setText(cellData);
                        row.getCell(0).setText(String.valueOf(i+1));
                        m++;
                    }
                }
                i++;
            }
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    XWPFParagraph paragraph1=cell.getParagraphArray(0);
                    paragraph1.setAlignment(ParagraphAlignment.CENTER);
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                }
            }
        }

        try {
            FileOutputStream out = new FileOutputStream(fileName);
            doc.write(out);
            out.close();
            return R.ok(new File(fileName));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<List<FInputParam>> queryInputParam(String methodId) {
        if (!StringUtils.hasText(methodId)) {
            return R.fail("未选择方法");
        }
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail("方法不存在");
        }
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);
        List<FInputParam> inputParams = parameter.getSampleInputParams();

        return R.ok(inputParams);
    }

    @Override
    public R<List<FInputParamMapTableHead>> queryInputParamMapTableHead(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            return R.fail("未选择任务");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return R.fail("任务不存在");
        }
        List<FInputParamMapTableHead> ans = new ArrayList<>();
        if (task.getTableInfo() != null) {
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            ans = snapshot.getMapRelation();
        }
        return R.ok(ans);

    }

    @Override
    public R<Boolean> saveInputParamMapTableHead(FTaskHeadSnapshotDTO snapshotDTO) {

        if (!StringUtils.hasText(snapshotDTO.getTaskId())) {
            return R.fail("未选择任务");
        }

        FMeasureTask task = fMeasureTaskService.getById(snapshotDTO.getTaskId());
        if (task == null) {
            return R.fail("任务不存在");
        }

        FTaskHeadSnapshot taskHeadSnapshot = new FTaskHeadSnapshot();
        List<FHeadVO> taskHeads = new ArrayList<>();
        List<FHeadVO> sampleHeads = new ArrayList<>();
        for (FHeadVO head : snapshotDTO.getTaskHeads()) {
            if (head.getLocate().equals(TableHeadILocateEnums.sample.getIdentity()) || head.getLocate().equals(TableHeadILocateEnums.system.getIdentity())) {
                sampleHeads.add(head);
            } else {
                taskHeads.add(head);
            }
        }
        BeanUtils.copyProperties(snapshotDTO, taskHeadSnapshot);
        taskHeadSnapshot.setTaskHeads(taskHeads);
        taskHeadSnapshot.setSampleHeads(sampleHeads);

        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));

        return fMeasureTaskService.updateById(task) ? R.ok(true) : R.fail("保存失败");
    }

    /**
     * 多格式文件导出（支持word/txt/xlsx）
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, Integer digit) {
        log.info("开始导出，taskId: {}, fileType: {}", taskId, fileType);
        // 1. 基础参数校验（复用原逻辑，无修改）
        if (!StringUtils.hasText(taskId)) {
            return R.fail("缺少任务id");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            log.error("导出失败：任务不存在，taskId: {}", taskId);
            return R.fail("任务不存在");
        }
        // 处理fileType默认值和合法性（复用原逻辑）
        if (!StringUtils.hasText(fileType)) {
            fileType = "word"; 
        } else {
            fileType = fileType.toLowerCase();
        }
        if (!"word".equals(fileType) && !"txt".equals(fileType) && !"xlsx".equals(fileType)) {
            return R.fail("不支持的导出格式，仅支持word/txt/xlsx");
        }
        if (allDisplay == null) {
            allDisplay = true;
        }

        // 2. 准备表头和数据（重点修改：空数据处理）
        String methodId = task.getMethodKey();
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        if (method == null) {
            return R.fail("测量方法不存在");
        }

        // 构建表头（与原逻辑一致，先加动态表头，再在第0位加序号列）
        List<FHeadVO> heads = new ArrayList<>();
        List<FHeadVO> vos = tableHead(taskId, methodId, allDisplay, null).getData();
        heads.addAll(vos);
        heads.add(0, new FHeadVO("序号", "serialNumber")); // 序号列固定在第0位

        // 3. 查询数据（关键修改：空数据时构造“空行”）
        FQueryDataDTO queryDataDTO = new FQueryDataDTO();
        queryDataDTO.setOption(new ArrayList<>());
        queryDataDTO.setTaskId(taskId);
        queryDataDTO.setAllDisplay(allDisplay);
        queryDataDTO.setDigit(digit);
        List<Map<String, Object>> data = queryData(queryDataDTO).getData();

        // === 核心调整：无数据时手动构造“空行”（与表头列数匹配） ===
        if (data == null || data.isEmpty()) {
            log.warn("无实际数据，构造空行导出，taskId: {}", taskId);
            data = new ArrayList<>(); // 初始化空列表，避免后续NPE
            Map<String, Object> emptyRow = new HashMap<>();
            // 1. 序号列设为1（空数据时仅1行空数据，序号为1）
            emptyRow.put("serialNumber", 1);
            // 2. 为所有动态表头（除序号列）设置空值（确保列数与表头一致）
            for (FHeadVO head : vos) { // vos是动态表头（不含序号列）
                if (head != null && StringUtils.hasText(head.getIdentity())) {
                    emptyRow.put(head.getIdentity(), ""); // 空字符串占位，避免单元格空指针
                }
            }
            data.add(emptyRow); // 将空行加入数据列表
        } else {
            // 有数据时正常加序号（原逻辑保留）
            for (int i = 0; i < data.size(); i++) {
                data.get(i).put("serialNumber", i + 1);
            }
        }

        // 4. 按格式生成文件（原逻辑保留，无需修改）
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
            return R.ok(file); // 无论有无数据，只要文件生成成功就返回200
        } catch (Exception e) {
            log.error("文件生成失败", e);
            if (file.exists()) {
                file.delete();
            }
            return R.fail("文件生成失败：" + e.getMessage());
        }
    }


    // ---------------------- 新增：三格式文件生成具体实现 ----------------------
    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, FMeasureTask task, FMeasureMethod method,
                             List<FHeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        CTDocument1 document = doc.getDocument();
        CTBody body = document.getBody();
        if (body == null) { // 防止body为null，主动创建
            body = document.addNewBody();
        }

        // 页面设置
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        CTSectPr section = body.getSectPr();
        if (section == null) {
            section = body.addNewSectPr();
        }
        if (!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        CTPageSz pageSize = section.getPgSz();
        if (vosSize > 5) {
            pageSize.setW(BigInteger.valueOf(15840));
            pageSize.setH(BigInteger.valueOf(12240));
        } else {
            pageSize.setW(BigInteger.valueOf(11906)); // 默认A4宽度
            pageSize.setH(BigInteger.valueOf(16838)); // 默认A4高度
        }

        // 添加标题（处理task/method为null的情况，避免空指针）
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        String taskName = (task != null && task.getTaskName() != null) ? task.getTaskName() : "未知任务";
        String methodName = (method != null && method.getMethodName() != null) ? method.getMethodName() : "未知方法";
        run.setText("任务：" + taskName + "\r");
        run.setText("方法：" + methodName + "\r");
        run.setText("测试数据：");

        // 表格创建（校验表头非空，避免列数异常）
        if (heads == null || heads.isEmpty()) {
            throw new IllegalArgumentException("表头列表为空，无法创建Word表格");
        }
        int columnCount = heads.size();
        XWPFTable table = doc.createTable(1, columnCount);
        table.setWidth("100%");

        // 填充表头（处理表头为null的情况）
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < columnCount; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            if (cell == null) {
                cell = headerRow.createCell();
            }
            FHeadVO head = heads.get(i);
            String headName = (head != null && head.getName() != null) ? head.getName() : "未命名列";
            cell.setText(headName);
        }

        // 填充数据行（核心修复：sampleType空值判断、sampleLocation前缀移除）
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据列表为空，无法生成Word内容");
        }
        int rowIndex = 0;
        for (Map<String, Object> map : data) {
            XWPFTableRow row = table.createRow();
            rowIndex++;
            // 第0列：序号
            row.getCell(0).setText(String.valueOf(rowIndex));

            // 填充其他列（与表头顺序对应）
            for (int i = 1; i < columnCount; i++) {
                FHeadVO head = heads.get(i);
                String identity = head.getIdentity();
                
                // 1. 获取原始值（不做默认值替换）
                Object originalValue = map.get(identity);
                
                // 2. 处理空值：null或空字符串用'-'代替，其他值保持原始
                String cellData;
                if (originalValue == null) {
                    // 原始值为null时用'-'
                    cellData = "-";
                } else {
                    // 转换为字符串
                    String originalStr = originalValue.toString();
                    // 空字符串或"null"用'-'，其他值保持原始
                    cellData = originalStr.trim().isEmpty() || "null".equals(originalStr) ? "-" : originalStr;
                }

                // 3. 写入单元格
                row.getCell(i).setText(cellData);
            }
        }

        // 表格样式（处理段落为null的情况）
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                XWPFParagraph paragraph1 = cell.getParagraphArray(0);
                if (paragraph1 == null) { // 若段落为null，通过单元格的段落列表获取（POI正确用法）
                    List<XWPFParagraph> paras = cell.getParagraphs();
                    if (paras == null || paras.isEmpty()) {
                        paragraph1 = cell.addParagraph(); // 无段落时新增
                    } else {
                        paragraph1 = paras.get(0);
                    }
                }
                paragraph1.setAlignment(ParagraphAlignment.CENTER);
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        // 写入文件
        if (file == null) {
            throw new IllegalArgumentException("文件对象为null，无法写入Word");
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.write(out);
        } finally {
            doc.close();
        }
    }

    /**
     * 生成TXT文件（UTF-8编码，制表符分隔）
     */
    private void generateTxtFile(File file, List<FHeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 使用BufferedWriter保证效率，UTF-8避免中文乱码
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // 1. 写入表头（制表符分隔，处理空表头）
            String headerLine = heads.stream()
                    .map(head -> StringUtils.hasText(head.getName()) ? head.getName() : "未命名列") // 表头空值保护
                    .collect(Collectors.joining("\t"));
            writer.write(headerLine);
            writer.newLine(); // 换行

            // 2. 写入数据（与表头顺序一致，仅空值用"-"，其他保留原始值）
            for (Map<String, Object> map : data) {
                List<String> rowData = new ArrayList<>();
                for (FHeadVO head : heads) {
                    String identity = head.getIdentity();
                    // 1. 获取原始值（不默认替换为空字符串，保留null）
                    Object originalValue = map.get(identity);
                    String cellData;

                    // 2. 空值处理：null/空字符串/"null" 统一替换为"-"
                    if (originalValue == null) {
                        cellData = "-";
                    } else {
                        // 原始值转字符串（不做任何额外格式化）
                        String originalStr = originalValue.toString();
                        // 处理空字符串或"null"（不区分大小写）
                        cellData = originalStr.trim().isEmpty() || "null".equalsIgnoreCase(originalStr) 
                                ? "-" 
                                : originalStr;
                    }

                    // 3. 清除TXT列分裂风险的特殊字符（制表符/换行符，不修改原始内容）
                    cellData = cellData.replace("\t", " ").replace("\r", "").replace("\n", "");
                    rowData.add(cellData);
                }
                // 制表符分隔列，写入一行
                writer.write(String.join("\t", rowData));
                writer.newLine();
            }
        }
    }

    /**
     * 简化版：生成 XLSX 文件（仅导出数据，无复杂样式，空值用"-"）
     */
    private void generateExcelFile(File file, List<FHeadVO> heads, List<Map<String, Object>> data) throws Exception {
        // 1. 转换表头（处理空表头，避免显示异常）
        List<List<String>> excelHeads = new ArrayList<>();
        for (FHeadVO head : heads) {
            List<String> singleHead = new ArrayList<>();
            // 表头空值时显示“未命名列”，避免Excel表头为空
            singleHead.add(StringUtils.hasText(head.getName()) ? head.getName() : "未命名列");
            excelHeads.add(singleHead);
        }

        // 2. 转换数据（核心：保留原始值，仅空值用"-"）
        List<List<Object>> excelData = new ArrayList<>();
        for (Map<String, Object> map : data) {
            List<Object> rowData = new ArrayList<>();
            for (FHeadVO head : heads) {
                String identity = head.getIdentity();
                // 1. 获取原始值（不预设默认值，完全保留数据源状态）
                Object originalValue = map.get(identity);
                String cellData;

                // 2. 空值处理：null/空字符串/"null" 统一替换为"-"
                if (originalValue == null) {
                    cellData = "-";
                } else {
                    // 原始值转字符串（不做枚举转换、前缀拼接等额外处理）
                    cellData = originalValue.toString();
                    // 清除Excel解析异常的特殊字符（换行/制表符，不修改原始内容）
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

        // 3. 写入Excel（try-with-resources自动关闭流，避免资源泄漏）
        try (OutputStream out = new FileOutputStream(file)) {
            EasyExcel.write(out)
                    .head(excelHeads)
                    .sheet("测量数据") // Excel工作表名称
                    .doWrite(excelData);
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
