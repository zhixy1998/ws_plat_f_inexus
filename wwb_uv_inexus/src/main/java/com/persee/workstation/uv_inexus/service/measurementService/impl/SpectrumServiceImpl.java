package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.dao.systemctlConfigDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.ExperimentMethodDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryMeasuredDataVo;
import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.systemctlConfigPo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.OriginalMeasuredDataVo;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            Double y = Double.valueOf(yList.get(i));
            double value = BigDecimal.valueOf(y).setScale(3, RoundingMode.HALF_UP).doubleValue();
            vo.setY(String.valueOf(value));
            vo.setFeature("false");
            vo.setTag("-"); // 或其他逻辑判断
            resultList.add(vo);
        }
        // 遍历resultList，将peak_y中的数据与resultList中的数据进行匹配
        for (OriginalMeasuredDataVo measuredDataVo : resultList) {
            for (Double str : peak_y) {
                // 将measuredDataVo.getY()转换成 double类型和 str比较
                Double parsed = Double.parseDouble(measuredDataVo.getY());
                if (parsed.equals(str)) {
                    measuredDataVo.setFeature("true");
                }
            }
        }

        // 分页
        // 获取所有数据
        int total = resultList.size();
        // fromIndex 开始，toIndex 结束
        int fromIndex = (dataVo.getPageNum() - 1) * dataVo.getPageSize();
        int toIndex = Math.min(fromIndex + dataVo.getPageSize(), total);
        if (fromIndex >= total || fromIndex < 0) {
            return Result.ok(resultList);
        }
        // 截取分页数据
        List<OriginalMeasuredDataVo> pagedList = resultList.subList(fromIndex, toIndex);
        return Result.ok(pagedList);
    }

    @Override
    // 重写父类方法
    public Result newMethod(ExperimentMethodDto methodDto) {
        // 查询所有方法的名称
        List<TMeasureMethod> list = tMeasureMethodService.list();
        for (TMeasureMethod tMeasureMethod : list) {
            if (tMeasureMethod.getMethodName().equals(methodDto.getMethodName())) {
                return Result.fail("方法名称重复");
            }
        }
        TMeasureProject defaultProject = null;
        if (methodDto.getProjectId().equals("")){
            // 获取默认项目的id
            defaultProject = tMeasureProjectService.getOne(Wrappers.lambdaQuery(TMeasureProject.class).eq(TMeasureProject::getProjectName, "default"));
            if (defaultProject == null){
                defaultProject = new TMeasureProject();
                defaultProject.setProjectName("default");
                defaultProject.setCreateTime(LocalDateTime.now());
                defaultProject.setCreatorKey("admin");
                defaultProject.setRemark("默认项目");
                tMeasureProjectService.save(defaultProject);
            }
        }else{
            defaultProject = tMeasureProjectService.getById(methodDto.getProjectId());
        }
        // 根据项目中的用户名查询用户
        UserConfigPO userConfigPO = userConfigDAO.selectOne(Wrappers.lambdaQuery(UserConfigPO.class).eq(UserConfigPO::getUserName, defaultProject.getCreatorKey()));
        // 创建测量参数表
        TMeasureStandard tMeasureStandard = new TMeasureStandard();
        // 创建光谱参数对象
        SpectrumParameter spectrumParameter = new SpectrumParameter();
        BeanUtils.copyProperties(methodDto, spectrumParameter);
        // 将需要的字段转成json
        ObjectMapper objectMapper = new ObjectMapper();
        String s=null;
        try {
            s = objectMapper.writeValueAsString(spectrumParameter);
            tMeasureStandard.setMeasurementParamInfo(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // 添加其他字段
        tMeasureStandard.setTestType("紫外-光谱");
        // 调用方法存储测量标准表的内容
        tMeasureStandardService.save(tMeasureStandard);


        // 创建TMethod对象
        TMeasureMethod tMethod = new TMeasureMethod();
        tMethod.setMethodName(methodDto.getMethodName());
        tMethod.setMeasurementType(methodDto.getMeasureType());
        tMethod.setIsSharingMethod(methodDto.getIsShare());
        // 将测量参数表的id存入方法表中
        tMethod.setStandardKey(tMeasureStandard.getId());
        tMethod.setMethodCreateTime(LocalDateTime.now());
        tMethod.setMeasurementParamInfo(s);
        // 将项目id存入方法表中
        tMethod.setProjectItemKey(defaultProject.getId());
        tMeasureMethodService.save(tMethod);

//        // 创建一个任务对象
//        TMeasureTask tMeasureTask = new TMeasureTask();
//        // UUID一个随机的任务名称
//        Random random = new Random();
//        long randomNumber = 1000000000L + random.nextLong() % 9000000000L;
//        tMeasureTask.setTaskName("任务: "+randomNumber);
//        tMeasureTask.setUserId(userConfigPO.getId());
//        tMeasureTask.setCreateTime(new Date());
//        tMeasureTask.setMethodKey(tMethod.getId());
//        tMeasureTask.setProjectItemKey(defaultProject.getId());
//        tMeasureTaskService.save(tMeasureTask);
        return Result.ok(defaultProject.getId()+","+tMethod.getId());
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
     * 查询当前用户的所有项目
     * @param updateBy
     * @return
     */
    @Override
    public Result queryAllProject(String updateBy) {
        // 根据用户名查询项目
        List<TMeasureProject> projects = tMeasureProjectService.list(new QueryWrapper<TMeasureProject>().eq("creator_key", updateBy));
        return Result.ok(projects);
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
    public Result saveMethodParameter(ExperimentMethodDto methodDto) {
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
    public Result measurement() {
        // 创建测量数据表对象
        TMeasureData tMeasureData = new TMeasureData();
        log.info("开始测量");
        tMeasureData.setMeasurementStartTime(LocalDateTime.now());
        // 填充测量数据对象数据


        log.info("测量结束");
        tMeasureData.setMeasurementEndTime(LocalDateTime.now());
        // 将数据进行json转换

        // 将测量出来的结果存入测量数据表中
        return null;
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
    public Result queryMethodProt(String projectId) {
        // 根据项目id和测量类型查询方法
        List<TMeasureMethod> list = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", MeasureTypeEnum.SPECTRAL_SCAN));
        if (list==null){
            return Result.fail("未查询到数据");
        }
        return Result.ok(list);
    }

    @Override
    public Result queryTaskProt(String projectId, String methodId, Integer pageNum, Integer pageSize) {
        // 判断方法id是all还是id
        Page<TMeasureTask> page = null;
        if (methodId.equals("all")){
            // 分页查询所有当前项目id的任务
            page = tMeasureTaskService.page(new Page<>(pageNum, pageSize), new QueryWrapper<TMeasureTask>().eq("project_item_key", projectId));
            if (page==null||page.getRecords().size() == 0){
                return Result.fail("未查询到数据");
            }
        }else{
            // 根据项目id和方法id分页查询任务
            page = tMeasureTaskService.page(new Page<>(pageNum, pageSize), new QueryWrapper<TMeasureTask>().eq("project_item_key", projectId).eq("method_id", methodId));
            if (page==null||page.getRecords().size() == 0){
                return Result.fail("未查询到数据");
            }
        }
        return Result.ok(page);
    }


//    @Override
//    public boolean newMethod(ExperimentMethodDto methodDto) throws JsonProcessingException {
//        // 绑定默认项目
//        TMeasureProject defaultProject =tMeasureProjectService.defaultProject();
//        TMeasureMethod tMeasureMethod = new TMeasureMethod();
//        // 创建对应参数表的记录
//        TMeasureStandard tMeasureStandard = new TMeasureStandard();
//        ObjectMapper objectMapper = new ObjectMapper();
//        Json json = objectMapper.readValue(s, Json.class);
//        tMeasureStandard.setMeasurementParamInfo(json);
//
//        // 根据方法名称对参数表进行查询
//        // 创建任务
//        tMeasureMethod.setProjectItemKey(defaultProject.getId());
//        tMeasureMethod.setMethodName(methodDto.getMethodName());
//
//        return false;
//    }


    /**
     * 处理Python返回的json数据
     * @param rawStr
     * @return
     */
    public Map<String, Object> parseJsonResponse(String rawStr) {
        if (rawStr == null || rawStr.trim().isEmpty()) {
            throw new RuntimeException("响应数据为空");
        }

        int jsonStart = rawStr.indexOf("{");
        if (jsonStart == -1) {
            throw new RuntimeException("不是合法的JSON字符串: " + rawStr);
        }

        String cleanJson = rawStr.substring(jsonStart);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(cleanJson, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
        }
    }

}
