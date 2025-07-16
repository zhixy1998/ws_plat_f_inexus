package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.CmdConstant;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoIt.TestUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequestDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.ScanParamInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
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

    @Autowired
    private DevMessageUtil devMessageUtil;


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
                if (xList.get(i).equals(peakX.get(j))&&yList.get(i).equals(peakY.get(j))){
                    vo.setFeature("1");
                }
            }
            if (vo.getFeature()==null){
                vo.setFeature("0");
            }
            // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
            for (int j = 0; j < valleyX.size(); j++) {
                if (xList.get(i).equals(valleyX.get(j))&&yList.get(i).equals(valleyY.get(j))){
                    vo.setGrain("1");
                }
            }
            // 如果对象中的谷属性为null,则说明不是谷,则设置为0
            if (vo.getGrain()==null){
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

        ResponseEntity<String> response = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, String.class);
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

    /**
     * 四则运算接口
     * @param paramDto
     * @return
     */
    @Override
    public Result fourOperationsPort(FourOperationsParamDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
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
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakX));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyX));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 高级运算接口
     * @param paramDto
     * @return
     */
    @Override
    public Result advancedOperationsPort(AdvancedOperationsDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
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
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        body1.put("height", "0.5");
        body1.put("distance", "1");
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        // 直接拿寻峰数据进行一次寻谷
        Map<String, Object> resultMap2 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request1, Map.class).getBody();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakX));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyX));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 光度模式转换
     * @param paramDto
     * @return
     */
    @Override
    public Result luminanceModeConversionPort(LuminosityConversionDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
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
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        body1.put("height", "0.5");
        body1.put("distance", "1");
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request1, Map.class).getBody();
        Map<String, Object> resultMap2 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request1, Map.class).getBody();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakX));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyX));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 样品列表
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result sampleListPort(String taskId,String type) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            // 如果type为1则跳过
            if (type==null&&taskSpeciman.getDeleteType().equals("1")) continue;
            SampleListVo vo = new SampleListVo();
            TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
            QueryWrapper<TMeasureData> eq = new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", byId.getId());
            if (type==null){
                eq.eq("is_delete", "0");
            }
            // 根据样品id和任务id两个条件去查询测量数据
            TMeasureData measureData = tMeasureDataService.getOne(eq);
            vo.setKey(measureData.getId());
            vo.setTitle(byId.getSampleName());
            if (measureData== null){
                vo.setData(null);
            }else {
                List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
                List<Double> gauges = JSON.parseArray(measureData.getGaugePoint(), Double.class);
                List<Double> peaks = JSON.parseArray(measureData.getPeak(), Double.class);
                List<Double> grades = JSON.parseArray(measureData.getGrain(), Double.class);
                List<Map<String, Object>> resultList = new ArrayList<>();
                for (String s : list) {
                    String[] split = s.split(",");
                    Map<String, Object> map = new HashMap<>();
                    map.put("x", split[0]);
                    map.put("y", split[1]);
                    if (gauges!=null){
                        for (Double gauge : gauges) {
                            if (split[0].equals(String.valueOf(gauge))){
                                map.put("marked","1");
                                break;
                            }else{
                                map.put("marked","0");
                            }
                        }
                    }else{
                        map.put("marked","0");
                    }
                   if (peaks!=null){
                       for (Double peak : peaks) {
                           if (split[0].equals(String.valueOf(peak))){
                               map.put("feng","1");
                               break;
                           }else{
                               map.put("feng","0");
                           }
                       }
                   }else{
                       map.put("feng","0");
                   }
                   if (grades!=null){
                       for (Double grade : grades){
                           if (split[0].equals(String.valueOf(grade))){
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
            }
            vo.setColor(byId.getColor());
            vo.setDeleteType(taskSpeciman.getDeleteType());
            vo.setType("0");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    /**
     * 变换列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result changeTheListPort(String taskId,String type) {
        // 根据任务id去查询分析数据
        QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        if(type==null||type.equals("")){
            queryWrapper.eq("is_delete", "0");
        }
        List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(queryWrapper);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TMeasureAnalysisData tMeasureAnalysisData : list) {
            SampleListVo vo = new SampleListVo();
            vo.setKey(tMeasureAnalysisData.getId());
            vo.setTitle(tMeasureAnalysisData.getAnalysisName());
            vo.setColor(tMeasureAnalysisData.getColor());
            vo.setDeleteType(tMeasureAnalysisData.getIsDelete());
            List<String> results = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
            List<Double> gauges = JSON.parseArray(tMeasureAnalysisData.getGaugePoint(), Double.class);
            List<Double> peaks = JSON.parseArray(tMeasureAnalysisData.getPeak(), Double.class);
            List<Double> grades = JSON.parseArray(tMeasureAnalysisData.getGrain(), Double.class);
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (String s : results){
                String[] split = s.split(",");
                Map<String, Object> map = new HashMap<>();
                map.put("x", split[0]);
                map.put("y", split[1]);
                if (gauges != null){
                    for (Double gauge : gauges) {
                        if (split[0].equals(String.valueOf(gauge))){
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
                    for (Double peak : peaks) {
                        if (split[0].equals(String.valueOf(peak))){
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
                    for (Double grade : grades){
                        if (split[0].equals(String.valueOf(grade))){
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
     * 平滑接口
     * @param paramDto
     * @return
     */
    @Override
    public Result smoothnessPort(SmoothnessDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        if (Double.valueOf(paramDto.getCoefficient())>Double.valueOf(paramDto.getPointNum())){
            return Result.fail("系数不能大于点数");
        }
        // 根据测量数据的id查询出测量数据
        TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> list = new ArrayList<>();
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        try {
            list=objectMapper.readValue(tMeasureData.getOriginalCalculationData(), ArrayList.class);
        } catch (JsonProcessingException e) {
            return Result.fail("数据转换失败");
        }
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
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakX));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyX));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 微分接口
     * @param paramDto
     * @return
     */
    @Override
    public Result differentialPort(DifferentialDto paramDto) {
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        if (Double.valueOf(paramDto.getCoefficient())>Double.valueOf(paramDto.getPointNum())){
            return Result.fail("系数不能大于点数");
        }
        // 根据测量数据的id查询出测量数据
        TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        // 将测量数据中的原始计算数据转换成List
        List<String> list = new ArrayList<>();
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        try {
            list=objectMapper.readValue(tMeasureData.getOriginalCalculationData(), ArrayList.class);
        } catch (JsonProcessingException e) {
            return Result.fail("数据转换失败");
        }
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
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakX));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyX));
        tMeasureAnalysisData.setColor("#000000");
        tMeasureAnalysisData.setIsDelete("0");
        boolean save = tMeasureAnalysisDataService.save(tMeasureAnalysisData);
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
     * 查询测量标记点接口
     * @param measureId
     * @return
     */
    @Override
    public Result queryMeasureGaugePoint(String measureId) {
        // 根据测量id去测量数据表中查询数据
        TMeasureData byId = tMeasureDataService.getById(measureId);
        // 将测量数据中的标记点转换成List集合
        List<String> list = new ArrayList<>();
        try {
            list=objectMapper.readValue(byId.getGaugePoint(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Result.ok(list);
    }

    @Override
    public Result peakSearchingPort(String measureDataId) {
        // 根据测量数据id查询出测量数据
        TMeasureData tMeasureData = tMeasureDataService.getById(measureDataId);
        // 校验一下测量数据
        if (tMeasureData==null){
            return Result.fail("测量数据不存在");
        }
        List<String> list = new ArrayList<>();
       // 将测量数据中的原始计算数据转换成List
        try {
            list=objectMapper.readValue(tMeasureData.getOriginalCalculationData(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
        body.put("height", 0.5);
        body.put("distance", 1);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
        List<Map<String, Object>> listVos = new ArrayList<>();
        List<String> xData = (List<String>) resultMap.get("peak_x");
        List<String> yData = (List<String>) resultMap.get("peak_y");
        for (int i = 0; i < xData.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xData.get(i));
            map.put("y", yData.get(i));
            listVos.add(map);
        }
        return Result.ok(listVos);
    }

    @Override
    public Result conversionPeakSearching(String dataId) {
        // 根据分析数据id查询出分析数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(dataId);
        // 校验一下数据
        if (byId==null){
            return Result.fail("测量数据不存在");
        }
        List<String> list = new ArrayList<>();
        // 将数据中的结果数据转换成List
        try {
            list=objectMapper.readValue(byId.getAnalysisResult(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // 分割数据
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
        body.put("height", 0.5);
        body.put("distance", 1);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
        List<Map<String, Object>> listVos = new ArrayList<>();
        List<String> xData = (List<String>) resultMap.get("peak_x");
        List<String> yData = (List<String>) resultMap.get("peak_y");
        for (int i = 0; i < xData.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xData.get(i));
            map.put("y", yData.get(i));
            listVos.add(map);
        }
        return Result.ok(listVos);
    }

    @Override
    public Result valleySearchingPort(String measureDataId) {
        // 根据测量数据id查询出测量数据
        TMeasureData tMeasureData = tMeasureDataService.getById(measureDataId);
        if (tMeasureData==null){
            return Result.fail("测量数据不存在");
        }
        // 将数据转换成List
        List<String> list = new ArrayList<>();
        try {
            list=objectMapper.readValue(tMeasureData.getOriginalCalculationData(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // 分割数据
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
        body.put("height", 0.5);
        body.put("distance", 10);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
        List<Map<String, Object>> listVos = new ArrayList<>();
        List<String> xData = (List<String>) resultMap.get("valley_x");
        List<String> yData = (List<String>) resultMap.get("valley_y");
        for (int i = 0; i < xData.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xData.get(i));
            map.put("y", yData.get(i));
            listVos.add(map);
        }
        return Result.ok(resultMap);
    }

    @Override
    public Result conversionValleySearching(String measureDataId) {
        // 根据分析数据id查询出分析数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(measureDataId);
        if (byId==null){
            return Result.fail("测量数据不存在");
        }
        // 将数据转换成List
        List<String> list = new ArrayList<>();
        try {
            list=objectMapper.readValue(byId.getAnalysisResult(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // 分割数据
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
        body.put("height", 0.5);
        body.put("distance", 1);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
        List<Map<String, Object>> listVos = new ArrayList<>();
        List<String> xData = (List<String>) resultMap.get("valley_x");
        List<String> yData = (List<String>) resultMap.get("valley_y");
        for (int i = 0; i < xData.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("x", xData.get(i));
            map.put("y", yData.get(i));
            listVos.add(map);
        }
        return Result.ok(resultMap);
    }

    @Override
    public Result addMeasureDataGaugePointPort(MeasureDataGaugePointDto paramDto) {
        // 根据测量数据id查询出测量数据
        TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getMeasureDataId());
        if (tMeasureData==null){
            return Result.fail("测量数据不存在");
        }
        // 将标记点转换成List
        List<String> list = new ArrayList<>();
        try {
            list=objectMapper.readValue(tMeasureData.getGaugePoint(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        list.add(paramDto.getGaugePoint());
        tMeasureData.setGaugePoint(JSON.toJSONString(list));
        boolean update = tMeasureDataService.updateById(tMeasureData);
        return Result.ok(update);
    }

    @Override
    public Result queryAnalysisGaugePoint(String analysisId) {
        // 根据测量id去测量数据表中查询数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
        // 将测量数据中的标记点转换成List集合
        List<String> list = new ArrayList<>();
        try {
            list=objectMapper.readValue(byId.getGaugePoint(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Result.ok(list);
    }

    @Override
    public Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto) {
        // 根据测量数据id查询出测量数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(paramDto.getAnalysisId());
        if (byId==null){
            return Result.fail("测量数据不存在");
        }
        // 将标记点转换成List
        List<String> list = new ArrayList<>();
        try {
            list=objectMapper.readValue(byId.getGaugePoint(), List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        list.add(paramDto.getGaugePoint());
        byId.setGaugePoint(JSON.toJSONString(list));
        boolean update = tMeasureAnalysisDataService.updateById(byId);
        return Result.ok(update);
    }

    @Override
    public Result addGaugePoint(AddGaugePointDto paramDto) {
        // 根据类型判断是测量数据还是分析数据
        if (paramDto.getType().equals("0")){
            // 根据数据id去测量数据中进行查询
            TMeasureData tMeasureData = tMeasureDataService.getById(paramDto.getDataId());
            if (tMeasureData==null){
                return Result.fail("测量数据不存在");
            }
            tMeasureData.setGaugePoint(JSON.toJSONString(paramDto.getGaugePoints()));
            boolean update = tMeasureDataService.updateById(tMeasureData);
            return Result.ok(update);
        }else if (paramDto.getType().equals("1")){
            // 根据数据id去分析数据中进行查询
            TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
            if (analysisData==null){
                return Result.fail("分析数据不存在");
            }
            analysisData.setGaugePoint(JSON.toJSONString(paramDto.getGaugePoints()));
            boolean update = tMeasureAnalysisDataService.updateById(analysisData);
            return Result.ok(update);
        }else {
            return Result.fail("type参数错误");
        }
    }

    @Override
    public Result setUpParametersPort(String methodId) {
        // 根据methodId去方法表中查询数据
        TMeasureMethod tMeasureMethod = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id", methodId).eq("measurement_type","光谱扫描"));
        if (tMeasureMethod==null){
            return Result.fail("方法不存在");
        }

        ScanParamInfo scanParamInfo = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), ScanParamInfo.class);

        List<Command> commands = new ArrayList<>();

        // 光度模式
        Command com1 = new Command();
        com1.setCmdId(SetParamEnum.SetPhotoMode.getComId());
        com1.setParams(new Object[]{scanParamInfo.getLuminosity()});
        commands.add(com1);
        // 开始波长
        Command com2 = new Command();
        com2.setCmdId(SetParamEnum.SetStartWl.getComId());
        com2.setParams(new Object[]{scanParamInfo.getStartWaveLength()});
        commands.add(com2);
        // 结束波长
        Command com3 = new Command();
        com3.setCmdId(SetParamEnum.SetEndWl.getComId());
        com3.setParams(new Object[]{scanParamInfo.getEndWaveLength()});
        commands.add(com3);
        // 波长间隔
        Command com4 = new Command();
        com4.setCmdId(SetParamEnum.SetScanWlInterval.getComId());
        com4.setParams(new Object[]{scanParamInfo.getWaveLengthInterval()});
        commands.add(com4);
        // 响应时间
        Command com5 = new Command();
        com5.setCmdId(SetParamEnum.SetResponseTime.getComId());
        com5.setParams(new Object[]{scanParamInfo.getResponseTime()});
        commands.add(com5);
        // 换灯波长
        Command com6 = new Command();
        com6.setCmdId(SetParamEnum.SetChangeLampWl.getComId());
        com6.setParams(new Object[]{scanParamInfo.getChangeLampWaveLength()});
        commands.add(com6);
        // 能量灯
        Command com7 = new Command();
        com7.setCmdId(SetParamEnum.SetLampPos.getComId());
        com7.setParams(new Object[]{scanParamInfo.getEnergyLamp()});
        commands.add(com7);
        // 增益
        Command com8 = new Command();
        com8.setCmdId(SetParamEnum.SetGain.getComId());
        com8.setParams(new Object[]{scanParamInfo.getGain()});
        commands.add(com8);
        R<String> stringR = devMessageUtil.spectrumScanSet(commands);
        return Result.ok(stringR);
    }

    @Override
    public void testScan() {
        CommandRequestDTO commandRequestDTO = new CommandRequestDTO();
        List<CommandDTO> commands = new ArrayList<>();

        CommandDTO com = new CommandDTO();
        com.setCmdId(SetParamEnum.Scan.getComId());
        com.setParams(new Object[]{});
        commands.add(com);
        commandRequestDTO.setCommands(commands);
        TestUtil.connect(commandRequestDTO);
    }

    @Override
    public void testStart() {
        CommandRequestDTO commandRequestDTO = new CommandRequestDTO();
        List<CommandDTO> commands = new ArrayList<>();
        CmdConstant.connect.forEach(comId -> {
            CommandDTO com = new CommandDTO();
            com.setCmdId(comId);
            if (comId.equals("NewIP")) {
                com.setParams(new Object[]{0, 0, 0, 0, 0, 0, 52, 218, 166, 101, 144, 86, 169, 254, 180, 253});
            }
            commands.add(com);
        });
        commandRequestDTO.setCommands(commands);
        TestUtil.connect(commandRequestDTO);
        commands.clear();
        CmdConstant.init.forEach(comId -> {
            CommandDTO com = new CommandDTO();
            com.setCmdId(comId);
            if (comId.equals("SetSlit1")) {
                com.setParams(new Object[]{20});
            }
            commands.add(com);
        });
        commandRequestDTO.setCommands(commands);
        TestUtil.connect(commandRequestDTO);

    }

    /**
     * 查询样品列表
     * @param taskId
     * @return
     */
    @Override
    public Result querySampleList(String taskId) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        if (task.getSampleKey() == null) {
            return Result.ok(null);
        }
        // 分割任务中的样品数据
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Sample> sampleList = new ArrayList<>();
        if (taskSpecimen == null || taskSpecimen.size() == 0) {
            return Result.fail("任务中没有样品数据");
        }
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if (taskSpeciman.getDeleteType().equals("0")){
                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                Sample sample = new Sample();
                sample.setId(byId.getId());
                sample.setSampleName(byId.getSampleName());
                sample.setSampleNumber(byId.getSampleNumber());
                sample.setSampleLocation(byId.getSampleLocation());
                sampleList.add(sample);
            }
        }
        return Result.ok(sampleList);
    }

    /**
     * 新增样本接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addSamplePort(AddSampleDto paramDto) {
        // 创建一个String数组用于存放新增后的样品id
        List<String> sampleIdList = new ArrayList<>();
        // 根据传过来的参数进行遍历添加样品
        for (Sample sample : paramDto.getSamples()) {
            TMeasureSample sampleParam = new TMeasureSample();
            sampleParam.setSampleName(sample.getSampleName());
            sampleParam.setSampleNumber(sample.getSampleNumber());
            sampleParam.setSampleLocation(sample.getSampleLocation());
            tMeasureSampleService.save(sampleParam);
            sampleIdList.add(sampleParam.getId());
        }
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }
        if (task.getSampleKey() != null){
            // 将原有在任务中的样品字段提取出来
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            // 遍历样品id数组,将新增的样品id添加到任务中的样品字段中
            for (String sampleId : sampleIdList) {
                TaskSpecimen taskSpeciman = new TaskSpecimen();
                taskSpeciman.setSpecimenKey(sampleId);
                taskSpeciman.setDeleteType("0");
                taskSpecimen.add(taskSpeciman);
            }
            // 将修改后的样品字段存入任务数据中
            task.setSampleKey(JSON.toJSONString(taskSpecimen));
        }else if (task.getSampleKey() == null) {
            // 如果任务中的样品字段为空,则直接将样品id存入任务数据中
            List<TaskSpecimen> taskSpecimen = new ArrayList<>();
            for (String sampleId : sampleIdList) {
                TaskSpecimen taskSpeciman = new TaskSpecimen();
                taskSpeciman.setSpecimenKey(sampleId);
                taskSpeciman.setDeleteType("0");
                taskSpecimen.add(taskSpeciman);
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimen));
        }
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }


    /**
     * 删除样品接口
     * @param sampleId
     * @param taskId
     * @return
     */

    @Override
    public Result deleteSamplePort(String sampleId, String taskId) {
        // 获取任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
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

    @Override
    public Result uploadImage(SpectrumImageUpload upload) {
        String s = upload.getImage().split(",")[1];
        byte[] bytes = Base64.decodeBase64(s);
        if (upload.getType().equals("0")){
            TMeasureData byId = tMeasureDataService.getById(upload.getId());
            byId.setDataImage(bytes);
            boolean update = tMeasureDataService.updateById(byId);
            return Result.ok(update);
        }else if (upload.getType().equals("1")){
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(upload.getId());
            byId.setAnalysisDataImage(bytes);
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            return Result.ok(update);
        }else{
            return Result.fail("type类型错误");
        }
    }

    @Override
    public Result queryTheTaskImage(String id, String type) {
        // 根据type类型查询测量数据或分析数据的图片
        if (type.equals("0")){
            TMeasureData byId = tMeasureDataService.getById(id);
            if (byId.getDataImage() == null) {
                return Result.fail("图片不存在");
            }
            String encoded = Base64.encodeBase64String(byId.getDataImage());
            return Result.ok(encoded);
        }else if (type.equals("1")){
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(id);
            if (byId.getAnalysisDataImage() == null) {
                return Result.fail("图片不存在");
            }
            String encoded = Base64.encodeBase64String(byId.getAnalysisDataImage());
            return Result.ok(encoded);
        }else {
            return Result.fail("type类型错误");
        }
    }

    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        TMeasureAnalysisData  byId = tMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            return Result.fail("测量数据不存在");
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = tMeasureAnalysisDataService.updateById(byId);
        if (update){
            return Result.ok(true);
        }else {
            return Result.fail(false);
        }
    }

    @Override
    public Result deleteSampleListPort(String measureId) {
        // 根据measureId查询测量数据
        TMeasureData  byId = tMeasureDataService.getById(measureId);
        if (byId == null) {
            return Result.fail("测量数据不存在");
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = tMeasureDataService.updateById(byId);
        if (update){
            return Result.ok(true);
        }else {
            return Result.fail(false);
        }
    }

    @Override
    public Result obtainReportParameters(String taskId) {
        SpectralReportVo vo = new SpectralReportVo();
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        // 根据任务中的method_key字段查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 根据任务中的project_item_key查询项目数据
        TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
        // 根据任务中的sample_key字段查询样品数据
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<TMeasureSample> sampleList = new ArrayList<>();
        for (TaskSpecimen taskSpecimen : taskSpecimenList) {
            if (taskSpecimen.getDeleteType().equals("1")) continue;
            TMeasureSample sample = tMeasureSampleService.getById(taskSpecimen.getSpecimenKey());
            sampleList.add(sample);
        }
        // 根据任务id查询测量数据
        List<TMeasureData> dataList = tMeasureDataService.list(new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("is_delete", "0"));
        // 根据任务id查询分析数据
        List<TMeasureAnalysisData> analysisDataList = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).eq("is_delete", "0"));
        vo.setTask(task);
        vo.setData(dataList);
        vo.setAnalysisData(analysisDataList);
        vo.setSample(sampleList);
        vo.setProject(project);
        vo.setMethod(method);
        return Result.ok(vo);
    }

}
