/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TableHeadILocateEnums;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskHeadSnapshot;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.parseReport.CalculationFormulaService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.StandardPretreatmentService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
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
import com.persee.workstation.uv_inexus.vo.concise.common.FSampleVO;
import com.persee.workstation.uv_inexus.vo.concise.spectrum.FSpectrumParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.OriginalMeasuredDataVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.QueryMethodParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleListVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SpectrumBasicParameterVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SampleVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
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

import javax.servlet.http.HttpServletResponse;
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
 * 光谱扫描服务实现类
 */
@Slf4j
@Service("spectrumService")
public class SpectrumServiceImpl implements SpectrumService {



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
    private FormulaUtil formulaUtil;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private TMeasureTaskMapper tMeasureTaskMapper;

    @Autowired
    private PythonPathConstant pythonPathConstant;

    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;

    // 计算公式的服务
    @Autowired
    private CalculationFormulaService calculationFormulaService;

    // 标准预处理服务
    @Autowired
    private StandardPretreatmentService standardPretreatmentService;
    @Autowired
    private TMeasureTaskAttachmentService tMeasureTaskAttachmentService;
    @Autowired
    private TMeasureAnalysisImageService tMeasureAnalysisImageService;

    @Autowired
    private DevOperationUtilService devOperationUtilService;

    @Value("1")
    String deleteType;

    @Value("0")
    String createType;

    @Value("0")
    String noTested;


    /**
     * 查询项目任务方法
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryProjectTaskMethod(String updateBy, String measureType) {
        SystemUserManagePO user = userConfigDAO.selectOne(new QueryWrapper<SystemUserManagePO>().eq("user_name", updateBy));
        if (user == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        MeasureTypeEnum measureTypeEnum = MeasureTypeEnum.fromDescription(measureType);
        if (measureTypeEnum == null) {
            //暂不支持
            return Result.fail(YamlMessageSource.getMessageStatic("0000002B"));
        }
        // 根据用户id和测量类型然后通过创建时间查找到最近的一次任务
        TMeasureTask task = tMeasureTaskService.getOne(new QueryWrapper<TMeasureTask>()
                .eq("user_id", user.getId())
                .eq("measure_type", measureTypeEnum.getDescription())
                .orderByDesc("create_time")
                .last("limit 1"));

        if (task != null) {
            // 根据任务id查询方法
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            // 根据项目id查询项目
            TMeasureProject project = tMeasureProjectService.getById(task.getProjectItemKey());
            SpectrumBasicParameterVo spectrumBasicParameterVo = new SpectrumBasicParameterVo();
            spectrumBasicParameterVo.setTaskId(task.getId());
            spectrumBasicParameterVo.setTaskName(task.getTaskName());
            if (method == null) {
                spectrumBasicParameterVo.setMethodId(null);
                spectrumBasicParameterVo.setMethodName(null);
            } else {
                spectrumBasicParameterVo.setMethodId(method.getId());
                spectrumBasicParameterVo.setMethodName(method.getMethodName());
            }
            if (project == null) {
                spectrumBasicParameterVo.setProjectId(null);
                spectrumBasicParameterVo.setProjectName(null);
            } else {
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
        if (method == null) {
            TMeasureProject project = tMeasureProjectService.getOne(new QueryWrapper<TMeasureProject>()
                    .eq("creator_key", updateBy)
                    .orderByDesc("create_time")
                    .last("limit 1"));
            if (project == null) {
                return Result.ok(vo);
            }
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
        } else {
            TMeasureProject project = tMeasureProjectService.getById(method.getProjectItemKey());
            vo.setMethodId(method.getId());
            vo.setMethodName(method.getMethodName());
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
        }
        return Result.ok(vo);
    }

    /**
     * 查询项目下所有方法
     * @param projectId
     * @param measureForm
     * @return
     */
    @Override
    public Result queryProjectAllMethods(String projectId, String measureForm) {
        List<TMeasureMethod> methods = tMeasureMethodService.list(new QueryWrapper<TMeasureMethod>().eq("project_item_key", projectId).eq("measurement_type", measureForm));

        if (methods == null) {
            //没有此类型的数据
            return Result.fail(YamlMessageSource.getMessageStatic("0030000A"));
        }
        return Result.ok(methods);
    }

