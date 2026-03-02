/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.spectrum.FParameterValidationDto;
import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumImageUpload;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TableHeadILocateEnums;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.common.FTaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.LinearSegment;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
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
import com.persee.workstation.uv_inexus.vo.concise.common.FSampleListVo;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSampleVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.spectrum.FSpectrumParameterVo;
import com.persee.workstation.uv_inexus.vo.concise.spectrum.SpectrumNearestMethodVo;
import com.persee.workstation.uv_inexus.vo.concise.spectrum.SpectrumSearchMethodVo;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
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

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Double.valueOf;

/**
 * 简洁模式: 光谱扫描服务业务层实现
 */

@Service("SpectrumConciseService")
@Slf4j
public class SpectrumConciseServiceImpl implements SpectrumConciseService {

    @Autowired
    private FMeasureMethodService fMeasureMethodService;

    @Autowired
    private FMeasureTaskService fMeasureTaskService;

    @Autowired
    private FMeasureSampleService fMeasureSampleService;

    @Autowired
    private FMeasureDataService fMeasureDataService;

    @Autowired
    private FMeasureAnalysisDataService fMeasureAnalysisDataService;
    @Autowired
    private FMeasureAnalysisImageService fMeasureAnalysisImageService;
    @Autowired
    private SamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    private FMeasureTaskAttachmentService fMeasureTaskAttachmentService;
    @Autowired
    private ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    private SystemUserManageDAO systemUserManageDAO;

    @Autowired
    private FMeasureTaskMapper fMeasureTaskMapper;

    @Autowired
    private com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO userManageDAO;


    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private FormulaUtil formulaUtil;

    @Autowired
    private PythonPathConstant pythonPathConstant;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DevOperationUtilService devOperationUtilService;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;

    private R<Boolean> checkParam(FSpectrumParameter parameter) {
        // 1. 根对象非空校验
        if (parameter == null) {
            //光谱扫描参数对象不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00303017"));
        }

        // 2. 光度模式校验（枚举值：1-4）
        String luminosity = parameter.getLuminosity();
        if (isBlank(luminosity)) {
            //光度模式不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00105001"));
        }
        if (!Arrays.asList("1", "2", "3", "4").contains(luminosity)) {
            //"光度模式不合法（可选值：1-abs、2-T%、3-Es、4-Er），当前值：" + luminosity
            return R.fail(YamlMessageSource.getMessageStatic("00104005", luminosity));
        }

        // 3. 开始波长校验（190-900，数字且格式正确）
        String startWave = parameter.getStartWaveLength();
        if (isBlank(startWave)) {
            //开始波长不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00104006"));
        }
        try {
            double startVal = Double.parseDouble(startWave);
            if (startVal < 190 || startVal > 900) {
                //开始波长需在190-900范围内，当前值：
                return R.fail(YamlMessageSource.getMessageStatic("00104007", startVal));
            }
            // 校验小数点后位数（最多1位）
            if (startWave.contains(".") && startWave.split("\\.")[1].length() > 1) {
                //开始波长小数点后最多保留1位，当前值：
                return R.fail(YamlMessageSource.getMessageStatic("00104008", startWave));
            }
        } catch (NumberFormatException e) {
            //"开始波长格式错误（需为数字），当前值：" + startWave
            return R.fail(YamlMessageSource.getMessageStatic("00104009" + startWave));
        }

        // 4. 结束波长校验（190-900，数字且小于开始波长）
        String endWave = parameter.getEndWaveLength();
        if (isBlank(endWave)) {
            //结束波长不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010400A"));
        }
        try {
            double endVal = Double.parseDouble(endWave);
            if (endVal < 190 || endVal > 900) {
                //"结束波长需在190-900范围内，当前值：" + endVal
                return R.fail(YamlMessageSource.getMessageStatic("0010400B", endVal));
            }
            // 校验小数点后位数（最多1位）
            if (endWave.contains(".") && endWave.split("\\.")[1].length() > 1) {
                //"结束波长小数点后最多保留1位，当前值：" + endWave
                return R.fail(YamlMessageSource.getMessageStatic("0010400C", endWave));
            }
            // 校验开始波长 > 结束波长
            double startVal = Double.parseDouble(startWave);
            if (startVal <= endVal) {
                //"开始波长必须大于结束波长（开始：" + startVal + "，结束：" + endVal + "）"
                return R.fail(YamlMessageSource.getMessageStatic("0010400D", startVal, endVal));
            }
        } catch (NumberFormatException e) {
            //结束波长格式错误（需为数字），当前值：endWave
            return R.fail(YamlMessageSource.getMessageStatic("0010400E", endWave));
        }

        // 5. 波长间隔校验（枚举值：0.025,0.05,0.1,0.2,0.5,1.0,2.0,5.0）
        String waveInterval = parameter.getWaveLengthInterval();
        if (isBlank(waveInterval)) {
            //波长间隔不能为空
            return R.fail(YamlMessageSource.getMessageStatic("0010400F"));
        }
        List<String> validIntervals = Arrays.asList("0.025", "0.05", "0.1", "0.2", "0.5", "1.0", "2.0", "5.0");
        if (!validIntervals.contains(waveInterval)) {
            //"波长间隔不合法（可选值：" + String.join("、", validIntervals) + "），当前值：" + waveInterval
            return R.fail(YamlMessageSource.getMessageStatic("00104010", String.join("、", validIntervals), waveInterval));
        }

        // 6. 重复模式校验（枚举值：0-不重复、1-单池重复）
        String repeatPattern = parameter.getRepetitionPattern();
        if (isBlank(repeatPattern)) {
            //重复模式不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00103007"));
        }
        if (!Arrays.asList("0", "1").contains(repeatPattern)) {
            //"重复模式不合法（可选值：0-不重复、1-单池重复），当前值：" + repeatPattern
            return R.fail(YamlMessageSource.getMessageStatic("00104011", repeatPattern));
        }

        // 7. 单池重复（1）时的关联校验
        if ("1".equals(repeatPattern)) {
            // 7.1 重复次数（2-10的整数）
            String repeatTime = parameter.getRepetitionTime();
            if (isBlank(repeatTime)) {
                //单池重复模式下，重复次数不能为空
                return R.fail(YamlMessageSource.getMessageStatic("00102017"));
            }
            try {
                int repeatCount = Integer.parseInt(repeatTime);
                if (repeatCount < 2 || repeatCount > 10) {
                    //重复次数需在2-10范围内，当前值：repeatCount
                    return R.fail(YamlMessageSource.getMessageStatic("00102018", repeatCount));
                }
            } catch (NumberFormatException e) {
                //重复次数格式错误（需为整数），当前值：repeatTime
                return R.fail(YamlMessageSource.getMessageStatic("00102019", repeatTime));
            }

            // 7.2 时间间隔（0-60秒，保留1位小数）
            String timeInterval = parameter.getTimeInterval();
            if (isBlank(timeInterval)) {
                //单池重复模式下，时间间隔不能为空
                return R.fail(YamlMessageSource.getMessageStatic("0010201A"));
            }
            try {
                double intervalVal = Double.parseDouble(timeInterval);
                if (intervalVal < 0 || intervalVal > 60) {
                    //时间间隔需在0-60秒范围内，当前值：intervalVal
                    return R.fail(YamlMessageSource.getMessageStatic("0010201B", intervalVal));
                }
                // 校验小数点后位数（最多1位）
                if (timeInterval.contains(".") && timeInterval.split("\\.")[1].length() > 1) {
                    //"时间间隔小数点后最多保留1位，当前值：" + timeInterval
                    return R.fail(YamlMessageSource.getMessageStatic("00104012", timeInterval));
                }
            } catch (NumberFormatException e) {
                //时间间隔格式错误（需为数字），当前值：
                return R.fail(YamlMessageSource.getMessageStatic("0010201D", timeInterval));
            }
        }

        // 8. 扫描速度校验（枚举值：0-3）
        String scanSpeed = parameter.getScanSpeed();
        if (isBlank(scanSpeed)) {
            //扫描速度不能为空
            return R.fail(YamlMessageSource.getMessageStatic("00104013"));
        }
        if (!Arrays.asList("0", "1", "2", "3").contains(scanSpeed)) {
            //"扫描速度不合法（可选值：0-慢速、1-中速、2-快速、3-极快），当前值：" + scanSpeed
            return R.fail(YamlMessageSource.getMessageStatic("00104014", scanSpeed));
        }


        // 所有校验通过
        return R.ok(true);
    }

    // 内部工具方法：判断字符串是否为空（适配JDK1.8）
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 查询最近方法数据
     *
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryNearestMethodData(String updateBy, String measureType) {
        if (updateBy == null) {
            //更新人不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00100009"));
        }

        if (measureType == null) {
            //方法类型不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00100006"));
        }
        SysUserPO sysUserPO = userManageDAO.selectByUsername(updateBy);
        if (sysUserPO == null) {
            //更新人不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0020400A"));
        }
        SpectrumNearestMethodVo vo = new SpectrumNearestMethodVo();

        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", updateBy);
        wrapper.eq("measurement_type", measureType);
        wrapper.orderByDesc("method_use_time");
        wrapper.last("LIMIT 1");

        String id = sysUserPO.getId();
        FMeasureMethod method = fMeasureMethodService.getOne(wrapper);

        if (method == null) {
            // 查询对应类型的默认方法
            QueryWrapper<FMeasureMethod> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("measurement_type", measureType);
            queryWrapper.eq("method_name", "默认方法");
            queryWrapper.last("LIMIT 1");
            method = fMeasureMethodService.getOne(queryWrapper);
            if (method == null) {
                //没有默认方法
                return Result.fail(YamlMessageSource.getMessageStatic("00100007"));
            }
        }

        // 根据方法id查询出最近创建的任务
        QueryWrapper<FMeasureTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("method_key", method.getId());
        taskWrapper.eq("is_delete", "0");
        taskWrapper.eq("user_id", id);
        taskWrapper.orderByDesc("create_time");
        taskWrapper.last("LIMIT 1");
        FMeasureTask task = fMeasureTaskService.getOne(taskWrapper);

        BeanUtils.copyProperties(method, vo);
        FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        FSpectrumParameterVo parameterVo = new FSpectrumParameterVo();
        String s = "1".equals(parameter.getLuminosity()) ? "Abs" : "2".equals(parameter.getLuminosity()) ? "T%" : "3".equals(parameter.getLuminosity()) ? "Es" : "Er";
        BeanUtils.copyProperties(parameter, parameterVo);
        parameterVo.setLuminosityName(s);
        if (parameter.getLadderCorrection() != null) {
            parameterVo.setLadderCorrectionName(parameter.getLadderCorrection() ? "启用" : "不启用");
        }
        if (parameter.getScanSpeed() != null) {
            String s1 = "0".equals(parameter.getScanSpeed()) ? "慢速" : "1".equals(parameter.getScanSpeed()) ? "中速" : "2".equals(parameter.getScanSpeed()) ? "快速" : "极快";
            parameterVo.setScanSpeedName(s1);
        }
        if (parameter.getEnergyLamp() != null) {
            // 0.钨灯,1.氘灯,2.汞灯
            String lamp = parameter.getEnergyLamp().equals("0") ? "钨灯" : parameter.getEnergyLamp().equals("1") ? "氘灯" : "汞灯";
            parameterVo.setEnergyLampName(lamp);
        }
        vo.setMethodParam(parameterVo);
        // 将创建时间从LocalDateTime转换成String并且将里面的T 去掉
        String format = method.getMethodCreateTime().format(formatter);
        vo.setMethodCreateTime(format);
        String format1 = method.getMethodUseTime().format(formatter);
        vo.setMethodUseTime(format1);
        vo.setMethodId(method.getId());
        if (task != null) {
            vo.setTaskId(task.getId());
        }
        if (Objects.isNull(task)) {

            FMeasureTask fMeasureTask = new FMeasureTask();


            fMeasureTask.setCreateTime(LocalDateTime.now());
            fMeasureTask.setMethodKey(vo.getMethodId());
            fMeasureTask.setMeasureType(measureType);
            fMeasureTask.setEndSaveTime(LocalDateTime.now());
            fMeasureTask.setIsSave("1");
            fMeasureTask.setIsDelete(createType);

            fMeasureTask.setUserId(id);
            fMeasureTask.setTaskName(vo.getMethodName() + "文件数据");

            boolean save = fMeasureTaskService.save(fMeasureTask);
            vo.setTaskId(fMeasureTask.getId());

        }

        return Result.ok(vo);
    }

