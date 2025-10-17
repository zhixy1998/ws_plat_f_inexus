/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
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
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.concise.timesweep.TimeUseMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumImageUpload;
import com.persee.workstation.uv_inexus.dto.measurementDto.TimeSweepParamter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TableHeadILocateEnums;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.common.FTaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.concise.common.FSampleListVo;
import com.persee.workstation.uv_inexus.vo.concise.common.FSampleVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.timesweep.FTimeSweepParamterVo;
import com.persee.workstation.uv_inexus.vo.concise.timesweep.TimeSweepNearestMethodVo;
import com.persee.workstation.uv_inexus.vo.concise.timesweep.TimeSweepSearchMethodVo;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

/**
 * 简洁模式: 时间扫描服务实现类
 */
@Service("timeSweepConciseService")
@Slf4j
public class TimeSweepConciseServiceImpl implements TimeSweepConciseService {

    @Autowired
    private FMeasureMethodService fMeasureMethodService;

    @Autowired
    private FMeasureTaskService fMeasureTaskService;

    @Autowired
    private FMeasureSampleService fMeasureSampleService;


    @Autowired
    private FMeasureTaskAttachmentService fMeasureTaskAttachmentService;
    @Autowired
    private FMeasureAnalysisDataService fMeasureAnalysisDataService;

    @Autowired
    private  FMeasureAnalysisImageService fMeasureAnalysisImageService;

    @Autowired
    private FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    private ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    private FMeasureDataService fMeasureDataService;

    @Autowired
    private FMeasureTaskMapper fMeasureTaskMapper;

    @Autowired
    private SystemUserManageDAO systemUserManageDAO;


    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysUserMapper userManageDAO;

    @Autowired
    private DevMessageUtil devMessageUtil;


    @Autowired
    private RestTemplate restTemplate;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;

    private R<Boolean> checkParam(FTimeSweepParamter parameter) {
        // 1. 校验光度模式
        if (parameter.getLuminosity() == null || parameter.getLuminosity().trim().isEmpty()) {
            return R.fail("光度模式不能为空");
        }
        if (!Arrays.asList("1", "2", "3", "4").contains(parameter.getLuminosity())) {
            return R.fail("光度模式只能是1(abs)、2(T%)、3(Es)、4(Er)");
        }

        // 2. 校验波长值
        if (parameter.getWaveLength() == null || parameter.getWaveLength().trim().isEmpty()) {
            return R.fail("波长值不能为空");
        }
        try {
            double waveLength = Double.parseDouble(parameter.getWaveLength());
            if (waveLength < 190 || waveLength > 900) {
                return R.fail("波长值必须在190-900范围内");
            }
            // 校验小数点后最多1位
            String[] parts = parameter.getWaveLength().split("\\.");
            if (parts.length > 1 && parts[1].length() > 1) {
                return R.fail("波长值小数点后最多保留1位");
            }
        } catch (NumberFormatException e) {
            return R.fail("波长值必须为有效的数字");
        }

        // 3. 校验扫描总时长
        if (parameter.getScanTime() == null || parameter.getScanTime().trim().isEmpty()) {
            return R.fail("扫描总时长不能为空");
        }
        try {
            double scanTime = Double.parseDouble(parameter.getScanTime());
            if (scanTime < 0 || scanTime > 10000) {
                return R.fail("扫描总时长必须在0-10000范围内");
            }
            // 校验小数点后最多1位
            String[] parts = parameter.getScanTime().split("\\.");
            if (parts.length > 1 && parts[1].length() > 1) {
                return R.fail("扫描总时长小数点后最多保留1位");
            }
        } catch (NumberFormatException e) {
            return R.fail("扫描总时长必须为有效的数字");
        }

        // 4. 校验扫描单位
        if (parameter.getScanUnit() == null || parameter.getScanUnit().trim().isEmpty()) {
            return R.fail("扫描单位不能为空");
        }
        if (!Arrays.asList("0", "1").contains(parameter.getScanUnit())) {
            return R.fail("扫描单位只能是0(分)、1(秒)");
        }

        // 5. 校验时间间隔
        if (parameter.getIntervalTime() == null || parameter.getIntervalTime().trim().isEmpty()) {
            return R.fail("时间间隔不能为空");
        }
        try {
            double intervalTime = Double.parseDouble(parameter.getIntervalTime());
            if (intervalTime < 0 || intervalTime > 100) {
                return R.fail("时间间隔必须在0-100范围内");
            }
            // 校验小数点后最多1位
            String[] parts = parameter.getIntervalTime().split("\\.");
            if (parts.length > 1 && parts[1].length() > 1) {
                return R.fail("时间间隔小数点后最多保留1位");
            }
        } catch (NumberFormatException e) {
            return R.fail("时间间隔必须为有效的数字");
        }

        // 6. 校验扫描总个数（非空即可，未明确范围限制）
        if (parameter.getTotalNumber() == null || parameter.getTotalNumber().trim().isEmpty()) {
            return R.fail("扫描总个数不能为空");
        }
        try {
            // 仅校验为数字格式
            Integer.parseInt(parameter.getTotalNumber());
        } catch (NumberFormatException e) {
            return R.fail("扫描总个数必须为有效的整数");
        }

        // 7. 校验多联池模式
        if (parameter.getMultiPoolMode() == null || parameter.getMultiPoolMode().trim().isEmpty()) {
            return R.fail("多联池模式不能为空");
        }
        if (!Arrays.asList("0", "1").contains(parameter.getMultiPoolMode())) {
            return R.fail("多联池模式只能是0(顺序单池连续采样)、1(循环多池交替采样)");
        }

        // 所有校验通过
        return R.ok(true);
    }