    /**
     * 查询方法的参数
     *
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
                && queryMethodParameterVo.getLuminosity().equals("4")) {
            //请选择正确的光度模式
            return Result.fail(YamlMessageSource.getMessageStatic("00304002"));
        }
        queryMethodParameterVo.setLuminosityName(queryMethodParameterVo.getLuminosity().equals("1") ? "Abs" : queryMethodParameterVo.getLuminosity().equals("2") ? "T%" : queryMethodParameterVo.getLuminosity().equals("3") ? "Es" : "Er");
        if (queryMethodParameterVo.getScanSpeed().equals("0")
                && queryMethodParameterVo.getScanSpeed().equals("1")
                && queryMethodParameterVo.getScanSpeed().equals("2")
                && queryMethodParameterVo.getScanSpeed().equals("3")) {
            //请选择正确的扫描速度
            return Result.fail(YamlMessageSource.getMessageStatic("00304003"));
        }
        queryMethodParameterVo.setScanSpeedName(queryMethodParameterVo.getScanSpeed().equals("0") ? "慢速" : queryMethodParameterVo.getScanSpeed().equals("1") ? "中速" : queryMethodParameterVo.getScanSpeed().equals("2") ? "快速" : "极快");
        return Result.ok(queryMethodParameterVo);
    }

    /**
     * 创建方法接口
     * @param methodDto
     * @param updateBy
     * @param spectrumParameter
     * @return
     */
    @Override
    public Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, SpectrumParameter spectrumParameter) {
        // 创建测量方法表
        TMeasureMethod tMeasureMethod = new TMeasureMethod();
        if (methodDto.getPretreatmentIds() != null) {
            tMeasureMethod.setPreprocessKey(JSON.toJSONString(methodDto.getPretreatmentIds()));
        }
        tMeasureMethod.setProjectItemKey(methodDto.getProjectId());
        tMeasureMethod.setMethodName(methodDto.getMethodName());
        tMeasureMethod.setMeasurementType(methodDto.getMethodType());
        tMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(spectrumParameter));
        tMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        tMeasureMethod.setMethodCreator(updateBy);
        tMeasureMethod.setIsDelete(Constants.PARAM_ZERO);
        boolean save = tMeasureMethodService.save(tMeasureMethod);
        if (save) {
            //创建成功
            return Result.ok(YamlMessageSource.getMessageStatic("0000000F"));
        } else {
            //创建失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000010"));
        }
    }

    /**
     * 查询测量数据列表接口
     *
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
            //没有数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000002A"));
        }
        // 将测量的数据从json转换为OriginalMeasuredDataVo对象
        OriginalMeasuredDataVo originalMeasuredDataVo = new OriginalMeasuredDataVo();
        List<String> list;
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();

        try {
            list = JSON.parseObject(tMeasureData.getOriginalCalculationData(), ArrayList.class);
        } catch (Exception e) {
            return Result.fail(YamlMessageSource.getMessageStatic("00304001"));
        }
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        double[] y_data = new double[yList.size()];
        for (int i = 0; i < yList.size(); i++) {
            y_data[i] = Double.parseDouble(yList.get(i));
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
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request, Map.class).getBody();
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
                if (xList.get(i).equals(peakX.get(j)) && yList.get(i).equals(peakY.get(j))) {
                    vo.setFeature("1");
                }
            }
            if (vo.getFeature() == null) {
                vo.setFeature("0");
            }
            // 遍历谷的长度,如果谷的x坐标和y坐标都相等,则说明是谷
            for (int j = 0; j < valleyX.size(); j++) {
                if (xList.get(i).equals(valleyX.get(j)) && yList.get(i).equals(valleyY.get(j))) {
                    vo.setGrain("1");
                }
            }
            // 如果对象中的谷属性为null,则说明不是谷,则设置为0
            if (vo.getGrain() == null) {
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

    /**
     * 查询测量参数接口
     * @param taskId
     * @return
     */
    @Override
    public Result queryMeasurementParameters(String taskId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey() == null || task.getSampleKey().equals("[]")) {
            return Result.ok(new ArrayList<>());
        }
        // 根据任务的方法key查询方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String, String>> list = new ArrayList<>();
        // 判断是否是重复模式
        if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpeciman.getSpecimenKey())
                            .eq("analysis_type", "5")
                            .eq("is_delete", "0")
                            .eq("analysis_number", 1));
                    TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                    if (one.getAnalysisResult() != null) {
                        Map<String, String> map = new HashMap<>();
                        map.put("measureId", one.getId());
                        map.put("sampleName", byId.getSampleName());
                        list.add(map);
                    }
                }
            }
        } else {
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                    List<TMeasureAnalysisData> measureDataList = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpeciman.getSpecimenKey())
                            .orderByAsc("analysis_number"));
                    TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                    for (TMeasureAnalysisData tMeasureData : measureDataList) {
                        if (tMeasureData.getAnalysisResult() != null) {
                            Map<String, String> map = new HashMap<>();
                            map.put("measureId", tMeasureData.getId());
                            map.put("sampleName", byId.getSampleName() + "-" + tMeasureData.getAnalysisNumber());
                            list.add(map);
                        }
                    }
                }
            }
        }

        return Result.ok(list);
    }

    /**
     * 设置参数接口
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
        TMeasureMethod tMeasureMethod = tMeasureMethodService.getOne(new QueryWrapper<TMeasureMethod>().eq("id", methodId).eq("measurement_type", "光谱扫描"));
        if (tMeasureMethod == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        SpectrumParameter spectrumParam = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), SpectrumParameter.class);
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
     * 保存数据接口
     * @param taskId
     * @return
     */
    @Override
    public Result saveData(String taskId) {
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        task.setEndTime(LocalDateTime.now());
        task.setIsDelete("1");
        boolean update = tMeasureTaskService.updateById(task);
        if (update) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    /**
     * 获取样本输入表头接口
     * @param taskId
     * @return
     */
    @Override
    public Result<List<HeadVO>> querySampleHeads(String taskId) {

        if (!StringUtils.hasText(taskId)) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
            return Result.ok(preHead);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        if (!StringUtils.hasText(task.getTableInfo())) {
            List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
            return Result.ok(preHead);
        } else {
            TaskHeadSnapshot snapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
            if (snapshot.getSampleHeads() != null && !snapshot.getSampleHeads().isEmpty()) {
                return Result.ok(snapshot.getSampleHeads());
            } else {
                List<HeadVO> preHead = tMeasureTaskMapper.getPreHead(MeasureTypeEnum.SPECTRAL_SCAN.getDescription(), null);
                return Result.ok(preHead);
            }
        }
    }

    /**
     * 获取样本按钮列表接口
     *
     * @param taskId
     * @return
     */
    @Override
    public Result querySampleList(String taskId) {
        List<JSONObject> sampleVOList = new ArrayList<>();
        if (!StringUtils.hasText(taskId)) {
            return Result.ok(sampleVOList);
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
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
     * 添加或删除样本接口
     *
     * @param dto
     * @return
     */
    @Override
    public Result addOrDeleteSamplePort(AddOrDeleteSampleDTO dto) {

        if (!StringUtils.hasText(dto.getMethodId())) {
            //请先创建方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000011"));
        }
        if (dto.getHeads() == null || dto.getHeads().isEmpty()) {
            //至少需要一个表头
            return Result.fail(YamlMessageSource.getMessageStatic("00000012"));
        }

        if (dto.getHeads() != null && dto.getHeads().size() > 10) {
            //表格新增列数不能超过10列
            return Result.fail(YamlMessageSource.getMessageStatic("00000013"));
        }
        if (!StringUtils.hasText(dto.getTaskId())) {
            //创建任务
            TMeasureTask newTask = new TMeasureTask();

            TMeasureMethod method = tMeasureMethodService.getById(dto.getMethodId());
            SystemUserManagePO po = userConfigDAO.selectOne(Wrappers.lambdaQuery(SystemUserManagePO.class).eq(SystemUserManagePO::getUserName, dto.getUpdateBy()));
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
                if (tMeasureTaskService.save(newTask)) {
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


        TMeasureTask task = tMeasureTaskService.getById(dto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        /// 校验是否有重复的样品编号
        // 取出所有样品编号
        List<String> sampleNos = dto.getNewSamples().stream()
                .map(m -> JSON.parseObject(JSON.toJSONString(m), FSampleVO.class).getSampleNumber()) // 取出 sampleNo
                .filter(Objects::nonNull)    // 过滤掉空值
                .collect(Collectors.toList());
        // 查重：找到重复的样品编号
        Set<String> duplicates = sampleNos.stream()
                .filter(no -> Collections.frequency(sampleNos, no) > 1)
                .collect(Collectors.toSet());
        // 判断如果isTested为true，
        if (!duplicates.isEmpty()) {
            //样品编号重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000003F"));
        }
        if(taskSpecimen == null){
            taskSpecimen = new ArrayList<>();
        }
        if (taskSpecimen != null){
            for (TaskSpecimen specimen : taskSpecimen) {
                if (dto.getDelSampleIds().contains(specimen.getSpecimenKey())) {
                    specimen.setDeleteType(deleteType);
                    LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                            .eq(TMeasureData::getTaskId, task.getId())
                            .orderByDesc(TMeasureData::getSerialNumber);
                    List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
                    if (list != null && !list.isEmpty()) {
                        TMeasureData one = list.get(0);
                        one.setIsDelete(deleteType);
                        tMeasureDataService.updateById(one);
                    }
                }
            }
        }

        TaskHeadSnapshot taskHeadSnapshot = new TaskHeadSnapshot();

        if (StringUtils.hasText(task.getTableInfo())) {
            taskHeadSnapshot = JSON.parseObject(task.getTableInfo(), TaskHeadSnapshot.class);
        }

        if (dto.getHeads() != null && !dto.getHeads().isEmpty()) {
            taskHeadSnapshot.setSampleHeads(dto.getHeads());
        }
        taskHeadSnapshot.setTaskId(task.getId());
        task.setTableInfo(JSON.toJSONString(taskHeadSnapshot));


        for (Map<String, String> map : dto.getNewSamples()) {
            String jsonString = JSON.toJSONString(map);
            SampleVO sample = JSON.parseObject(jsonString, SampleVO.class);
            TMeasureSample newSample = new TMeasureSample();
            BeanUtils.copyProperties(sample, newSample);
            newSample.setId(sample.getSampleId());

            if (dto.getHeads() != null) {
                JSONObject newColData = new JSONObject();

                for (HeadVO head : dto.getHeads()) {
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
                tMeasureSampleService.updateById(newSample);
            } else {
                TaskSpecimen specimen = new TaskSpecimen();
                specimen.setDeleteType(createType);
                specimen.setIsTested(noTested);
                specimen.setSampleType(newSample.getSampleType());
                log.info("新增的样品：{}", newSample);
                tMeasureSampleService.save(newSample);
                specimen.setSpecimenKey(newSample.getId());
                taskSpecimen.add(specimen);
            }
        }

        String jsonString = JSONArray.toJSONString(taskSpecimen);
        task.setSampleKey(jsonString);

        return Result.ok(tMeasureTaskService.updateById(task));
    }

    /**
     * 删除样本接口
     *
     * @param sampleId
     * @param taskId
     * @return
     */
    @Override
    public Result deleteSamplePort(String sampleId, String taskId) {
        // 获取任务数据
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
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

    /**
     * 获取样本列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result sampleListPort(String taskId, String type, Integer digit) {
        // 先对前端传过来的数据进行校验
        if (taskId == null || taskId.equals("")) {
            //任务id不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000003"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // 根据任务中的方法id查找方法并获取里面的方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<SampleListVo> listVos = new ArrayList<>();
        // 判断是否是重复模式
        if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
            // 不重复模式
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                SampleListVo vo = new SampleListVo();
                // 根据样品id查询样品数据
                TMeasureSample sample = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    //样品不存在
                    return Result.fail(YamlMessageSource.getMessageStatic("0000000B"));
                }
                // 如果没有测量数据则设置为无测量数据的参数返回
                if (taskSpeciman.getIsTested().equals("0")) {
                    vo.setTitle(sample.getSampleName());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisId(null);
                    vo.setAnalysisNumber(1);
                    vo.setColor(null);
                    vo.setOriginalData(null);
                } else {
                    QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", taskId);
                    queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                    queryWrapper.eq("analysis_type", "5");
                    queryWrapper.orderByDesc("analysis_number");
                    if (type == null) {
                        queryWrapper.eq("is_delete", "0");
                    }
                    queryWrapper.last("limit 1");
                    TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);
                    if (measureData == null) {
                        vo.setTitle(sample.getSampleName());
                        vo.setAnalysisId(null);
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(null);
                        vo.setColor(null);
                        vo.setOriginalData(null);
                    } else {
                        vo.setTitle(sample.getSampleName());
                        vo.setAnalysisId(measureData.getId());
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(measureData.getAnalysisNumber());
                        vo.setColor(measureData.getColor());
                        if (measureData.getAnalysisResult() == null) {
                            vo.setOriginalData(null);
                        } else {
                            vo.setOriginalData(getSampleData(measureData, digit));
                        }
                    }
                }
                vo.setType("0");
                vo.setDeleteType(taskSpeciman.getDeleteType());
                listVos.add(vo);
            }
        } else {
            // 重复模式
            if (spectrumParameter.getRepetitionTime() == null || spectrumParameter.getRepetitionTime().equals("")) {
                //请填写重复次数
                return Result.fail(YamlMessageSource.getMessageStatic("00000037"));
            }
            Integer count = Integer.valueOf(spectrumParameter.getRepetitionTime());
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (type == null && taskSpeciman.getDeleteType().equals("1")) {
                    continue;
                }
                // 根据样品id查询样品数据
                TMeasureSample sample = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    //样品不存在
                    return Result.fail(YamlMessageSource.getMessageStatic("0000000B"));
                }
                int key = 0;
                // 根据方法中的方法参数的重复次数进行循环
                for (int i = 0; i < count; i++) {
                    SampleListVo vo = new SampleListVo();
                    QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", taskId);
                    queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                    queryWrapper.eq("analysis_type", "5");
                    if (type == null) {
                        queryWrapper.eq("is_delete", "0");
                    }
                    queryWrapper.eq("analysis_number", (i + 1));
                    queryWrapper.last("limit 1");
                    TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getOne(queryWrapper);
                    if (measureData == null && taskSpeciman.getIsTested().equals("0")) {
                        vo.setTitle(sample.getSampleName() + "-" + (i + 1));
                        vo.setAnalysisId(null);
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(i + 1);
                        vo.setColor(null);
                        vo.setOriginalData(null);
                        vo.setDeleteType("0");
                    } else {
                        vo.setTitle(sample.getSampleName() + "-" + (i + 1));
                        vo.setAnalysisId(measureData.getId());
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(measureData.getAnalysisNumber());
                        vo.setColor(measureData.getColor());
                        if (measureData.getAnalysisResult() == null) {
                            vo.setOriginalData(null);
                        } else {
                            vo.setOriginalData(getSampleData(measureData, digit));
                        }
                        vo.setDeleteType(measureData.getIsDelete());
                    }
                    vo.setType("0");

                    listVos.add(vo);
                }
            }
            for (SampleListVo listVo : listVos) {
                if( listVo.getAnalysisNumber() == null){
                    //样品编号不能位空
                    return Result.fail(YamlMessageSource.getMessageStatic("00304005"));
                }
            }
        }
        return Result.ok(listVos);
    }

    /**
     * 删除样品列表接口
     *
     * @param taskId
     * @param sampleId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String taskId, String sampleId, String analysisId) {
        // 校验传过来的参数
        if (taskId == null || sampleId == null || analysisId == null) {
            //参数错误
            return Result.fail(YamlMessageSource.getMessageStatic("00104003"));
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        List<TaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        // 根据任务id查询方法表数据并取出方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        // 判断是否是重复模式
        if (spectrumParameter.getRepetitionPattern().equals("不重复")) {
            // 根据分析id查询分析数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
            }
            byId.setIsDelete("1");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            // 循环遍历样品列表找到该样品
            for (TaskSpecimen taskSpecimen : taskSpecimenList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("1");
                }
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimenList));
            boolean update1 = tMeasureTaskService.updateById(task);
            if (update && update1) {
                //成功删除数据
                return Result.ok(YamlMessageSource.getMessageStatic("00000043"));
            } else {
                //删除数据失败
                return Result.fail(YamlMessageSource.getMessageStatic("00000044"));
            }
        } else {
            // 根据分析id查询分析数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
            }
            byId.setIsDelete("1");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            // 查询跟这个样品和任务id关联的分析数据
            List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("task_id", taskId).eq("sample_key", sampleId).eq("is_delete", "0"));
            if (list.size() == 0) {
                // 循环遍历样品列表找到该样品
                for (TaskSpecimen taskSpecimen : taskSpecimenList) {
                    if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                        taskSpecimen.setDeleteType("1");
                        break;
                    }
                }
                task.setSampleKey(JSON.toJSONString(taskSpecimenList));
                boolean update1 = tMeasureTaskService.updateById(task);
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
     * 获取变换列表接口
     * @param taskId
     * @param type
     * @return
     */
    @Override
    public Result changeTheListPort(String taskId, String type, Integer digit) {
        // 根据任务id去查询分析数据
        QueryWrapper<TMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.ne("analysis_type", "5");
        if (type == null || type.equals("")) {
            queryWrapper.eq("is_delete", "0");
        }
        List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(queryWrapper);
        List<SampleListVo> listVos = new ArrayList<>();
        for (TMeasureAnalysisData tMeasureAnalysisData : list) {
            SampleListVo vo = new SampleListVo();
            vo.setAnalysisId(tMeasureAnalysisData.getId());
            vo.setTitle(tMeasureAnalysisData.getAnalysisName());
            vo.setColor(tMeasureAnalysisData.getColor());
            vo.setDeleteType(tMeasureAnalysisData.getIsDelete());
            List<String> results = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
            List<PeakValleyMarking> gauges = JSON.parseArray(tMeasureAnalysisData.getGaugePoint(), PeakValleyMarking.class);
            List<PeakValleyMarking> peaks = JSON.parseArray(tMeasureAnalysisData.getPeak(), PeakValleyMarking.class);
            List<PeakValleyMarking> grades = JSON.parseArray(tMeasureAnalysisData.getGrain(), PeakValleyMarking.class);
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (String s : results) {
                String[] split = s.split(",");
                Map<String, Object> map = new HashMap<>();
                if (digit != null) {
                    BigDecimal value = new BigDecimal(split[1]);
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    map.put("x", split[0]);
                    map.put("y", value.stripTrailingZeros().toPlainString());
                } else {
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
                if (grades != null) {
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
            vo.setOriginalData(resultList);
            vo.setType("1");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    /**
     * 删除变换列表接口
     *
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = tMeasureAnalysisDataService.updateById(byId);
        if (update) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    /**
     * 恢复已删除数据接口
     *
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
            TMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task == null) {
                //没有找到该任务
                return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
            }
            List<TaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen taskSpecimen : sampleList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("0");
                    break;
                }
            }
            task.setSampleKey(JSON.toJSONString(sampleList));
            boolean update1 = tMeasureTaskService.updateById(task);
            // 根据分析id查询分析数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //没有找到该分析数据
                return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
            }
            byId.setIsDelete("0");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            if (update1 && update) {
                return Result.ok(true);
            } else {
                return Result.fail(false);
            }
        } else {
            // 根据变换数据id查询变换数据
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                //变换数据不存在
                return Result.fail(YamlMessageSource.getMessageStatic("0030000B"));
            }
            byId.setIsDelete("0");
            boolean update = tMeasureAnalysisDataService.updateById(byId);
            if (update) {
                return Result.ok(update);
            } else {
                return Result.fail(false);
            }
        }
    }

    /**
     * 更新数据颜色接口
     * @param paramDto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(UpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }

        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;


        // 分析数据
        TMeasureAnalysisData analysis = tMeasureAnalysisDataService.getById(dataId);
        if (analysis == null) {
            //样品数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000047"));
        }
        analysis.setColor(color);
        updated = tMeasureAnalysisDataService.updateById(analysis);


        if (!updated) {
            //更新失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000048"));
        }

        return Result.ok();
    }

    /**
     * 获取分析标记点接口
     * @param analysisId
     * @return
     */
    @Override
    public Result queryAnalysisGaugePoint(String analysisId) {
        // 根据测量id去测量数据表中查询数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(analysisId);
        // 将测量数据中的标记点转换成List集合
        List<String> list = new ArrayList<>();
        try {

            list =JSON.parseObject(byId.getGaugePoint(), List.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Result.ok(list);
    }

    /**
     * 添加分析标记点接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addAnalysisDataGaugePointPort(AnalysisDataGaugePointDto paramDto) {
        // 根据测量数据id查询出测量数据
        TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(paramDto.getAnalysisId());
        if (byId == null) {
            //测量数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0000003B"));
        }
        // 将标记点转换成List
        List<String> list;
        try {
            list     =  JSON.parseObject(byId.getGaugePoint(), List.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        list.add(paramDto.getGaugePoint());
        byId.setGaugePoint(JSON.toJSONString(list));
        boolean update = tMeasureAnalysisDataService.updateById(byId);
        return Result.ok(update);
    }

    /**
     * 获取峰谷接口
     *
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
        if (measureData.getPeak() != null) {
            peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
            dataAll.addAll(peaks);
        }
        if (measureData.getGrain() != null) {
            grains = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
            dataAll.addAll(grains);
        }
        if (dataAll != null && dataAll.size() != 0) {
            if (digit != null) {
                for (PeakValleyMarking data : dataAll) {
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
                }
            }
        }
        return Result.ok(dataAll);
    }

    /**
     * 寻峰寻谷接口
     * @param paramDto
     * @return
     */
    @Override
    public Result peakSearchingPort(SeekingPeaksValleysDto paramDto) {
        paramDto.setHeight(paramDto.getHeight() / 100);
        TMeasureAnalysisData tMeasureAnalysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureAnalysisData == null) {
            //测量数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0000003B"));
        }
        List<String> list = JSON.parseArray(tMeasureAnalysisData.getAnalysisResult(), String.class);
        // 分割测量数据
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        for (String s : list) {
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
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getPeakSearchingAndValleySearching(), request, Map.class).getBody();
        List<PeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        if (xPeak == null || yPeak == null) {
            //height参数太高
            return Result.fail(YamlMessageSource.getMessageStatic("0000003C"));
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
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
        }
        analysisData.setPeak(JSON.toJSONString(peakData));
        analysisData.setGrain(JSON.toJSONString(valleyData));
        flag = tMeasureAnalysisDataService.updateById(analysisData);
        //数据保存失败
        return flag ? Result.ok() : Result.fail(YamlMessageSource.getMessageStatic("00000034"));
    }

    /**
     * 添加峰谷备注接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00306003"));
        }
        if (paramDto.getRemark() == null) {
            //请先添加备注
            return Result.fail(YamlMessageSource.getMessageStatic("00000046"));
        }
        // 根据测量id去分析数据表中进行查询
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            //测量数据不存在
            return Result.fail(YamlMessageSource.getMessageStatic("0000003B"));
        }
        // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
        List<PeakValleyMarking> list;
        if (paramDto.getType().equals("波峰")) {
            list = JSON.parseArray(tMeasureData.getPeak(), PeakValleyMarking.class);
        } else {
            list = JSON.parseArray(tMeasureData.getGrain(), PeakValleyMarking.class);
        }
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (PeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        if (paramDto.getType().equals("波峰")) {
            tMeasureData.setPeak(JSON.toJSONString(list));
        } else {
            tMeasureData.setGrain(JSON.toJSONString(list));
        }
        boolean update = tMeasureAnalysisDataService.updateById(tMeasureData);
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 获取标记点接口
     * @param dataId
     * @param digit
     * @return
     */
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
        if (measureData.getGaugePoint() != null) {
            gaugePoints = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
            if (digit != null) {
                for (PeakValleyMarking data : gaugePoints) {
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
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
    public Result addGaugePoint(AddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            //请选择标记点
            return Result.fail(YamlMessageSource.getMessageStatic("0000003E"));
        }
        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
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
        return update ? Result.ok(true) : Result.fail(false);
    }

    /**
     * 添加标记点备注接口
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointRemarks(PeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }
        if (paramDto.getRemark() == null) {
            //请先添加备注
            return Result.fail(YamlMessageSource.getMessageStatic("00000046"));
        }
        // 根据测量id去分析数据表中进行查询
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            //没有找到该分析数据
            return Result.fail(YamlMessageSource.getMessageStatic("0000003D"));
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
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 查询运算源数据接口
     * @param taskId
     * @return
     */
    @Override
    public Result queryArithmeticSourceData(String taskId) {
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.ok(new ArrayList<>());
        }
        if (task.getSampleKey() == null || task.getSampleKey().equals("[]")) {
            return Result.ok(new ArrayList<>());
        }
        // 根据任务的方法key查询方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
        List<Map<String, String>> list = new ArrayList<>();
        // 判断是否是重复模式
        if ("不重复".equals(spectrumParameter.getRepetitionPattern())) {
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                    TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpeciman.getSpecimenKey())
                            .eq("analysis_type", "5")
                            .eq("is_delete", "0")
                            .eq("analysis_number", 1)
                            .last("limit 1"));
                    TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                    if (one == null) {
                        one = new TMeasureAnalysisData();
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
            for (TaskSpecimen taskSpeciman : taskSpecimen) {
                if (taskSpeciman.getIsTested().equals("1") && taskSpeciman.getDeleteType().equals("0")) {
                    List<TMeasureAnalysisData> measureDataList = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", taskSpeciman.getSpecimenKey())
                            .orderByAsc("analysis_number"));
                    TMeasureSample byId = tMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                    for (TMeasureAnalysisData tMeasureData : measureDataList) {
                        if (tMeasureData.getAnalysisResult() != null) {
                            Map<String, String> map = new HashMap<>();
                            map.put("measureId", tMeasureData.getId());
                            map.put("sampleName", byId.getSampleName() + "-" + tMeasureData.getAnalysisNumber());
                            list.add(map);
                        }
                    }
                }
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
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        // 根据传过来的名称去分析数据表中进行查询如果查询出来数据则直接返回并且返回信息提示变换名称重复
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
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
        TMeasureAnalysisData measureData1 = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId1());
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
            TMeasureAnalysisData measureData2 = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId2());
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
        body.put("operation", paramDto.getArithmeticOperators());
        // 创建请求对象
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        // 将请求结果转换成对象
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getSpectrumFourOperation(), request, Map.class).getBody();
        if (resultMap == null) {
            //python接口调用失败
            return Result.fail(YamlMessageSource.getMessageStatic("00000032"));
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
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request1, Map.class).getBody();
        // 直接拿寻峰数据进行一次寻谷
        Map<String, Object> resultMap2 = restTemplate.postForEntity(pythonPathConstant.getValleySearching(), request1, Map.class).getBody();
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
     * 高级运算接口
     * @param paramDto
     * @return
     */
    @Override
    public Result advancedOperationsPort(AdvancedOperationsDto paramDto) {
        if (paramDto.getAnalysisDataName() == null || paramDto.getAnalysisDataName().equals("")) {
            //请输入变换名称
            return Result.fail(YamlMessageSource.getMessageStatic("0000002C"));
        }
        if (paramDto.getTaskId() == null || paramDto.getTaskId().equals("")) {
            //缺少参数
            return Result.fail(YamlMessageSource.getMessageStatic("00000007"));
        }
        // 根据任务id查询任务数据
        TMeasureTask task = tMeasureTaskService.getById(paramDto.getTaskId());
        if (task == null) {
            //没有找到该任务
            return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
        }
        // 根据任务中的方法key查询方法并且提取方法参数
        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        // 提取方法参数
        SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
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
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
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
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getSpectrumTransformation(), request, Map.class).getBody();
        body.put("x_data", xList);
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request1, Map.class).getBody();
        // 直接拿寻峰数据进行一次寻谷
        Map<String, Object> resultMap2 = restTemplate.postForEntity(pythonPathConstant.getValleySearching(), request1, Map.class).getBody();
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
     * 平滑度接口
     * @param paramDto
     * @return
     */
    @Override
    public Result   smoothnessPort(SmoothnessDto paramDto) {
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
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据测量数据的id查询出测量数据
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getSmoothed(), request, Map.class).getBody();
        Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
        List<Double> smoothedY = (List<Double>) data.get("smoothed_y");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", smoothedY);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request1, Map.class).getBody();
        Map<String, Object> resultMap2 = restTemplate.postForEntity(pythonPathConstant.getValleySearching(), request1, Map.class).getBody();
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
     * 微分接口
     * @param paramDto
     * @return
     */
    @Override
    public Result differentialPort(DifferentialDto paramDto) {
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
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.
                list(new QueryWrapper<TMeasureAnalysisData>().
                        eq("analysis_name", paramDto.getAnalysisDataName()).
                        eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据测量数据的id查询出测量数据
        TMeasureAnalysisData tMeasureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getDifferentiated(), request, Map.class).getBody();
        Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
        List<Double> differentiatedY = (List<Double>) data.get("differentiated_y");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", differentiatedY);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request1, Map.class).getBody();
        Map<String, Object> resultMap2 = restTemplate.postForEntity(pythonPathConstant.getValleySearching(), request1, Map.class).getBody();
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
     * 光度模式转换
     * @param paramDto
     * @return
     */
    @Override
    public Result luminanceModeConversionPort(LuminosityConversionDto paramDto) {
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
        List<TMeasureAnalysisData> analysisData = tMeasureAnalysisDataService.list(new QueryWrapper<TMeasureAnalysisData>().eq("analysis_name", paramDto.getAnalysisDataName()).eq("task_id", paramDto.getTaskId()));
        if (analysisData.size() > 0) {
            //变换名称重复
            return Result.fail(YamlMessageSource.getMessageStatic("0000002D"));
        }
        // 根据方法id查询方法表的数据
        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());
        // 获取方法表中的方法参数信息
        String measurementParamInfo = method.getMeasurementParamInfo();
        SpectrumParameter spectrumParameter = JSON.parseObject(measurementParamInfo, SpectrumParameter.class);
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
        TMeasureAnalysisData measureData = tMeasureAnalysisDataService.getById(paramDto.getMeasureDataId());
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
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(pythonPathConstant.getSpectrumTransformation(), request, Map.class).getBody();
        List<Double> y_data = (List<Double>) resultMap.get("result_y");
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        Map<String, Object> body1 = new HashMap<>();
        body1.put("x_data", xList);
        body1.put("y_data", y_data);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body1.put("height", height);
        body1.put("distance", confVO.getPeakInterval());
        HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(body1, headers);
        Map<String, Object> resultMap1 = restTemplate.postForEntity(pythonPathConstant.getPeakSearching(), request1, Map.class).getBody();
        Map<String, Object> resultMap2 = restTemplate.postForEntity(pythonPathConstant.getValleySearching(), request1, Map.class).getBody();
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
     * 图片上传
     * @param upload
     * @return
     */
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
            QueryWrapper<TMeasureAnalysisImage> analysisId = new QueryWrapper<TMeasureAnalysisImage>().eq("analysis_id", upload.getId());
            analysisId.last("limit 1");
            TMeasureAnalysisImage byId = tMeasureAnalysisImageService.getOne(analysisId);
            if (byId == null) {
                //图片不存在
                return Result.fail(YamlMessageSource.getMessageStatic("00000040"));
            }
            byId.setImageType(fileType);
            byId.setImageBase64(bytes);
            // 将当前日期时间存储
            byId.setCreateTime(LocalDateTime.now());
            boolean update = tMeasureAnalysisImageService.updateById(byId);
            return Result.ok(update);
        } else {
            QueryWrapper<TMeasureTaskAttachment> eq = new QueryWrapper<TMeasureTaskAttachment>()
                    .eq("task_id", upload.getId());
            eq.last("limit 1");
            // 根据任务id查询任务附属表的内容
            TMeasureTaskAttachment byId = tMeasureTaskAttachmentService.getOne(eq);
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
            boolean update = tMeasureTaskAttachmentService.updateById(byId);
            return Result.ok(update);
        }
    }

    /**
     * 获取任务图片接口
     * @param id
     * @param type
     * @return
     */
    @Override
    public Result queryTheTaskImage(String id, String type) {
        // 根据type类型查询测量数据或分析数据的图片
        if (type.equals("0")) {
            TMeasureData byId = tMeasureDataService.getById(id);
            if (byId.getDataImage() == null) {
                //图片不存在
                return Result.fail(YamlMessageSource.getMessageStatic("00000040"));
            }
            String encoded = Base64.encodeBase64String(byId.getDataImage());
            return Result.ok(encoded);
        } else if (type.equals("1")) {
            TMeasureAnalysisData byId = tMeasureAnalysisDataService.getById(id);
            // 根据分析数据的id查询分析数据图片
            TMeasureAnalysisImage analysisId = tMeasureAnalysisImageService.getOne(new QueryWrapper<TMeasureAnalysisImage>().eq("analysis_id", id));
            String encoded = Base64.encodeBase64String(analysisId.getImageBase64());
            return Result.ok(encoded);
        } else {
            //type类型错误
            return Result.fail(YamlMessageSource.getMessageStatic("00000042"));
        }
    }


    /**
     * 查询预处理操作接口
     * @param methodId
     * @return
     */
    @Override
    public Result queryPreprocessingOperation(String methodId) {
        // 根据方法id查询方法数据
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        if (method == null) {
            //没有找到该方法
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        if (method.getPreprocessKey() == null) {
            //预处理参数不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00304006"));
        } else {
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
    public Result obtainTheBaseline(String taskId, String methodId, String deviceSerialNumber) {
        if (methodId == null || methodId.equals("")) {
            //方法编号不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00304007"));
        }
        if (deviceSerialNumber == null || deviceSerialNumber.equals("")) {
            //设备编号不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00304008"));
        }
        Result result = setUpParametersPort(methodId, deviceSerialNumber);
        if (result.getCode() != 200) {
            //设置参数失败
            return Result.ok(YamlMessageSource.getMessageStatic("00304009"));
        }
        // 根据序列号获取设备的ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        Command command = buildCommand(SetParamEnum.BaseLine, new Object[]{});
        R<List<ChangeParam>> listR = devMessageUtil.spectrumScanGetBaseline(command, ip);
        if (listR.getSuccess() != true) {
            return Result.ok(listR.getMessage());
        }
        TMeasureTask task = null;
        if (taskId != null && !taskId.equals("")) {
            task = tMeasureTaskService.getById(taskId);
        } else {
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
            if (!save) {
                //保存失败
                return Result.fail(YamlMessageSource.getMessageStatic("00300008"));
            }
        }
        // 将listR中的数据保存到task的校零参数中
        task.setZeroParam(JSON.toJSONString(listR.getData()));
        boolean update = tMeasureTaskService.updateById(task);
        return Result.ok(update);
    }

    /**
     * 导出word接口
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

    /**
     * 测试对比接口
     * @param taskId
     * @param spectrumParameter
     * @return
     */
    @Override
    public Result testCompare(String taskId, SpectrumParameter spectrumParameter) {
        // 根据任务id查询出任务,再根据任务中的方法id找到方法并且再提取出方法参数
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        TMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());

        SpectrumParameter spectrumParameter1 = JSON.parseObject(byId.getMeasurementParamInfo(), SpectrumParameter.class);

        String dbJson = JSON.toJSONString(spectrumParameter1, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(spectrumParameter, SerializerFeature.SortField);

        return Result.ok(dbJson.equals(frontJson));
    }

    /**
     * 多格式文件下载接口（支持word/excel/txt）
     * @param taskId     任务ID
     * @param allDisplay 是否显示全部数据
     * @param fileType   导出格式（word/txt/xlsx）
     * @return 生成的文件对象
     */
    public R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds) {
        // log.info("===== 导出流程开始 =====");
        // log.info("开始导出，taskId: {}, fileType: {}, 需要筛选的analysisId数量: {}",
        //         taskId, fileType, analysisIds != null ? analysisIds.size() : 0);

        // 1. 基础参数校验阶段（保持不变）
        // log.info("【步骤1/5】开始基础参数校验");
        if (!StringUtils.hasText(taskId)) {
            // log.error("【步骤1/5】校验失败：缺少任务id");
            //缺少任务id
            return R.fail(YamlMessageSource.getMessageStatic("00000019"));
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
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
        List<SampleListVo> allSamples = new ArrayList<>();

        // 处理sampleListPort数据
        Result<List<SampleListVo>> sampleListResult = sampleListPort(taskId, null, null);
        if (sampleListResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode()) && sampleListResult.getData() != null) {
            // log.info("【步骤2/5】sampleListPort查询成功，返回样品数量：{}", sampleListResult.getData().size());
            allSamples.addAll(sampleListResult.getData());
        } else {
            // log.warn("【步骤2/5】sampleListPort查询异常：{}", sampleListResult.getMessage());
        }

        // 处理changeTheListPort数据（与sampleListPort逻辑相同）
        Result<List<SampleListVo>> changeListResult = changeTheListPort(taskId, null, null);
        if (changeListResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode()) && changeListResult.getData() != null) {
            // log.info("【步骤2/5】changeTheListPort查询成功，返回样品数量：{}", changeListResult.getData().size());
            allSamples.addAll(changeListResult.getData());
        } else {
            // log.warn("【步骤2/5】changeTheListPort查询异常：{}", changeListResult.getMessage());
        }

        // 2.2 根据analysisIds筛选数据
        List<SampleListVo> filteredSamples = new ArrayList<>();
        if (CollectionUtils.isEmpty(analysisIds)) {
            // 无筛选条件，使用全部数据
            filteredSamples = allSamples;
            // log.info("【步骤2/5】未指定筛选条件，使用全部样品数据，总数量：{}", filteredSamples.size());
        } else {
            // 有筛选条件，匹配analysisId
            for (SampleListVo sample : allSamples) {
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
                SampleListVo vo = filteredSamples.get(i);
                // log.info("【步骤2/5】样品{}: title={}, analysisId={}, data是否为空={}",
                //         i+1, vo.getTitle(), vo.getAnalysisId(),
                //         vo.getData() == null || vo.getData().isEmpty());

                // 打印第一个样品的前3条数据，确认x和y的值
                if (i == 0 && vo.getOriginalData() != null && !vo.getOriginalData().isEmpty()) {
                    int dataLogCount = Math.min(3, vo.getOriginalData().size());
                    for (int j = 0; j < dataLogCount; j++) {
                        Map<String, Object> data = vo.getOriginalData().get(j);
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
            for (SampleListVo vo : filteredSamples) {
                String title = vo.getTitle();
                List<Map<String, Object>> dataList = vo.getOriginalData();

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
        List<HeadVO> heads = new ArrayList<>();
        heads.add(new HeadVO("波长nm", "wavelength")); // 波长列标题
        sampleYMap.keySet().forEach(title -> heads.add(new HeadVO(title, title))); // 样品列标题

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
                    TMeasureMethod method = null;
                    if (StringUtils.hasText(methodKey)) {
                        method = tMeasureMethodService.getById(methodKey);
                        // log.info("【步骤5/5】查询到测量方法：{}", method != null ? method.getMethodName() : "未找到");
                    }
                    if (method == null) {
                        // log.warn("【步骤5/5】测量方法不存在，使用默认值");
                        method = new TMeasureMethod();
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
            //"文件生成失败：" + e.getMessage()
            return R.fail(YamlMessageSource.getMessageStatic("00302006",e.getMessage()));
        } finally {
            // log.info("===== 导出流程结束 =====");
        }
    }

    /**
     * (实验测试)方法参数修改接口
     * @param paramDto
     * @return
     */
    @Override
    public Result updateMethodParameter(SpectralMethodParameterDto paramDto) {
        if (paramDto.getLuminosity().equals("3") || paramDto.getLuminosity().equals("4")) {
            paramDto.setLadderCorrection(false);
        }
        boolean update1 = Double.parseDouble(paramDto.getWaveLengthInterval()) < 1;
        if (paramDto.getScanSpeed().equals("3") && update1) {
            return Result.fail("极快扫描速度时,扫描间隔不能小于1秒");
        }
        double start = Double.parseDouble(paramDto.getStartWaveLength());
        double end = Double.parseDouble(paramDto.getEndWaveLength());
        // 校验方法参数中起始波长不能小于终止波长,小于则友好提示
        if (start <= end) {
            //起始波长不能小于等于停止波长
            return Result.fail(YamlMessageSource.getMessageStatic("00104015"));
        }
        // 校验幻灯波长在330~390之间
        double changeWavelength = Double.parseDouble(paramDto.getChangeLampWaveLength());
        if (changeWavelength < 330.0 || changeWavelength > 390.0) {
            return Result.fail("幻灯波长在330~390之间");
        }
        if (paramDto.getScanSpeed().equals("3") && (start > 1100 || start < 380)) {
            return Result.fail("极快扫描速度时,波长区间应在1100.0~380.0");
        }
        // 下发参数
        SetParam setParam = new SetParam();
        setParam.setPhotoMode(Integer.valueOf(paramDto.getLuminosity()));
        setParam.setSetWl(Double.valueOf(paramDto.getStartWaveLength()));
        boolean b = setInstrumentParameters(paramDto.getDeviceSerialNumber(), setParam);
        if (b) {
            return Result.fail("设置参数失败");
        }

        // 判断参数是否发生变化，如果没有变化，直接返回，不再执行后续操作
        if (paramDto.getMethodId() != null && (paramDto.getMethodId2() == null || paramDto.getMethodId().equals(paramDto.getMethodId2()))) {
            TMeasureMethod currentMethod = tMeasureMethodService.getById(paramDto.getMethodId());
            if (currentMethod != null) {
                // 1. 获取数据库中的旧参数
                SpectrumParameter oldParam = JSON.parseObject(currentMethod.getMeasurementParamInfo(), SpectrumParameter.class);

                // 2. 获取传入的新参数
                SpectrumParameter newParam = new SpectrumParameter();
                BeanUtils.copyProperties(paramDto, newParam);

                // 3. 比较参数
                if (compareParam(oldParam, newParam)) {
                    // 参数未变，直接返回成功结果，跳过后续所有数据库更新/新建操作
                    Map<String, Object> map = new HashMap<>();
                    map.put("method", currentMethod);
                    return Result.ok(map);
                }
            }
        }

        Map<String, Object> map = new HashMap<>();
        if (paramDto.getMethodId2() != null) {
            TMeasureMethod method2 = tMeasureMethodService.getById(paramDto.getMethodId2());
            if (method2 == null) {
                //没有找到该方法
                return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
            }
            // 将方法的参数取出来
            SpectrumParameter parameter2 = JSON.parseObject(method2.getMeasurementParamInfo(), SpectrumParameter.class);
            SpectrumParameter parameter = new SpectrumParameter();
            BeanUtils.copyProperties(paramDto, parameter);
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
            boolean update = tMeasureMethodService.updateById(method2);
            if (!update) {
                //应用失败
                return Result.fail(YamlMessageSource.getMessageStatic("00100002"));
            }
            map.put("method", method2);
            return Result.ok(map);
        }

        TMeasureMethod method = tMeasureMethodService.getById(paramDto.getMethodId());

        SpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);

        SpectrumParameter newParam = new SpectrumParameter();
        // 将方法参数拷贝出来
        BeanUtils.copyProperties(paramDto, newParam);

        // 3. 参数比较
        boolean sameParam = compareParam(parameter, newParam);

        // 2. 查询历史方法
        if (method == null) {
            TMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail(YamlMessageSource.getMessageStatic("0010000A"));
            }
            map.put("method", newMethod);
            return Result.ok(map);
        }

        if (sameParam) {
            if (method.getId().equals(paramDto.getMethodId())) {
                //请勿重复使用
                return Result.fail(YamlMessageSource.getMessageStatic("00105016"));
            } else {
                map.put("method", method);
                return Result.ok(map);
            }
        } else {
            TMeasureMethod newMethod = createNewMethod(paramDto);
            if (newMethod == null) {
                //新建方法失败
                return Result.fail(YamlMessageSource.getMessageStatic("0010000A"));
            }
            map.put("method", newMethod);
            return Result.ok(map);
        }
    }

    /**
     * (实验测试)查询表格头
     * @param taskId
     * @param methodId
     * @param sampleIds
     * @return
     */
    @Override
    public Result<List<HeadVO>> tableHead(String taskId, String methodId, List<String> sampleIds) {
        // 根据方法id查找方法
        TMeasureMethod method = tMeasureMethodService.getById(methodId);
        SpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
        List<HeadVO> ans = new ArrayList<>();

        // 1. 编号
        ans.add(new HeadVO("编号", "id", "system", "编号"));

        // 2. 波长
        ans.add(new HeadVO("波长(nm)", "waveLength", "measure", "波长(nm)"));

        // 如果样品长度为1则是单样品表头,如果样品长度超过1则是多样品表头
        if (sampleIds.size() == 1){
            addLuminosityMode(ans,parameter.getLuminosity());
            ans.add(new HeadVO("峰谷/标记点", "peakValleyGaugePoint", "measure", "峰谷"));
            ans.add(new HeadVO("备注", "Remark", "analysis", "备注"));
            return Result.ok(ans);
        }else{
            // 根据样品id查询多个样品的样品信息
            QueryWrapper<TMeasureSample> qw = new QueryWrapper<>();
            qw.in("sample_key", sampleIds);
            List<TMeasureSample> samples = tMeasureSampleService.list(qw);
            if (samples == null || samples.isEmpty()){
                //样品信息不存在
                return Result.fail(YamlMessageSource.getMessageStatic("00000049"));
            }
            for (TMeasureSample sample : samples) {
                ans.add(new HeadVO(sample.getSampleName(), "sampleNumber", "name", "样品名称"));
                ans.add(new HeadVO("峰/谷", "peakValley", "system", "峰谷"));
            }
            return Result.ok(ans);
        }
    }

    /**
     * (实验测试)查询表格数据
     * @param taskId
     * @param sampleIds
     * @return
     */
    @Override
    public List<Map<String, Object>> queryTableData(String taskId, List<String> sampleIds) {
        // 校验参数
        if (StringUtils.isEmpty(taskId) || sampleIds == null || sampleIds.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> arrayList = new ArrayList<>();

        // 判断样品长度是否为1
        if (sampleIds.size() == 1) {
            // 单样品表数据
            QueryWrapper<TMeasureAnalysisData> wrapper = new QueryWrapper<>();
            wrapper.eq("task_id", taskId);
            wrapper.eq("analysis_number",1);
            wrapper.eq("sample_key", sampleIds.get(0));
            wrapper.last("limit 1");
            TMeasureAnalysisData measureAnalysisData = tMeasureAnalysisDataService.getOne(wrapper);
            if (measureAnalysisData == null) {
                return null;
            }
            int id =1;
            List<PeakValleyMarking> peaks = JSON.parseArray(measureAnalysisData.getPeak(), PeakValleyMarking.class);
            List<PeakValleyMarking> valleys = JSON.parseArray(measureAnalysisData.getGrain(), PeakValleyMarking.class);
            List<PeakValleyMarking> gaugePoints = JSON.parseArray(measureAnalysisData.getGaugePoint(), PeakValleyMarking.class);
            if (peaks != null){
                for (PeakValleyMarking peak : peaks) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(SampleTableEnum.ID.getIdentity(),id++);
                    map.put(SampleTableEnum.WAVELENGTH.getIdentity(), peak.getX());
                    map.put(SampleTableEnum.LUMINOSITY.getIdentity(), peak.getY());
                    map.put(SampleTableEnum.PEAKVALLEYGAUGEPOINT.getIdentity(), "峰");
                    map.put(SampleTableEnum.REMARK.getIdentity(), peak.getRemark());
                    arrayList.add(map);
                }
            }
            if (valleys != null){
                for (PeakValleyMarking valley : valleys) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(SampleTableEnum.ID.getIdentity(),id++);
                    map.put(SampleTableEnum.WAVELENGTH.getIdentity(), valley.getX());
                    map.put(SampleTableEnum.LUMINOSITY.getIdentity(), valley.getY());
                    map.put(SampleTableEnum.PEAKVALLEYGAUGEPOINT.getIdentity(), "谷");
                    map.put(SampleTableEnum.REMARK.getIdentity(), valley.getRemark());
                    arrayList.add(map);
                }
            }
            if (gaugePoints != null){
                for (PeakValleyMarking gaugePoint : gaugePoints) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(SampleTableEnum.ID.getIdentity(),id++);
                    map.put(SampleTableEnum.WAVELENGTH.getIdentity(), gaugePoint.getX());
                    map.put(SampleTableEnum.LUMINOSITY.getIdentity(), gaugePoint.getY());
                    map.put(SampleTableEnum.PEAKVALLEYGAUGEPOINT.getIdentity(), "标记点");
                    map.put(SampleTableEnum.REMARK.getIdentity(), gaugePoint.getRemark());
                    arrayList.add(map);
                }
            }

            return arrayList;
        }else{
            // 多样品表数据
            // 根据任务id和样品id查询分析数据
            QueryWrapper<TMeasureAnalysisData> eq = new QueryWrapper<>();
            eq.eq("task_id", taskId);
            eq.eq("analysis_number",1);
            eq.in("sample_key", sampleIds);

            List<TMeasureAnalysisData> analysisDataList = tMeasureAnalysisDataService.list(eq);
            if (analysisDataList == null || analysisDataList.isEmpty()) {
                return null;
            }

            List<Integer> list = new ArrayList<>();
            List<String> result = JSON.parseArray(analysisDataList.get(0).getAnalysisResult(), String.class);
            for (String s : result) {
                String[] split = s.split(",");
                list.add(Integer.parseInt(split[0]));
            }

            for (Integer i : list) {
                Map<String, Object> map = new HashMap<>();
                Map<String,Object> resultMap = new HashMap<>();
                resultMap.put(SampleTableEnum.ID.getIdentity(),i);
                for (TMeasureAnalysisData TMeasureAnalysisData : analysisDataList) {
                    List<String> datas = JSON.parseArray(TMeasureAnalysisData.getAnalysisResult(), String.class);
                    List<PeakValleyMarking> peaks = JSON.parseArray(TMeasureAnalysisData.getPeak(), PeakValleyMarking.class);
                    List<PeakValleyMarking> valleys = JSON.parseArray(TMeasureAnalysisData.getGrain(), PeakValleyMarking.class);
                    for (String data : datas) {
                        String[] split = data.split(",");
                        if (Integer.parseInt(split[0])== i) {
                            if (resultMap.get("sampleNumber") == null){
                                List<String> y = new ArrayList<>();
                                y.add(split[1]);
                                resultMap.put("sampleNumber", y);
                            }else{
                                List<String> y = (List<String>) map.get("sampleNumber");
                                y.add(split[1]);
                                map.put("sampleNumber", y);
                            }
                            boolean statePeak = false;
                            boolean stateValley = false;
                            if (peaks != null){
                                for (PeakValleyMarking peak : peaks) {
                                    if (peak.getX().equals(split[0])){
                                        if (resultMap.get("peakValley")==null){
                                            List<String> peakList = new ArrayList<>();
                                            peakList.add("峰");
                                        }else{
                                            List<String> peakList = (List<String>) map.get("peakValley");
                                            peakList.add("峰");
                                        }
                                        statePeak = true;
                                    }
                                }
                            }
                            if (valleys != null){
                                for (PeakValleyMarking valley : valleys) {
                                    if (valley.getX().equals(split[0])){
                                        if (resultMap.get("peakValley")==null){
                                            List<String> peakList = new ArrayList<>();
                                            peakList.add("谷");
                                        }else{
                                            List<String> peakList = (List<String>) map.get("peakValley");
                                            peakList.add("谷");
                                        }
                                        stateValley = true;
                                    }
                                }
                            }
                            /// 校验是否有峰/谷进行添加,如果没有添加则进行补充为null
                            if (!statePeak&&!stateValley){
                                if (resultMap.get("peakValley")==null){
                                    List<String> peakList = new ArrayList<>();
                                    peakList.add(null);
                                }else{
                                    List<String> peakList = (List<String>) map.get("peakValley");
                                    peakList.add(null);
                                }
                            }
                            break;
                        }
                    }
                }
                map.put(String.valueOf(i),resultMap);
                arrayList.add(map);
            }
            return arrayList;


        }











//        // 根据任务id和样品id查询分析数据
//        QueryWrapper<TMeasureAnalysisData> eq = new QueryWrapper<>();
//        eq.eq("task_id", taskId);
//        eq.eq("sample_key", sampleId);
//        eq.orderByDesc("analysis_number");
//        eq.last("LIMIT 1");
//        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(eq);
//        if (analysisData == null) {
//            return null;
//        }
//        // 解析分析数据
//        List<String> result = JSON.parseArray(analysisData.getAnalysisResult(), String.class);
//        // 解析峰谷数据
//        List<PeakValleyMarking> peaks = JSON.parseArray(analysisData.getPeak(), PeakValleyMarking.class);
//        List<PeakValleyMarking> valleys = JSON.parseArray(analysisData.getGrain(), PeakValleyMarking.class);
//        List<Map<String, Object>> arrayList = new ArrayList<>();
//        // 根据长度遍历
//        for (int i = 0; i < result.size(); i++) {
//            // 根据,进行分割数据
//            String[] split = result.get(i).split(",");
//            Map<String, Object> map = new HashMap<>();
//            map.put(SampleTableEnum.ID.getIdentity(), i + 1);
//            for (PeakValleyMarking peak : peaks) {
//                if (peak.getX().equals(split[0])){
//                    map.put(SampleTableEnum.PEAKVALLEY.getIdentity(), "峰");
//                }
//            }
//            for (PeakValleyMarking grade : valleys) {
//                if (grade.getX().equals(split[0])){
//                    map.put(SampleTableEnum.PEAKVALLEY.getIdentity(), "谷");
//                }
//            }
//            map.put(SampleTableEnum.WAVELENGTH.getIdentity(), split[0]);
//            map.put(SampleTableEnum.LUMINOSITY.getIdentity(), split[1]);
//            map.put(SampleTableEnum.REMARK.getIdentity(), null);
//            arrayList.add(map);
//        }
//        return arrayList;
    }

    /**
     * 获取样品的详细数据
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureAnalysisData measureData, Integer digit) {
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            if (digit != null) {
                BigDecimal value = new BigDecimal(split[1]);
                value = value.setScale(digit, RoundingMode.HALF_UP);
                map.put("x", split[0]);
                map.put("y", value.stripTrailingZeros().toPlainString());
            } else {
                map.put("x", split[0]);
                map.put("y", split[1]);
            }
            if (gauges != null && peaks!= null) {
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
            if (peaks != null && peaks.size() != 0) {
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
            if (grades != null && peaks.size() != 0) {
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
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureData measureData, Integer digit) {
        List<String> list = JSON.parseArray(measureData.getOriginalCalculationData(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            if (digit != null) {
                BigDecimal value = new BigDecimal(split[1]);
                value = value.setScale(digit, RoundingMode.HALF_UP);
                map.put("x", split[0]);
                map.put("y", value.stripTrailingZeros().toPlainString());
            } else {
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
     * 生成Word文件
     *
     * @param task
     * @param samples
     * @param response
     * @throws Exception
     */
    private void generateWord(TMeasureTask task, List<TMeasureSample> samples, List<TMeasureData> measureDataList, HttpServletResponse response) throws Exception {
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
        addTextRun(taskInfo, "任务名称: " + task.getTaskName(), 12, false, true);
        addTextRun(taskInfo, "测量类型: " + task.getMeasureType(), 12, false, true);
        addTextRun(taskInfo, "创建时间: " + task.getCreateTime().toString(), 12, false, true);
        addTextRun(taskInfo, "方法参数: ", 14, true, true);
        addTextRun(taskInfo, "方法名称: " + method.getMethodName(), 12, false, true);
        addTextRun(taskInfo, "是否共享: " + (method.getIsSharingMethod() == null || method.getIsSharingMethod() == "0" ? "否" : "是"), 12, false, true);
        addTextRun(taskInfo, "创建时间: " + method.getMethodCreateTime().format(formatter), 12, false, true);
        addTextRun(taskInfo, "创建人: " + method.getMethodCreator(), 12, false, true);
        addTextRun(taskInfo, "光度模式: " + (spectrumParameter.getLuminosity() == "1" ? "Abs" : spectrumParameter.getLuminosity() == "2" ? "T%" : spectrumParameter.getLuminosity() == "3" ? "Es" : spectrumParameter.getLuminosity() == "4" ? "Er" : ""), 12, false, true);
        addTextRun(taskInfo, "起始波长: " + spectrumParameter.getStartWaveLength(), 12, false, true);
        addTextRun(taskInfo, "结束波长: " + spectrumParameter.getEndWaveLength(), 12, false, true);
        addTextRun(taskInfo, "波长间隔: " + spectrumParameter.getWaveLengthInterval(), 12, false, true);
        addTextRun(taskInfo, "重复模式: " + (spectrumParameter.getRepetitionPattern() == "0" ? "不重复" : "单池重复"), 12, false, true);
        addTextRun(taskInfo, "重复次数: " + spectrumParameter.getRepetitionTime(), 12, false, true);
        addTextRun(taskInfo, "时间间隔: " + spectrumParameter.getTimeInterval(), 12, false, true);
        addTextRun(taskInfo, "阶梯矫正: " + (String.valueOf(spectrumParameter.getLadderCorrection()) == "true" ? "启用" : "不启用"), 12, false, true);
        addTextRun(taskInfo, "扫描速度: " + (spectrumParameter.getScanSpeed() == "0" ? "慢速" : spectrumParameter.getScanSpeed() == "1" ? "中速" : spectrumParameter.getScanSpeed() == "2" ? "快速" : "极快"), 12, false, true);

        addTextRun(taskInfo, "样品信息: ", 14, true, true);
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

    /**
     * 添加文本
     * @param paragraph
     * @param text
     * @param fontSize
     * @param bold
     * @param isLineFeed
     */
    private void addTextRun(XWPFParagraph paragraph, String text, int fontSize, boolean bold, boolean isLineFeed) {
        XWPFRun run = paragraph.createRun();
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("宋体");
        run.setText(text);
        run.addBreak();
        if (isLineFeed) {
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

    /**
     * 保留原有的downloadWord方法，并适配新参数
     * @param taskId
     * @param allDisplay
     * @param analysisIds
     * @return
     */
    public R<File> downloadWord(String taskId, Boolean allDisplay, List<String> analysisIds) {
        return downloadFile(taskId, allDisplay, "word", analysisIds);
    }

    /**
     * 生成Word文件（修复原逻辑的表格列错位问题）
     */
    private void generateWordFile(File file, TMeasureTask task, TMeasureMethod method,
                                  List<HeadVO> heads, List<Map<String, Object>> data, int vosSize) throws Exception {
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
            HeadVO head = heads.get(i);
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
                HeadVO head = heads.get(i);
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
    private void generateTxtFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
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
                for (HeadVO head : heads) {
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
    private void generateExcelFile(File file, List<HeadVO> heads, List<Map<String, Object>> data) throws Exception {
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
            HeadVO head = heads.get(i);
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
                HeadVO head = heads.get(colIdx);
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

    /**
     * 添加光度模式
     * @param ans
     * @param mode
     */
    public void addLuminosityMode(List<HeadVO> ans, String mode) {
        switch (mode) {
            case "1":
                ans.add(new HeadVO("Abs", "luminosity", "measure", "Abs"));
                break;
            case "2":
                ans.add(new HeadVO("T%", "luminosity", "measure", "T%"));
                break;
            case "3":
                ans.add(new HeadVO("Es", "luminosity", "measure", "Es"));
                break;
            case "4":
                ans.add(new HeadVO("Er", "luminosity", "measure", "Er"));
                break;
            default:
                // 可以在这里处理无效的模式
                System.out.println("无效的光度模式: " + mode);
        }
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

    /**
     * 比较参数
     *
     * @param method
     * @param newParam
     * @return
     */
    private boolean compareParam(SpectrumParameter method, SpectrumParameter newParam) {
        String dbJson = JSON.toJSONString(method, SerializerFeature.SortField);
        String frontJson = JSON.toJSONString(newParam, SerializerFeature.SortField);

        return dbJson.equals(frontJson);
    }

    /**
     * 创建新的方法
     */
    private TMeasureMethod createNewMethod(SpectralMethodParameterDto paramDto) {
        TMeasureMethod fMeasureMethod = new TMeasureMethod();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String collect = new Random().ints(8, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());

        fMeasureMethod.setMethodName(paramDto.getMeasureType() + collect);

        FSpectrumParameter parameter = new FSpectrumParameter();
        // 将前端参数复制给数据库参数
        BeanUtils.copyProperties(paramDto, parameter);
        fMeasureMethod.setMeasurementParamInfo(JSON.toJSONString(parameter));
        fMeasureMethod.setMethodCreateTime(LocalDateTime.now());
        fMeasureMethod.setMethodUseTime(LocalDateTime.now());
        fMeasureMethod.setMethodCreator(paramDto.getUpdateBy());
        fMeasureMethod.setMeasurementType(paramDto.getMeasureType());
        boolean save = tMeasureMethodService.save(fMeasureMethod);
        if (save) {
            return fMeasureMethod;
        } else {
            return null;
        }
    }


}