    /**
     * 查找方法按钮
     *
     * @param pageNum
     * @param pageSize
     * @param updateBy
     * @param measureType
     * @param methodName
     * @return
     */
    @Override
    public Result searchMethodButton(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName) {
        // 对传入的参数进行校验
        if (updateBy == null) {
            //用户不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00101002"));
        }
        if (measureType == null) {
            //方法类型不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00100006"));
        }
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        // 设置查询条件
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", updateBy);
        wrapper.eq("measurement_type", measureType);
        if (methodName != null) {
            wrapper.like("method_name", methodName);
        }
        // 分页查询
        Page<TMeasureMethod> page = tMeasureMethodService.page(new Page<>(pageNum, pageSize), wrapper);
        SpectrumSearchMethodVo vo = new SpectrumSearchMethodVo();
        vo.setTotal(page.getTotal());
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        List<SpectrumNearestMethodVo> methodVos = new ArrayList<>();
        for (TMeasureMethod record : page.getRecords()) {
            SpectrumNearestMethodVo methodVo = new SpectrumNearestMethodVo();
            BeanUtils.copyProperties(record, methodVo);
            // 将创建时间从LocalDateTime转换成String并且将里面的T 去掉
            String format = record.getMethodCreateTime().format(formatter);
            methodVo.setMethodCreateTime(format);

            SpectrumParameter parameter = JSON.parseObject(record.getMeasurementParamInfo(), SpectrumParameter.class);
            FSpectrumParameterVo parameterVo = new FSpectrumParameterVo();
            BeanUtils.copyProperties(parameter, parameterVo);
            String s = "1".equals(parameter.getLuminosity()) ? "Abs" : "2".equals(parameter.getLuminosity()) ? "T%" : "3".equals(parameter.getLuminosity()) ? "Es" : "Er";
            parameterVo.setLuminosityName(s);
            if (parameter.getLadderCorrection() != null) {
                parameterVo.setLadderCorrectionName(parameter.getLadderCorrection() ? "启用" : "不启用");
            }
            if (parameter.getScanSpeed() != null) {
                String s1 = "0".equals(parameter.getScanSpeed()) ? "慢速" : "1".equals(parameter.getScanSpeed()) ? "中速" : "2".equals(parameter.getScanSpeed()) ? "快速" : "极快";
                parameterVo.setScanSpeedName(s1);
            }
            if (parameter.getEnergyLamp() != null) {
                // 0.钨灯,1.氘灯,2.汞灯
                String lamp = parameter.getEnergyLamp().equals("0") ? "钨灯" : parameter.getEnergyLamp().equals("1") ? "氘灯" : "汞灯";
                parameterVo.setEnergyLampName(lamp);
            }
            methodVo.setMethodParam(parameterVo);
            methodVos.add(methodVo);
        }
        vo.setRecords(methodVos);
        return Result.ok(vo);
    }