    /**
     * 查询最近方法数据
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryNearestMethodData(String updateBy, String measureType) {
        if (updateBy == null) {
            return Result.fail("更新人不能为空");
        }

        if (measureType == null) {
            return Result.fail("方法类型不能为空");
        }

        TimeSweepNearestMethodVo vo = new TimeSweepNearestMethodVo();

        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", updateBy);
        wrapper.eq("measurement_type", measureType);
        wrapper.orderByDesc("method_use_time");
        wrapper.last("LIMIT 1");

        FMeasureMethod method = fMeasureMethodService.getOne(wrapper);

        if (method == null) {
            // 查询对应类型的默认方法
            QueryWrapper<FMeasureMethod> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("measurement_type", measureType);
            queryWrapper.eq("method_name", "默认方法");
            queryWrapper.last("LIMIT 1");
            method = fMeasureMethodService.getOne(queryWrapper);
            if (method == null) {
                return Result.fail("没有默认方法");
            }
            BeanUtils.copyProperties(method, vo);
            FTimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
            FTimeSweepParamterVo parameterVo = new FTimeSweepParamterVo();
            BeanUtils.copyProperties(parameter, parameterVo);
            String s = "1".equals(parameter.getLuminosity()) ? "Abs" : "2".equals(parameter.getLuminosity()) ? "T%" : "3".equals(parameter.getLuminosity()) ? "Es" : "Er";
            parameterVo.setLuminosityName(s);
            if (parameter.getScanUnit()!=null){
                parameterVo.setScanUnitName(parameter.getScanUnit().equals("0") ? "分" : "秒");
            }
            if (parameter.getMultiPoolMode()!=null){
                String s1 = "0".equals(parameter.getMultiPoolMode()) ? "顺序单池连续采样" : "循环多池交替采样";
                parameterVo.setMultiPoolModeName(s1);
            }
            if (parameter.getEnergyLamp()!=null){
                // 0.钨灯,1.氘灯,2.汞灯
                String lamp = parameter.getEnergyLamp().equals("0") ? "钨灯" : parameter.getEnergyLamp().equals("1") ? "氘灯" : "汞灯";
                parameterVo.setEnergyLampName(lamp);
            }
            vo.setParameter(parameterVo);
            // 将创建时间从LocalDateTime转换成String并且将里面的T 去掉
            String format = method.getMethodCreateTime().format(formatter);
            vo.setMethodCreateTime(format);
            String format1 = method.getMethodUseTime().format(formatter);
            vo.setMethodUseTime(format1);
            vo.setMethodId(method.getId());
            return Result.ok(vo);
        }

        // 根据方法id查询出最近创建的任务
        QueryWrapper<FMeasureTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("method_key", method.getId());
        taskWrapper.orderByDesc("create_time");
        taskWrapper.last("LIMIT 1");
        FMeasureTask task = fMeasureTaskService.getOne(taskWrapper);

        BeanUtils.copyProperties(method, vo);
        FTimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
        FTimeSweepParamterVo parameterVo = new FTimeSweepParamterVo();
        BeanUtils.copyProperties(parameter, parameterVo);
        String s = "1".equals(parameter.getLuminosity()) ? "Abs" : "2".equals(parameter.getLuminosity()) ? "T%" : "3".equals(parameter.getLuminosity()) ? "Es" : "Er";
        parameterVo.setLuminosityName(s);
        if (parameter.getScanUnit()!=null){
            parameterVo.setScanUnitName(parameter.getScanUnit().equals("0") ? "分" : "秒");
        }
        if (parameter.getMultiPoolMode()!=null){
            String s1 = "0".equals(parameter.getMultiPoolMode()) ? "顺序单池连续采样" : "循环多池交替采样";
            parameterVo.setMultiPoolModeName(s1);
        }
        if (parameter.getEnergyLamp()!=null){
            // 0.钨灯,1.氘灯,2.汞灯
            String lamp = parameter.getEnergyLamp().equals("0") ? "钨灯" : parameter.getEnergyLamp().equals("1") ? "氘灯" : "汞灯";
            parameterVo.setEnergyLampName(lamp);
        }
        vo.setParameter(parameterVo);
        // 将创建时间从LocalDateTime转换成String并且将里面的T 去掉
        String format = method.getMethodCreateTime().format(formatter);
        vo.setMethodCreateTime(format);
        String format1 = method.getMethodUseTime().format(formatter);
        vo.setMethodUseTime(format1);
        if (task!=null){
            vo.setTaskId(task.getId());
        }
        vo.setMethodId(method.getId());
        return Result.ok(vo);
    }

    /**
     * 查询最近使用的方法
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
            return Result.fail("用户不能为空");
        }
        if (measureType == null) {
            return Result.fail("方法类型不能为空");
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
        TimeSweepSearchMethodVo vo = new TimeSweepSearchMethodVo();
        vo.setTotal(page.getTotal());
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        List<TimeSweepNearestMethodVo> methodVos = new ArrayList<>();
        for (TMeasureMethod record : page.getRecords()) {
            TimeSweepNearestMethodVo methodVo = new TimeSweepNearestMethodVo();
            BeanUtils.copyProperties(record, methodVo);
            // 将创建时间从LocalDateTime转换成String并且将里面的T 去掉
            String format = record.getMethodCreateTime().format(formatter);
            methodVo.setMethodCreateTime(format);

            TimeSweepParamter parameter = JSON.parseObject(record.getMeasurementParamInfo(), TimeSweepParamter.class);
            FTimeSweepParamterVo parameterVo = new FTimeSweepParamterVo();
            BeanUtils.copyProperties(parameter, parameterVo);
            String s = "1".equals(parameter.getLuminosity()) ? "Abs" : "2".equals(parameter.getLuminosity()) ? "T%" : "3".equals(parameter.getLuminosity()) ? "Es" : "Er";
            parameterVo.setLuminosityName(s);
            if (parameter.getScanUnit()!=null){
                parameterVo.setScanUnit(parameter.getScanUnit().equals("0") ? "分" : "秒");
            }
            if (parameter.getMultiPoolMode()!=null){
                String s1 = "0".equals(parameter.getMultiPoolMode()) ? "顺序单池连续采样" : "循环多池交替采样";
                parameterVo.setMultiPoolModeName(s1);
            }
            if (parameter.getEnergyLamp()!=null){
                // 0.钨灯,1.氘灯,2.汞灯
                String lamp = parameter.getEnergyLamp().equals("0") ? "钨灯" : parameter.getEnergyLamp().equals("1") ? "氘灯" : "汞灯";
                parameterVo.setEnergyLampName(lamp);
            }
            methodVo.setParameter(parameterVo);
            methodVos.add(methodVo);
        }
        vo.setRecords(methodVos);
        return Result.ok(vo);
    }

    /**
     * 使用方法
     * @param paramDto
     * @return
     */
    @Override
    public Result use(TimeUseMethodDto paramDto) {
        Map<String, Object> map = new HashMap<>();
        if (paramDto.getMethodId2() != null){
            FMeasureMethod method2 = fMeasureMethodService.getById(paramDto.getMethodId2());
            if (method2 == null){
                return Result.fail("方法不存在");
            }
            // 将方法的参数取出来
            FTimeSweepParamter parameter2 = JSON.parseObject(method2.getMeasurementParamInfo(), FTimeSweepParamter.class);
            FTimeSweepParamter parameter = new FTimeSweepParamter();
            BeanUtils.copyProperties(paramDto.getParameter(), parameter);
            boolean memberKeys =  compareParam(parameter2, parameter);
            if( memberKeys){
                if (method2.getId().equals(paramDto.getMethodId())){
                    return Result.fail("请勿重复使用");
                }else{
                    method2.setMethodUseTime(LocalDateTime.now());
                }
            }else{
                method2.setMethodUseTime(LocalDateTime.now());
            }
            boolean update = fMeasureMethodService.updateById(method2);
            if (!update){
                return Result.fail("应用失败");
            }
            Result draftButton = createDraftButton(method2.getId(), paramDto.getUpdateBy());
            if (draftButton.getCode()==200){
                map.put("taskId", draftButton.getData());
            }
            map.put("method", method2);
            return Result.ok(map);
        }
        FMeasureMethod method = fMeasureMethodService.getById(paramDto.getMethodId());

        FTimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);

