package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.dao.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.userManagePo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.OriginalMeasuredDataVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.ReportParametersVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 光谱扫描服务实现类
 */
@Slf4j
@Service("spectrumService")
public class SpectrumServiceImpl implements SpectrumService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TMeasureStandardService tMeasureStandardService;

    @Autowired
    private TMeasureProjectService tMeasureProjectService;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private UserConfigDAO userConfigDAO;

    @Autowired
    private TMeasureTaskService tMeasureTaskService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TMeasureSampleService tMeasureSampleService;


    /**
     * 查询测量数据
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
        // 将测量的数据从json转换为OriginalMeasuredDataVo对象
        OriginalMeasuredDataVo originalMeasuredDataVo = new OriginalMeasuredDataVo();
        List<String> list = new ArrayList<>();
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();

        try {
            list = objectMapper.readValue(tMeasureData.getOriginalCalculationData(), ArrayList.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        double[] y_data = new double[yList.size()];
        for (int i = 0; i < yList.size(); i++) {
            y_data[i]= Double.parseDouble(yList.get(i));
        }

        double max = Arrays.stream(y_data).max().orElse(0);
        double height = max * 0.2;

        // 调用python接口
        String pythonUrl = "http://192.168.32.62:8002/get_peak/find_peaks";
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
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonUrl, request, Map.class).getBody();
        List<Double> peak_y = (List<Double>) resultMap.get("peak_y");
        List<OriginalMeasuredDataVo> resultList = new ArrayList<>();
        // 将xList和yList转换为OriginalMeasuredDataVo对象
        for (int i = 0; i < xList.size(); i++) {
            OriginalMeasuredDataVo vo = new OriginalMeasuredDataVo();
            vo.setId(i + 1);
            vo.setX(xList.get(i));
            vo.setY(yList.get(i));
            vo.setFeature("0");
            vo.setTag("-"); // 或其他逻辑判断
            resultList.add(vo);
        }
        // 遍历resultList，将peak_y中的数据与resultList中的数据进行匹配
        for (OriginalMeasuredDataVo measuredDataVo : resultList) {
            for (Double str : peak_y) {
                // 将measuredDataVo.getY()转换成 double类型和 str比较
                Double parsed = Double.parseDouble(measuredDataVo.getY());
                if (parsed.equals(str)) {
                    measuredDataVo.setFeature("1");
                }
            }
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

    @Override
    public Result queryProjectTaskMethod(String updateBy) {
        // 根据用户名查询所有项目
        List<TMeasureProject> projectList = tMeasureProjectService.list(Wrappers.lambdaQuery(TMeasureProject.class).eq(TMeasureProject::getCreatorKey, updateBy));
        // 查询所有项目下最近创建的方法
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>()
                .in("project_item_key", projectList.stream().map(TMeasureProject::getId).collect(Collectors.toList()))
                .eq("measurement_type","光谱扫描")
                .orderByDesc("method_create_time")
                .last("LIMIT 1"));
        // 根据方法中的项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
        // 根据方法中的测量标准表id查询测量数据
        TMeasureStandard standard = tMeasureStandardService.getById(method.getStandardKey());
        // 将测量数据中的json转换成对象存入返回值中
        SpectrumBasicParameterVo spectrumBasicParameterVo = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            spectrumBasicParameterVo = objectMapper.readValue(standard.getMeasurementParamInfo(), SpectrumBasicParameterVo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // 将项目,方法名称存入返回对象中
        spectrumBasicParameterVo.setMethodId(method.getId());
        spectrumBasicParameterVo.setTaskName(method.getMethodName());
        spectrumBasicParameterVo.setTaskId(method.getId());
        spectrumBasicParameterVo.setProjectId(project.getId());
        spectrumBasicParameterVo.setProjectName(project.getProjectName());
        spectrumBasicParameterVo.setMethodName(method.getMethodName());
        return Result.ok(spectrumBasicParameterVo);
    }

    @Override
    public Result queryProjectAllMethods(String projectId,String measureForm) {
        List<TMeasureMethod> methods = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type",  measureForm));
        if (methods==null){
            return Result.fail("没有此类型的数据");
        }
        return Result.ok(methods);
    }



    /**
     * 查询方法的参数
     * @param methodId
     * @return
     */
    @Override
    public Result queryMethodParameter(String methodId) {
        // 根据方法id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        // 根据测量标准表id查询测量数据
        TMeasureStandard standard = tMeasureStandardService.getById(method.getStandardKey());
        // 将标准表中的json转换成对象存入返回值中
        SpectrumBasicParameterVo spectrumBasicParameterVo = null;
        try {
            spectrumBasicParameterVo = new ObjectMapper().readValue(standard.getMeasurementParamInfo(), SpectrumBasicParameterVo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        spectrumBasicParameterVo.setMethodName(method.getMethodName());
        return Result.ok(spectrumBasicParameterVo);
    }

    @Override
    public Result saveMethodParameter(SpectralMethodParameterDto methodDto) {
        // 根据方法id查询当前方法
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id",methodDto.getMethodId()));        // 根据当前方法中的测量标准表id查询测量数据
        TMeasureStandard standard = tMeasureStandardService.getById(method.getStandardKey());
        // 将ExperimentMethodDto中的值转入SpectrumParameter对象中
        SpectrumParameter spectrumParameter=  new SpectrumParameter();
        BeanUtils.copyProperties(methodDto,spectrumParameter);
        try {
            String s = new ObjectMapper().writeValueAsString(spectrumParameter);
            standard.setMeasurementParamInfo(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        boolean update = tMeasureStandardService.updateById(standard);
        if (!methodDto.getMethodName().equals(method.getMethodName())){
            method.setMethodName(methodDto.getMethodName());
            tMeasureMethodService.updateById(method);
        }
        return Result.ok(update);
    }

    @Override
    public void testPythonProt() {
        int[] x={5, 19, 3, 7, 12, 8, 2, 15, 9, 4};
        double[] y = {1.5, 2.3, 3.7, 4.1, 5.9, 6.2, 7.8, 8.4, 9.0, 0.6};
        String pythonUrl = "http://192.168.32.62:8002/get_peak/find_peaks"; // Python接口地址

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("x_data", Arrays.asList(5, 19, 3, 7, 12, 8, 2, 15, 9, 4));
        body.put("y_data", Arrays.asList(1.5, 2.3, 3.7, 4.1, 5.9, 6.2, 7.8, 8.4, 9.0, 0.6));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(pythonUrl, request, String.class);
        System.err.println(response.getBody());
    }

    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter) {
        // 创建测量参数表
        TMeasureStandard tMeasureStandard = new TMeasureStandard();
        tMeasureStandard.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
        tMeasureStandard.setTestType("紫外-光谱");
        tMeasureStandardService.save(tMeasureStandard);
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setStandardKey(tMeasureStandard.getId());
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setTestType("紫外-光谱");
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        tMeasureMethod.setMethodCreator(updateBy);
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if ( save){
            return Result.ok("创建成功");
        }else {
            return Result.fail("创建失败");
        }
    }

    @Override
    public Result queryMethodProt(String projectId,String measureType) {
        String type = MeasureTypeEnum.SPECTRAL_SCAN.toString();
        if (!measureType.equals(type)){
            return Result.fail("请选择光谱扫描");
        }
        // 根据项目id和测量类型查询方法
        List<TMeasureMethod> list = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", measureType));
        if (list==null||list.size() == 0){
            return Result.fail("未查询到数据");
        }
        return Result.ok(list);
    }

    @Override
    public Result queryTaskPort(QueryTaskDto queryTask) {
        // 判断方法id是all还是id
        QueryWrapper<TMeasureTask> wrapper =  new QueryWrapper<>();
        wrapper.eq("project_item_key", queryTask.getProjectId());
        if (!queryTask.getMethodIds().isEmpty()){
            wrapper.in("method_key", queryTask.getMethodIds());
        }
        if ((queryTask.getStartTime()!=null&&queryTask.getStartTime()!="")&&(queryTask.getEndTime()!=null&&queryTask.getEndTime()!="")){
            wrapper.between("create_time", queryTask.getStartTime(), queryTask.getEndTime());
        }
        Page<TMeasureTask> page = tMeasureTaskService.page(new Page<>(queryTask.getPageNum(), queryTask.getPageSize()), wrapper);
        if (page==null||page.getRecords().size() == 0){
            return Result.fail("未查询到数据");
        }
        page.getRecords().forEach(task -> {
            // 根据任务中的用户id查询用户
            UserConfigPO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getId, task.getUserId()));
            task.setUserId(userConfigPO.getUserName());
        });
        return Result.ok(page);
    }

    @Override
    public Result obtainReportParameters(String methodId, String updateBy) {
        // 根据方法id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        // 获取测量参数信息 使用ObjectMapper将json转换成对象
        SpectrumParameter spectrumParameter = null;
        try {
            spectrumParameter=objectMapper.readValue(method.getMeasurementParamInfo(), SpectrumParameter.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // TODO 补充报告参数
        // 存入Vo返回对象
        // 获取报告参数将spectrumParameter中的值存入reportParametersVo中

        ReportParametersVo reportParametersVo = new ReportParametersVo();
        BeanUtils.copyProperties(spectrumParameter,reportParametersVo);
        reportParametersVo.setTestAPerson(updateBy);
        return Result.ok(reportParametersVo);
    }

    @Override
    public Result createTask(CreateTaskParamDto paramDto) {
        // 根据项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(paramDto.getProjectId());
        //根据项目中的用户名称查询用户
        UserConfigPO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getUserName, project.getCreatorKey()));
        // 创建任务类
        TMeasureTask tMeasureTask = new TMeasureTask();
        tMeasureTask.setUserId(userConfigPO.getId());
        tMeasureTask.setTaskName(paramDto.getTaskName());
        tMeasureTask.setCreateTime(LocalDateTime.now());
        tMeasureTask.setMethodKey(paramDto.getMethodId());
        tMeasureTask.setProjectItemKey(project.getId());
        tMeasureTask.setDeviceSerialNumber(paramDto.getDeviceSerialNumber());
        List<String> list = new ArrayList<>();
        // 遍历CreateTaskParamDto中的样品list
        for (SpectrumSpecimenDto specimen : paramDto.getSpecimens()) {
            // 创建样品表
            TMeasureSample sample = new TMeasureSample();
            sample.setSampleNumber(specimen.getSampleNumber());
            sample.setSampleName(specimen.getSampleName());
            sample.setSampleLocation(specimen.getSampleLocation());
            tMeasureSampleService.save(sample);
            list.add(sample.getId());
        }
        String join = String.join(",", list);
        tMeasureTask.setSampleKey(join);
        boolean save = tMeasureTaskService.save(tMeasureTask);
        return Result.ok(save);
    }

}