    /**
     * 查询方法详情
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result parameterValidation(FParameterValidationDto paramDto) {
        // 校验参数
        if (paramDto.getMethodId().isEmpty()) {
            //方法ID不能为空，请创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000021"));
        }
        if (paramDto.getMeasureType().isEmpty()) {
            //类型不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("0000004B"));
        }
        if (paramDto.getParameter().isEmpty()) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }

        // 1. 查数据库参数
        FMeasureMethod method = fMeasureMethodService.getById(paramDto.getMethodId());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        switch (paramDto.getMeasureType()) {
            case "光度测量":
//                dbParam = JSON.parseObject(dbParamJson, AbsorbanceParam.class);
//                newParam = paramDto.getParameter().toJavaObject(AbsorbanceParam.class);
//                break;
            case "定量测量":
//                dbParam = JSON.parseObject(dbParamJson, TransmittanceParam.class);
//                newParam = paramDto.getParameter().toJavaObject(TransmittanceParam.class);
//                break;
            case "光谱扫描": {
                FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
                FSpectrumParameter newParameter = JSON.parseObject(String.valueOf(paramDto.getParameter()), FSpectrumParameter.class);
                // 1. 比较参数
                boolean sameParam = compareParam(parameter, newParameter);
                return Result.ok(sameParam);
            }
            case "时间扫描":
//            {
//                FTimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
//                FTimeSweepParamter newParameter =  JSON.parseObject(String.valueOf(paramDto.getParameter()), FTimeSweepParamter.class);
//                // 1. 比较参数
//                boolean sameParam = compareParam(parameter, newParameter);
//                return Result.ok(sameParam);
//            }
            default:
                return Result.fail(YamlMessageSource.getMessageStatic("0000000D"));//未知的测量类型
        }

    }

    /**
     * 应用接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result use(FUseMethodDto paramDto) {
        // 先进行非空校验
        Result validateResult = validateSpectrumParam(paramDto.getParameter());
        if (validateResult.getCode() != 200) {
            return validateResult;
        }
        if (paramDto.getParameter().getLuminosity().equals("3") || paramDto.getParameter().getLuminosity().equals("4")) {
            paramDto.getParameter().setLadderCorrection(false);
        }
        boolean update1 = Double.parseDouble(paramDto.getParameter().getWaveLengthInterval()) < 1;
        if (paramDto.getParameter().getScanSpeed().equals("3") && update1) {
            return Result.fail("极快扫描速度时,扫描间隔不能小于1秒");
        }
        double start = Double.parseDouble(paramDto.getParameter().getStartWaveLength());
        double end = Double.parseDouble(paramDto.getParameter().getEndWaveLength());
        // 校验方法参数中起始波长不能小于终止波长,小于则友好提示
        if (start <= end) {
            //起始波长不能小于等于停止波长
            return Result.fail(YamlMessageSource.getMessageStatic("00104015"));
        }
        // 校验幻灯波长在330~390之间
        double changeWavelength = Double.parseDouble(paramDto.getParameter().getChangeLampWaveLength());
        if (changeWavelength < 330.0 || changeWavelength > 390.0) {
            return Result.fail("幻灯波长在330~390之间");
        }
        if (paramDto.getParameter().getScanSpeed().equals("3") && (start > 1100 || start < 380)) {
            return Result.fail("极快扫描速度时,波长区间应在1100.0~380.0");
        }
        // 下发参数
        SetParam setParam = new SetParam();
        setParam.setPhotoMode(Integer.valueOf(paramDto.getParameter().getLuminosity()));
        setParam.setSetWl(Double.valueOf(paramDto.getParameter().getStartWaveLength()));
        boolean b = setInstrumentParameters(paramDto.getDeviceSerialNumber(), setParam);
        if (!b) {
            return Result.fail("设置参数失败");
        }

        // 判断参数是否发生变化，如果没有变化，直接返回，不再执行后续操作
        if (paramDto.getMethodId() != null && (paramDto.getMethodId2() == null || paramDto.getMethodId().equals(paramDto.getMethodId2()))) {
            FMeasureMethod currentMethod = fMeasureMethodService.getById(paramDto.getMethodId());
            if (currentMethod != null) {
                // 1. 获取数据库中的旧参数
                FSpectrumParameter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), FSpectrumParameter.class);

                // 2. 获取传入的新参数
                FSpectrumParameter newParam = new FSpectrumParameter();
                BeanUtils.copyProperties(paramDto.getParameter(), newParam);

                // 3. 比较参数
                if (compareParam(oldParam, newParam)) {
                    // 参数未变，直接返回成功结果，跳过后续所有数据库更新/新建操作
                    Map<String, Object> map = new HashMap<>();
                    map.put("method", currentMethod);
                    Result draftButton = createDraftButton(currentMethod.getId(), null, paramDto.getUpdateBy(), paramDto.getDeviceSerialNumber());
                    if (draftButton.getCode() == 200) {
                        map.put("taskId", draftButton.getData());
                    }
                    return Result.ok(map);
                }
            }
        }

        Map<String, Object> map = new HashMap<>();
        if (paramDto.getMethodId2() != null) {
            FMeasureMethod method2 = fMeasureMethodService.getById(paramDto.getMethodId2());
            if (method2 == null) {
                //没有找到该方法
                return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
            }
            // 将方法的参数取出来
            FSpectrumParameter parameter2 = JSON.parseObject(method2.getMeasurementParamInfo(), FSpectrumParameter.class);
            FSpectrumParameter parameter = new FSpectrumParameter();
            BeanUtils.copyProperties(paramDto.getParameter(), parameter);
            boolean memberKeys = compareParam(parameter2, parameter);
            if (memberKeys) {
                if (method2.getId().equals(paramDto.getMethodId())) {
                    //请勿重复使用
                    return Result.fail(YamlMessageSource.getMessageStatic("00105016"));
                } else {
                    method2.setMethodUseTime(LocalDateTime.now());
                }
            } else {
                method2.setMethodUseTime(LocalDateTime.now());
            }
            boolean update = fMeasureMethodService.updateById(method2);
            if (!update) {
                //应用失败
                return Result.fail(YamlMessageSource.getMessageStatic("00100002"));
            }
            Result draftButton = createDraftButton(method2.getId(), null, paramDto.getUpdateBy(), paramDto.getDeviceSerialNumber());
            if (draftButton.getCode() == 200) {
                map.put("taskId", draftButton.getData());
            }
            map.put("method", method2);
            return Result.ok(map);
        }

        FMeasureMethod method = fMeasureMethodService.getById(paramDto.getMethodId());

        FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        FSpectrumParameter newParam = new FSpectrumParameter();
        // 将方法参数拷贝出来
        BeanUtils.copyProperties(paramDto.getParameter(), newParam);

        // 3. 参数比较
        boolean sameParam = compareParam(parameter, newParam);

        // 1. 判断来源
        if ("0".equals(paramDto.getType())) {
            if (sameParam) {
                //方法参数一致
                return Result.fail(YamlMessageSource.getMessageStatic("00104001"));
            }
            FMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail(YamlMessageSource.getMessageStatic("0010000A"));
            }
            map.put("method", newMethod);
            Result draftButton = createDraftButton(newMethod.getId(), null, paramDto.getUpdateBy(), paramDto.getDeviceSerialNumber());
            if (draftButton.getCode() == 200) {
                map.put("taskId", draftButton.getData());
            }
            return Result.ok(map);
        }

        // 2. 查询历史方法
        if (method == null) {
            FMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail(YamlMessageSource.getMessageStatic("0010000A"));
            }
            map.put("method", newMethod);
            Result draftButton = createDraftButton(newMethod.getId(), null, paramDto.getUpdateBy(), paramDto.getDeviceSerialNumber());
            if (draftButton.getCode() == 200) {
                map.put("taskId", draftButton.getData());
            }
            return Result.ok(map);
        }

        if (sameParam) {
            if (method.getId().equals(paramDto.getMethodId())) {
                //请勿重复使用
                return Result.fail(YamlMessageSource.getMessageStatic("00105016"));
            } else {
                map.put("method", method);
                Result draftButton = createDraftButton(method.getId(), null, paramDto.getUpdateBy(), paramDto.getDeviceSerialNumber());
                if (draftButton.getCode() == 200) {
                    map.put("taskId", draftButton.getData());
                }
                return Result.ok(map);
            }
        } else {
            FMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail(YamlMessageSource.getMessageStatic("0010000A"));
            }
            map.put("method", newMethod);
            Result draftButton = createDraftButton(newMethod.getId(), null, paramDto.getUpdateBy(), paramDto.getDeviceSerialNumber());
            if (draftButton.getCode() == 200) {
                map.put("taskId", draftButton.getData());
            }
            return Result.ok(map);
        }
    }

    /**
     * 保存数据接口
     *
     * @param taskId
     * @param isSave
     * @return
     */
    @Override
    public Result whetherSaveData(String taskId, Boolean isSave) {
        if (taskId == null) {
            //该方法下没有草稿,请新建草稿
            return Result.fail(YamlMessageSource.getMessageStatic("00100010"));
        }
        if (isSave) {
            // 根据任务id查询测量数据表的测量数据
            QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
            wrapper.eq("task_id", taskId);
            wrapper.eq("is_save", "1");
            wrapper.last("limit 1");
            List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
            if (measureDataList.isEmpty()) {
                return Result.ok();
            }
            for (FMeasureData measureData : measureDataList) {
                measureData.setIsSave("0");
                // 根据测量数据id和任务id查询分析数据
                QueryWrapper<FMeasureAnalysisData> wrapper1 = new QueryWrapper<>();
                wrapper1.eq("task_id", taskId);
                wrapper1.eq("measure_data_id", measureData.getId());
                wrapper1.last("limit 1");
                FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(wrapper1);
                analysisData.setIsSave("0");
                boolean update = fMeasureAnalysisDataService.updateById(analysisData);
                if (!update) {
                    //数据保存失败
                    return Result.fail(YamlMessageSource.getMessageStatic("00000034"));
                }
            }
            boolean update = fMeasureDataService.updateBatchById(measureDataList);
            // 根据taskid查询任务数据
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            task.setEndSaveTime(LocalDateTime.now());
            task.setIsSave("1");
            boolean update1 = fMeasureTaskService.updateById(task);
            if (update && update1) {
                return Result.ok();
            } else {
                //数据保存失败
                return Result.fail(YamlMessageSource.getMessageStatic("00000034"));
            }
        } else {
            // 根据任务id查询测量数据表
            QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
            wrapper.eq("task_id", taskId);
            wrapper.eq("is_save", "1");
            wrapper.last("limit 1");
            List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
            if (measureDataList.isEmpty()) {
                return Result.ok();
            }
            // 根据任务id查询任务数据
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            List<FTaskSpecimen> fTaskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FMeasureData measureData : measureDataList) {
                // 根据测量id任务id以及is_save字段为1的条件去分析数据表中查询数据
                QueryWrapper<FMeasureAnalysisData> wrapper1 = new QueryWrapper<>();
                wrapper1.eq("task_id", taskId);
                wrapper1.eq("measure_data_id", measureData.getId());
                wrapper1.eq("is_save", "1");
                wrapper1.last("limit 1");
                List<FMeasureAnalysisData> measureAnalysisList = fMeasureAnalysisDataService.list(wrapper1);
                boolean remove = false;
                if (measureAnalysisList != null) {
                    for (FMeasureAnalysisData measureAnalysis : measureAnalysisList) {
                        for (FTaskSpecimen fTaskSpeciman : fTaskSpecimen) {
                            if (fTaskSpeciman.getSpecimenKey().equals(measureAnalysis.getSampleKey())) {
                                fTaskSpeciman.setDeleteType("0");
                            }
                        }
                        // 删除当前分析数据
                        remove = fMeasureAnalysisDataService.removeById(measureAnalysis.getId());
                    }
                }

                // 删除当前测量数据
                boolean remove1 = fMeasureDataService.removeById(measureData.getId());
                if (!remove && !remove1) {
                    //删除失败
                    return Result.fail(YamlMessageSource.getMessageStatic("00100008"));
                }
            }
            task.setSampleKey(JSON.toJSONString(fTaskSpecimen));
            task.setEndSaveTime(LocalDateTime.now());
            task.setIsSave("1");
            boolean update = fMeasureTaskService.updateById(task);
            if (update) {
                //删除成功
                return Result.ok(YamlMessageSource.getMessageStatic("00100022"));
            } else {
                //删除失败
                return Result.fail(YamlMessageSource.getMessageStatic("00100008"));
            }
        }
    }

    /**
     * 创建草稿接口
     *
     * @param methodId
     * @return
     */
    @Override
    public Result createDraftButton(String methodId,
                                    String taskId,
                                    String updateBy,
                                    String deviceSerialNumber) {
        if (methodId == null) {
            //请先创建方法或者选择方法
            return Result.fail(YamlMessageSource.getMessageStatic("0010000B"));
        }
        // 根据方法id查询是否有任务使用该方法,查询条件分别是方法id,最近创建时间,只取一条,如果不存在则查询方法,使用方法名称+草稿凑成任务名称
        QueryWrapper<FMeasureTask> wrapper = new QueryWrapper<>();
        wrapper.eq("method_key", methodId);
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 1");
        FMeasureTask task = fMeasureTaskService.getOne(wrapper);
        FMeasureTask fMeasureTask = new FMeasureTask();
        SysUserPO sysUserPO = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        if (sysUserPO == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        String userId = sysUserPO.getId();
        if (task == null) {
            // 根据方法id查询方法名称,将名称+草稿赋值到任务名称
            FMeasureMethod method = fMeasureMethodService.getById(methodId);
            if (method == null) {
                //请先创建方法
                return Result.fail(YamlMessageSource.getMessageStatic("00000011"));
            }
            fMeasureTask.setTaskName(method.getMethodName() + "草稿");
        } else {
            fMeasureTask.setTaskName(task.getTaskName() + "草稿");
        }
        fMeasureTask.setUserId(userId);
        fMeasureTask.setMethodKey(methodId);
        fMeasureTask.setMeasureType(MeasureTypeEnum.SPECTRAL_SCAN.getDescription());
        fMeasureTask.setCreateTime(LocalDateTime.now());
        fMeasureTask.setIsSave("0");
        fMeasureTask.setIsDelete(createType);
        fMeasureTask.setDeviceSerialNumber(deviceSerialNumber);
        if (taskId != null) {
            // 根据taskId查询上一个草稿数据
            FMeasureTask task1 = fMeasureTaskService.getById(taskId);
            // 判断上一套草稿是否有校零参数
            if (task1.getZeroParam() != null) {
                fMeasureTask.setZeroParam(task1.getZeroParam());
            }
        }
        boolean save = fMeasureTaskService.save(fMeasureTask);
        return save ? Result.ok(fMeasureTask.getId()) : Result.fail();
    }


    /**
     * 查询样品列表按钮
     *
     * @param taskId
     * @return
     */
    @Override
    public Result querySampleListButton(String taskId) {
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
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

    /**
     * 查询样品表头
     * 该方法根据任务ID查询样品表头信息，如果任务ID为空或任务不存在或任务的表信息为空，
     * <p>
     * 则返回默认的光谱扫描类型的预定义表头
     *
     * @param taskId 任务ID，用于标识特定的测量任务
     * @return 返回一个Result对象，其中包含样品表头列表(FHeadVO)，
     * 如果任务不存在则返回错误信息
     */
    @Override
    public Result<List<FHeadVO>> querySampleHeads(String taskId) {

        // 检查任务ID是否为空
        if (!StringUtils.hasText(taskId)) {
            // 如果任务ID为空，获取光谱扫描类型的预定义表头并返回
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
            return Result.ok(preHead);
        }

        // 根据任务ID获取任务信息
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        // 检查任务是否存在
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        // 检查任务的表信息是否为空
        if (!StringUtils.hasText(task.getTableInfo())) {
            // 如果表信息为空，获取光谱扫描类型的预定义表头并返回
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
            return Result.ok(preHead);
        } else {
            // 如果表信息不为空，解析表信息为快照对象
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            // 检查快照中的样品表头是否存在且不为空
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                // 如果样品表头存在且不为空，直接返回
                return Result.ok(snapshot.getSampleHeads());
            } else {
                // 如果样品表头为空，获取光谱扫描类型的预定义表头并返回
                List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
                return Result.ok(preHead);
            }
        }
    }

    /**
     * 检测任务状态
     *
     * @param taskId
     * @return
     */
    @Override
    public Result detectionTaskStatus(String taskId) {
        if (taskId == null) {
            //任务id不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000003"));
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        return Result.ok(!task.getIsSave().equals("0"));
    }

    /**
     * 保存草稿按钮
     *
     * @param taskId
     * @return
     */
    @Override
    public Result saveDraftButton(String taskId, String taskName) {
        if (!StringUtils.hasText(taskId)) {
            //请先创建任务
            return Result.fail(YamlMessageSource.getMessageStatic("00101006"));
        }
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (taskName != null) {
            task.setTaskName(taskName);
        }
        // 根据任务id和is_save字段为1的条件查询
        QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId).eq("is_save", "1");
        List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
        if (measureDataList != null) {
            for (FMeasureData measureData : measureDataList) {
                measureData.setIsSave("0");
                boolean update1 = fMeasureDataService.updateById(measureData);
                if (!(update1)) {
                    //保存草稿失败
                    return Result.fail(YamlMessageSource.getMessageStatic("00100005"));
                }
            }
        }
        // 根据测量id,任务id还有is_save为1的条件查询分析数据
        QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("is_save", "1");
        List<FMeasureAnalysisData> analysisDataList = fMeasureAnalysisDataService.list(queryWrapper);
        if (analysisDataList != null) {
            for (FMeasureAnalysisData fMeasureAnalysisData : analysisDataList) {
                fMeasureAnalysisData.setIsSave("0");
                boolean update = fMeasureAnalysisDataService.updateById(fMeasureAnalysisData);
                if (!(update)) {
                    //保存草稿失败
                    return Result.fail(YamlMessageSource.getMessageStatic("00100005"));
                }
            }
        }
        task.setEndSaveTime(LocalDateTime.now());
        boolean update = fMeasureTaskService.updateById(task);
        if (update) {
            return Result.ok();
        } else {
            //保存草稿失败
            return Result.fail(YamlMessageSource.getMessageStatic("00100005"));
        }
    }

    /**
     * 添加或删除样品接口
     * <p>
     * 该方法用于处理样品的添加或删除操作，包括创建新任务、验证样品信息、更新样品数据等功能。
     *
     * @param dto 包含样品操作相关数据传输对象，包括方法ID、表头信息、样品列表等
     * @return 返回操作结果，成功返回Result.ok()，失败返回Result.fail()并附带错误信息
     */
    @Override
    public Result addOrDeleteSamplePort(FAddOrDeleteSampleDTO dto) {
        if (!StringUtils.hasText(dto.getMethodId())) {
            //请先创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000011"));
        }
        if (dto.getHeads() == null || dto.getHeads().isEmpty()) {
            //至少需要一个表头
            return Result.fail(YamlMessageSource.getMessageStatic("00000012"));
        }
        if (dto.getHeads().size() > 10) {
            //表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000013"));
        }

        // 获取方法信息，用于判断重复模式
        FMeasureMethod method = fMeasureMethodService.getById(dto.getMethodId());
        if (method == null) {
            // 方法不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // 将方法参数用json解析出来
        FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);

        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            FMeasureTask newTask = new FMeasureTask();

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
                //系统出错
                return Result.fail(YamlMessageSource.getMessageStatic("00000014"));
            }
            dto.setTaskId(newTask.getId());
        }

        FMeasureTask task = fMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        List<FTaskSpecimen> taskSpecimen = new ArrayList<>();
        if (StringUtils.hasText(task.getSampleKey())) {
            taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        }

        Map<String, FTaskSpecimen> oldMap = new HashMap<>();
        for (FTaskSpecimen specimen : taskSpecimen) {
            oldMap.put(specimen.getSpecimenKey(), specimen);
        }

        // 处理要删除的样品
        if (dto.getDelSampleIds() != null && !dto.getDelSampleIds().isEmpty()) {
            // 获取方法的重复模式（假设方法中有isRepeatMode字段，0-不重复，1-重复）
            Integer repeatMode = parameter.getRepetitionPattern().equals("单池重复") ? 1 : null; // 请根据实际情况调整字段名

            for (String delSampleId : dto.getDelSampleIds()) {
                FTaskSpecimen specimen = oldMap.get(delSampleId);
                if (specimen != null) {
                    specimen.setDeleteType(deleteType);

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
        com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskHeadSnapshot taskHeadSnapshot = new com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskHeadSnapshot();

        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskHeadSnapshot.class);
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
                return Result.fail(YamlMessageSource.getMessageStatic("0000004E"));
            }

            // 查重：找到重复的样品编号
            Set<String> duplicates = sampleNos.stream()
                    .filter(no -> Collections.frequency(sampleNos, no) > 1)
                    .collect(Collectors.toSet());

            if (!duplicates.isEmpty()) {
                //标准样品编号重复
                return Result.fail(YamlMessageSource.getMessageStatic("00102001"));
            }

            for (Map<String, String> map : dto.getNewSamples()) {
                String jsonString = JSON.toJSONString(map);
                com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSampleVO sample = JSON.parseObject(jsonString, com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSampleVO.class);

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
                    // 更新现有样品
                    fMeasureSampleService.updateById(newSample);
                    FTaskSpecimen specimen = oldMap.get(newSample.getId());
                    if (specimen != null) {
                        specimen.setSampleType(newSample.getSampleType());
                        oldMap.put(newSample.getId(), specimen);
                    }
                } else {
                    // 新增样品
                    FTaskSpecimen specimen = new FTaskSpecimen();
                    specimen.setDeleteType(createType);
                    specimen.setIsTested(noTested);
                    specimen.setSampleType(newSample.getSampleType());
                    log.info("新增的样品：{}", newSample);
                    fMeasureSampleService.save(newSample);
                    specimen.setSpecimenKey(newSample.getId());
                    oldMap.put(newSample.getId(), specimen);
                    taskSpecimen.add(specimen);
                }
            }
        }

        // 更新任务中的样品信息
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);
        boolean b = fMeasureTaskService.updateById(task);
        if (!b) {
            return Result.fail(YamlMessageSource.getMessageStatic("00100003"));
        }

        // 处理样品池自动切换
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

    /**
     * 处理重复模式下的删除逻辑
     * 删除所有相关的测量数据和分析数据
     */
    private void handleDeleteForRepeatMode(String sampleId, String taskId) {
        // 查询所有未删除的测量数据
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getSampleKey, sampleId)
                .eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getIsDelete, 0);
        List<FMeasureData> measureDataList = fMeasureDataService.list(queryWrapper);

        if (measureDataList != null && !measureDataList.isEmpty()) {
            for (FMeasureData measureData : measureDataList) {
                // 标记测量数据为删除
                measureData.setIsDelete(deleteType);
                fMeasureDataService.updateById(measureData);

                // 查询并标记相关的分析数据为删除
                LambdaQueryWrapper<FMeasureAnalysisData> analysisQueryWrapper = new LambdaQueryWrapper<>();
                analysisQueryWrapper.eq(FMeasureAnalysisData::getSampleKey, sampleId)
                        .eq(FMeasureAnalysisData::getTaskId, taskId)
                        .eq(FMeasureAnalysisData::getMeasureDataId, measureData.getId())
                        .eq(FMeasureAnalysisData::getIsDelete, 0);
                List<FMeasureAnalysisData> analysisDataList = fMeasureAnalysisDataService.list(analysisQueryWrapper);

                if (analysisDataList != null && !analysisDataList.isEmpty()) {
                    for (FMeasureAnalysisData analysisData : analysisDataList) {
                        analysisData.setIsDelete(deleteType);
                        fMeasureAnalysisDataService.updateById(analysisData);
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
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getSampleKey, sampleId)
                .eq(FMeasureData::getTaskId, taskId)
                .eq(FMeasureData::getIsDelete, 0)
                .last("LIMIT 1");

        // 获取最新的一条
        List<FMeasureData> list = fMeasureDataService.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            FMeasureData latestData = list.get(0);

            // 标记测量数据为删除
            latestData.setIsDelete(deleteType);
            fMeasureDataService.updateById(latestData);

            // 查询并标记相关的分析数据为删除
            LambdaQueryWrapper<FMeasureAnalysisData> analysisQueryWrapper = new LambdaQueryWrapper<>();
            analysisQueryWrapper.eq(FMeasureAnalysisData::getSampleKey, sampleId)
                    .eq(FMeasureAnalysisData::getTaskId, taskId)
                    .eq(FMeasureAnalysisData::getMeasureDataId, latestData.getId())
                    .eq(FMeasureAnalysisData::getIsDelete, 0);
            List<FMeasureAnalysisData> analysisDataList = fMeasureAnalysisDataService.list(analysisQueryWrapper);

            if (analysisDataList != null && !analysisDataList.isEmpty()) {
                for (FMeasureAnalysisData analysisData : analysisDataList) {
                    analysisData.setIsDelete(deleteType);
                    fMeasureAnalysisDataService.updateById(analysisData);
                }
            }
        }
    }


    /**
     * 查询样品数据列表
     *
     * @param taskId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result querySampleDataList(String taskId, String type, Integer digit) {
        if (taskId == null) {
            //方法id不能为空,请创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000021"));
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //该方法下没有草稿,请新建草稿
            return Result.fail(YamlMessageSource.getMessageStatic("00100010"));
        }
        // 根据方法id查询方法
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        List<FSampleListVo> listVos = new ArrayList<>();
        if (taskSpecimen == null || taskSpecimen.isEmpty()) {
            return Result.ok(listVos);
        }
        // 判断是否是重复模式
        if ("不重复".equals(parameter.getRepetitionPattern())) {
            // 不重复模式
            for (FTaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getDeleteType().equals("1") && type == null) {
                    continue;
                }
                FSampleListVo vo = new FSampleListVo();
                // 根据样品id查询样品数据
                FMeasureSample sample = fMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    //样品不存在
                    return Result.fail(YamlMessageSource.getMessageStatic("0000000B"));
                }
                QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("task_id", task.getId());
                queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                queryWrapper.eq("analysis_type", "5");
                queryWrapper.orderByDesc("analysis_number");
                if (type == null) {
                    queryWrapper.eq("is_delete", "0");
                }
                queryWrapper.last("limit 1");
                FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getOne(queryWrapper);
                // 如果没有测量数据则设置为无测量数据的参数返回
                if (measureData == null || taskSpeciman.getIsTested().equals("0") && measureData.getAnalysisResult() == null) {
                    vo.setTitle(sample.getSampleName());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisId(null);
                    vo.setSampleLocation(sample.getSampleLocation());
                    vo.setAnalysisNumber(1);
                    vo.setColor(null);
                    vo.setData(null);
                    vo.setIsTested(taskSpeciman.getIsTested());
                } else {
                    vo.setTitle(sample.getSampleName());
                    vo.setAnalysisId(measureData.getId());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setSampleLocation(sample.getSampleLocation());
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
                return Result.fail(YamlMessageSource.getMessageStatic("00000037"));
            }
            Integer count = Integer.valueOf(parameter.getRepetitionTime());
            for (FTaskSpecimen taskSpeciman : taskSpecimen) {
                if (type == null && taskSpeciman.getDeleteType().equals("1")) {
                    continue;
                }
                // 根据样品id查询样品数据
                FMeasureSample sample = fMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    //样品不存在
                    return Result.fail(YamlMessageSource.getMessageStatic("0000000B"));
                }
                int key = 0;
                // 根据方法中的方法参数的重复次数进行循环
                for (int i = 0; i < count; i++) {
                    FSampleListVo vo = new FSampleListVo();
                    QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", task.getId());
                    queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                    queryWrapper.eq("analysis_type", "5");
                    queryWrapper.eq("analysis_number", (i + 1));
                    queryWrapper.last("limit 1");
                    FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getOne(queryWrapper);

                    if (measureData == null && taskSpeciman.getIsTested().equals("0")) {
                        vo.setTitle(sample.getSampleName() + "-" + (i + 1));
                        vo.setAnalysisId(null);
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(i + 1);
                        vo.setSampleLocation(sample.getSampleLocation());
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
                        vo.setSampleLocation(sample.getSampleLocation());
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
     * 校验数据完整性
     *
     * @param dataList
     * @param parameter
     * @return
     */
    private boolean verifyDataIntegrity(List<String> dataList, SpectrumParameter parameter) {
        // 检查列表是否为空
        if (dataList == null || dataList.isEmpty()) {
            return false;  // 如果数据为空，返回 false
        }

        // 获取第一个和最后一个数据
        String firstData = dataList.get(0);
        String lastData = dataList.get(dataList.size() - 1);

        // 使用逗号分割数据
        String[] firstDataParts = firstData.split(",");
        String[] lastDataParts = lastData.split(",");

        // 判断第一个和最后一个数据的第一个值是否符合起始波长和终止波长的条件
        boolean isStartWaveLengthValid = firstDataParts.length > 0 && firstDataParts[0].equals(String.valueOf(parameter.getStartWaveLength()));
        boolean isEndWaveLengthValid = lastDataParts.length > 0 && lastDataParts[0].equals(String.valueOf(parameter.getEndWaveLength()));

        // 如果第一个值等于起始波长且最后一个值等于终止波长，返回 true
        return isStartWaveLengthValid && isEndWaveLengthValid;
    }


    /**
     * 删除样品列表
     *
     * @param taskId
     * @param sampleId
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String taskId, String sampleId, String analysisId) {
        // 校验传过来的参数
        if (taskId == null || sampleId == null || analysisId == null) {
            //参数错误
            return Result.fail(YamlMessageSource.getMessageStatic("0000000B"));
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<FTaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        // 根据任务id查询方法表数据并取出方法参数
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        // 判断是否是重复模式
        if (spectrumParameter.getRepetitionPattern().equals("不重复")) {
            // 根据分析id查询分析数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
            }
            byId.setIsDelete("1");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            // 循环遍历样品列表找到该样品
            for (FTaskSpecimen taskSpecimen : taskSpecimenList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("1");
                }
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimenList));
            boolean update1 = fMeasureTaskService.updateById(task);
            if (update && update1) {
                //成功删除数据
                return Result.ok(YamlMessageSource.getMessageStatic("00000043"));
            } else {
                //删除数据失败
                return Result.fail(YamlMessageSource.getMessageStatic("00000044"));
            }
        } else {
            // 根据分析id查询分析数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
            }
            byId.setIsDelete("1");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            // 查询跟这个样品和任务id关联的分析数据
            List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("task_id", taskId).eq("sample_key", sampleId));
            boolean flat = false;
            for (FMeasureAnalysisData analysisData : list) {
                if (analysisData.getIsDelete().equals("0")) {
                    flat = true;
                    break;
                }
            }
            if (list.size() == 3 && !flat) {
                // 循环遍历样品列表找到该样品
                for (FTaskSpecimen taskSpecimen : taskSpecimenList) {
                    if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                        taskSpecimen.setDeleteType("1");
                        break;
                    }
                }
                task.setSampleKey(JSON.toJSONString(taskSpecimenList));
                boolean update1 = fMeasureTaskService.updateById(task);
                if (update && update1) {
                    //成功删除数据
                    return Result.ok(YamlMessageSource.getMessageStatic("00000043"));
                } else {
                    //删除数据失败
                    return Result.fail(YamlMessageSource.getMessageStatic("00000044"));
                }
            }
            if (update) {
                //成功删除数据
                return Result.ok(YamlMessageSource.getMessageStatic("00000043"));
            } else {
                //删除数据失败
                return Result.fail(YamlMessageSource.getMessageStatic("00000044"));
            }
        }
    }

    /**
     * 查询分析数据列表
     *
     * @param taskId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result queryAnalysisDataList(String taskId, String type, Integer digit) {
        if (taskId == null) {
            //方法id不能为空,请创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000021"));
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            //该方法下没有草稿,请新建草稿
            return Result.fail(YamlMessageSource.getMessageStatic("00100010"));
        }

        // 根据方法id查询方法
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }

        // 根据任务id去查询分析数据
        QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", task.getId());
        queryWrapper.ne("analysis_type", "5");
        if (type == null || type.isEmpty()) {
            queryWrapper.eq("is_delete", "0");
        }
        List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(queryWrapper);
        List<FSampleListVo> listVos = new ArrayList<>();

        for (FMeasureAnalysisData fMeasureAnalysisData : list) {
            FSampleListVo vo = new FSampleListVo();
            vo.setAnalysisId(fMeasureAnalysisData.getId());
            vo.setTitle(fMeasureAnalysisData.getAnalysisName());
            vo.setColor(fMeasureAnalysisData.getColor());
            vo.setDeleteType(fMeasureAnalysisData.getIsDelete());
            if (fMeasureAnalysisData.getAnalysisResult() == null) {
                vo.setData(null);
                vo.setOriginalData(null);
            } else {
                vo.setData(getSampleData(fMeasureAnalysisData, digit));
                vo.setOriginalData(getOriginalSampleData(fMeasureAnalysisData, digit));
            }
            vo.setType("1");
            listVos.add(vo);
        }

        return Result.ok(listVos);
    }


    /**
     * 删除分析数据列表接口
     *
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = fMeasureAnalysisDataService.updateById(byId);
        if (update) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    /**
     * 恢复删除的数据
     *
     * @param taskId
     * @param sampleId
     * @param type
     * @param analysisId
     * @return
     **/
    @Override
    public Result restoreDeletedListData(String taskId, String sampleId, String type, String analysisId) {
        // 根据类型判断是样品数据还是变换数据
        if (type.equals("0")) {
            // 根据任务id查询任务数据
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            if (task == null) {
                //没有找到该任务
                return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
            }
            List<FTaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen taskSpecimen : sampleList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("0");
                    break;
                }
            }
            task.setSampleKey(JSON.toJSONString(sampleList));
            boolean update1 = fMeasureTaskService.updateById(task);
            // 根据分析id查询分析数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
            }
            byId.setIsDelete("0");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            if (update1 && update) {
                return Result.ok(true);
            } else {
                return Result.fail(false);
            }
        } else {
            // 根据变换数据id查询变换数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //变换数据不存在
                return Result.fail(YamlMessageSource.getMessageStatic("0030000B"));
            }
            byId.setIsDelete("0");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
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
    @DS("concise")
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(FUpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00306003"));
        }

        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;


        // 分析数据
        FMeasureAnalysisData analysis = fMeasureAnalysisDataService.getById(dataId);
        if (analysis == null) {
            //样品数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000047"));
        }
        analysis.setColor(color);
        updated = fMeasureAnalysisDataService.updateById(analysis);


        if (!updated) {
            //更新失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000048"));
        }

        return Result.ok();
    }

    /**
     * 获取分析标记点接口
     *
     * @param dataId
     * @param digit
     * @return
     */
    @Override
    public Result queryGaugePoint(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<FPeakValleyMarking> gaugePoints = new ArrayList<>();
        FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getById(dataId);
        if (measureData == null) {
            return Result.ok(new ArrayList<>());
        }
        if (measureData.getGaugePoint() != null) {
            gaugePoints = JSON.parseArray(measureData.getGaugePoint(), FPeakValleyMarking.class);
            if (digit != null) {
                for (FPeakValleyMarking data : gaugePoints) {
                    Double value = new Double(data.getY());
                    String valueY = DataCountUtil.addZero(value, digit);
                    data.setY(valueY);
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
    public Result addGaugePointPort(FAddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            //请选择标记点
            return Result.fail(YamlMessageSource.getMessageStatic("0000003E"));
        }
        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        gaugePointJson = analysisData.getGaugePoint();
        rawData = JSON.parseArray(analysisData.getAnalysisResult(), String.class);

        /// 校验
        // 校验：所有传入的标记点是否都存在于原始数据
        Set<String> rawXSet = rawData.stream()
                .map(s -> normalizeNumberString(s.split(",")[0]))
                .collect(Collectors.toSet());

        for (String xStr : paramDto.getGaugePoints()) {
            String normX = normalizeNumberString(xStr);
            if (!rawXSet.contains(normX)) {
                //"波长 " + xStr + " 不存在，请重新选择"
                return Result.fail(YamlMessageSource.getMessageStatic("00104004", xStr));
            }
        }

        // 原标记点集合
        List<FPeakValleyMarking> oldList = gaugePointJson == null
                ? new ArrayList<>()
                : JSON.parseArray(gaugePointJson, FPeakValleyMarking.class);

        // 先构造原有标记点 Map（X -> 对象）
        Map<String, FPeakValleyMarking> oldMap = oldList.stream()
                .collect(Collectors.toMap(
                        m -> normalizeNumberString(m.getX()),
                        m -> m,
                        (a, b) -> a
                ));

        // 构造新的最终集合（保证顺序）
        Map<String, FPeakValleyMarking> finalMap = new LinkedHashMap<>();

        // 遍历前端传的 X 值
        for (String xStr : paramDto.getGaugePoints()) {
            String normX = normalizeNumberString(xStr);
            FPeakValleyMarking existing = oldMap.get(normX);

            if (existing != null) {
                // 已存在则直接用旧值
                finalMap.put(normX, existing);
            } else {
                // 新增点时，从原始数据中查找对应 Y
                for (String s : rawData) {
                    String[] split = s.split(",");
                    String rawNormX = normalizeNumberString(split[0]);
                    if (normX.equals(rawNormX)) {
                        FPeakValleyMarking newMark = new FPeakValleyMarking();
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
        FMeasureAnalysisData analysisData1 = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        analysisData1.setGaugePoint(updatedJson);
        boolean update = fMeasureAnalysisDataService.updateById(analysisData1);
        return update ? Result.ok(true) : Result.fail(false);
    }

    /**
     * 添加标记点备注接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointRemarks(FPeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00306003"));
        }
        if (paramDto.getRemark() == null) {
            //请先添加备注
            return Result.fail(YamlMessageSource.getMessageStatic("00000046"));
        }
        // 根据测量id去分析数据表中进行查询
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        List<FPeakValleyMarking> list = JSON.parseArray(tMeasureData.getGaugePoint(), FPeakValleyMarking.class);
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (FPeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        tMeasureData.setGaugePoint(JSON.toJSONString(list));
        boolean update = fMeasureAnalysisDataService.updateById(tMeasureData);
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 获取峰谷接口
     *
     * @param dataId
     * @param digit
     * @return
     */
    @Override
    public Result queryPeakAndValleysPort(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<FPeakValleyMarking> peaks = new ArrayList<>();
        List<FPeakValleyMarking> grains = new ArrayList<>();
        List<FPeakValleyMarking> dataAll = new ArrayList<>();
        FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getById(dataId);
        if (measureData == null) {
            return Result.ok(new ArrayList<>());
        }
        if (measureData.getPeak() != null) {
            peaks = JSON.parseArray(measureData.getPeak(), FPeakValleyMarking.class);
            dataAll.addAll(peaks);
        }
        if (measureData.getGrain() != null) {
            grains = JSON.parseArray(measureData.getGrain(), FPeakValleyMarking.class);
            dataAll.addAll(grains);
        }
        if (dataAll != null && dataAll.size() != 0) {
            if (digit != null) {
                for (FPeakValleyMarking data : dataAll) {
                    Double value = new Double(data.getY());
                    String valueY = DataCountUtil.addZero(value, digit);
                    data.setY(valueY);
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
    public Result peakSearchingPort(FSeekingPeaksValleysDto paramDto) {
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        if (paramDto.getDataId() == null || paramDto.getDataId().equals("")) {
            //数据ID不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00100011"));
        }
        if (paramDto.getHeight() == null) {
            paramDto.setHeight(Double.parseDouble(confVO.getPeakValleyDiff()));
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
        if (paramDto.getHeight() > 100) {
            return Result.fail(YamlMessageSource.getMessageStatic("00100027"));
        }
        paramDto.setHeight(paramDto.getHeight() / 100);
        FMeasureAnalysisData fMeasureAnalysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (fMeasureAnalysisData == null) {
            //测量数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0000003B"));
        }
        List<String> list = JSON.parseArray(fMeasureAnalysisData.getAnalysisResult(), String.class);
        // 分割测量数据
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", xList);
        body.put("y_data", yList);
        body.put("peak_height", paramDto.getHeight());
        body.put("valley_height", paramDto.getHeight());
        body.put("distance", paramDto.getDistance());
        Map<String, Object> resultMap;
        try {
            resultMap = callPythonService(
                    pythonPathConstant.getPeakSearchingAndValleySearching(),
                    body
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        if (xPeak == null || yPeak == null) {
            //height参数太高
            return Result.fail(YamlMessageSource.getMessageStatic("0000003C"));
        }
        for (int i = 0; i < xPeak.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(xPeak.get(i).toString());
            marking.setY(yPeak.get(i));
            marking.setType("波峰");
            peakData.add(marking);
        }
        List<FPeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> xValley = (List<Double>) resultMap.get("valley_x");
        List<String> yValley = (List<String>) resultMap.get("valley_y");
        for (int i = 0; i < xValley.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(xValley.get(i).toString());
            marking.setY(yValley.get(i));
            marking.setType("波谷");
            valleyData.add(marking);
        }
        Boolean flag;
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        analysisData.setPeak(JSON.toJSONString(peakData));
        analysisData.setGrain(JSON.toJSONString(valleyData));
        flag = fMeasureAnalysisDataService.updateById(analysisData);
        return flag ? Result.ok() : Result.fail(YamlMessageSource.getMessageStatic("00000034"));//数据保存失败
    }

    /**
     * 添加峰谷备注接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addPeakValleyRemarks(FPeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }
        if (paramDto.getRemark() == null) {
            //请先添加备注
            return Result.fail(YamlMessageSource.getMessageStatic("00000046"));
        }
        // 根据测量id去分析数据表中进行查询
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            //测量数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0000003B"));
        }
        // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
        List<FPeakValleyMarking> list;
        if (paramDto.getType().equals("波峰")) {
            list = JSON.parseArray(tMeasureData.getPeak(), FPeakValleyMarking.class);
        } else {
            list = JSON.parseArray(tMeasureData.getGrain(), FPeakValleyMarking.class);
        }
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (FPeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        if (paramDto.getType().equals("波峰")) {
            tMeasureData.setPeak(JSON.toJSONString(list));
        } else {
            tMeasureData.setGrain(JSON.toJSONString(list));
        }
        boolean update = fMeasureAnalysisDataService.updateById(tMeasureData);
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 设置测量参数接口
     *
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result setUpParametersPort(String methodId, String deviceSerialNumber) {
        if (methodId == null || methodId.equals("")){
            return Result.fail(YamlMessageSource.getMessageStatic("00000008"));
        }
        // 根据methodId去方法表中查询数据
        FMeasureMethod tMeasureMethod = fMeasureMethodService.getOne(new QueryWrapper<FMeasureMethod>().eq("id", methodId).eq("measurement_type", "光谱扫描"));
        if (tMeasureMethod == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        FSpectrumParameter spectrumParam = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), FSpectrumParameter.class);
        double waveLengthInterval = Double.parseDouble(spectrumParam.getWaveLengthInterval());

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
        if (StringUtils.hasText(spectrumParam.getSpectralBandwidth())&&deviceInfoService.isNeedSetSit(deviceSerialNumber)) {
            setParam.setSlit1(Double.valueOf(spectrumParam.getSpectralBandwidth()));
        }
        // 6. 扫描速度
        setParam.setScanSpeed(Integer.valueOf(spectrumParam.getScanSpeed()));
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
            return Result.ok(true);
        } else {
            return Result.fail(r.getMessage());
        }
    }

    /**
     * 查询历史方法接口
     *
     * @param pageNum
     * @param pageSize
     * @param updateBy
     * @param measureType
     * @param methodName
     * @return
     */
    @Override
    public Result queryHistoryMethod(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName) {
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        // 根据方法的用户名,测量类型,分页查询,如果有方法名称就加个方法名的模糊查询,如果没有就不加这个条件
        Page<FMeasureMethod> page = new Page<>(pageNum, pageSize);
        QueryWrapper<FMeasureMethod> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("update_by", updateBy)
                .eq("measurement_type", measureType)
                .orderByDesc("method_use_time");
        if (methodName != null) {
            queryWrapper.like("method_name", methodName);
        }
        Page<FMeasureMethod> methodPage = fMeasureMethodService.page(page, queryWrapper);
        if (methodPage.getTotal() == 0) {
            //没有查询到数据
            return Result.fail(YamlMessageSource.getMessageStatic("00100013"));
        }

        return Result.ok(methodPage);
    }

    /**
     * 查询历史任务接口
     *
     * @param pageNum
     * @param pageSize
     * @param updateBy
     * @param measureType
     * @param taskName
     * @return
     */
    @Override
    public Result queryHistoryTask(Integer pageNum, Integer pageSize, String updateBy, String measureType, String taskName) {
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        // 根据任务的创建人,测量类型,分页查询,如果有任务名称就加个任务名称的模糊查询,如果没有就不加这个条件
        Page<FMeasureTask> page = new Page<>(pageNum, pageSize);
        QueryWrapper<FMeasureTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_by", updateBy)
                .eq("measurement_type", measureType)
                .orderByDesc("create_time");
        if (taskName != null) {
            queryWrapper.like("task_name", taskName);
        }
        Page<FMeasureTask> taskPage = fMeasureTaskService.page(page, queryWrapper);
        return Result.ok(taskPage);
    }

    /**
     * 查询算术源数据接口
     *
     * @param taskId
     * @return
     */
    @Override
    public Result queryArithmeticSourceData(String taskId) {
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey() == null || task.getSampleKey().equals("[]")) {
            return Result.ok(new ArrayList<>());
        }
        // 根据任务的方法key查询方法参数
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        List<Map<String, String>> list = new ArrayList<>();
        // 判断是否是重复模式
        if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
            for (FTaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                    FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(new QueryWrapper<FMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpeciman.getSpecimenKey())
                            .eq("analysis_type", "5")
                            .eq("is_delete", "0")
                            .eq("analysis_number", 1)
                            .last("limit 1"));
                    FMeasureSample byId = fMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                    if (one == null) {
                        one = new FMeasureAnalysisData();
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
            for (FTaskSpecimen taskSpecimans : taskSpecimen) {
                // 仅判断样品未删除（deleteType = "0"）
                if (taskSpecimans.getDeleteType().equals("0")) {
                    List<FMeasureAnalysisData> measureDataList = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpecimans.getSpecimenKey())
                            .eq("is_delete", "0")
                            .orderByAsc("analysis_number"));

                    // 1. 判断查询结果列表不为空
                    if (measureDataList != null && !measureDataList.isEmpty()) {
                        FMeasureSample byId = fMeasureSampleService.getById(taskSpecimans.getSpecimenKey());

                        for (FMeasureAnalysisData tMeasureData : measureDataList) {
                            // 2. 判断 analysisResult 不为 null 且不是空数组（假设 analysisResult 是 List 类型）
                            // 如果 analysisResult 是字符串，可替换为: !StringUtils.isEmpty(tMeasureData.getAnalysisResult())
                            Object analysisResult = tMeasureData.getAnalysisResult();
                            if (analysisResult != null &&
                                    (analysisResult instanceof List && !((List<?>) analysisResult).isEmpty() ||
                                            (analysisResult instanceof String && !((String) analysisResult).isEmpty()))) {

                                Map<String, String> map = new HashMap<>();
                                map.put("measureId", tMeasureData.getId());
                                map.put("sampleName", byId.getSampleName() + "-" + tMeasureData.getAnalysisNumber());
                                list.add(map);
                            }
                        }
                    }
                }
            }
        }

        return Result.ok(list);
    }

    /**
     * 四元组运算接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result fourOperationsPort(FFourOperationsParamDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
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
            //请选择运算符
            return Result.fail(YamlMessageSource.getMessageStatic("00000030"));
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //请选择数据类型
            return Result.fail(YamlMessageSource.getMessageStatic("00000031"));
        }
        // 根据测量数据id查询测量数据
        FMeasureAnalysisData measureData1 = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId1());
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
            FMeasureAnalysisData measureData2 = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId2());
            if (measureData2 == null) {
                //请选择源2
                return Result.fail(YamlMessageSource.getMessageStatic("00100014"));
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
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<FPeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        // 将分析数据存储到分析数据表中
        FMeasureAnalysisData tMeasureAnalysisData = new FMeasureAnalysisData();
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 高级运算
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result advancedOperationsPort(FAdvancedOperationsDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        // 根据任务id查询任务数据
        FMeasureTask task = fMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // 根据任务中的方法key查询方法并且提取方法参数
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        // 提取方法参数
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        if (spectrumParameter == null) {
            //方法参数错误
            return Result.fail(YamlMessageSource.getMessageStatic("00105017"));
        }
        // 判断光度模式是否是1
        if (spectrumParameter.getLuminosity().equals("1")) {
            //Abs光度模式不可使用高级运算
            return Result.fail(YamlMessageSource.getMessageStatic("00100015"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
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
        FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<FPeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        // 将分析数据存储到分析数据表中
        FMeasureAnalysisData tMeasureAnalysisData = new FMeasureAnalysisData();
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
        if (!save) {
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
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result smoothnessPort(FSmoothnessDto paramDto) {
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
        if (valueOf(paramDto.getCoefficient()) > valueOf(paramDto.getPointNum())) {
            //系数不能大于点数
            return Result.fail(YamlMessageSource.getMessageStatic("0000003A"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据测量数据的id查询出测量数据
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<FPeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        FMeasureAnalysisData tMeasureAnalysisData = new FMeasureAnalysisData();
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 微分
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result differentialPort(FDifferentialDto paramDto) {
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
            return Result.fail(YamlMessageSource.getMessageStatic("00000038"));
        }
        if (valueOf(paramDto.getCoefficient()) > valueOf(paramDto.getPointNum())) {
            //系数不能大于点数
            return Result.fail(YamlMessageSource.getMessageStatic("0000003A"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.
                list(new QueryWrapper<FMeasureAnalysisData>().
                        eq("analysis_name", paramDto.getAnalysisDataName()).
                        eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据测量数据的id查询出测量数据
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<FPeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        FMeasureAnalysisData tMeasureAnalysisData = new FMeasureAnalysisData();
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 光度转换
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result luminanceModeConversionPort(FLuminosityConversionDto paramDto) {
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
            return Result.fail(YamlMessageSource.getMessageStatic("00000035"));
        }
        if (paramDto.getMethodId() == null || paramDto.getMethodId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        if (paramDto.getDataType() == null || paramDto.getDataType().equals("")) {
            //变换类型错误
            return Result.fail(YamlMessageSource.getMessageStatic("00304004"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据方法id查询方法表的数据
        FMeasureMethod method = fMeasureMethodService.getById(paramDto.getMethodId());
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
            //您当前的光度模式不支持进行转换
            return Result.fail(YamlMessageSource.getMessageStatic("00000036"));
        }
        // 根据测量数据id查询测量数据
        FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<FPeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            FPeakValleyMarking peakValleyMarking = new FPeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
        FMeasureAnalysisData tMeasureAnalysisData = new FMeasureAnalysisData();
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 查询表格头数据
     *
     * @param taskId
     * @param methodId
     * @param analysisIds
     * @return
     */
    @Override
    public Result<List<FHeadVO>> tableHead(String taskId, String methodId, List<String> analysisIds) {
        // 根据方法id查找方法
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        List<FHeadVO> ans = new ArrayList<>();

        // 1. 编号
        ans.add(new FHeadVO("编号", "id", "system", "编号"));

        // 2. 波长
        ans.add(new FHeadVO("波长(nm)", "waveLength", "measure", "波长(nm)"));
        // 如果样品长度为1则是单样品表头,如果样品长度超过1则是多样品表头
        if (analysisIds.size() == 1) {
            addLuminosityMode(ans, parameter.getLuminosity());
            ans.add(new FHeadVO("峰谷/标记点", "peakValleyGaugePoint", "measure", "峰谷"));
            ans.add(new FHeadVO("备注", "remark", "analysis", "备注"));
            return Result.ok(ans);
        } else {

            // 遍历样品id进行逐个查询
            for (String analysisId : analysisIds) {
                // 根据单个样品id查询样品信息
                QueryWrapper<FMeasureAnalysisData> qw = new QueryWrapper<>();
                qw.eq("id", analysisId);
                FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(qw);

                // 如果样品信息不存在，则跳过当前id或返回错误
                if (analysisData == null) {
                    return Result.fail(YamlMessageSource.getMessageStatic("00104016: ", analysisId));
                }
                if (analysisData.getSampleKey() != null) {
                    // 根据样品id查询样品
                    FMeasureSample sample = fMeasureSampleService.getById(analysisData.getSampleKey());
                    if (parameter.getRepetitionPattern().equals("不重复")) {
                        ans.add(new FHeadVO(sample.getSampleName(), "sampleNumber", "name", "样品名称"));
                        ans.add(new FHeadVO("峰/谷", "peakValley", "system", "峰谷"));
                    } else {
                        ans.add(new FHeadVO(sample.getSampleName() + "-" + analysisData.getAnalysisNumber(), "sampleNumber", "name", "样品名称"));
                        ans.add(new FHeadVO("峰/谷", "peakValley", "system", "峰谷"));
                    }
                } else {
                    // 按顺序添加到结果列表
                    ans.add(new FHeadVO(analysisData.getAnalysisName(), "sampleNumber", "name", "样品名称"));
                    ans.add(new FHeadVO("峰/谷", "peakValley", "system", "峰谷"));
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
            FMeasureAnalysisData measureAnalysisData = fMeasureAnalysisDataService.getById(analysisIds.get(0));
            if (measureAnalysisData == null) {
                return null;
            }
            int id = 1;
            List<FPeakValleyMarking> peaks = JSON.parseArray(measureAnalysisData.getPeak(), FPeakValleyMarking.class);
            List<FPeakValleyMarking> valleys = JSON.parseArray(measureAnalysisData.getGrain(), FPeakValleyMarking.class);
            List<FPeakValleyMarking> gaugePoints = JSON.parseArray(measureAnalysisData.getGaugePoint(), FPeakValleyMarking.class);
            if (peaks != null && !peaks.isEmpty()) {
                for (FPeakValleyMarking peak : peaks) {
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
                for (FPeakValleyMarking valley : valleys) {
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
                for (FPeakValleyMarking gaugePoint : gaugePoints) {
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
            List<FMeasureAnalysisData> analysisDataList = new ArrayList<>();
            List<List<String>> allDataResults = new ArrayList<>(); // 存储所有样品的数据结果
            int maxDataSize = 0; // 最大数据量

            for (String analysisId : analysisIds) {
                FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(analysisId);
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
                    FMeasureAnalysisData analysisData = analysisDataList.get(i);
                    List<String> datas = allDataResults.get(i);
                    List<FPeakValleyMarking> peaks = JSON.parseArray(analysisData.getPeak(), FPeakValleyMarking.class);
                    List<FPeakValleyMarking> valleys = JSON.parseArray(analysisData.getGrain(), FPeakValleyMarking.class);

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
                                        for (FPeakValleyMarking peak : peaks) {
                                            if (Objects.equals(formatBigDecimal(peak.getX(), 1), wavelength)) {
                                                peakValleyType = "峰";
                                                break;
                                            }
                                        }
                                    }
                                    if (valleys != null && peakValleyType.isEmpty()) {
                                        for (FPeakValleyMarking valley : valleys) {
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
     * 比较参数
     *
     * @param method
     * @param newParam
     * @return
     */
    private boolean compareParam(FSpectrumParameter method, FSpectrumParameter newParam) {
        String dbJson = JSON.toJSONString(method, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newParam, SerializerFeature.SortField);

        return dbJson.equals(frontJson);
    }


    /**
     * 获取样品的详细数据
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(FMeasureAnalysisData measureData, Integer digit) {
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<FPeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), FPeakValleyMarking.class);
        List<FPeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), FPeakValleyMarking.class);
        List<FPeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), FPeakValleyMarking.class);
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
            if (gauges != null && peaks.size() != 0) {
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
            if (peaks != null && peaks.size() != 0) {
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
            if (grades != null && peaks.size() != 0) {
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
    public List<Map<String, Object>> getOriginalSampleData(FMeasureAnalysisData measureData,
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
     * 数字字符串归一化（防止 360 / 360.0 / 3.6E2 比较失败）
     */
    private String normalizeNumberString(String numStr) {
        return new BigDecimal(numStr)
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * 创建新的方法
     */
    private FMeasureMethod createNewMethod(FUseMethodDto paramDto) {
        FMeasureMethod fMeasureMethod = new FMeasureMethod();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String collect = new Random().ints(8, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());

        fMeasureMethod.setMethodName(paramDto.getMeasureType() + collect);

        FSpectrumParameter parameter = new FSpectrumParameter();
        // 将前端参数复制给数据库参数
        BeanUtils.copyProperties(paramDto.getParameter(), parameter);
        if (paramDto.getType().equals("0")) {
            if (paramDto.getParameter().getScanSpeed() == null) {
                parameter.setScanSpeed("3");
            }
            if (paramDto.getType().equals("0")) {
                parameter.setEnergyLampName(paramDto.getParameter().getEnergyLamp().equals("0") ? "钨灯" : paramDto.getParameter().getEnergyLamp().equals("1") ? "氘灯" : "汞灯");
            }
        }
        fMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(parameter));
        fMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        fMeasureMethod.setMethodUseTime(LocalDateTime.now());
        fMeasureMethod.setMethodCreator(paramDto.getUpdateBy());
        fMeasureMethod.setMeasurementType(paramDto.getMeasureType());
        boolean save = fMeasureMethodService.save(fMeasureMethod);
        if (save) {
            return fMeasureMethod;
        } else {
            return null;
        }
    }

    /**
     * 多格式文件导出（支持word/txt/xlsx）
     *
     * @param taskId     任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType   导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds, Integer digit) {
        // log.info("===== 导出流程开始 =====");
        // log.info("开始导出，taskId: {}, fileType: {}, 需要筛选的analysisId数量: {}", 
        //         taskId, fileType, analysisIds != null ? analysisIds.size() : 0);

        // 1. 基础参数校验阶段（保持不变）
        // log.info("【步骤1/5】开始基础参数校验");
        if (!StringUtils.hasText(taskId)) {
            // log.error("【步骤1/5】校验失败：缺少任务id");
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
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
        List<FSampleListVo> allSamples = new ArrayList<>();

        // 处理sampleListPort数据
        Result<List<FSampleListVo>> sampleListResult = querySampleDataList(taskId, null, digit);
        if (sampleListResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode()) && sampleListResult.getData() != null) {
            // log.info("【步骤2/5】sampleListPort查询成功，返回样品数量：{}", sampleListResult.getData().size());
            allSamples.addAll(sampleListResult.getData());
        } else {
            // log.warn("【步骤2/5】sampleListPort查询异常：{}", sampleListResult.getMessage());
        }

        // 处理changeTheListPort数据（与sampleListPort逻辑相同）
        Result<List<FSampleListVo>> changeListResult = queryAnalysisDataList(taskId, null, digit);
        if (changeListResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode()) && changeListResult.getData() != null) {
            // log.info("【步骤2/5】changeTheListPort查询成功，返回样品数量：{}", changeListResult.getData().size());
            allSamples.addAll(changeListResult.getData());
        } else {
            // log.warn("【步骤2/5】changeTheListPort查询异常：{}", changeListResult.getMessage());
        }

        // 2.2 根据analysisIds筛选数据
        List<FSampleListVo> filteredSamples = new ArrayList<>();
        if (CollectionUtils.isEmpty(analysisIds)) {
            // 无筛选条件，使用全部数据
            filteredSamples = allSamples;
            // log.info("【步骤2/5】未指定筛选条件，使用全部样品数据，总数量：{}", filteredSamples.size());
        } else {
            // 有筛选条件，匹配analysisId
            for (FSampleListVo sample : allSamples) {
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
                FSampleListVo vo = filteredSamples.get(i);
                // log.info("【步骤2/5】样品{}: title={}, analysisId={}, data是否为空={}", 
                //         i+1, vo.getTitle(), vo.getAnalysisId(), 
                //         vo.getData() == null || vo.getData().isEmpty());

                // 打印第一个样品的前3条数据，确认x和y的值
                if (i == 0 && vo.getData() != null && !vo.getData().isEmpty()) {
                    int dataLogCount = Math.min(3, vo.getData().size());
                    for (int j = 0; j < dataLogCount; j++) {
                        Map<String, Object> data = vo.getData().get(j);
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
            for (FSampleListVo vo : filteredSamples) {
                String title = vo.getTitle();
                List<Map<String, Object>> dataList = vo.getData();

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
        //         wavelengths.size(), sampleYMap.size());

        // 4. 构建表头和数据阶段（保持不变）
        // log.info("【步骤4/5】开始构建导出表头和数据");
        List<FHeadVO> heads = new ArrayList<>();
        heads.add(new FHeadVO("波长nm", "wavelength")); // 波长列标题
        sampleYMap.keySet().forEach(title -> heads.add(new FHeadVO(title, title))); // 样品列标题

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
                    FMeasureMethod method = null;
                    if (StringUtils.hasText(methodKey)) {
                        method = fMeasureMethodService.getById(methodKey);
                        // log.info("【步骤5/5】查询到测量方法：{}", method != null ? method.getMethodName() : "未找到");
                    }
                    if (method == null) {
                        // log.warn("【步骤5/5】测量方法不存在，使用默认值");
                        method = new FMeasureMethod();
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
            //文件生成失败：
            return R.fail(YamlMessageSource.getMessageStatic("00302006", e.getMessage()));
        } finally {
            // log.info("===== 导出流程结束 =====");
        }
    }

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
            QueryWrapper<FMeasureAnalysisImage> analysisId = new QueryWrapper<FMeasureAnalysisImage>().eq("analysis_id", upload.getId());
            analysisId.last("limit 1");
            FMeasureAnalysisImage byId = fMeasureAnalysisImageService.getOne(analysisId);
            if (byId == null) {
                //图片不存在
                return Result.fail(YamlMessageSource.getMessageStatic("00000040"));
            }
            byId.setImageType(fileType);
            byId.setImageBase64(bytes);
            // 将当前日期时间存储
            byId.setCreateTime(LocalDateTime.now());
            boolean update = fMeasureAnalysisImageService.updateById(byId);
            return Result.ok(update);
        } else {
            QueryWrapper<FMeasureTaskAttachment> eq = new QueryWrapper<FMeasureTaskAttachment>()
                    .eq("task_id", upload.getId());
            eq.last("limit 1");
            // 根据任务id查询任务附属表的内容
            FMeasureTaskAttachment byId = fMeasureTaskAttachmentService.getOne(eq);
            if (byId == null) {
                //任务附属不存在
                return Result.fail(YamlMessageSource.getMessageStatic("00000041"));
            }
            if ("0".equals(upload.getImageType())) {
                byId.setOverlapImage(bytes);
            } else if ("1".equals(upload.getImageType())) {
                byId.setTiledImage(bytes);
            } else if ("2".equals(upload.getImageType())) {
                byId.setAddrImage(bytes);
            } else {
                //Type类型错误
                return Result.fail(YamlMessageSource.getMessageStatic("00000042"));
            }
            boolean update = fMeasureTaskAttachmentService.updateById(byId);
            return Result.ok(update);
        }

    }

    @Override
    public Result calculateData(String taskId, String sampleId) {
        // 校验参数
        if (!StringUtils.hasText(taskId)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        if (!StringUtils.hasText(sampleId)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        List<LinearSegment> linearSegment = formulaUtil.getLinearSegment("34-U1702-01-0002");
        Double coefficient = deviceInfoService.queryCoefficient("34-U1702-01-0002");
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        // 将任务中的校零参数取出
        List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
        // 根据任务id+样品id查询测量数据
        FMeasureData data = fMeasureDataService.getOne(new QueryWrapper<FMeasureData>()
                .eq("task_id", taskId)
                .eq("sample_key", sampleId));
        // 将测量数据中的原始数据转换为List<ChangeParam>
        List<ChangeParam> originData = JSON.parseArray(data.getGaugePoint(), ChangeParam.class);
        List<Map<String, String>> result = new ArrayList<>();
        while (params.size() > 0) {
            Map<String, String> map = new HashMap<>();
            String s = DataCountUtil.countTPercent(params.get(0), originData.get(0), 1.0);
            String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
            double parsed = Double.parseDouble(params.get(0).getWl());

            Double v = formulaUtil.calculateWavelength(parsed, linearSegment, coefficient);
            double parsed1 = Double.parseDouble(originData.get(0).getWl());
            Double v1 = formulaUtil.calculateWavelength(parsed1, linearSegment, coefficient);
            map.put("BaseLineStep", params.get(0).getStep());
            map.put("MeasureStep", originData.get(0).getStep());
            map.put("Abs", yy);
            map.put("MeasureWl", String.valueOf(v1));
            map.put("BaseLineWl", String.valueOf(v));
            result.add(map);
            params.remove(0);
            originData.remove(0);
        }

        Double s = 12107.0;
        Double ans = formulaUtil.calculateWl(s, "34-U1702-01-0002");
        Double wavelength = formulaUtil.calculateWavelength(s, linearSegment, coefficient);
        System.out.println("---------------"+linearSegment.toString());
        System.out.println("---------------"+ans);
        System.out.println("++++++++++++++"+wavelength);

        return Result.ok(result);

    }

    /**
     * 停止测量接口
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result stopMeasure(String deviceSerialNumber) {
        // 1. 校验参数
        if (!StringUtils.hasText(deviceSerialNumber)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
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
        }else{
            return Result.fail(r.getMessage());
        }
    }

    /**
     * 设置方法参数
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


    // 保留原有的downloadWord方法，并适配新参数
    public R<File> downloadWord(String taskId, Boolean allDisplay, List<String> analysisIds, Integer digit) {
        return downloadFile(taskId, allDisplay, "word", analysisIds, digit);
    }

    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, FMeasureTask task, FMeasureMethod method,
                                  List<FHeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
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
            FHeadVO head = heads.get(i);
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
                FHeadVO head = heads.get(i);
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
    private void generateTxtFile(File file, List<FHeadVO> heads, List<Map<String, Object>> data) throws Exception {
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
                for (FHeadVO head : heads) {
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
    private void generateExcelFile(File file, List<FHeadVO> heads, List<Map<String, Object>> data) throws Exception {
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
            FHeadVO head = heads.get(i);
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
                FHeadVO head = heads.get(colIdx);
                // 防护：表头为null时，直接填充默认值
                if (head == null) {
                    rowData.add("-");
                    continue;
                }

                // 提取字段原始值（不预设默认值，完全保留数据源状态）
                String identity = head.getIdentity();
                Object originalValue = map.get(identity);
                String cellData;

                // 核心：空值处理（null/空字符串/"null" 统一替换为"-"，其他保留原始值）
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

    public void addLuminosityMode(List<FHeadVO> ans, String mode) {
        switch (mode) {
            case "1":
                ans.add(new FHeadVO("Abs", "luminosity", "measure", "Abs"));
                break;
            case "2":
                ans.add(new FHeadVO("T%", "luminosity", "measure", "T%"));
                break;
            case "3":
                ans.add(new FHeadVO("Es", "luminosity", "measure", "Es"));
                break;
            case "4":
                ans.add(new FHeadVO("Er", "luminosity", "measure", "Er"));
                break;
            default:
                // 可以在这里处理无效的模式
                System.out.println("无效的光度模式: " + mode);
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
     * 验证参数
     * @param parameter
     * @return
     */
    private Result validateSpectrumParam(FSpectrumParameterVo parameter) {
        // 必填字段校验
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getLuminosity())) {
            return Result.fail("光度模式不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getStartWaveLength())) {
            return Result.fail("开始波长不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getEndWaveLength())) {
            return Result.fail("结束波长不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getWaveLengthInterval())) {
            return Result.fail("波长间隔不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getTimeInterval())) {
            return Result.fail("时间间隔不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getScanSpeed())) {
            return Result.fail("扫描速度不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getSpectralBandwidth())) {
            return Result.fail("光谱带宽不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getChangeLampWaveLength())) {
            return Result.fail("换灯波长不能为空");
        }
        if (com.alibaba.excel.util.StringUtils.isBlank(parameter.getEnergyLamp())) {
            return Result.fail("能量灯不能为空");
        }
        if (parameter.getGain() == null) {
            return Result.fail("增益不能为空");
        }

        return Result.ok();
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

}
