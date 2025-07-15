package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddOrDeleteSampleDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricMethodParameterDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.standardPo.TReportStandardDetection;
import com.persee.workstation.uv_inexus.po.standardPo.TReportStandardPretreatment;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.service.standardService.TReportStandardDetectionService;
import com.persee.workstation.uv_inexus.service.standardService.TReportStandardPretreatmentService;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricTableHeadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("photometricService")
public class PhotometricServiceImpl implements PhotometricService {
    @Autowired
    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private TMeasureSampleService tMeasureSampleService;
    @Autowired
    private UserConfigDAO userConfigDAO;
    @Autowired
    private TMeasureMethodService tMeasureMethodService;
    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    private TReportStandardDetectionService tReportStandardDetectionService;

    @Autowired
    private TReportStandardPretreatmentService tReportStandardPretreatmentService;

    @Value("1")
    private String deleteType;

    @Value("0")
    private String createType;


    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, PhotometricParameter photometricParameter) {
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(photometricParameter));
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if (save) {
            return Result.ok("创建成功");
        } else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result updateMethodParameter(PhotometricMethodParameterDto methodDto) {
        // 根据方法id查询当前方法
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id", methodDto.getMethodId()));        // 根据当前方法中的测量标准表id查询测量数据
        // 将ExperimentMethodDto中的值转入SpectrumParameter对象中
        PhotometricParameter param = new PhotometricParameter();
        BeanUtils.copyProperties(methodDto, param);

        String s = JSON.toJSONString(param);
//            String s = new ObjectMapper().writeValueAsString(param);
        method.setMeasurementParamInfo(s);

        if (!methodDto.getMethodName().equals(method.getMethodName())) {
            method.setMethodName(methodDto.getMethodName());
        }
        boolean update = tMeasureMethodService.updateById(method);

        return Result.ok(update);
    }

    @Override
    public Result addOrDeleteSample(AddOrDeleteSampleDTO dto) {
        if (!StringUtils.hasText(dto.getTaskId())) {
            return Result.fail("缺少任务id");
        }

        if (dto.getNewSamples().isEmpty() && dto.getDelSampleIds().isEmpty()) {
            return Result.ok("没有要新增或删除的样品！");
        }

        TMeasureTask task = tMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                specimen.setDeleteType(deleteType);
                LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureData::getTaskId, task.getId());
                TMeasureData one = tMeasureDataService.getOne(queryWrapper);
                if (one != null) {
                    one.setIsDelete(deleteType);
                    tMeasureDataService.updateById(one);
                }
            }
        }

        for (TMeasureSample newSample : dto.getNewSamples()) {
            TaskSpecimen specimen = new TaskSpecimen();
            specimen.setSpecimenKey(newSample.getId());
            specimen.setDeleteType(createType);
            taskSpecimen.add(specimen);
            log.info("新增的样品：{}", newSample);
            tMeasureSampleService.save(newSample);
        }

        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(tMeasureTaskService.updateById(task));
    }

    @Override
    public Result revokeDel(String taskId, String sampleId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        String sampleKey = task.getSampleKey();

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(sampleKey, TaskSpecimen.class);
        for (TaskSpecimen specimen : taskSpecimen) {
            if (specimen.getSpecimenKey().equals(sampleId)) {
                specimen.setDeleteType(createType);
            }
            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                    .eq(TMeasureData::getTaskId, task.getId());
            TMeasureData one = tMeasureDataService.getOne(queryWrapper);
            if (one != null) {
                one.setIsDelete(createType);
                tMeasureDataService.updateById(one);
            }

        }
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);


        return Result.ok(tMeasureTaskService.updateById(task));
    }

    @Override
    public Result queryMethodParam(String methodId) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }

        if (!StringUtils.hasText(method.getMeasurementParamInfo())) {
            return Result.fail("未查询到该方法参数");
        }
        PhotometricParameter param = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        return Result.ok(param);
    }

    @Override
    public Result queryStandardInfo(String standardName) {

        LambdaQueryWrapper<TReportStandardDetection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(TReportStandardDetection::getStandardName, standardName);
        List<TReportStandardDetection> list = tReportStandardDetectionService.list(queryWrapper);
        for (TReportStandardDetection detection : list) {
            LambdaQueryWrapper<TReportStandardPretreatment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TReportStandardPretreatment::getDetectionId, detection.getId());
            List<TReportStandardPretreatment> pretreatmentList = tReportStandardPretreatmentService.list(wrapper);
            detection.setPretreatments(pretreatmentList);
        }

        return Result.ok(list);
    }

    @Override
    public Result<PhotometricTableHeadVO> getTableHead(String taskId, String methodId) {

        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        PhotometricParameter Parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);
        List<String> waveLengths = Parameter.getWaveLengths();
        PhotometricTableHeadVO photometricTableHeadVO = new PhotometricTableHeadVO();
        BeanUtils.copyProperties(Parameter, photometricTableHeadVO);
        return Result.ok(photometricTableHeadVO);
    }

    @Override
    public Result<List<PhotometricSampleAndDataVO>> queryDataInfo(String taskId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<PhotometricSampleAndDataVO> list = new ArrayList<>();

        for (TaskSpecimen specimen : taskSpecimen) {
            PhotometricSampleAndDataVO sampleAndDataVO = new PhotometricSampleAndDataVO();
            sampleAndDataVO.setTaskId(taskId);
            sampleAndDataVO.setSampleId(specimen.getSpecimenKey());

            TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
            sampleAndDataVO.setSample(sample);

            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey());
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);

            List<PhotometricOriginDataInfo> wlDataList = JSON.parseArray(data.getOriginalCalculationData(), PhotometricOriginDataInfo.class);
            sampleAndDataVO.setWavelengthData(wlDataList);

            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId);
            //TODO 添加样品id

            TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);

            List<PhotometricCountDataInfo> countList = JSON.parseArray(one.getAnalysisResult(), PhotometricCountDataInfo.class);
            sampleAndDataVO.setCountData(countList);
            list.add(sampleAndDataVO);

        }
        return Result.ok(list);
    }

    @Override
    public Result savePhotometricMeasureData(LocalDateTime startTime, LocalDateTime endTime, List<PhotometricSampleAndDataVO> vo) {
        for (PhotometricSampleAndDataVO sampleAndDataVO : vo) {
            String taskId = sampleAndDataVO.getTaskId();
            String sampleId = sampleAndDataVO.getSampleId();
            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getTaskId, taskId)
                    .eq(TMeasureData::getSampleKey, sampleId);
            TMeasureData data = tMeasureDataService.getOne(queryWrapper);
            if (data != null) {
                //认为是重测，覆盖数据
                data.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
                data.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
                tMeasureDataService.updateById(data);
            } else {
                TMeasureData tMeasureData = new TMeasureData();
                tMeasureData.setTaskId(taskId);
                tMeasureData.setSampleKey(sampleId);
                tMeasureData.setInstrumentData(JSON.toJSONString(sampleAndDataVO.getMachineData()));
                tMeasureData.setOriginalCalculationData(JSON.toJSONString(sampleAndDataVO.getWavelengthData()));
                tMeasureData.setMeasurementStartTime(startTime);
                tMeasureData.setMeasurementEndTime(endTime);
                tMeasureData.setIsDelete(createType);
                tMeasureDataService.save(tMeasureData);
            }
        }

        return Result.ok(true);
    }

    @Override
    public Result saveDataInfo(PhotometricSampleAndDataVO vo) {

        String taskId = vo.getTaskId();
        String sampleId = vo.getSampleId();
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                .eq(TMeasureData::getSampleKey, sampleId);
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);
        if (data == null || data.getIsDelete().equals(deleteType)) {
            log.info("数据不存在或者已删除");
            return Result.fail("数据不存在或已删除");
        }

        data.setRemark(vo.getRemark());
        data.setSamplingTime(vo.getSampleTime());
        tMeasureDataService.updateById(data);

        return Result.ok( true);
    }
}