        FTimeSweepParamter newParam = new FTimeSweepParamter();
        // 将方法参数拷贝出来
        BeanUtils.copyProperties(paramDto.getParameter(),newParam);

        // 3. 参数比较
        boolean sameParam = compareParam(parameter, newParam);

        // 1. 判断来源
        if ("0".equals(paramDto.getType())) {
            if (sameParam) {
                return Result.fail("方法参数一致");
            }
            FMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                return Result.fail("新建方法失败");
            }
            map.put("method", newMethod);
            Result draftButton = createDraftButton(newMethod.getId(), paramDto.getUpdateBy());
            if (draftButton.getCode()==200){
                map.put("taskId", draftButton.getData());
            }

            return Result.ok(map);
        }

        // 2. 查询历史方法
        if (method == null) {
            FMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                return Result.fail("新建方法失败");
            }
            map.put("method", newMethod);
            Result draftButton = createDraftButton(newMethod.getId(), paramDto.getUpdateBy());
            if (draftButton.getCode()==200){
                map.put("taskId", draftButton.getData());
            }

            return Result.ok(map);
        }

        if (sameParam) {
            if (method.getId().equals(paramDto.getMethodId())){
                return Result.fail("请勿重复使用");
            }else{
                map.put("method", method);
                Result draftButton = createDraftButton(method.getId(), paramDto.getUpdateBy());
                if (draftButton.getCode()==200){
                    map.put("taskId", draftButton.getData());
                }
                return Result.ok(map);
            }
        }else{
            FMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                return Result.fail("新建方法失败");
            }
            map.put("method", newMethod);
            Result draftButton = createDraftButton(newMethod.getId(), paramDto.getUpdateBy());
            if (draftButton.getCode()==200){
                map.put("taskId", draftButton.getData());
            }

            return Result.ok(map);
        }
    }

    /**
     * 是否保存数据
     * @param taskId
     * @param isSave
     * @return
     */
    @Override
    public Result whetherSaveData(String taskId, Boolean isSave) {
        if (taskId == null){
            return Result.fail("请先创建草稿");
        }
        if (isSave){
            // 根据任务id查询测量数据表的测量数据
            QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
            wrapper.eq("task_id", taskId);
            wrapper.eq("is_save", "1");
            List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
            if (measureDataList.isEmpty()){
                return Result.ok();
            }
            for (FMeasureData measureData : measureDataList) {
                measureData.setIsSave("0");
                // 根据测量数据id和任务id查询分析数据
                QueryWrapper<FMeasureAnalysisData> wrapper1 = new QueryWrapper<>();
                wrapper1.eq("task_id", taskId);
                wrapper1.eq("measure_data_id", measureData.getId());
                FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(wrapper1);
                analysisData.setIsSave("0");
                boolean update = fMeasureAnalysisDataService.updateById(analysisData);
                if (!update){
                    return Result.fail("数据保存失败");
                }
            }
            boolean update = fMeasureDataService.updateBatchById(measureDataList);
            if ( update){
                return Result.ok();
            }else{
                return Result.fail("数据保存失败");
            }
        }else{
            // 根据任务id查询测量数据表
            QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
            wrapper.eq("task_id",taskId);
            wrapper.eq("is_save","1");
            List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
            if(measureDataList.isEmpty()){
                return Result.ok();
            }
            // 根据任务id查询任务数据
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            List<FTaskSpecimen> fTaskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FMeasureData measureData : measureDataList) {
                // 根据测量id任务id以及is_save字段为1的条件去分析数据表中查询数据
                QueryWrapper<FMeasureAnalysisData> wrapper1 = new QueryWrapper<>();
                wrapper1.eq("task_id", taskId);
                wrapper1.eq("measure_id", measureData.getId());
                wrapper1.eq("is_save","1");
                FMeasureAnalysisData measureAnalysis = fMeasureAnalysisDataService.getOne(wrapper1);
                for (FTaskSpecimen fTaskSpeciman : fTaskSpecimen) {
                    if (fTaskSpeciman.getSpecimenKey().equals(measureAnalysis.getSampleKey())) {
                        fTaskSpeciman.setDeleteType("0");
                    }
                }
                // 删除当前分析数据
                boolean remove = fMeasureAnalysisDataService.removeById(measureAnalysis.getId());
                // 删除当前测量数据
                boolean remove1 = fMeasureDataService.removeById(measureData.getId());
                if (!remove&&!remove1) {
                    return Result.fail("删除失败");
                }
            }
            task.setSampleKey(JSON.toJSONString(fTaskSpecimen));
            boolean update = fMeasureTaskService.updateById(task);
            if (update) {
                return Result.ok("删除成功");
            } else {
                return Result.fail("删除失败");
            }
        }
    }

    /**
     * 创建草稿按钮
     * @param methodId
     * @param updateBy
     * @return
     */

    @Override
    public Result createDraftButton(String methodId, String updateBy) {
        if (methodId == null) {
            return Result.fail("请先创建方法或者选择方法");
        }
        // 根据方法id查询是否有任务使用该方法,查询条件分别是方法id,最近创建时间,只取一条,如果不存在则查询方法,使用方法名称+草稿凑成任务名称
        QueryWrapper<FMeasureTask> wrapper = new QueryWrapper<>();
        wrapper.eq("method_key", methodId);
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 1");
        FMeasureTask task = fMeasureTaskService.getOne(wrapper);
        FMeasureTask fMeasureTask = new FMeasureTask();
        SysUserPO sysUserPO = userManageDAO.selectOne(Wrappers.lambdaQuery(SysUserPO.class).eq(SysUserPO::getUsername, updateBy));
        if (sysUserPO == null){
            return Result.fail("用户不存在");
        }
        String userId = sysUserPO.getId();
        if (task == null) {
            // 根据方法id查询方法名称,将名称+草稿赋值到任务名称
            FMeasureMethod method = fMeasureMethodService.getById(methodId);
            if (method==null){
                return Result.fail("请先创建方法");
            }
            fMeasureTask.setTaskName(method.getMethodName() + "草稿");
        } else {
            fMeasureTask.setTaskName(task.getTaskName() + "草稿");
        }
        fMeasureTask.setUserId(userId);
        fMeasureTask.setMethodKey(methodId);
        fMeasureTask.setMeasureType(MeasureTypeEnum.TIME_SCAN.getDescription());
        fMeasureTask.setCreateTime(LocalDateTime.now());
        fMeasureTask.setIsSave("0");
        fMeasureTask.setIsDelete(createType);
        boolean save = fMeasureTaskService.save(fMeasureTask);
        return save ? Result.ok(fMeasureTask.getId()) : Result.fail();
    }

    /**
     * 查询样本列表按钮
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

    /**
     * 查询样本表头
     * @param taskId
     * @return
     */
    @Override
    public Result<List<FHeadVO>> querySampleHeads(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.TIME_SCAN.getDescription(), null);
            return Result.ok(preHead);
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.TIME_SCAN.getDescription(), null);
            return Result.ok(preHead);
        } else {
            FTaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), FTaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                List<FHeadVO> preHead = fMeasureTaskMapper.getPreHead(MeasureTypeEnum.TIME_SCAN.getDescription(), null);
                return Result.ok(preHead);
            }
        }
    }

    /**
     * 添加或删除样本
     * @param dto
     * @return
     */
    @Override
    public Result addOrDeleteSamplePort(FAddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            return Result.fail("请先创建方法");
        }
        if (dto.getHeads() == null || dto.getHeads().isEmpty()) {
            return Result.fail("至少需要一个表头");
        }

        if (dto.getHeads() != null && dto.getHeads().size() > 10) {
            return Result.fail("表格新增列数不能超过10列");
        }
        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            FMeasureTask newTask = new FMeasureTask();

            FMeasureMethod method = fMeasureMethodService.getById(dto.getMethodId());
            SystemUserManagePO po = systemUserManageDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, dto.getUpdateBy()));
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

        /// 校验是否有重复的样品编号
        // 取出所有样品编号（不做过滤）
        List<String> sampleNos = dto.getNewSamples().stream()
                .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleNumber())
                .collect(Collectors.toList());

        // 校验是否存在空编号（null、""、空格都算空）
        boolean hasEmpty = sampleNos.stream()
                .anyMatch(no -> no == null || no.trim().isEmpty());
        if (hasEmpty) {
            return Result.fail("存在空的样品编号");
        }

        // 校验样品编号长度是否超过 256
        boolean tooLongNo = sampleNos.stream()
                .anyMatch(no -> no.length() > 256);
        if (tooLongNo) {
            return Result.fail("样品编号长度不能超过256个字符");
        }

        // 校验样品名称长度是否超过 256
        boolean tooLongName = dto.getNewSamples().stream()
                .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleName())
                .filter(Objects::nonNull)
                .anyMatch(name -> name.length() > 256);
        if (tooLongName) {
            return Result.fail("样品名称长度不能超过256个字符");
        }


        // 查重：找到重复的样品编号
        Set<String> duplicates = sampleNos.stream()
                .filter(no -> Collections.frequency(sampleNos, no) > 1)
                .collect(Collectors.toSet());

        if (!duplicates.isEmpty()) {
            return Result.fail("样品编号重复: " + String.join(",", duplicates));
        }
        if(taskSpecimen == null){
            taskSpecimen = new ArrayList<>();
        }
        if (taskSpecimen != null){
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
                if (newSample.getSampleNumber() == null) {
                    return Result.fail("样品编号不能位空");
                }
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
            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(dto.getDeviceSerialNumber());
            concisePublicUtilService.changeSampleLocation(task.getId(), usefulLocation);
        }

        return Result.ok();
    }

    /**
     * 检测任务状态接口
     * @param taskId
     * @return
     */
    @Override
    public Result detectionTaskStatus(String taskId) {
        if (taskId == null) {
            return Result.fail("任务id不能为空");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        return Result.ok(!task.getIsSave().equals("0"));
    }

    /**
     * 保存草稿按钮接口
     * @param taskId
     * @param taskName
     * @return
     */
    @Override
    public Result saveDraftButton(String taskId, String taskName) {
        if (!StringUtils.hasText(taskId)) {
            return Result.fail("请先创建任务");
        }
        // 根据任务id查询任务
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (taskName != null){
            task.setTaskName(taskName);
        }
        // 根据任务id和is_save字段为1的条件查询
        QueryWrapper<FMeasureData> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId).eq("is_save", "1");
        List<FMeasureData> measureDataList = fMeasureDataService.list(wrapper);
        if (measureDataList == null) {
            return Result.ok();
        }
        for (FMeasureData measureData : measureDataList) {
            // 根据测量id,任务id还有is_save为1的条件查询分析数据
            QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("measure_data_id", measureData.getId()).eq("task_id", taskId).eq("is_save", "1");
            FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(queryWrapper);
            analysisData.setIsSave("0");
            boolean update = fMeasureAnalysisDataService.updateById(analysisData);
            measureData.setIsSave("0");
            boolean update1 = fMeasureDataService.updateById(measureData);
            if(!(update&&update1)){
                return Result.fail("保存草稿失败");
            }
        }
        task.setEndSaveTime(LocalDateTime.now());
        boolean update = fMeasureTaskService.updateById(task);
        if ( update){
            return Result.ok();
        }else {
            return Result.fail("保存草稿失败");
        }
    }


    /**
     * 查询样品数据列表
     * @param taskId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result querySampleDataList(String taskId, String type, Integer digit) {
        // 先对前端传过来的数据进行校验
        if (taskId == null || taskId.equals("")) {
            return Result.fail("任务id不能为空");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        List<FSampleListVo> listVos = new ArrayList<>();

        if (taskSpecimen != null){
            for (FTaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getDeleteType().equals("1")&&type==null){
                    continue;
                }
                FSampleListVo vo = new FSampleListVo();
                FMeasureSample sample = fMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    return Result.fail("样品不存在");
                }
                if (taskSpeciman.getIsTested().equals("0")) {
                    vo.setTitle(sample.getSampleName());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisId(null);
                    vo.setAnalysisNumber(1);
                    vo.setColor(null);
                    vo.setData(null);
                    vo.setDeleteType("0");
                } else {
                    QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", taskId);
                    queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                    queryWrapper.eq("analysis_type", "5");
                    if (type == null) {
                        queryWrapper.eq("is_delete", "0");
                    }
                    queryWrapper.orderByDesc("analysis_number");
                    queryWrapper.last("limit 1");
                    FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getOne(queryWrapper);
                    if (measureData == null) {
                        continue;
                    }
                    vo.setTitle(sample.getSampleName());
                    vo.setAnalysisId(measureData.getId());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisNumber(measureData.getAnalysisNumber());
                    vo.setColor(measureData.getColor());
                    vo.setData(getSampleData(measureData, digit));
                    vo.setDeleteType(measureData.getIsDelete());
                }
                vo.setType("0");
                listVos.add(vo);
            }
        }
        return Result.ok(listVos);
    }

    /**
     * 删除样品列表
     * @param taskId
     * @param sampleId
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String taskId, String sampleId, String analysisId) {
        // 获取任务数据
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        List<FTaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        for (FTaskSpecimen taskSpecimen : taskSpecimenList) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setDeleteType("1");
                break;
            }
        }
        // 根据样品id和任务id去查询分析数据id进行删除
        QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.eq("sample_key", sampleId);
        queryWrapper.eq("analysis_type", "5");
        queryWrapper.eq("is_delete", "0");
        queryWrapper.orderByDesc("analysis_number");
        queryWrapper.last("limit 1");
        FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getOne(queryWrapper);
        if (measureData == null) {
            return Result.fail("无测量数据");
        }
        measureData.setIsDelete("1");
        boolean update1 = fMeasureAnalysisDataService.updateById(measureData);
        task.setSampleKey(JSON.toJSONString(taskSpecimenList));
        boolean update = fMeasureTaskService.updateById(task);
        if ( update1 && update) {
            return Result.ok("删除成功");
        } else {
            return Result.fail("删除失败");
        }
    }

    /**
     * 查询分析数据列表
     * @param taskId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result queryAnalysisDataList(String taskId, String type, Integer digit) {
        // 根据任务id去查询分析数据
        QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.ne("analysis_type", "5");
        if(type==null||type.equals("")){
            queryWrapper.eq("is_delete", "0");
        }
        List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(queryWrapper);
        List<FSampleListVo> listVos = new ArrayList<>();
        for (FMeasureAnalysisData tMeasureAnalysisData : list) {
            FSampleListVo vo = new FSampleListVo();
            vo.setAnalysisId(tMeasureAnalysisData.getId());
            vo.setTitle(tMeasureAnalysisData.getAnalysisName());
            vo.setColor(tMeasureAnalysisData.getColor());
            vo.setDeleteType(tMeasureAnalysisData.getIsDelete());
            List<String> results = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
            List<FPeakValleyMarking> gauges = JSON.parseArray(tMeasureAnalysisData.getGaugePoint(), FPeakValleyMarking.class);
            List<FPeakValleyMarking> peaks = JSON.parseArray(tMeasureAnalysisData.getPeak(), FPeakValleyMarking.class);
            List<FPeakValleyMarking> grades = JSON.parseArray(tMeasureAnalysisData.getGrain(), FPeakValleyMarking.class);
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
                    for (FPeakValleyMarking gauge : gauges) {
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
                    for (FPeakValleyMarking peak : peaks) {
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
                    for (FPeakValleyMarking grade : grades){
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
            vo.setData(resultList);
            vo.setType("1");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    /**
     * 删除分析数据
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            return Result.fail("分析数据不存在");
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = fMeasureAnalysisDataService.updateById(byId);
        if (update){
            return Result.ok(true);
        }else {
            return Result.fail(false);
        }
    }

    /**
     * 恢复删除的测量数据
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
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            if (task == null) {
                return Result.fail("任务不存在");
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
                return Result.fail("分析数据不存在");
            }
            byId.setIsDelete("0");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            if (update1 && update){
                return Result.ok(true);
            }else{
                return Result.fail(false);
            }
        }else{
            // 根据变换数据id查询变换数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                return Result.fail("变换数据不存在");
            }
            byId.setIsDelete("0");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            if (update){
                return Result.ok(update);
            }else{
                return Result.fail(false);
            }
        }
    }

    /**
     * 更新数据颜色
     * @param paramDto
     * @return
     */
    @Override
    @DS("concise")
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(FUpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()) {
            return Result.fail("参数不能为空");
        }

        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;


        // 分析数据
        FMeasureAnalysisData analysis = fMeasureAnalysisDataService.getById(dataId);
        if (analysis == null) {
            return Result.fail("样品数据不存在");
        }
        analysis.setColor(color);
        updated = fMeasureAnalysisDataService.updateById(analysis);


        if (!updated) {
            return Result.fail("更新失败");
        }

        return Result.ok();
    }

    /**
     * 查询标记点数据列表
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
        if (measureData.getGaugePoint()!=null){
            gaugePoints = JSON.parseArray(measureData.getGaugePoint(), FPeakValleyMarking.class);
            if (digit!=null) {
                for (FPeakValleyMarking data : gaugePoints){
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
                }
            }
        }
        return Result.ok(gaugePoints);
    }

    /**
     * 添加标记点
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointPort(FAddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            return Result.fail("请选择标记点");
        }
        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            return Result.fail("分析数据不存在");
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
                return Result.fail("时间 " + xStr + " 不存在，请重新选择");
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
        return update?Result.ok(true):Result.fail(false);
    }

    /**
     * 添加标记点备注
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointRemarks(FPeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark()==null){
            return Result.fail("请先添加备注");
        }
        // 根据测量id去分析数据表中进行查询
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData==null){
            return Result.fail("分析数据不存在");
        }
        List<FPeakValleyMarking> list = JSON.parseArray(tMeasureData.getGaugePoint(), FPeakValleyMarking.class);
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (FPeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        tMeasureData.setGaugePoint(JSON.toJSONString(list));
        boolean update = fMeasureAnalysisDataService.updateById(tMeasureData);
        return update?Result.ok():Result.fail();
    }

    /**
     * 获取峰谷数据
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
        if (measureData.getPeak()!=null){
            peaks = JSON.parseArray(measureData.getPeak(), FPeakValleyMarking.class);
            dataAll.addAll( peaks);
        }
        if (measureData.getGrain()!=null){
            grains = JSON.parseArray(measureData.getGrain(), FPeakValleyMarking.class);
            dataAll.addAll( grains);
        }
        if (dataAll!=null && dataAll.size()!=0){
            if (digit!=null){
                for (FPeakValleyMarking data : dataAll){
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
                }
            }
        }
        return Result.ok(dataAll);
    }

    /**
     * 寻峰寻谷
     * @param paramDto
     * @return
     */
    @Override
    public Result peakSearchingPort(FSeekingPeaksValleysDto paramDto) {
        if (paramDto.getDataId() == null||paramDto.getDataId().equals("")) {
            return Result.fail("数据ID不能为空");
        }
        if (paramDto.getHeight() == null||paramDto.getHeight().equals("")) {
            paramDto.setHeight(50.0);
        }
        if (paramDto.getDistance() == null||paramDto.getDistance().equals("")){
            paramDto.setDistance("1");
        }
        paramDto.setHeight(paramDto.getHeight()/100);
        FMeasureAnalysisData tMeasureAnalysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureAnalysisData==null){
            return Result.fail("测量数据不存在");
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
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING_AND_VALLEY_SEARCHING, request, Map.class).getBody();
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        if(xPeak==null||yPeak==null){
            return Result.fail("height参数太高");
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
        if (analysisData==null){
            return Result.fail("分析数据不存在");
        }
        analysisData.setPeak(JSON.toJSONString(peakData));
        analysisData.setGrain(JSON.toJSONString(valleyData));
        flag=fMeasureAnalysisDataService.updateById(analysisData);
        return flag?Result.ok():Result.fail("数据保存失败");
    }

    /**
     * 添加峰谷备注
     * @param paramDto
     * @return
     */
    @Override
    public Result addPeakValleyRemarks(FPeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark()==null){
            return Result.fail("请先添加备注");
        }
        // 根据测量id去分析数据表中进行查询
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData==null){
            return Result.fail("测量数据不存在");
        }
        // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
        List<FPeakValleyMarking> list;
        if (paramDto.getType().equals("波峰")){
            list = JSON.parseArray(tMeasureData.getPeak(), FPeakValleyMarking.class);
        }else{
            list = JSON.parseArray(tMeasureData.getGrain(), FPeakValleyMarking.class);
        }
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (FPeakValleyMarking marking : list) {
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
        boolean update = fMeasureAnalysisDataService.updateById(tMeasureData);
        return update?Result.ok():Result.fail();
    }

    /**
     * 设置测量参数
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result setUpParametersPort(String methodId, String deviceSerialNumber) {
        // 根据序列号查询设备ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        FMeasureMethod method = fMeasureMethodService.getOne(new QueryWrapper<FMeasureMethod>().eq("id", methodId).eq("measurement_type","时间扫描"));
        if (method == null) {
            return Result.fail("方法不存在");
        }
        FTimeSweepParamter time = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);
        List<Command> commands = new ArrayList<>();
        commands.add(buildCommand(SetParamEnum.SetPhotoMode, time.getSpectralBandwidth()));
        commands.add(buildCommand(SetParamEnum.SetChangeLampWl, time.getChangeLampWaveLength()));

        if (time.getLuminosity().equals("2")||time.getLuminosity().equals("3")) {
            commands.add(buildCommand(SetParamEnum.SetLampPos, time.getEnergyLamp()));
            commands.add(buildCommand(SetParamEnum.SetGain, time.getGain()));
        }

        R<String> stringR = devMessageUtil.timeSweepSet(commands, ip);
        return Result.ok(stringR.getData());
    }

    /**
     * 查询历史方法
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
        if (methodPage.getTotal() == 0){
            return Result.fail("没有查询到数据");
        }

        return Result.ok(methodPage);
    }

    /**
     * 查询历史任务
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
     * 查询算术源数据
     * @param taskId
     * @return
     */
    @Override
    public Result queryArithmeticSourceData(String taskId) {
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null){
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey()==null||task.getSampleKey().equals("[]")){
            return Result.ok(new ArrayList<>());
        }
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        List<Map<String,String>> list = new ArrayList<>();
        for (FTaskSpecimen taskSpeciman : taskSpecimen) {
            if(taskSpeciman.getIsTested().equals("1")&&taskSpeciman.getDeleteType().equals("0")){
                Map<String, String> map = new HashMap<>();
                FMeasureAnalysisData one = fMeasureAnalysisDataService.getOne(new QueryWrapper<FMeasureAnalysisData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", taskSpeciman.getSpecimenKey())
                        .orderByDesc("analysis_number")
                        .last("limit 1"));
                FMeasureSample byId = fMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
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
    public Result fourOperationsPort(FFourOperationsParamDto paramDto) {
        if (paramDto.getAnalysisDataName()==null||paramDto.getAnalysisDataName().equals("")){
            return Result.fail("请输入变换名称");
        }
        if (paramDto.getTaskId()==null||paramDto.getTaskId().equals("")) {
            return Result.fail("缺少参数");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 对所有传过来的对象数据进行校验,判断是否为null或""
        if (paramDto.getMeasureDataId1()==null||paramDto.getMeasureDataId1().equals("")){
            return Result.fail("请选择测量数据1");
        }
        if (paramDto.getMeasureDataId2()==null||paramDto.getMeasureDataId2().equals("")){
            return Result.fail("请选择测量数据2");
        }
        if (paramDto.getArithmeticOperators()==null||paramDto.getArithmeticOperators().equals("")){
            return Result.fail("请选择运算符");
        }
        if (paramDto.getDataType()==null||paramDto.getDataType().equals("")){
            return Result.fail("请选择数据类型");
        }
        // 根据测量数据id查询测量数据
        FMeasureAnalysisData measureData1 = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId1());
        List<String> xList = new ArrayList<>();
        List<String> yList1 = new ArrayList<>();
        List<String> yList2 = new ArrayList<>();
        List<String> list = JSON.parseArray(measureData1.getAnalysisResult(), String.class);
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
            FMeasureAnalysisData measureData2 = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId2());
            // 将测量数据1和2的测量数据提取出来
            List<String> list2 = JSON.parseArray(measureData2.getAnalysisResult(), String.class);
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
    public Result advancedOperationsPort(FAdvancedOperationsDto paramDto) {
        if (paramDto.getAnalysisDataName()==null||paramDto.getAnalysisDataName().equals("")){
            return Result.fail("请输入变换名称");
        }
        if (paramDto.getTaskId()==null||paramDto.getTaskId().equals("")) {
            return Result.fail("缺少参数");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 将所有传过来的数据校验是否为空是否是""
        if (paramDto.getMeasureDataId()==null||paramDto.getMeasureDataId().equals("")){
            return Result.fail("请选择测量数据");
        }
        if (paramDto.getDataType()==null||paramDto.getDataType().equals("")){
            return Result.fail("请选择变换类型");
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
    public Result smoothnessPort(FSmoothnessDto paramDto) {
        if (paramDto.getAnalysisDataName()==null||paramDto.getAnalysisDataName().equals("")){
            return Result.fail("请输入变换名称");
        }
        // 将所有参数校验是否为空或""
        if (paramDto.getMeasureDataId()==null||paramDto.getMeasureDataId().equals("")){
            return Result.fail("请选择测量数据");
        }
        if (paramDto.getTaskId()==null||paramDto.getTaskId().equals("")){
            return Result.fail("缺少参数");
        }
        if (paramDto.getCoefficient()==null||paramDto.getCoefficient().equals("")){
            return Result.fail("请输入系数");
        }
        if (paramDto.getPointNum()==null||paramDto.getPointNum().equals("")){
            return Result.fail("请输入点数");
        }
        if (Double.valueOf(paramDto.getCoefficient())>Double.valueOf(paramDto.getPointNum())){
            return Result.fail("系数不能大于点数");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 根据测量数据的id查询出测量数据
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(tMeasureData.getAnalysisResult(), String.class);
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
    public Result differentialPort(FDifferentialDto paramDto) {
        if (paramDto.getAnalysisDataName()==null||paramDto.getAnalysisDataName().equals("")){
            return Result.fail("请输入变换名称");
        }
        // 将所有参数校验是否为空或""
        if (paramDto.getMeasureDataId()==null||paramDto.getMeasureDataId().equals("")){
            return Result.fail("请选择测量数据");
        }
        if (paramDto.getTaskId()==null||paramDto.getTaskId().equals("")){
            return Result.fail("缺少参数");
        }
        if (paramDto.getCoefficient()==null||paramDto.getCoefficient().equals("")){
            return Result.fail("请输入系数");
        }
        if (paramDto.getPointNum()==null||paramDto.getPointNum().equals("")){
            return Result.fail("请输入点数");
        }
        if (Double.valueOf(paramDto.getCoefficient())>Double.valueOf(paramDto.getPointNum())){
            return Result.fail("系数不能大于点数");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 根据测量数据的id查询出测量数据
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        List<String> list = JSON.parseArray(tMeasureData.getAnalysisResult(), String.class);
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 光度模式转换
     * @param paramDto
     * @return
     */
    @Override
    public Result luminanceModeConversionPort(FLuminosityConversionDto paramDto) {
        if (paramDto.getAnalysisDataName()==null||paramDto.getAnalysisDataName().equals("")){
            return Result.fail("请输入变换名称");
        }
        if (paramDto.getTaskId()==null||paramDto.getTaskId().equals("")) {
            return Result.fail("缺少参数");
        }
        if (paramDto.getMeasureDataId()==null||paramDto.getMeasureDataId().equals("")) {
            return Result.fail("请选择测量数据");
        }
        if (paramDto.getMethodId()==null||paramDto.getMethodId().equals("")) {
            return Result.fail("缺少参数");
        }
        if (paramDto.getDataType()==null||paramDto.getDataType().equals("")) {
            return Result.fail("变换类型错误");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<FMeasureAnalysisData> analysisData = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 根据方法id查询方法表的数据
        FMeasureMethod method = fMeasureMethodService.getById(paramDto.getMethodId());
        // 获取方法表中的方法参数信息
        String measurementParamInfo = method.getMeasurementParamInfo();
        FSpectrumParameter spectrumParameter = JSON.parseObject(measurementParamInfo, FSpectrumParameter.class);
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
        boolean save = fMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
                BigDecimal value = new BigDecimal(split[1]);
                value = value.setScale(digit, RoundingMode.HALF_UP);
//                map.put("x", split[0]);
//                map.put("y", value.stripTrailingZeros().toPlainString());
                map.put("x", split[0]);
                map.put("y", DataCountUtil.addZero(Double.valueOf(value.stripTrailingZeros().toPlainString()), digit));

            } else {
//                map.put("x", split[0]);
//                map.put("y", split[1]);
                map.put("x", split[0]);
                map.put("y", DataCountUtil.addZero(Double.valueOf(split[1]), 8));
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
     * 数字字符串归一化（防止 360 / 360.0 / 3.6E2 比较失败）
     */
    private String normalizeNumberString(String numStr) {
        return new BigDecimal(numStr)
                .stripTrailingZeros()
                .toPlainString();
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
     * 比较参数
     * @param method
     * @param newParam
     * @return
     */
    private boolean compareParam(FTimeSweepParamter method, FTimeSweepParamter newParam) {
        String dbJson = JSON.toJSONString(method, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newParam, SerializerFeature.SortField);

        return dbJson.equals(frontJson);
    }

    /**
     * 创建新的任务
     */
    private FMeasureMethod createNewMethod(TimeUseMethodDto paramDto){
        FMeasureMethod fMeasureMethod = new FMeasureMethod();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String collect = new Random().ints(8, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());

        fMeasureMethod.setMethodName(paramDto.getMeasureType()  + collect);

        FTimeSweepParamter parameter = new FTimeSweepParamter();
        // 将前端参数复制给数据库参数
        BeanUtils.copyProperties(paramDto.getParameter(), parameter);
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
     * @param taskId 任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType 导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds, Integer digit) {
        // log.info("===== 导出流程开始 =====");
        // log.info("开始导出，taskId: {}, fileType: {}, 需要筛选的analysisId数量: {}", 
                // taskId, fileType, analysisIds != null ? analysisIds.size() : 0);

        // 1. 基础参数校验阶段（保持不变）
        // log.info("【步骤1/5】开始基础参数校验");
        if (!StringUtils.hasText(taskId)) {
            // log.error("【步骤1/5】校验失败：缺少任务id");
            return R.fail("缺少任务id");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            // log.error("【步骤1/5】校验失败：任务不存在，taskId: {}", taskId);
            return R.fail("任务不存在");
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
            return R.fail("不支持的导出格式，仅支持word/txt/xlsx");
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
                // wavelengths.size(), sampleYMap.size());

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
                return R.fail("文件生成失败：文件不存在");
            }
        } catch (Exception e) {
            // log.error("【步骤5/5】文件生成异常", e);
            if (file.exists()) {
                boolean deleted = file.delete();
                // log.info("【步骤5/5】删除异常文件: {}", deleted ? "成功" : "失败");
            }
            return R.fail("文件生成失败：" + e.getMessage());
        } finally {
            // log.info("===== 导出流程结束 =====");
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
            // log.debug("【generateWordFile】写入波长: {}", wavelength);

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

        // 2. 转换表头（处理空表头和null元素，修复原代码循环语法错误）
        List<List<String>> excelHeads = new ArrayList<>();
        for (int i = 0; i < heads.size(); i++) {
            FHeadVO head = heads.get(i);
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
     * 表格头
     * @param taskId
     * @param methodId
     * @param allDisplay
     * @return
     */
    @Override
    public Result<List<FHeadVO>> tableHead(String taskId, String methodId, Boolean allDisplay) {
        // 根据方法id查找方法
        FMeasureMethod method = fMeasureMethodService.getById(methodId);
        TimeSweepParamter parameter = JSON.parseObject(method.getMeasurementParamInfo(), TimeSweepParamter.class);
        List<FHeadVO> ans = new ArrayList<>();

        // 1. 编号
        ans.add(new FHeadVO("编号", "id", "system", "编号"));

        ans.add(new FHeadVO("峰/谷", "peakValley", "system", "编号"));

        ans.add(new FHeadVO("时间", "TIME", "measure", "波长(nm)"));

        // 2. 波长 + 光度模式
        String mode = parameter.getLuminosity(); // abs / t% / es / er
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
        }


        // 3. 备注
        ans.add(new FHeadVO("备注", "remark", "other", "备注"));


        return Result.ok(ans);
    }

    /**
     * 表格数据
     * @param taskId
     * @param sampleId
     * @return
     */
    @Override
    public List<Map<String, Object>> queryTableData(String taskId, String sampleId) {
        // 校验参数
        if (StringUtils.isEmpty(taskId) || StringUtils.isEmpty(sampleId)) {
            return null;
        }
        // 根据任务id和样品id查询分析数据
        QueryWrapper<FMeasureAnalysisData> eq = new QueryWrapper<>();
        eq.eq("task_id", taskId);
        eq.eq("sample_key", sampleId);
        eq.orderByDesc("analysis_number");
        eq.last("LIMIT 1");
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getOne(eq);
        if (analysisData == null) {
            return null;
        }
        // 解析分析数据
        List<String> result = JSON.parseArray(analysisData.getAnalysisResult(), String.class);
        // 解析峰谷数据
        List<FPeakValleyMarking> peaks = JSON.parseArray(analysisData.getPeak(), FPeakValleyMarking.class);
        List<FPeakValleyMarking> valleys = JSON.parseArray(analysisData.getGrain(), FPeakValleyMarking.class);
        List<Map<String, Object>> arrayList = new ArrayList<>();
        // 根据长度遍历
        for (int i = 0; i < result.size(); i++) {
            // 根据,进行分割数据
            String[] split = result.get(i).split(",");
            Map<String, Object> map = new HashMap<>();
            map.put(SampleTableEnum.ID.getIdentity(), i + 1);
            for (FPeakValleyMarking peak : peaks) {
                if (peak.getX().equals(split[0])){
                    map.put(SampleTableEnum.PEAKVALLEY.getIdentity(), "峰");
                }
            }
            for (FPeakValleyMarking grade : valleys) {
                if (grade.getX().equals(split[0])){
                    map.put(SampleTableEnum.PEAKVALLEY.getIdentity(), "谷");
                }
            }
            map.put(SampleTableEnum.TIME.getIdentity(), split[0]);
            map.put(SampleTableEnum.LUMINOSITY.getIdentity(), split[1]);
            map.put(SampleTableEnum.REMARK.getIdentity(), null);
            arrayList.add(map);
        }
        return arrayList;
    }

    @Override
    public Result uploadImage(SpectrumImageUpload upload) {
        if (upload == null) {
            return Result.fail("参数不能为空");
        }
        String[] split = upload.getImage().split(",");
        String s = split[1];
        String fileType ;
        if (split[0].startsWith("data:image/png")) {
            fileType = "png";
        } else if (split[0].startsWith("data:image/jpeg")) {
            fileType = "jpg";
        } else if (split[0].startsWith("data:image/gif")) {
            fileType = "gif";
        } else {
            fileType = "unknown"; // 兜底
        }
        byte[] bytes;
        try {
            // 用 Apache 的 decodeBase64 方法解码
            bytes = Base64.decodeBase64(s);
        } catch (Exception e) {
            // 解码失败时的处理（比如打印错误信息）
            System.out.println("Base64 解码失败：" + e.getMessage());
            return Result.fail("图片解码失败"); // 根据你的业务返回错误
        }
        if(upload.getImageType() == null){
            return Result.fail();
        }

        if (upload.getImageType().equals("0")) {
            LambdaQueryWrapper<FMeasureAnalysisImage> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(FMeasureAnalysisImage::getAnalysisId,upload.getId());
            FMeasureAnalysisImage byId = fMeasureAnalysisImageService.getOne(lambdaQueryWrapper);
//            QueryWrapper<FMeasureAnalysisImage> analysisId = new QueryWrapper<FMeasureAnalysisImage>().eq("analysis_id", upload.getId());
//            analysisId.last("limit 1");
//
//            FMeasureAnalysisImage byId = fMeasureAnalysisImageService.getOne(analysisId);
            if (byId == null) {
                return Result.fail("图片不存在");
            }
            byId.setImageType(fileType);
            byId.setImageBase64(bytes);
            // 将当前日期时间存储

            byId.setCreateTime(LocalDateTime.now());
            boolean update = fMeasureAnalysisImageService.updateById(byId);
            return Result.ok(update);

        }else{
            // 根据任务id查询任务附属表的内容
            FMeasureTaskAttachment byId = fMeasureTaskAttachmentService.getOne(Wrappers.lambdaQuery(FMeasureTaskAttachment.class).eq(FMeasureTaskAttachment::getTaskId, upload.getId()).last("limit 1"));
            if (byId == null){
                return Result.fail("任务附属不存在");
            }
            if ("0".equals(upload.getType())){
                byId.setOverlapImage(bytes);
            }else if ("1".equals(upload.getType())){
                byId.setTiledImage(bytes);
            }else if ("2".equals(upload.getType())){
                byId.setAddrImage(bytes);
            }else{
                return Result.fail("Type类型错误");
            }
            boolean update = fMeasureTaskAttachmentService.updateById(byId);
            return Result.ok(update);
        }
    }

}
