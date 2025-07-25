package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.service.parseReport.CalculationFormulaService;
import com.persee.workstation.uv_inexus.service.parseReport.StandardPretreatmentService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.CmdConstant;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoIt.TestUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequestDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.measurementVo.*;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private SystemUserManageDAO userConfigDAO;

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

    @Autowired
    private DeviceInfoService deviceInfoService;

    // 计算公式的服务
    @Autowired
    private CalculationFormulaService calculationFormulaService;

    // 标准预处理服务
    @Autowired
    private StandardPretreatmentService standardPretreatmentService;


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
    public Result queryProjectTaskMethod(String updateBy,String measureType) {
        SystemUserManagePO user = userConfigDAO.selectOne(new QueryWrapper<SystemUserManagePO>().eq("user_name", updateBy));
        if (user==null){
            return Result.fail("用户不存在");
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(measureType);
        if (measureTypeEnum==null){
            return Result.fail("暂不支持");
        }
        // 根据用户id和测量类型然后通过创建时间查找到最近的一次任务
        TMeasureTask task = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>().eq("user_id", user.getId()).eq("measure_type", measureTypeEnum.getDescription()).orderByDesc("create_time").last("limit 1"));
        if (task==null){
            return Result.fail("没有任务");
        }
        // 根据任务id查询方法
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        // 根据项目id查询项目
        TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
        SpectrumBasicParameterVo spectrumBasicParameterVo = new SpectrumBasicParameterVo();
        spectrumBasicParameterVo.setTaskId(task.getId());
        spectrumBasicParameterVo.setTaskName(task.getTaskName());
        if (method==null){
            spectrumBasicParameterVo.setMethodId(null);
            spectrumBasicParameterVo.setMethodName(null);
        }else{
            spectrumBasicParameterVo.setMethodId(method.getId());
            spectrumBasicParameterVo.setMethodName(method.getMethodName());
        }
        if (project==null){
            spectrumBasicParameterVo.setProjectId(null);
            spectrumBasicParameterVo.setProjectName(null);
        }else{
            spectrumBasicParameterVo.setProjectId(project.getId());
            spectrumBasicParameterVo.setProjectName(project.getProjectName());
        }
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
        QueryMethodParameterVo queryMethodParameterVo = JSON.parseObject(method.getMeasurementParamInfo(), QueryMethodParameterVo.class);
        queryMethodParameterVo.setMethodName(method.getMethodName());
        queryMethodParameterVo.setMethodId(method.getId());
        return Result.ok(queryMethodParameterVo);
    }

    @Override
    public Result updateMethodParameter(SpectralMethodParameterDto methodDto) {
        // TODO 修改方法目前只覆盖,后续再续逻辑
//        // 根据方法名称查询方法表中是否有此数据
//        List<TMeasureMethod> list = tMeasureMethodService.list(Wrappers.lambdaQuery(TMeasureMethod.class).eq(TMeasureMethod::getMethodName, methodDto.getMethodName()));
//        if (list.size() > 0) {
//            return Result.fail("方法名称重复");
//        }
//        // 根据用户名称进行查询用户表数据
//        SystemUserManagePO user = userConfigDAO.selectOne(new QueryWrapper<SystemUserManagePO>().eq("user_name", methodDto.getUpdateBy()));
//        TMeasureMethod measureMethod = new TMeasureMethod();
//        if (methodDto.getProjectId()==null||methodDto.getProjectId().equals("")){
//            measureMethod.setIsSharingMethod("1");
//        }else{
//            measureMethod.setIsSharingMethod("0");
//            measureMethod.setProjectItemKey(methodDto.getProjectId());
//        }
//        measureMethod.setMethodName(methodDto.getMethodName());
//        measureMethod.setMeasurementType("光谱扫描");
//        // 将前端传过来的值复制到SpectrumParameter类中
//        SpectrumParameter spectrumParameter = new SpectrumParameter();
//        BeanUtils.copyProperties(methodDto, spectrumParameter);
//        measureMethod.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
//        measureMethod.setMethodCreateTime(LocalDateTime.now());
//        measureMethod.setMethodCreator(user.getUserName());
//        boolean save = tMeasureMethodService.save(measureMethod);
//        if (!save) {
//            return Result.fail("创建失败");
//        }
//        return Result.ok(measureMethod.getId());

        // 根据方法id查询方法
        TMeasureMethod measureMethod = tMeasureMethodService.getById(methodDto.getMethodId());
        if (measureMethod==null){
            return Result.fail("方法不存在");
        }
        // 将前端传过来的值复制到SpectrumParameter类中
        SpectrumParameter spectrumParameter = new SpectrumParameter();
        BeanUtils.copyProperties(methodDto, spectrumParameter);
        measureMethod.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
        measureMethod.setMethodName(methodDto.getMethodName());
        boolean update = tMeasureMethodService.updateById(measureMethod);
        if (!update){
            return Result.fail("更新失败");
        }
        return Result.ok(measureMethod.getId());
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
        if (methodDto.getPretreatmentIds()!= null){
            tMeasureMethod.setPreprocessKey(JSON.toJSONString(methodDto.getPretreatmentIds()));
        }
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
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.ok("任务不存在");
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String,String>> list = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if(taskSpeciman.getIsTested().equals("1")){
                Map<String, String> map = new HashMap<>();
                TMeasureData one = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", taskSpeciman.getSpecimenKey())
                        .orderByDesc("serial_number")
                        .last("limit 1"));
                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                map.put("measureId", one.getId());
                map.put("sampleName", byId.getSampleName());
                list.add(map);
            }
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
            for (int i = 0; i < yList1.size(); i++) {
                yList2.add(String.valueOf(paramDto.getMeasureDataId2()));
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
        // TODO 暂时先切换成一个测量数据一个样品
//        // 根据任务id查询任务数据
//        TMeasureTask task = tMeasureTaskService.getById(taskId);
//        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
//        List<SampleListVo> listVos = new ArrayList<>();
//        for (TaskSpecimen taskSpeciman : taskSpecimen) {
//            // 如果type为空同时删除类型为1则跳过
//            if (type == null && taskSpeciman.getDeleteType().equals("1")) continue;
//            SampleListVo vo = new SampleListVo();
//            // 根据样品id查询样品数据
//            TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
//            vo.setTitle(byId.getSampleName());
//            if (taskSpeciman.getIsTested().equals("0")) {
//                vo.setData(null);
//                vo.setMeasureId(null);
//                vo.setSampleId(byId.getId());
//                vo.setColor(byId.getColor());
//                vo.setType("0");
//                vo.setDeleteType(taskSpeciman.getDeleteType());
//                listVos.add(vo);
//            } else {
//                // 根据样品id和任务id两个条件去查询测量数据
//                QueryWrapper<TMeasureData> eq = new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", byId.getId());
//                if (type == null) {
//                    eq.eq("is_delete", "0");
//                }
//                // 增加查询条件,按照序列号从大到小排序
//                eq.orderByDesc("serial_number");
//                // 根据样品id和任务id两个条件去查询测量数据
//                List<TMeasureData> measureDataList = tMeasureDataService.list(eq);
//                for (TMeasureData measureData : measureDataList) {
//                    if (measureDataList.size() > 1) {
//                        SampleListVo vos = new SampleListVo();
//                        vos.setTitle(byId.getSampleName() + "(" + measureData.getSerialNumber() + ")");
//                        vos.setMeasureId(measureData.getId());
//                        vos.setSampleId(byId.getId());
//                        vos.setData(getSampleData(measureData));
//                        vos.setColor(byId.getColor());
//                        vos.setDeleteType(taskSpeciman.getDeleteType());
//                        vos.setType("0");
//                        listVos.add(vos);
//                    }else{
//                        vo.setMeasureId(measureData.getId());
//                        vo.setData(getSampleData(measureData));
//                        vo.setColor(byId.getColor());
//                        vo.setSampleId(byId.getId());
//                        vo.setDeleteType(taskSpeciman.getDeleteType());
//                        vo.setType("0");
//                        listVos.add(vo);
//                    }
//                }
//            }
//        }
//        return Result.ok(listVos);

        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            // 如果type为空同时删除类型为1则跳过
            if (type == null && taskSpeciman.getDeleteType().equals("1")) continue;
            SampleListVo vo = new SampleListVo();
            // 根据样品id查询样品数据
            TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
            vo.setTitle(byId.getSampleName());
            if (taskSpeciman.getIsTested().equals("0")) {
                vo.setData(null);
                vo.setMeasureId(null);
                vo.setSampleId(byId.getId());
                vo.setColor(byId.getColor());
                vo.setType("0");
                vo.setDeleteType(taskSpeciman.getDeleteType());
                listVos.add(vo);
            } else {
                // 根据样品id和任务id两个条件去查询测量数据
                QueryWrapper<TMeasureData> eq = new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", byId.getId());
                if (type == null) {
                    eq.eq("is_delete", "0");
                }
                // 增加查询条件,按照序列号从大到小排序
                eq.orderByDesc("serial_number");
                eq.last("limit 1");
                // 根据样品id和任务id两个条件去查询测量数据
                TMeasureData measureData = tMeasureDataService.getOne(eq);
                vo.setMeasureId(measureData.getId());
                vo.setData(getSampleData(measureData));
                vo.setColor(byId.getColor());
                vo.setSampleId(byId.getId());
                vo.setDeleteType(taskSpeciman.getDeleteType());
                vo.setType("0");
                listVos.add(vo);
            }
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
            vo.setMeasureId(tMeasureAnalysisData.getId());
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
        boolean update = false;
        if (tMeasureData.getGaugePoint()!=null){
            List<String> list = JSON.parseArray(tMeasureData.getGaugePoint(), String.class);
            list.add(paramDto.getGaugePoint());
            tMeasureData.setGaugePoint(JSON.toJSONString(list));
            update = tMeasureDataService.updateById(tMeasureData);
        }else{
            List<String> list = new ArrayList<>();
            list.add(paramDto.getGaugePoint());
            tMeasureData.setGaugePoint(JSON.toJSONString(list));
            update = tMeasureDataService.updateById(tMeasureData);
        }
        return update?Result.ok(true):Result.fail(false);
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
    public Result setUpParametersPort(String methodId,String deviceSerialNumber) {
        // 根据序列号查询设备ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        // 根据methodId去方法表中查询数据
        TMeasureMethod tMeasureMethod = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id", methodId).eq("measurement_type","光谱扫描"));
        if (tMeasureMethod==null){
            return Result.fail("方法不存在");
        }

        SpectrumParameter scanParamInfo = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), SpectrumParameter.class);

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
        // 扫描速度
        Command com5 = new Command();
        com5.setCmdId(SetParamEnum.SetScanSpeed.getComId());
        com5.setParams(new Object[]{scanParamInfo.getScanSpeed()});
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
        R<String> stringR = devMessageUtil.spectrumScanSet(commands,ip);
        return Result.ok(stringR.getData());
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
            return Result.ok("任务不存在");
        }
        if (task.getSampleKey() == null) {
            return Result.ok(null);
        }
        // 分割任务中的样品数据
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Sample> sampleList = new ArrayList<>();
        if (taskSpecimen == null || taskSpecimen.size() == 0) {
            return Result.ok(new ArrayList<>());
        }
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if (taskSpeciman.getDeleteType().equals("0")){
                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                Sample sample = new Sample();
                sample.setSampleId(byId.getId());
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
    public Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto) {
//        // 创建一个String数组用于存放新增后的样品id
//        List<String> sampleIdList = new ArrayList<>();
//        // 根据传过来的参数进行遍历添加样品
//        for (SpectrumSpecimenDto sample : paramDto.getSamples()) {
//            TMeasureSample sampleParam = new TMeasureSample();
//            sampleParam.setSampleName(sample.getSampleName());
//            sampleParam.setSampleNumber(sample.getSampleNumber());
//            sampleParam.setSampleLocation(sample.getSampleLocation());
//            sampleParam.setColor("#000000");
//            tMeasureSampleService.save(sampleParam);
//            sampleIdList.add(sampleParam.getId());
//        }
//        // 根据任务id查询任务数据
//        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
//        if (task == null) {
//            return Result.fail("任务不存在");
//        }
//        if (task.getSampleKey() != null){
//            // 将原有在任务中的样品字段提取出来
//            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
//            // 遍历样品id数组,将新增的样品id添加到任务中的样品字段中
//            for (String sampleId : sampleIdList) {
//                TaskSpecimen taskSpeciman = new TaskSpecimen();
//                taskSpeciman.setSpecimenKey(sampleId);
//                taskSpeciman.setDeleteType("0");
//                taskSpeciman.setIsTested("0");
//                taskSpecimen.add(taskSpeciman);
//            }
//            // 将修改后的样品字段存入任务数据中
//            task.setSampleKey(JSON.toJSONString(taskSpecimen));
//        }else if (task.getSampleKey() == null) {
//            // 如果任务中的样品字段为空,则直接将样品id存入任务数据中
//            List<TaskSpecimen> taskSpecimen = new ArrayList<>();
//            for (String sampleId : sampleIdList) {
//                TaskSpecimen taskSpeciman = new TaskSpecimen();
//                taskSpeciman.setSpecimenKey(sampleId);
//                taskSpeciman.setDeleteType("0");
//                taskSpecimen.add(taskSpeciman);
//            }
//            task.setSampleKey(JSON.toJSONString(taskSpecimen));
//        }
//        boolean update = tMeasureTaskService.updateById(task);
//        return Result.ok(update);
        if (paramDto.getNewSamples().isEmpty() && paramDto.getDelSampleIds().isEmpty()) {
            return Result.ok("没有要新增或删除的样品！");
        }
        if (paramDto.getDelSampleIds()==null){
            paramDto.setDelSampleIds(new ArrayList<>());
        }
        if (paramDto.getNewSamples()==null){
            paramDto.setNewSamples(new ArrayList<>());
        }

        if (paramDto.getHeads() != null && paramDto.getHeads().size() > 10) {
            return Result.fail("表格新增列数不能超过10列");
        }
        // 根据任务id进行查询
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : taskSpecimenList) {
            for (String delSampleId : paramDto.getDelSampleIds()) {
                if (taskSpecimen.getSpecimenKey().equals(delSampleId)) {
                    taskSpecimen.setDeleteType("1");
                }
            }
        }

        // 遍历新增的样品列表,如果id为null则进行新增,如果id不为null则进行方法修改
        for (Map<String, String> map : paramDto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            SampleVO newSample = JSON.parseObject(jsonString, SampleVO.class);

            JSONObject newColData = new JSONObject();
            if (paramDto.getHeads() != null) {

                for (HeadVO head : paramDto.getHeads()) {
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }
            }

            if (newSample.getSampleId()!=null){
                TMeasureSample byId = tMeasureSampleService.getById(newSample.getSampleId());
                if (byId == null){
                    return Result.ok("样品不存在");
                }
                byId.setSampleName(newSample.getSampleName());
                byId.setSampleNumber(newSample.getSampleNumber());
                byId.setSampleLocation(newSample.getSampleLocation());

                byId.setHeads(JSON.toJSONString(paramDto.getHeads()));
                byId.setNewColData(newColData.toJSONString());
                boolean update = tMeasureSampleService.updateById(byId);
                if (!update){
                    return Result.ok("修改失败");
                }
            }else{
                TMeasureSample sample = new TMeasureSample();
                sample.setSampleName(newSample.getSampleName());
                sample.setSampleNumber(newSample.getSampleNumber());
                sample.setSampleLocation(newSample.getSampleLocation());
                sample.setColor("#000000");
                sample.setSampleType(SampleTypeEnum.kind_stand.getIdentity());

                sample.setHeads(JSON.toJSONString(paramDto.getHeads()));
                sample.setNewColData(newColData.toJSONString());
                boolean save = tMeasureSampleService.save(sample);
                if (!save){
                    return Result.ok("新增失败");
                }
                taskSpecimenList.add(new TaskSpecimen(sample.getId(),"0","0"));
            }
        }

        task.setSampleKey(JSON.toJSONString(taskSpecimenList));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update== true?"修改成功":"修改失败");
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

    /**
     * 删除分析数据接口
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        TMeasureAnalysisData  byId = tMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            return Result.fail("分析数据不存在");
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

    /**
     * 删除样本列表参数接口
     * @param taskId
     * @param sampleId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String taskId, String sampleId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        for (TaskSpecimen taskSpecimen : taskSpecimenList){
            if (taskSpecimen.getSpecimenKey().equals(sampleId)){
                taskSpecimen.setDeleteType("1");
                break;
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimenList));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }

    @Override
    public Result saveData(String taskId) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        task.setIsDelete("1");
        boolean update = tMeasureTaskService.updateById(task);
        if ( update){
            return Result.ok(true);
        }else {
            return Result.fail(false);
        }
    }

    @Override
    public Result queryPreprocessingOperation(String methodId) {
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            return Result.fail("方法不存在");
        }
        if (method.getPreprocessKey() == null) {
             return Result.fail("预处理参数不存在");
        }else{
            List<List> lists = JSON.parseArray(method.getPreprocessKey(), List.class);
            return Result.ok(lists);
        }
    }

    /**
     * 获取基线接口
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result obtainTheBaseline(String methodId,String deviceSerialNumber) {
        if (methodId == null|| methodId.equals("")){
            return Result.fail("方法编号不能为空");
        }
        if (deviceSerialNumber == null|| deviceSerialNumber.equals("")){
            return Result.fail("设备编号不能为空");
        }
        Result result = setUpParametersPort(methodId, deviceSerialNumber);
        if (result.getCode() != 200){
            return Result.ok("设置参数失败");
        }
        // 根据序列号获取设备的ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = buildCommand(SetParamEnum.BaseLine, new Object[]{});
        R<List<ChangeParam>> listR = devMessageUtil.spectrumScanGetBaseline(command, ip);
        if (listR.getSuccess() != true){
            return Result.ok(listR.getMessage());
        }
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        // 将listR中的数据保存到spectrumParameter的baselineData中
        spectrumParameter.setBaselineData(listR.getData());
        method.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
        boolean update = tMeasureMethodService.updateById(method);
        return Result.ok(update);
    }

    /**
     * 导出Word接口
     * @param taskId
     * @param response
     * @throws Exception
     */
    @Override
    public void exportToWord(String taskId, HttpServletResponse response) throws Exception {

        // 查询任务信息
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        List<TaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<TMeasureSample> samples = new ArrayList<>();
        List<TMeasureData> measureDataList = new ArrayList<>();
        for (TaskSpecimen tMeasureSample : sampleList) {
            if (tMeasureSample.getIsTested().equals("1") && tMeasureSample.getDeleteType().equals("0")) {
                TMeasureSample byId = tMeasureSampleService.getById(tMeasureSample.getSpecimenKey());
                TMeasureData tMeasureData = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>()
                        .eq("task_id", taskId)
                        .eq("sample_key", byId.getId())
                        .orderByDesc("serial_number")
                        .last("LIMIT 1"));
                samples.add(byId);
                measureDataList.add(tMeasureData);
            }
        }

        // 生成 Word 文件
        generateWord(task, samples, measureDataList, response);
    }

    @Override
    public Result restoreDeletedListData(String taskId,String paramId, String type) {
        // 校验传过来的数据
        if (taskId == null || paramId == null || type == null) {
            return Result.ok("参数错误");
        }
        // 根据类型判断是样品数据还是变换数据
        if (type.equals("0")) {
            // 根据任务id查询任务数据
            TMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                return Result.fail("任务不存在");
            }
            List<TaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen taskSpecimen : sampleList) {
                if (taskSpecimen.getSpecimenKey().equals(paramId)) {
                    taskSpecimen.setDeleteType("0");
                    break;
                }
            }
            task.setSampleKey(JSON.toJSONString(sampleList));
            tMeasureTaskService.updateById(task);
        }else{
            // 根据变换数据id查询变换数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(paramId);
            if (byId == null) {
                return Result.fail("变换数据不存在");
            }
            byId.setIsDelete("0");
            tMeasureAnalysisDataService.updateById(byId);
        }
        return Result.ok();
    }

    @Override
    public Result obtainReportParameters(String taskId) {

        if ((taskId == null)) {
            return Result.fail("任务id不能为空");
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (task.getMeasureType().equals(MeasureTypeEnum.PHOTOMETRIC_MEASUREMENT.getDescription())) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            SpectralReportVo vo = new SpectralReportVo();
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            SystemUserManagePO user = userConfigDAO.selectById(task.getUserId());
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            PhotometricParameter methodparam = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

            if (StringUtils.hasText(task.getSampleKey())) {
                List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                if (taskSpecimen.size() > 1) {
                    TaskSpecimen specimen = taskSpecimen.get(0);
                    TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                    vo.setSampleName(sample.getSampleName());
                    vo.setSampleNumber(sample.getSampleNumber());

                    LambdaQueryWrapper<TMeasureData> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(TMeasureData::getTaskId, taskId)
                            .eq(TMeasureData::getSampleKey, sample.getId());
                    TMeasureData one = tMeasureDataService.getOne(wrapper);
                    if (one != null) {
                        vo.setDetectionStartTime(one.getMeasurementStartTime().format(formatter));
                        vo.setDetectionEndTime(one.getMeasurementEndTime().format(formatter));
                        vo.setOriginalData(one.getInstrumentData());
                    }
                }
            }

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(methodparam.getReferenceStandardName());

            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setLuminosityMode(WaveLengthEnum.getName(methodparam.getLuminosity()));
            vo.setSpectrumBandwidth(methodparam.getSpectralBandwidth());

            vo.setReferenceMethod(method.getMethodName());
            vo.setReferenceStandard(methodparam.getReferenceStandardName());
            vo.setSteps(methodparam.getPreProcessParameters());
            vo.setDetectionStartTime(task.getStartTime().format(formatter));
            vo.setDetectionEndTime(task.getEndTime());
            vo.setDataType("测量数据");

            vo.setProject(project.getProjectName());
            vo.setDetector(user.getUserName());
            vo.setEnergyLight(methodparam.getEnergyLamp());
            vo.setMethodName(method.getMethodName());

            return Result.ok(vo);

        } else {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            SpectralReportVo vo = new SpectralReportVo();
//            TMeasureTask task = tMeasureTaskService.getById(taskId);
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            SystemUserManagePO user = userConfigDAO.selectById(task.getUserId());
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            SpectrumParameter methodparam = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
            TMeasureData data = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", taskId).orderByDesc("measurement_end_time").last("LIMIT 1"));
            // 根据方法表的计算公式id和前处理id进行查询
            CalculationFormula calculationFormula = calculationFormulaService.findById(Integer.valueOf(method.getMultipleCalculationFormulaKey()));
            vo.setCalculationFormula(calculationFormula.getResultName());
            StandardPretreatment serviceById = standardPretreatmentService.findById(Integer.valueOf(method.getPreprocessKey()));
            vo.setReferenceStandard(serviceById.getPretreatmentDescription());
//        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).last("LIMIT 1"));
            TMeasureSample byId = tMeasureSampleService.getById(data.getSampleKey());
            vo.setSampleName(byId.getSampleName());
            vo.setSampleNumber(byId.getSampleNumber());
            vo.setDataType("测量数据");
            vo.setScanSpeed(methodparam.getScanSpeed());
            vo.setInstrumentWavelength(methodparam.getWaveLengthInterval());
            vo.setInstrumentSerialNumber(task.getDeviceSerialNumber());
            vo.setLuminosityMode(methodparam.getLuminosity());
            vo.setSpectrumBandwidth(methodparam.getSpectralBandwidth());
            vo.setReferenceMethod(method.getMethodName());
            vo.setRemark(data.getRemark());
            vo.setDetectionStartTime(data.getMeasurementStartTime().format(formatter));
            vo.setDetectionEndTime(data.getMeasurementEndTime().format(formatter));
            vo.setProject(project.getProjectName());
            vo.setDetector(user.getUserName());
            vo.setEnergyLight(methodparam.getEnergyLamp());
            vo.setStairCorrection(methodparam.getLadderCorrection() == "0" ? "启用" : "不启用");
            vo.setSpectrumScanImage(data.getDataImage());
            vo.setOriginalData(data.getOriginalCalculationData());
            vo.setGain(methodparam.getGain().toString());
            vo.setScanRange(methodparam.getStartWaveLength() + "-" + methodparam.getEndWaveLength());
            vo.setScanInterval(methodparam.getWaveLengthInterval());
            vo.setMethodName(method.getMethodName());
            vo.setRemark(data.getRemark());
            vo.setSpectrumScanImage(data.getDataImage());
            return Result.ok(vo);
        }
    }

    /**
     * 获取样品的详细数据
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureData measureData) {
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
            if (gauges != null) {
                for (Double gauge : gauges) {
                    if (split[0].equals(String.valueOf(gauge))) {
                        map.put("marked", "1");
                        break;
                    } else {
                        map.put("marked", "0");
                    }
                }
            } else {
                map.put("marked", "0");
            }
            if (peaks != null) {
                for (Double peak : peaks) {
                    if (split[0].equals(String.valueOf(peak))) {
                        map.put("feng", "1");
                        break;
                    } else {
                        map.put("feng", "0");
                    }
                }
            } else {
                map.put("feng", "0");
            }
            if (grades != null) {
                for (Double grade : grades) {
                    if (split[0].equals(String.valueOf(grade))) {
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
     * 生成Word文件
     * @param task
     * @param samples
     * @param response
     * @throws Exception
     */
    private void generateWord(TMeasureTask task, List<TMeasureSample> samples,List<TMeasureData> measureDataList, HttpServletResponse response) throws Exception {

        // 使用 Apache POI 创建 Word 文件
        XWPFDocument document = new XWPFDocument();

        // 添加标题
        XWPFParagraph title = document.createParagraph();
        XWPFRun titleRun = title.createRun();
        titleRun.setText("任务报告");
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.addBreak();

        // 添加任务信息
        XWPFParagraph taskInfo = document.createParagraph();
        taskInfo.setAlignment(ParagraphAlignment.LEFT);
        // 每条信息单独一行
        taskInfo.createRun().setText("任务名称: " + task.getTaskName());
        taskInfo.createRun().addBreak();  // 换行
        taskInfo.createRun().setText("测量类型: " + task.getMeasureType());
        taskInfo.createRun().addBreak();  // 换行
        taskInfo.createRun().setText("创建时间: " + task.getCreateTime().toString());
        taskInfo.createRun().addBreak();  // 换行

        // 创建表格
        XWPFTable table = document.createTable();

        // 设置表头
        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("样品ID");
        headerRow.addNewTableCell().setText("样品名称");
        headerRow.addNewTableCell().setText("样品位置");

        // 设置表格宽度：通过 XWPFTableCell 设置列宽
        for (int i = 0; i < table.getRows().size(); i++) {
            XWPFTableRow row = table.getRow(i);
            for (int j = 0; j < row.getTableCells().size(); j++) {
                XWPFTableCell cell = row.getCell(j);
                // 设置单元格的宽度（单位为 twips：1 twip = 1/1440 英寸）
                cell.setWidth("4000"); // 设置单元格宽度
            }
        }


        // 添加表格数据
        for (TMeasureSample sample : samples) {
            XWPFTableRow row = table.createRow();
            row.getCell(0).setText(sample.getSampleNumber());
            row.getCell(1).setText(sample.getSampleName());
            row.getCell(2).setText(sample.getSampleLocation());
        }

        // 添加样品图片信息
        XWPFParagraph sampleInfo = document.createParagraph();
        sampleInfo.setAlignment(ParagraphAlignment.LEFT);
        for (int i = 0; i < measureDataList.size(); i++) {
            if (measureDataList.get(i).getDataImage() == null) continue;
            InputStream inputStream = new ByteArrayInputStream(measureDataList.get(i).getDataImage());
            sampleInfo.createRun().setText(samples.get(i).getSampleName());
            sampleInfo.createRun().addBreak();  // 换行
            sampleInfo.createRun().addPicture(
                    inputStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "sample_image" + i + ".jpg",
                    Units.toEMU(300),
                    Units.toEMU(200)
            );
            sampleInfo.createRun().addBreak();  // 换行
            inputStream.close();
        }

        // 设置响应头，返回文件下载
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=task_report.docx");

        // 写文件
        document.write(response.getOutputStream());
    }

}
