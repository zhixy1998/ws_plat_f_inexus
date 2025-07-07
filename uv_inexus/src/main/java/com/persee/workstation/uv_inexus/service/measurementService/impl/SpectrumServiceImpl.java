package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
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

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;


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
        if (tMeasureData == null) {
            return Result.fail("没有数据");
        }
        // 将测量的数据从json转换为OriginalMeasuredDataVo对象
        OriginalMeasuredDataVo originalMeasuredDataVo = new OriginalMeasuredDataVo();
        List<String> list = new ArrayList<>();
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();

        try {
            list = objectMapper.readValue(tMeasureData.getOriginalCalculationData(), ArrayList.class);
        } catch (JsonProcessingException e) {
            return Result.fail("数据转换失败");
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
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
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
        // 将测量数据中的json转换成对象存入返回值中
        SpectrumBasicParameterVo spectrumBasicParameterVo = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            spectrumBasicParameterVo = objectMapper.readValue(method.getMeasurementParamInfo(), SpectrumBasicParameterVo.class);
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
        // 将标准表中的json转换成对象存入返回值中
        SpectrumBasicParameterVo spectrumBasicParameterVo = null;
        try {
            spectrumBasicParameterVo = new ObjectMapper().readValue(method.getMeasurementParamInfo(), SpectrumBasicParameterVo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        spectrumBasicParameterVo.setMethodName(method.getMethodName());
        return Result.ok(spectrumBasicParameterVo);
    }

    @Override
    public Result updateMethodParameter(SpectralMethodParameterDto methodDto) {
        // 根据方法id查询当前方法
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id",methodDto.getMethodId()));        // 根据当前方法中的测量标准表id查询测量数据
        // 将ExperimentMethodDto中的值转入SpectrumParameter对象中
        SpectrumParameter spectrumParameter=  new SpectrumParameter();
        BeanUtils.copyProperties(methodDto,spectrumParameter);
        method.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
        if (!methodDto.getMethodName().equals(method.getMethodName())){
            method.setMethodName(methodDto.getMethodName());
        }
        boolean update = tMeasureMethodService.updateById(method);
        return Result.ok(update);
    }

    @Override
    public void testPythonProt() {
        int[] x={5, 19, 3, 7, 12, 8, 2, 15, 9, 4};
        double[] y = {1.5, 2.3, 3.7, 4.1, 5.9, 6.2, 7.8, 8.4, 9.0, 0.6};

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("x_data", Arrays.asList(5, 19, 3, 7, 12, 8, 2, 15, 9, 4));
        body.put("y_data", Arrays.asList(1.5, 2.3, 3.7, 4.1, 5.9, 6.2, 7.8, 8.4, 9.0, 0.6));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("http://192.168.32.62:8002/get_peak/find_peaks_and_valleys", request, String.class);
        System.err.println(response.getBody());
    }

    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter) {
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
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
    public Result queryMeasurementParameters(String taskId) {
        // 根据任务id去测量数据表中查询数据
        List<TMeasureData> list = tMeasureDataService.list(new QueryWrapper<TMeasureData>().eq("task_id", taskId));
        if (list == null || list.size() == 0){
            return Result.fail("未查询到数据");
        }
        return Result.ok(list);
    }

    @Override
    public Result fourOperationsPort(FourOperationsParamDto paramDto) {
        // 根据测量数据id查询测量数据
        TMeasureData measureData1 = tMeasureDataService.getById(paramDto.getMeasureDataId1());
        List<String> list = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        List<String> xList = new ArrayList<>();
        List<String> yList1 = new ArrayList<>();
        List<String> yList2 = new ArrayList<>();
        try {
            list = objectMapper.readValue(measureData1.getOriginalCalculationData(), ArrayList.class);
        } catch (JsonProcessingException e) {
            return Result.fail("数据转换失败");
        }
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList1.add(split[1]);
        }
        if(paramDto.getDataType().equals("0")){
            // 将yList1中的每个数据都以arithmeticOperators运算measureDataId2
            if (paramDto.getArithmeticOperators().equals("0")){
                for (int i = 0; i < yList1.size(); i++) {
                    double result = Double.parseDouble(yList1.get(i)) + Double.parseDouble(paramDto.getMeasureDataId2());
                    yList2.add(String.valueOf(result));
                }
            }else if (paramDto.getArithmeticOperators().equals("1")){
                for (int i = 0; i < yList1.size(); i++) {
                    double result = Double.parseDouble(yList1.get(i)) - Double.parseDouble(paramDto.getMeasureDataId2());
                    yList2.add(String.valueOf(result));
                }
            }else if (paramDto.getArithmeticOperators().equals("2")){
                for (int i = 0; i < yList1.size(); i++) {
                    double result = Double.parseDouble(yList1.get(i)) * Double.parseDouble(paramDto.getMeasureDataId2());
                    yList2.add(String.valueOf(result));
                }
            }else if (paramDto.getArithmeticOperators().equals("3")){
                for (int i = 0; i < yList1.size(); i++) {
                    double result = Double.parseDouble(yList1.get(i)) / Double.parseDouble(paramDto.getMeasureDataId2());
                    yList2.add(String.valueOf(result));
                }
             }else {
                return Result.fail("未知运算符");
            }
        }else if (paramDto.getDataType().equals("1")){
            TMeasureData measureData2 = tMeasureDataService.getById(paramDto.getMeasureDataId2());
            // 将测量数据1和2的测量数据提取出来
            try {
                list2 = objectMapper.readValue(measureData2.getOriginalCalculationData(), ArrayList.class);
            } catch (JsonProcessingException e) {
                return Result.fail("数据转换失败");
            }
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

        // 将结果数据中的x_data和y_data取出来并存入List<String>中
        List<Double> x_data = (List<Double>) resultMap.get("x_data");
        List<Double> y_data = (List<Double>) resultMap.get("y_data");
        Map<String, Object> body1 = new HashMap<>();
        // 将分析数据存储到分析数据表中
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setTaskId(measureData1.getTaskId());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < x_data.size(); i++) {
            String s = x_data.get(i) + "," + y_data.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        // 将参数添加到请求体中
        body1.put("x_data", x_data);
        body1.put("y_data", y_data);
        body1.put("height", 0.5);
        body1.put("distance", 1);
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        List<FourOperationsVo> voList = new ArrayList<>();
        List<String> xData = x_data.stream().map(String::valueOf).collect(Collectors.toList());
        List<String> yData = y_data.stream().map(String::valueOf).collect(Collectors.toList());
        for (int i = 0; i < xData.size(); i++) {
            FourOperationsVo vo = new FourOperationsVo();
            vo.setX(xData.get(i));
            vo.setY(yData.get(i));
            // TODO 光谱扫描四则运算中的标记点未完成(暂未找到标记点的存取逻辑)
            vo.setMarked("0");
            List<String> peakX = (List<String>) resultMap1.get("peak_x");
            List<String> peakY = (List<String>) resultMap1.get("peak_y");
            List<String> valleyX = (List<String>) resultMap.get("valley_x");
            List<String> valleyY = (List<String>) resultMap.get("valley_y");
            for (int j = 0; j < peakX.size(); j++) {
                if (xData.get(i).equals(peakX.get(j))&&yData.get(i).equals(peakY.get(j))){
                    vo.setFeng("1");
                }
            }
            if (vo.getFeng()== null){
                vo.setFeng("0");
            }
            // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
            for (int j = 0; j < valleyX.size(); j++) {
                if (xData.get(i).equals(valleyX.get(j))&&yData.get(i).equals(valleyY.get(j))){
                    vo.setGu("1");
                }
            }
            // 如果对象中的谷属性为null,则说明不是谷,则设置为0
            if (vo.getGu()==null){
                vo.setGu("0");
            }
            voList.add(vo);
        }
        return Result.ok(voList);
    }

    @Override
    public Result advancedOperationsPort(AdvancedOperationsDto paramDto) {
        // 根据测量数据id查询测量数据
        TMeasureData measureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        List<String> list = new ArrayList<>();
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        try {
            list = objectMapper.readValue(measureData.getOriginalCalculationData(), ArrayList.class);
        } catch (JsonProcessingException e) {
            return Result.fail("数据转换失败");
        }
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
        // 创建分析数据对象
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        tMeasureAnalysisData.setTaskId(measureData.getTaskId());
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            String s = xList.get(i) + "," + y_data.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        Map<String, Object> body1 = new HashMap<>();
        // 将参数添加到请求体中
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        body1.put("height", 0.5);
        body1.put("distance", 1);
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        List<FourOperationsVo> voList = new ArrayList<>();
        List<String> xData = xList.stream().map(String::valueOf).collect(Collectors.toList());
        List<String> yData = y_data.stream().map(String::valueOf).collect(Collectors.toList());
        for (int i = 0; i < xData.size(); i++) {
            FourOperationsVo vo = new FourOperationsVo();
            vo.setX(xData.get(i));
            vo.setY(yData.get(i));
            vo.setMarked("0");
            List<String> peakX = (List<String>) resultMap1.get("peak_x");
            List<String> peakY = (List<String>) resultMap1.get("peak_y");
            List<String> valleyX = (List<String>) resultMap.get("valley_x");
            List<String> valleyY = (List<String>) resultMap.get("valley_y");
            for (int j = 0; j < peakX.size(); j++) {
                if (xData.get(i).equals(peakX.get(j))&&yData.get(i).equals(peakY.get(j))){
                    vo.setFeng("1");
                }
            }
            if (vo.getFeng()==null){
                vo.setFeng("0");
            }
            // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
            for (int j = 0; j < valleyX.size(); j++) {
                if (xData.get(i).equals(valleyX.get(j))&&yData.get(i).equals(valleyY.get(j))){
                    vo.setGu("1");
                }
            }
            // 如果对象中的谷属性为null,则说明不是谷,则设置为0
            if (vo.getGu()==null){
                vo.setGu("0");
            }
            voList.add(vo);
        }
        return Result.ok(voList);
    }

    @Override
    public Result luminanceModeConversionPort(LuminosityConversionDto paramDto) {
        // 根据方法id查询方法表的数据
        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
        // 获取方法表中的方法参数信息
        String measurementParamInfo = method.getMeasurementParamInfo();
        SpectrumParameter spectrumParameter = null;
        try {
            spectrumParameter = objectMapper.readValue(measurementParamInfo, SpectrumParameter.class);
        } catch (JsonProcessingException e) {
            return Result.fail("数据转换失败");
        }
        Integer type = null;
        if(paramDto.getDataType().equals("1")){
            if (spectrumParameter.getLuminosity().equals("1")){
                type = 0;
            }else if (spectrumParameter.getLuminosity().equals("2")){
                type = 1;
            }else {
                return Result.fail("请选择正确的光谱类型");
            }
        }else if (paramDto.getDataType().equals("2")){
            if (spectrumParameter.getLuminosity().equals("1")){
                type = 2;
            }else if (spectrumParameter.getLuminosity().equals("3")){
                type = 3;
            }else {
                return Result.fail("请选择正确的光谱类型");
            }
        }else{
            return Result.fail("测量数据光度模式错误!");
        }
        // 根据测量数据id查询测量数据
        TMeasureData measureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        List<String> list = new ArrayList<>();
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        try {
            list = objectMapper.readValue(measureData.getOriginalCalculationData(), ArrayList.class);
        }catch (JsonProcessingException e){
            return Result.fail("数据转换失败");
        }
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
        body.put("x_data", xList);
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        // 创建分析数据对象
        TMeasureAnalysisData tMeasureAnalysisData = new TMeasureAnalysisData();
        tMeasureAnalysisData.setAnalysisName(paramDto.getAnalysisDataName());
        tMeasureAnalysisData.setAnalysisParam(JSON.toJSONString(body));
        tMeasureAnalysisData.setTaskId(measureData.getTaskId());
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            String s = xList.get(i) + "," + y_data.get(i);
            listData.add(s);
        }
        tMeasureAnalysisData.setAnalysisResult(JSON.toJSONString(listData));
        tMeasureAnalysisDataService.save(tMeasureAnalysisData);
        Map<String, Object> body1 = new HashMap<>();
        // 将参数添加到请求体中
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        body1.put("height", 0.5);
        body1.put("distance", 1);
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        List<FourOperationsVo> voList = new ArrayList<>();
        List<String> xData = xList.stream().map(String::valueOf).collect(Collectors.toList());
        List<String> yData = y_data.stream().map(String::valueOf).collect(Collectors.toList());
        for (int i = 0; i < xData.size(); i++) {
            FourOperationsVo vo = new FourOperationsVo();
            vo.setX(xData.get(i));
            vo.setY(yData.get(i));
            vo.setMarked("0");
            List<String> peakX = (List<String>) resultMap1.get("peak_x");
            List<String> peakY = (List<String>) resultMap1.get("peak_y");
            List<String> valleyX = (List<String>) resultMap1.get("valley_x");
            List<String> valleyY = (List<String>) resultMap1.get("valley_y");
            for (int j = 0; j < peakX.size(); j++) {
                if (xData.get(i).equals(peakX.get(j))&&yData.get(i).equals(peakY.get(j))){
                    vo.setFeng("1");
                }
            }
            if (vo.getFeng()== null){
                vo.setFeng("0");
            }
            // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
            for (int j = 0; j < valleyX.size(); j++) {
                if (xData.get(i).equals(valleyX.get(j))&&yData.get(i).equals(valleyY.get(j))){
                    vo.setGu("1");
                }
            }
            // 如果对象中的谷属性为null,则说明不是谷,则设置为0
            if (vo.getGu()==null){
                vo.setGu("0");
            }
            voList.add(vo);
        }
        return Result.ok(voList);
    }

    @Override
    public Result sampleListPort(String taskId) {
        // 根据任务id查询测量数据
        List<TMeasureData> list = tMeasureDataService.list(new QueryWrapper<TMeasureData>().eq("task_id", taskId));
        List<SampleListVo> listVos = new ArrayList<>();
        for (TMeasureData tMeasureData : list) {
            SampleListVo vo = new SampleListVo();
            // 根据样品的key去样品表中查询数据
            TMeasureSample byId = tMeasureSampleService.getById(tMeasureData.getSampleKey());
            if (byId == null){
                return Result.fail("样品不存在");
            }
            vo.setKey(tMeasureData.getId());
            vo.setTitle(byId.getSampleName());
            vo.setColor("#000000");
            // 拆分重组测量数据
            ArrayList<String> dataList = new ArrayList<>();
            List<String> xList = new ArrayList<>();
            List<String> yList = new ArrayList<>();
            try {
                dataList = objectMapper.readValue(tMeasureData.getOriginalCalculationData(), ArrayList.class);
            } catch (JsonProcessingException e) {
                return Result.fail("数据转换失败");
            }
            for (String s : dataList) {
                String[] split = s.split(",");
                xList.add(split[0]);
                yList.add(split[1]);
            }
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
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
            List<FourOperationsVo> voList = new ArrayList<>();
            for (int i = 0; i < xList.size(); i++) {
                FourOperationsVo fourVo = new FourOperationsVo();
                fourVo.setX(xList.get(i));
                fourVo.setY(yList.get(i));
                fourVo.setMarked("0");
                List<String> peakX = (List<String>) resultMap.get("peak_x");
                List<String> peakY = (List<String>) resultMap.get("peak_y");
                List<String> valleyX = (List<String>) resultMap.get("valley_x");
                List<String> valleyY = (List<String>) resultMap.get("valley_y");
                for (int j = 0; j < peakX.size(); j++) {
                    if (xList.get(i).equals(peakX.get(j))&&yList.get(i).equals(peakY.get(j))){
                        fourVo.setFeng("1");
                    }
                }
                if (fourVo.getFeng()==null){
                    fourVo.setFeng("0");
                }
                // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
                for (int j = 0; j < valleyX.size(); j++) {
                    if (xList.get(i).equals(valleyX.get(j))&&yList.get(i).equals(valleyY.get(j))){
                        fourVo.setGu("1");
                    }
                }
                // 如果对象中的谷属性为null,则说明不是谷,则设置为0
                if (fourVo.getGu()==null){
                    fourVo.setGu("0");
                }
                voList.add(fourVo);
            }
            vo.setData(voList);
            if (tMeasureData.getMeasurementEndTime()== null||tMeasureData.getMeasurementEndTime().equals("")){
                vo.setMeasureType("0");
            }else{
                vo.setMeasureType("1");
            }
            vo.setDeleteType("0");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    @Override
    public Result changeTheListPort(String taskId) {
        // 根据任务id去分析数据表中查询数据
        List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId));
        List<SampleListVo> listVos = new ArrayList<>();
        for (TMeasureAnalysisData tMeasureAnalysisData : list) {
            SampleListVo vo = new SampleListVo();
            vo.setKey(tMeasureAnalysisData.getId());
            vo.setTitle(tMeasureAnalysisData.getAnalysisName());
            vo.setColor("#000000");
            // 拆分重组测量数据
            ArrayList<String> dataList = new ArrayList<>();
            List<String> xList = new ArrayList<>();
            List<String> yList = new ArrayList<>();
            try {
                dataList = objectMapper.readValue(tMeasureAnalysisData.getAnalysisResult(), ArrayList.class);
            } catch (JsonProcessingException e) {
                return Result.fail("数据转换失败");
            }
            for (String s : dataList) {
                String[] split = s.split(",");
                xList.add(split[0]);
                yList.add(split[1]);
            }
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
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
            List<FourOperationsVo> voList = new ArrayList<>();
            for (int i = 0; i < xList.size(); i++) {
                FourOperationsVo fourVo = new FourOperationsVo();
                fourVo.setX(xList.get(i));
                fourVo.setY(yList.get(i));
                fourVo.setMarked("0");
                List<String> peakX = (List<String>) resultMap.get("peak_x");
                List<String> peakY = (List<String>) resultMap.get("peak_y");
                List<String> valleyX = (List<String>) resultMap.get("valley_x");
                List<String> valleyY = (List<String>) resultMap.get("valley_y");
                // 遍历峰的长度,如果峰的x坐标和y坐标都相等,则说明是峰
                for (int j = 0; j < peakX.size(); j++) {
                    if (xList.get(i).equals(peakX.get(j))&&yList.get(i).equals(peakY.get(j))){
                        fourVo.setFeng("1");
                    }
                }
                // 如果对象中的峰属性为null,则说明不是峰,则设置为0
                if (fourVo.getFeng()==null){
                    fourVo.setFeng("0");
                }
                // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
                for (int j = 0; j < valleyX.size(); j++) {
                    if (xList.get(i).equals(valleyX.get(j))&&yList.get(i).equals(valleyY.get(j))){
                        fourVo.setGu("1");
                    }
                }
                // 如果对象中的谷属性为null,则说明不是谷,则设置为0
                if (fourVo.getGu()==null){
                    fourVo.setGu("0");
                }
                voList.add(fourVo);
            }
            vo.setData(voList);
            vo.setMeasureType("1");
            vo.setDeleteType("0");
            listVos.add(vo);
        }
        return Result.ok(listVos);
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


}
