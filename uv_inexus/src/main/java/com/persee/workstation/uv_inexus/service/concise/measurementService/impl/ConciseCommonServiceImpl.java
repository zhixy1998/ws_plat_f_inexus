package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConciseCommonService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.PhotometricService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.RationService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service("ConciseCommonService")
public class ConciseCommonServiceImpl implements ConciseCommonService {

    @Autowired
    private SpectrumConciseService spectrumConciseService;
    @Autowired
    private FMeasureMethodService fMeasureMethodService;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private FMeasureTaskService fMeasureTaskService;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private PhotometricService photometricService;

    @Autowired
    private RationService rationService;


    /**
     * 查询默认参数
     * @param measureType
     * @return
     */
    @Override
    public Result queryDefaultParameters(String measureType) {
        // 根据测量类型以及统一的"默认方法"的方法名称进行查询默认方法参数
        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_name", "默认方法");
        wrapper.eq("measurement_type", measureType);
        FMeasureMethod one = fMeasureMethodService.getOne(wrapper);
        if (one == null) {
            return Result.fail(measureType+"没有默认方法");
        }
        return Result.ok(one);

    }

    @Override
    public Result<FMethodListWithPageVO> searchMethodButton(FQueryMethodWithPageDto dto) {
        // 对传入的参数进行校验
        if (!StringUtils.hasText(dto.getUpdateBy())) {
            return Result.fail("用户不能为空");
        }
        if (!StringUtils.hasText(dto.getMeasureType())) {
            return Result.fail("方法类型不能为空");
        }

        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1);
        }

        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(10);
        }
        // 设置查询条件
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", dto.getUpdateBy());
        wrapper.eq("measurement_type", dto.getMeasureType());
        if (StringUtils.hasText(dto.getMethodName())) {
            wrapper.like("method_name", dto.getMethodName());
        }
        // 分页查询
        int pageNum = dto.getPageNum();
        int pageSize = dto.getPageSize();
        Page<TMeasureMethod> page = tMeasureMethodService.page(new Page<>(pageNum, pageSize), wrapper);
        List<TMeasureMethod> records = page.getRecords();
        FMethodListWithPageVO ans = new FMethodListWithPageVO();
        ans.setTotal(page.getTotal());
        ans.setSize(page.getSize());
        ans.setCurrent(page.getCurrent());
        if (dto.getMeasureType().equals(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription())) {
            List<FMethodInfoVO> infoVOList = new ArrayList<>();
            for (TMeasureMethod record : records) {
                FMethodInfoVO info = new FMethodInfoVO();
                BeanUtils.copyProperties(record, info);

                PhotometricParameter parameter = JSON.parseObject(record.getMeasurementParamInfo(), PhotometricParameter.class);
                parameter.setLuminosityName(WaveLengthEnum.getName(parameter.getLuminosity()));
                parameter.setSamplePoolWayName(RepeatConfigEnum.getName(parameter.getSamplePoolWay()));

                info.setMethodParam(JSONObject.parseObject(JSONObject.toJSONString(parameter)));
                infoVOList.add(info);

            }
            ans.setRecords(infoVOList);

        } else if (dto.getMeasureType().equals(MeasureTypeEnum.QUANTITATIVE_MEASUREMENT.getDescription())) {
            List<FMethodInfoVO> infoVOList = new ArrayList<>();
            for (TMeasureMethod record : records) {
                FMethodInfoVO info = new FMethodInfoVO();
                BeanUtils.copyProperties(record, info);
                RationParameter parameter = JSON.parseObject(record.getMeasurementParamInfo(), RationParameter.class);

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
                info.setMethodParam(JSONObject.parseObject(JSONObject.toJSONString(parameter)));

                infoVOList.add(info);
            }
            ans.setRecords(infoVOList);
        } else {
            return Result.fail("该类型暂未开发");
        }
        return Result.ok(ans);
    }

    @Override
    public Result<Page<FMeasureMethodParam>> queryHistoryMethod(QueryMethodDto queryMethodDto) {
        String methodType = queryMethodDto.getMethodType();
        Integer pageSize = queryMethodDto.getPageSize();
        Integer pageNum = queryMethodDto.getPageNum();
        String userName = queryMethodDto.getUpdateBy();
        String selectKey = queryMethodDto.getSelectKey();

        Page<FMeasureMethod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FMeasureMethod> methodLambdaQueryWrapper = new LambdaQueryWrapper<>();
        methodLambdaQueryWrapper.eq(FMeasureMethod::getMeasurementType, methodType)
                .eq(FMeasureMethod::getMethodCreator, userName);
        if (selectKey != null) {
            methodLambdaQueryWrapper.like(FMeasureMethod::getMethodName, selectKey);
        }

        Page<FMeasureMethod> fMeasureMethodPage = fMeasureMethodService.page(page, methodLambdaQueryWrapper);
        Page<FMeasureMethodParam> dtoPage = new Page<>();

        dtoPage.setCurrent(fMeasureMethodPage.getCurrent());
        dtoPage.setSize(fMeasureMethodPage.getSize());
        dtoPage.setTotal(fMeasureMethodPage.getTotal());
        dtoPage.setPages(fMeasureMethodPage.getPages());


        List<FMeasureMethodParam> dtoList = new ArrayList<>();
        fMeasureMethodPage.getRecords().forEach(fMeasureMethod -> {
            FMeasureMethodParam fMeasureMethodParam = new FMeasureMethodParam();
            BeanUtils.copyProperties(fMeasureMethod, fMeasureMethodParam);
            dtoList.add(fMeasureMethodParam);
        });

        dtoPage.setRecords(dtoList);

        return Result.ok(dtoPage);


    }

    @Override
    public Result<FMeasureMethodParam> detailInformation(String methodName, String methodType) {
        LambdaQueryWrapper<FMeasureMethod> fMeasureMethodLambdaQueryWrapper = new LambdaQueryWrapper<>();
      fMeasureMethodLambdaQueryWrapper.eq(FMeasureMethod::getMethodName, methodName).eq(FMeasureMethod::getMeasurementType,methodType);
        FMeasureMethod fMeasureMethod = fMeasureMethodService.getOne(fMeasureMethodLambdaQueryWrapper);
        String measurementParamInfo = fMeasureMethod.getMeasurementParamInfo();
        JSONObject jsonObject = JSONObject.parseObject(measurementParamInfo);
        FMeasureMethodParam fMeasureMethodParam = new FMeasureMethodParam();
        BeanUtils.copyProperties(fMeasureMethod, fMeasureMethodParam);
        fMeasureMethodParam.setMeasurementParamInfo(jsonObject);
        return Result.ok(fMeasureMethodParam);


    }

    @Override
    public Result<Page<FMeasureTask>> queryHistoryTask(SelectTaskDto selectTaskDto) {
        String selectKey = selectTaskDto.getSelectKey();
        String methodType = selectTaskDto.getMethodType();
        String userName = selectTaskDto.getUpdateBy();
        Integer pageNum = selectTaskDto.getPageNum();
        Integer pageSize = selectTaskDto.getPageSize();
        SysUserPO sysUserPO = userMapper.selectByUsername(userName);
        String userId = sysUserPO.getId();
        LambdaQueryWrapper<FMeasureTask> fMeasureTaskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        fMeasureTaskLambdaQueryWrapper.eq(FMeasureTask::getMeasureType, methodType)
                .eq(FMeasureTask::getUserId,userId);
        if (selectKey != null) {
            fMeasureTaskLambdaQueryWrapper.like(FMeasureTask::getTaskName, selectKey);
        }
        Page<FMeasureTask> page = new Page<>(pageNum,pageSize);

        Page<FMeasureTask> fMeasureTaskPage = fMeasureTaskService.page(page, fMeasureTaskLambdaQueryWrapper);
        return Result.ok(fMeasureTaskPage);
    }

    @Override
    public Result<String> updateMethod(UpdateMethodDto updateMethodDto) {
        if (updateMethodDto == null){
            return Result.fail("参数不能为空");
        }
        String methodName = updateMethodDto.getMethodName();
        List<FMeasureMethod> methods = fMeasureMethodService.list();
        for (FMeasureMethod fMeasureMethod : methods) {
            String everyMethodName= fMeasureMethod.getMethodName();
            if (everyMethodName.equals(methodName)) {
                return Result.fail("方法名已存在");
            }
        }
        String methodId = updateMethodDto.getMethodId();
        LambdaQueryWrapper<FMeasureMethod> fMeasureMethodLambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<FMeasureMethod> eq = fMeasureMethodLambdaQueryWrapper.eq(FMeasureMethod::getId, methodId);
        FMeasureMethod method = fMeasureMethodService.getOne(eq);
        BeanUtils.copyProperties(updateMethodDto, method);
        fMeasureMethodService.updateById(method);
        return Result.ok("修改成功");
    }




    }
