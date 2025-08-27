package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.parseReport.CalculationFormulaService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.StandardPretreatmentService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.CmdConstant;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoIt.TestUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequestDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.measurementVo.OriginalMeasuredDataVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.QueryMethodParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleListVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
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
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @Autowired
    private TMeasureTaskAttachmentService tMeasureTaskAttachmentService;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;


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
        TMeasureTask task = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>()
                .eq("user_id", user.getId())
                .eq("measure_type", measureTypeEnum.getDescription())
                .orderByDesc("create_time")
                .last("limit 1"));

        if (task!=null){
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
        TMeasureMethod method = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>()
                .eq("method_creator", user.getUserName())
                .eq("measurement_type", measureType)
                .orderByDesc("method_create_time")
                .last("limit 1"));
        SpectrumBasicParameterVo vo = new SpectrumBasicParameterVo();
        if (method==null){
            TMeasureProject project = tMeasureProjectService.getOne(new QueryWrapper<TMeasureProject>()
                    .eq("creator_key", updateBy)
                    .orderByDesc("create_time")
                    .last("limit 1"));
            if (project==null){
                return Result.ok(vo);
            }
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
        }else{
            TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
        }
        return Result.ok(vo);
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
        if (queryMethodParameterVo.getLuminosity().equals("1")
        && queryMethodParameterVo.getLuminosity().equals("2")
        && queryMethodParameterVo.getLuminosity().equals("3")
        && queryMethodParameterVo.getLuminosity().equals("4")){
            return Result.fail("请选择正确的光度模式");
        }
        queryMethodParameterVo.setLuminosityName(queryMethodParameterVo.getLuminosity().equals("1")?"Abs":queryMethodParameterVo.getLuminosity().equals("2")?"T%":queryMethodParameterVo.getLuminosity().equals("3")?"Es":"Er");
        if (queryMethodParameterVo.getScanSpeed().equals("0")
        && queryMethodParameterVo.getScanSpeed().equals("1")
        && queryMethodParameterVo.getScanSpeed().equals("2")
        && queryMethodParameterVo.getScanSpeed().equals("3")){
            return Result.fail("请选择正确的扫描速度");
        }
        queryMethodParameterVo.setScanSpeedName(queryMethodParameterVo.getScanSpeed().equals("0")?"慢速":queryMethodParameterVo.getScanSpeed().equals("1")?"中速":queryMethodParameterVo.getScanSpeed().equals("2")?"快速":"极快");
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
            return Result.ok(new ArrayList<>());
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
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey()==null||task.getSampleKey().equals("[]")){
            return Result.ok(new ArrayList<>());
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String,String>> list = new ArrayList<>();
        for (TaskSpecimen taskSpeciman : taskSpecimen) {
            if(taskSpeciman.getIsTested().equals("1")&&taskSpeciman.getDeleteType().equals("0")){
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
        if (paramDto.getAnalysisDataName()==null||paramDto.getAnalysisDataName().equals("")){
            return Result.fail("请输入变换名称");
        }
        if (paramDto.getTaskId()==null||paramDto.getTaskId().equals("")) {
            return Result.fail("缺少参数");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
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
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
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
        if (paramDto.getAnalysisDataName()==null||paramDto.getAnalysisDataName().equals("")){
            return Result.fail("请输入变换名称");
        }
        if (paramDto.getTaskId()==null||paramDto.getTaskId().equals("")) {
            return Result.fail("缺少参数");
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
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
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
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
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
        }
        // 根据方法id查询方法表的数据
        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
        // 获取方法表中的方法参数信息
        String measurementParamInfo = method.getMeasurementParamInfo();
        SpectrumParameter spectrumParameter = JSON.parseObject(measurementParamInfo, SpectrumParameter.class);
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
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
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
    public Result sampleListPort(String taskId,String type,Integer digit) {
        // TODO 暂时先切换成一个测量数据一个样品
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
                QueryWrapper<TMeasureAnalysisData> eq = new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).eq("sample_key", byId.getId());
                if (type == null) {
                    eq.eq("is_delete", "0");
                }
                // 增加查询条件,按照序列号从大到小排序
                eq.orderByDesc("analysis_number");
                // 根据样品id和任务id两个条件去查询测量数据
                List<TMeasureAnalysisData> measureDataList = tMeasureAnalysisDataService.list(eq);
                for (TMeasureAnalysisData measureData : measureDataList) {
                    if (measureDataList.size() > 1) {
                        SampleListVo vos = new SampleListVo();
                        vos.setTitle(byId.getSampleName() + "(" + measureData.getAnalysisNumber() + ")");
                        vos.setMeasureId(measureData.getId());
                        vos.setSampleId(byId.getId());
                        vos.setData(getSampleData(measureData,digit));
                        vos.setColor(byId.getColor());
                        vos.setDeleteType(taskSpeciman.getDeleteType());
                        vos.setType("0");
                        listVos.add(vos);
                    }else{
                        vo.setMeasureId(measureData.getId());
                        vo.setData(getSampleData(measureData, digit));
                        vo.setColor(byId.getColor());
                        vo.setSampleId(byId.getId());
                        vo.setDeleteType(taskSpeciman.getDeleteType());
                        vo.setType("0");
                        listVos.add(vo);
                    }
                }
            }
        }
        return Result.ok(listVos);

//        // 根据任务id查询任务数据
//        TMeasureTask task = tMeasureTaskService.getById(taskId);
//        if (task == null){
//            return Result.ok(new ArrayList<>());
//        }
//        if (task.getSampleKey()==null||task.getSampleKey().equals("[]")){
//            return Result.ok(new ArrayList<>());
//        }
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
//                eq.last("limit 1");
//                // 根据样品id和任务id两个条件去查询测量数据
//                TMeasureData measureData = tMeasureDataService.getOne(eq);
//                vo.setMeasureId(measureData.getId());
//                vo.setData(getSampleData(measureData,digit));
//                vo.setColor(byId.getColor());
//                vo.setSampleId(byId.getId());
//                vo.setDeleteType(taskSpeciman.getDeleteType());
//                vo.setType("0");
//                listVos.add(vo);
//            }
//        }
//        return Result.ok(listVos);


//         根据任务id查询任务数据
//        TMeasureTask task = tMeasureTaskService.getById(taskId);
//        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
//
//        List<SampleWithMeasuresVo> resultList = new ArrayList<>();
//
//        for (TaskSpecimen taskSpeciman : taskSpecimen) {
//            // 如果type为空同时删除类型为1则跳过
//            if (type == null && "1".equals(taskSpeciman.getDeleteType())) continue;
//
//            // 根据样品id查询样品数据
//            TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
//
//            SampleWithMeasuresVo sampleVo = new SampleWithMeasuresVo();
//            sampleVo.setSampleId(byId.getId());
//            sampleVo.setTitle(byId.getSampleName());
//            sampleVo.setColor(byId.getColor());
//            sampleVo.setDeleteType(taskSpeciman.getDeleteType());
//            sampleVo.setType("0");
//
//            List<MeasureVo> measureVos = new ArrayList<>();
//
//            if ("0".equals(taskSpeciman.getIsTested())) {
//                // 没有测量数据则不对他进行任何操作
//            } else {
//                // 根据样品id和任务id两个条件去查询测量数据
//                QueryWrapper<TMeasureAnalysisData> eq = new QueryWrapper<TMeasureAnalysisData>()
//                        .eq("task_id", taskId)
//                        .eq("sample_key", byId.getId())
//                        .eq("analysis_type", "5");
//                if (type == null) {
//                    eq.eq("is_delete", "0");
//                }
//                // 按照序列号从大到小排序
//                eq.orderByDesc("analysis_number");
//
//                List<TMeasureAnalysisData> measureDataList = tMeasureAnalysisDataService.list(eq);
//
//                for (TMeasureAnalysisData measureData : measureDataList) {
//                    MeasureVo mvo = new MeasureVo();
//                    mvo.setMeasureId(byId.getId()+"-"+measureData.getAnalysisNumber());
//                    mvo.setSerialNumber(measureData.getAnalysisNumber().toString());
//                    mvo.setTitle(measureData.getAnalysisName());
//                    mvo.setData(getSampleData(measureData, digit));
//                    mvo.setType("0");
//                    measureVos.add(mvo);
//                }
//                sampleVo.setChildren(measureVos);
//            }
//
//            resultList.add(sampleVo);
//        }
//
//        return Result.ok(resultList);
    }

    /**
     * 变换列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result changeTheListPort(String taskId,String type,Integer digit) {
        // 根据任务id去查询分析数据
        QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.ne("analysis_type", "5");
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
            List<PeakValleyMarking> gauges = JSON.parseArray(tMeasureAnalysisData.getGaugePoint(), PeakValleyMarking.class);
            List<PeakValleyMarking> peaks = JSON.parseArray(tMeasureAnalysisData.getPeak(), PeakValleyMarking.class);
            List<PeakValleyMarking> grades = JSON.parseArray(tMeasureAnalysisData.getGrain(), PeakValleyMarking.class);
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
                    for (PeakValleyMarking gauge : gauges) {
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
                    for (PeakValleyMarking peak : peaks) {
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
                    for (PeakValleyMarking grade : grades){
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
     * 平滑接口
     * @param paramDto
     * @return
     */
    @Override
    public Result smoothnessPort(SmoothnessDto paramDto) {
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
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
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
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
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
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size()>0){
            return Result.fail("变换名称重复");
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
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> peakX = (List<Double>) resultMap1.get("peak_x");
        List<String> peakY = (List<String>) resultMap1.get("peak_y");
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> valleyX = (List<Double>) resultMap2.get("valley_x");
        List<String> valleyY = (List<String>) resultMap2.get("valley_y");
        for (int i = 0; i < peakX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(peakX.get(i).toString());
            peakValleyMarking.setY(peakY.get(i));
            peakValleyMarking.setType("波峰");
            peakData.add(peakValleyMarking);
        }
        for (int i = 0; i < valleyX.size(); i++) {
            PeakValleyMarking peakValleyMarking = new PeakValleyMarking();
            peakValleyMarking.setX(valleyX.get(i).toString());
            peakValleyMarking.setY(valleyY.get(i));
            peakValleyMarking.setType("波谷");
            valleyData.add(peakValleyMarking);
        }
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
        tMeasureAnalysisData.setPeak(JSON.toJSONString(peakData));
        tMeasureAnalysisData.setGrain(JSON.toJSONString(valleyData));
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
    public Result peakSearchingPort(SeekingPeaksValleysDto paramDto) {
        paramDto.setHeight(paramDto.getHeight()/100);
         TMeasureAnalysisData tMeasureAnalysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
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
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        if(xPeak==null||yPeak==null){
            return Result.fail("height参数太高");
        }
        for (int i = 0; i < xPeak.size(); i++) {
            PeakValleyMarking marking = new PeakValleyMarking();
            marking.setX(xPeak.get(i).toString());
            marking.setY(yPeak.get(i));
            marking.setType("波峰");
            peakData.add(marking);
        }
        List<PeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> xValley = (List<Double>) resultMap.get("valley_x");
        List<String> yValley = (List<String>) resultMap.get("valley_y");
        for (int i = 0; i < xValley.size(); i++) {
            PeakValleyMarking marking = new PeakValleyMarking();
            marking.setX(xValley.get(i).toString());
            marking.setY(yValley.get(i));
            marking.setType("波谷");
            valleyData.add(marking);
        }
        Boolean flag;
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData==null){
            return Result.fail("分析数据不存在");
        }
        analysisData.setPeak(JSON.toJSONString(peakData));
        analysisData.setGrain(JSON.toJSONString(valleyData));
        flag=tMeasureAnalysisDataService.updateById(analysisData);
        return flag?Result.ok():Result.fail("数据保存失败");
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

    /**
     * 添加标记点
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePoint(AddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            return Result.fail("请选择标记点");
        }
        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            return Result.fail("分析数据不存在");
        }
        gaugePointJson = analysisData.getGaugePoint();
        rawData = JSON.parseArray(analysisData.getAnalysisResult(), String.class);


        // 原标记点集合
        List<PeakValleyMarking> oldList = gaugePointJson == null
                ? new ArrayList<>()
                : JSON.parseArray(gaugePointJson, PeakValleyMarking.class);

        // 先构造原有标记点 Map（X -> 对象）
        Map<String, PeakValleyMarking> oldMap = oldList.stream()
                .collect(Collectors.toMap(
                        m -> normalizeNumberString(m.getX()),
                        m -> m,
                        (a, b) -> a
                ));

        // 构造新的最终集合（保证顺序）
        Map<String, PeakValleyMarking> finalMap = new LinkedHashMap<>();

        // 遍历前端传的 X 值
        for (String xStr : paramDto.getGaugePoints()) {
            String normX = normalizeNumberString(xStr);
            PeakValleyMarking existing = oldMap.get(normX);

            if (existing != null) {
                // 已存在则直接用旧值
                finalMap.put(normX, existing);
            } else {
                // 新增点时，从原始数据中查找对应 Y
                for (String s : rawData) {
                    String[] split = s.split(",");
                    String rawNormX = normalizeNumberString(split[0]);
                    if (normX.equals(rawNormX)) {
                        PeakValleyMarking newMark = new PeakValleyMarking();
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
        TMeasureAnalysisData analysisData1 = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        analysisData1.setGaugePoint(updatedJson);
        boolean update = tMeasureAnalysisDataService.updateById(analysisData1);
        return update?Result.ok(true):Result.fail(false);
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
//        // 根据任务id查询任务数据
//        TMeasureTask task = tMeasureTaskService.getById(taskId);
//        if (task == null) {
//            return Result.ok(new ArrayList<>());
//        }
//        if (task.getSampleKey() == null) {
//            return Result.ok(null);
//        }
//        // 分割任务中的样品数据
//        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
//        List<Sample> sampleList = new ArrayList<>();
//        if (taskSpecimen == null || taskSpecimen.size() == 0) {
//            return Result.ok(new ArrayList<>());
//        }
//        for (TaskSpecimen taskSpeciman : taskSpecimen) {
//            if (taskSpeciman.getDeleteType().equals("0")){
//                TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
//                Sample sample = new Sample();
//                sample.setSampleId(byId.getId());
//                sample.setSampleName(byId.getSampleName());
//                sample.setSampleNumber(byId.getSampleNumber());
//                sample.setSampleLocation(byId.getSampleLocation());
//                sampleList.add(sample);
//            }
//        }
//        return Result.ok(sampleList);

        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(deleteType)) {
                    continue;
                }
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                SampleVO vo = new SampleVO();
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
     * 新增样本接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto) {
//        if (paramDto.getNewSamples().isEmpty() && paramDto.getDelSampleIds().isEmpty()) {
//            return Result.ok("没有要新增或删除的样品！");
//        }
//        if (paramDto.getDelSampleIds()==null){
//            paramDto.setDelSampleIds(new ArrayList<>());
//        }
//        if (paramDto.getNewSamples()==null){
//            paramDto.setNewSamples(new ArrayList<>());
//        }
//
//        if (paramDto.getHeads() != null && paramDto.getHeads().size() > 10) {
//            return Result.fail("表格新增列数不能超过10列");
//        }
//        // 根据方法id查询方法数据
//        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
//        // 根据用户名查询用户数据
//        SystemUserManagePO user = userConfigDAO.selectOne(new QueryWrapper<>(SystemUserManagePO.class).eq("user_name", paramDto.getUpdateBy()));
//        TMeasureTask task;
//        if (paramDto.getTaskId()!=null){
//            task = tMeasureTaskService.getById(paramDto.getTaskId());
//        }else{
//            task = new TMeasureTask();
//            task.setUserId(user.getId());
//            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
//            String collect = new Random().ints(8, 0, chars.length())
//                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
//                    .collect(Collectors.joining());
//            task.setTaskName(method.getMeasurementType() + "task_" + collect);
//            task.setCreateTime(LocalDateTime.now());
//            task.setMethodKey(method.getId());
//            task.setMeasureType(method.getMeasurementType());
//            task.setProjectItemKey(method.getProjectItemKey());
//            task.setIsDelete("0");
//            tMeasureTaskService.save(task);
//            TMeasureTaskAttachment tMeasureTaskAttachment = new TMeasureTaskAttachment();
//            tMeasureTaskAttachment.setTaskId(task.getId());
//            boolean save = tMeasureTaskAttachmentService.save(tMeasureTaskAttachment);
//            if (!save) {
//                return Result.fail("保存任务附属失败");
//            }
//        }
//        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
//        if (taskSpecimenList != null&&(paramDto.getDelSampleIds()!=null||paramDto.getDelSampleIds().isEmpty())) {
//            for (TaskSpecimen taskSpecimen : taskSpecimenList) {
//                for (String delSampleId : paramDto.getDelSampleIds()) {
//                    if (taskSpecimen.getSpecimenKey().equals(delSampleId)) {
//                        taskSpecimen.setDeleteType("1");
//                    }
//                }
//            }
//        }
//
//
//        // 遍历新增的样品列表,如果id为null则进行新增,如果id不为null则进行方法修改
//        for (Map<String, String> map : paramDto.getNewSamples()) {
//            String jsonString = JSON.toJSONString(map);
//            SampleVO newSample = JSON.parseObject(jsonString, SampleVO.class);
//
//            JSONObject newColData = new JSONObject();
//            if (paramDto.getHeads() != null) {
//
//                for (HeadVO head : paramDto.getHeads()) {
//                    String s = map.get(head.getIdentity());
//                    newColData.put(head.getIdentity(), s);
//                }
//            }
//
//            if (newSample.getSampleId()!=null){
//                TMeasureSample byId = tMeasureSampleService.getById(newSample.getSampleId());
//                if (byId == null){
//                    return Result.ok("样品不存在");
//                }
//                byId.setSampleName(newSample.getSampleName());
//                byId.setSampleNumber(newSample.getSampleNumber());
//                byId.setSampleLocation(newSample.getSampleLocation());
//
//                byId.setHeads(JSON.toJSONString(paramDto.getHeads()));
//                byId.setNewColData(newColData.toJSONString());
//                boolean update = tMeasureSampleService.updateById(byId);
//                if (!update){
//                    return Result.ok("修改失败");
//                }
//            }else{
//                TMeasureSample sample = new TMeasureSample();
//                sample.setSampleName(newSample.getSampleName());
//                sample.setSampleNumber(newSample.getSampleNumber());
//                sample.setSampleLocation(newSample.getSampleLocation());
//                sample.setColor("#000000");
//                sample.setSampleType(SampleTypeEnum.kind_stand.getIdentity());
//
//                sample.setHeads(JSON.toJSONString(paramDto.getHeads()));
//                sample.setNewColData(newColData.toJSONString());
//                boolean save = tMeasureSampleService.save(sample);
//                if (!save){
//                    return Result.ok("新增失败");
//                }
//                TaskSpecimen newTaskSpecimen = new TaskSpecimen();
//                newTaskSpecimen.setSpecimenKey(sample.getId());
//                newTaskSpecimen.setDeleteType("0");
//                newTaskSpecimen.setIsTested("0");
//                taskSpecimenList.add(newTaskSpecimen);
//            }
//        }
//
//        task.setSampleKey(JSON.toJSONString(taskSpecimenList));
//        boolean update = tMeasureTaskService.updateById(task);
//        return Result.ok(update);

        // 检查方法ID是否存在，如果不存在则返回错误信息
        if (!StringUtils.hasText(paramDto.getMethodId())) {
            return Result.fail("请先创建方法");
        }
        // 检查新增样品列表是否为空，如果为空则初始化为空列表
        if(paramDto.getNewSamples() == null) {
            paramDto.setNewSamples(new ArrayList<>());
        }
        // 检查删除样品ID列表是否为空，如果为空则初始化为空列表
        if (paramDto.getDelSampleIds() == null) {
            paramDto.setDelSampleIds(new ArrayList<>());
        }
        // 检查是否有新增或删除的样品，如果没有则返回提示信息
        if (paramDto.getNewSamples().isEmpty() && paramDto.getDelSampleIds().isEmpty()) {
            return Result.ok("没有要新增或删除的样品！");
        }

        // 检查表格新增列数是否超过10列，如果超过则返回错误信息
        if (paramDto.getHeads() != null && paramDto.getHeads().size() > 10) {
            return Result.fail("表格新增列数不能超过10列");
        }
        // 检查任务ID是否存在，如果不存在则创建新任务
        if (!StringUtils.hasText(paramDto.getTaskId())) {
            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            // 根据方法ID获取测量方法
            TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
            // 根据用户名查询系统用户管理信息
            SystemUserManagePO po = userConfigDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, paramDto.getUpdateBy()));
            // 获取用户ID
            String id = po.getId();
            // 设置新任务的用户ID
            newTask.setUserId(id);
            // 设置新任务的项目项目键
            newTask.setProjectItemKey(method.getProjectItemKey());
            // 设置新任务的方法键
            newTask.setMethodKey(method.getId());
            // 设置新任务的测量类型
            newTask.setMeasureType(method.getMeasurementType());
            // 生成随机字符串作为任务名称的一部分
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());

            // 设置新任务的任务名称，包含测量类型和随机字符串
            newTask.setTaskName(method.getMeasurementType() + "task_" + collect);
            // 设置新任务的创建时间为当前时间
            newTask.setCreateTime(LocalDateTime.now());
            // 初始化保存标志为false
            boolean save = false;
            // 尝试保存新任务，最多尝试3次
            for (int i = 0; i < 3; i++) {
                if (tMeasureTaskService.save(newTask)) {
                    save = true;
                    break;
                }
            }
            // 如果保存失败，返回系统错误信息
            if (!save) {
                return Result.fail("系统出错");
            }
            // 设置参数DTO的任务ID
            paramDto.setTaskId(newTask.getId());
        }


        // 根据任务ID获取测量任务
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        // 如果任务不存在，返回任务不存在信息
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 解析任务中的样品键，获取任务样品列表
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        // 遍历任务样品列表
        for (TaskSpecimen specimen : taskSpecimen) {
            // 检查当前样品是否在要删除的样品ID列表中
            if (paramDto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                // 设置样品的删除类型
                specimen.setDeleteType(deleteType);
                // 创建查询条件，查询指定样品键和任务ID的测量数据，按序列号降序排列
                LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                        .eq(TMeasureAnalysisData::getTaskId, task.getId())
                        .orderByDesc(TMeasureAnalysisData::getAnalysisNumber);
                // 执行查询，获取测量数据列表
                List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(queryWrapper);
                // 如果列表不为空，获取最新的一条记录
                if (list != null && !list.isEmpty()) {
                    TMeasureAnalysisData one = list.get(0);
                    // 设置测量数据的删除类型
                    one.setIsDelete(deleteType);
                    // 更新测量数据
                    tMeasureAnalysisDataService.updateById(one);
                }
            }
        }

        // 遍历参数DTO中的新样品列表
        for (Map<String, String> map : paramDto.getNewSamples()) {
            // 将Map转换为JSON字符串，再解析为SampleVO对象
            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
            // 创建新的测量样品对象
            TMeasureSample newSample = new TMeasureSample();
            // 复制SampleVO的属性到新样品对象
            BeanUtils.copyProperties(sample, newSample);
            // 设置新样品的ID
            newSample.setId(sample.getSampleId());

            // 如果参数DTO中的表头信息不为空
            if (paramDto.getHeads() != null) {

                // 将表头信息转换为JSON字符串并设置到新样品中
                newSample.setHeads(JSON.toJSONString(paramDto.getHeads()));

                // 创建新的列数据JSON对象
                JSONObject newColData = new JSONObject();

                // 遍历表头信息，从Map中获取对应的值并添加到列数据JSON对象中
                for (HeadVO head : paramDto.getHeads()) {
                    String s = map.get(head.getIdentity());
                    newColData.put(head.getIdentity(), s);
                }

                // 设置新样品的新列数据为JSON字符串
                newSample.setNewColData(JSON.toJSONString(newColData));
            }

            // 如果新样品的ID不为空，更新样品；否则，创建新样品
            if (StringUtils.hasText(newSample.getId())) {
                tMeasureSampleService.updateById(newSample);
            } else {
                // 创建新的任务样品对象
                TaskSpecimen specimen = new TaskSpecimen();
                // 设置任务样品的删除类型和测试状态
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                // 记录新增样品的日志
                log.info("新增的样品：{}", newSample);
                // 设置新样品的数据批量为0
                newSample.setDataBulk(0);
                // 保存新样品
                tMeasureSampleService.save(newSample);
                // 设置任务样品的样品键为新样品的ID
                specimen.setSpecimenKey(newSample.getId());
                // 将新任务样品添加到任务样品列表中
                taskSpecimen.add(specimen);
            }
        }

        // 将任务样品列表转换为JSON字符串并设置到任务中
        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        // 更新任务并返回操作结果
        return Result.ok(tMeasureTaskService.updateById(task));
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
        if (upload.getImageType().equals("0")){
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
        }else{
            // 根据任务id查询任务附属表的内容
            TMeasureTaskAttachment byId = tMeasureTaskAttachmentService.getById(upload.getId());
            if (byId == null){
                return Result.fail("任务附属不存在");
            }
            if ("0".equals(upload.getImageType())){
                byId.setOverlapImage(bytes);
            }else if ("1".equals(upload.getImageType())){
                byId.setTiledImage(bytes);
            }else if ("2".equals(upload.getImageType())){
                byId.setAddrImage(bytes);
            }else{
                return Result.fail("Type类型错误");
            }
            boolean update = tMeasureTaskAttachmentService.updateById(byId);
            return Result.ok(update);
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
        task.setEndTime(LocalDateTime.now());
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
    public Result obtainTheBaseline(String taskId,String methodId,String deviceSerialNumber) {
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
        TMeasureTask task =null;
        if (taskId != null && !taskId.equals("")){
            task = tMeasureTaskService.getById(taskId);
        }else{
            task = new TMeasureTask();
            TMeasureMethod method = tMeasureMethodService.getById(methodId);
            SystemUserManagePO user = userConfigDAO.selectOne(new QueryWrapper<>(SystemUserManagePO.class).eq("user_name", method.getMethodCreator()));
            task.setUserId(user.getId());
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            String collect = new Random().ints(8, 0, chars.length())
                    .mapToObj(i -> String.valueOf(chars.charAt(i)))
                    .collect(Collectors.joining());

            task.setTaskName(method.getMeasurementType() + "task_" + collect);
            task.setCreateTime(LocalDateTime.now());
            task.setMethodKey(methodId);
            task.setMeasureType(method.getMeasurementType());
            task.setProjectItemKey(method.getProjectItemKey());
            task.setStartTime(LocalDateTime.now());
            task.setDeviceSerialNumber(deviceSerialNumber);
            tMeasureTaskService.save(task);
            TMeasureTaskAttachment taskAttachment = new TMeasureTaskAttachment();
            taskAttachment.setTaskId(task.getId());
            boolean save = tMeasureTaskAttachmentService.save(taskAttachment);
            if (!save){
                return Result.fail("保存失败");
            }
        }
        // 将listR中的数据保存到task的校零参数中
        task.setZeroParam(JSON.toJSONString(listR.getData()));
        boolean update = tMeasureTaskService.updateById(task);
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

    /**
     * 查询峰谷数据
     * @return
     */
    @Override
    public Result queryPeakAndValleysPort(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<PeakValleyMarking> peaks = new ArrayList<>();
        List<PeakValleyMarking> grains = new ArrayList<>();
        List<PeakValleyMarking> dataAll = new ArrayList<>();
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(dataId);
        if (measureData == null) {
            return Result.ok(new ArrayList<>());
        }
        if (measureData.getPeak()!=null){
            peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
            dataAll.addAll( peaks);
        }
        if (measureData.getGrain()!=null){
            grains = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
            dataAll.addAll( grains);
        }
        if (dataAll!=null && dataAll.size()!=0){
            if (digit!=null){
                for (PeakValleyMarking data : dataAll){
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
                }
            }
        }
        return Result.ok(dataAll);
    }

    @Override
    public Result queryGaugePointPort(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<PeakValleyMarking> gaugePoints = new ArrayList<>();
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(dataId);
        if (measureData == null) {
            return Result.ok(new ArrayList<>());
        }
        if (measureData.getGaugePoint()!=null){
            gaugePoints = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
            if (digit!=null) {
                for (PeakValleyMarking data : gaugePoints){
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
                }
            }
        }
        return Result.ok(gaugePoints);
    }

    @Override
    public Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark()==null){
            return Result.fail("请先添加备注");
        }
         // 根据测量id去分析数据表中进行查询
         TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
         if (tMeasureData==null){
             return Result.fail("测量数据不存在");
         }
         // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
         List<PeakValleyMarking> list;
         if (paramDto.getType().equals("波峰")){
             list = JSON.parseArray(tMeasureData.getPeak(), PeakValleyMarking.class);
         }else{
             list = JSON.parseArray(tMeasureData.getGrain(), PeakValleyMarking.class);
         }
         // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
         for (PeakValleyMarking marking : list) {
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
        boolean update = tMeasureAnalysisDataService.updateById(tMeasureData);
        return update?Result.ok():Result.fail();
    }

    @Override
    public Result addGaugePointRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark()==null){
            return Result.fail("请先添加备注");
        }
        // 根据测量id去分析数据表中进行查询
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData==null){
            return Result.fail("分析数据不存在");
        }
        List<PeakValleyMarking> list = JSON.parseArray(tMeasureData.getGaugePoint(), PeakValleyMarking.class);
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (PeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        tMeasureData.setGaugePoint(JSON.toJSONString(list));
        boolean update = tMeasureAnalysisDataService.updateById(tMeasureData);
        return update?Result.ok():Result.fail();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(UpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()) {
            return Result.fail("参数不能为空");
        }

        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;


         // 分析数据
         TMeasureAnalysisData analysis = tMeasureAnalysisDataService.getById(dataId);
         if (analysis == null) {
             return Result.fail("样品数据不存在");
         }
         analysis.setColor(color);
         updated = tMeasureAnalysisDataService.updateById(analysis);


        if (!updated) {
            return Result.fail("更新失败");
        }

        return Result.ok();
    }

    /**
     * 获取样例表头
     * @param taskId
     * @return
     */
    @Override
    public Result<List<HeadVO>> querySampleHeads(String taskId) {
        List<HeadVO> ans = new ArrayList<>();

        if (!StringUtils.hasText(taskId)) {
            return Result.ok(ans);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        if (StringUtils.hasText(task.getSampleKey())) {
            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen specimen : taskSpecimen) {
                TMeasureSample measureSample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                List<HeadVO> heads = JSON.parseArray(measureSample.getHeads(), HeadVO.class);

                if (heads != null && !heads.equals(ans)) {
                    ans.addAll(heads);
                }
            }
        }
        return Result.ok(ans);
    }


    /**
     * 获取样品的详细数据
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureAnalysisData measureData,Integer digit) {
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
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
            if (gauges != null) {
                for (PeakValleyMarking gauge : gauges) {
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
            if (peaks != null) {
                for (PeakValleyMarking peak : peaks) {
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
            if (grades != null) {
                for (PeakValleyMarking grade : grades) {
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
     * 获取样品的详细数据
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureData measureData,Integer digit) {
        List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
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
            if (gauges != null) {
                for (PeakValleyMarking gauge : gauges) {
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
            if (peaks != null) {
                for (PeakValleyMarking peak : peaks) {
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
            if (grades != null) {
                for (PeakValleyMarking grade : grades) {
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
        // 根据taskId查询方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 使用 Apache POI 创建 Word 文件
        XWPFDocument document = new XWPFDocument();

        // 添加标题
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("任务报告");
        titleRun.setFontSize(18);
        titleRun.setBold(true);
        titleRun.setFontFamily("黑体");
        titleRun.addBreak();

        // 添加任务信息
        XWPFParagraph taskInfo = document.createParagraph();
        taskInfo.setAlignment(ParagraphAlignment.LEFT);
        // 每条信息单独一行
        addTextRun(taskInfo, "任务名称: " + task.getTaskName(), 12, false,true);
        addTextRun(taskInfo, "测量类型: " + task.getMeasureType(), 12, false,true);
        addTextRun(taskInfo, "创建时间: " + task.getCreateTime().toString(), 12, false,true);
        addTextRun(taskInfo, "方法参数: ", 14, true,true);
        addTextRun(taskInfo, "方法名称: " + method.getMethodName(), 12, false,true);
        addTextRun(taskInfo, "是否共享: " + (method.getIsSharingMethod()==null||method.getIsSharingMethod()=="0"?"否":"是"), 12, false,true);
        addTextRun(taskInfo, "创建时间: " + method.getMethodCreateTime().format(formatter), 12, false,true);
        addTextRun(taskInfo, "创建人: " + method.getMethodCreator(), 12, false,true);
        addTextRun(taskInfo, "光度模式: " + (spectrumParameter.getLuminosity()=="1"?"Abs":spectrumParameter.getLuminosity()=="2"?"T%":spectrumParameter.getLuminosity()=="3"?"Es":spectrumParameter.getLuminosity()=="4"?"Er":""), 12, false,true);
        addTextRun(taskInfo, "起始波长: " + spectrumParameter.getStartWaveLength(), 12, false,true);
        addTextRun(taskInfo, "结束波长: " + spectrumParameter.getEndWaveLength(), 12, false,true);
        addTextRun(taskInfo, "波长间隔: " + spectrumParameter.getWaveLengthInterval(), 12, false,true);
        addTextRun(taskInfo, "重复模式: " + (spectrumParameter.getRepetitionPattern()=="0"?"不重复":"单池重复"), 12, false,true);
        addTextRun(taskInfo, "重复次数: " + spectrumParameter.getRepetitionTime(), 12, false,true);
        addTextRun(taskInfo, "时间间隔: " + spectrumParameter.getTimeInterval(), 12, false,true);
        addTextRun(taskInfo, "阶梯矫正: " + (String.valueOf(spectrumParameter.getLadderCorrection())=="true"?"启用":"不启用"), 12, false,true);
        addTextRun(taskInfo, "扫描速度: " + (spectrumParameter.getScanSpeed()=="0"?"慢速":spectrumParameter.getScanSpeed()=="1"?"中速":spectrumParameter.getScanSpeed()=="2"?"快速":"极快"), 12, false,true);

        addTextRun(taskInfo, "样品信息: ", 14, true,true);
        // 创建表格
        XWPFTable table = document.createTable();

//        // 设置表头
//        XWPFTableRow headerRow = table.getRow(0);
//        headerRow.getCell(0).setText("样品ID");
//        headerRow.addNewTableCell().setText("样品名称");
//        headerRow.addNewTableCell().setText("样品位置");
//
//
//        // 设置表格宽度：通过 XWPFTableCell 设置列宽
//        for (int i = 0; i < table.getRows().size(); i++) {
//            XWPFTableRow row = table.getRow(i);
//            for (int j = 0; j < row.getTableCells().size(); j++) {
//                XWPFTableCell cell = row.getCell(j);
//                // 设置单元格的宽度（单位为 twips：1 twip = 1/1440 英寸）
//                cell.setWidth("4000"); // 设置单元格宽度
//            }
//        }

        // 设置表头
        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("样品ID");
        headerRow.addNewTableCell().setText("样品名称");
        headerRow.addNewTableCell().setText("样品位置");
        CTTblWidth tableWidth = table.getCTTbl().addNewTblPr().addNewTblW();
        tableWidth.setW(BigInteger.valueOf(9072)); // 总表格宽度
        tableWidth.setType(STTblWidth.DXA);

        table.setWidth("100%");

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                XWPFParagraph para = cell.getParagraphs().get(0);
                para.setAlignment(ParagraphAlignment.CENTER);
                para.setVerticalAlignment(TextAlignment.CENTER);
                XWPFRun run = para.createRun();
                run.setFontSize(11);
                run.setFontFamily("宋体");
                run.setBold(false);
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

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"task_report.docx\"");
        document.write(response.getOutputStream());
    }

    private void addTextRun(XWPFParagraph paragraph, String text, int fontSize, boolean bold, boolean isLineFeed) {
        XWPFRun run = paragraph.createRun();
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("宋体");
        run.setText(text);
        run.addBreak();
        if(isLineFeed){
            run.addBreak();
        }
    }

    /**
     * 数字字符串归一化（防止 360 / 360.0 / 3.6E2 比较失败）
     */
    private String normalizeNumberString(String numStr) {
        return new BigDecimal(numStr)
                .stripTrailingZeros()
                .toPlainString();
    }


}
