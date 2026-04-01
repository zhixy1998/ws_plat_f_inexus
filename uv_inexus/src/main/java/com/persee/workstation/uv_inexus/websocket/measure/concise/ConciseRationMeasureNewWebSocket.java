/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.measure.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationUtilService;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationTypeEnums;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.*;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketOperationEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketResponseVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import com.persee.workstation.uv_inexus.websocket.measure.param.PhotoAndRationMetricWebsocketParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ConciseRationMeasureNewWebSocket {
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private static volatile Boolean empty = false;
    private static volatile Boolean lastIsEmpty = false;
    private static volatile Double minuend = 0.0;
    //存放空白样数据（计算后的值）
    private static volatile List<FRationSaveCountDataInfo> emptyDataList = new ArrayList<>();
    //存放空白样计算出的Abs的值
    private static volatile List<FRationCountDataInfo> emptyList = new ArrayList<>();

    private static LocalDateTime repeatMiddleTime;

    @Autowired
    private FMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    private FMeasureMethodService tMeasureMethodService;

    @Autowired
    private FMeasureDataService tMeasureDataService;

    @Autowired
    RationConciseService rationService;

    @Autowired
    FMeasureSampleService tMeasureSampleService;

    @Autowired
    FMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    FMeasureTaskAttachmentService attachmentService;

    @Autowired
    SamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    WebSocketSendUtil webSocketSendUtil;

    @Autowired
    private DevOperationUtilService operationUtilService;

    @Value("1")
    String right;

    @Value("0")
    String error;

    @Value("0")
    String noSave;

    private static volatile int status = 0;

    private static volatile int digit = 4;

    private static volatile boolean isAvg = false;
    private static volatile boolean isSd = false;
    private static volatile boolean isRsd = false;
    private static volatile boolean reCount = false;

    private String deviceSerialNumber;


    public void startConciseRationMeasure(PhotoAndRationMetricWebsocketParam param) {


        initParams();

        // 解析参数
        String taskId = param.getTaskId();
        String sampleKind = param.getSampleKind();
        String deviceSerialNumber = param.getDeviceSerialNumber();
        String dig = param.getDigit();
        String oneSampleId = param.getSampleId();
        int di = digit;
        if (StringUtils.hasText(dig)) {
            di = Integer.parseInt(dig);
        }
        isAvg = param.getIsAvg() != null && param.getIsAvg();
        isSd = param.getIsSd() != null && param.getIsSd();
        isRsd = param.getIsRsd() != null && param.getIsRsd();

        infoService.updateDeviceCurrentStatus(deviceSerialNumber, WebSocketOperationEnum.ConciseRationMeasure.getOperation(), taskId, Constants.DEVICE_STATUS_MEASURE);

        if (taskId == null) {
            sendError(deviceSerialNumber, "请创建任务后再测量");
            return;
        }
        if (!StringUtils.hasText(sampleKind)) {
            sendError(deviceSerialNumber, "请选择样品类型");
            return;
        }
        
        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null) {
            sendError(deviceSerialNumber, "任务不存在");
            return;
        }

//        R<Boolean> createR = rationService.createSample(taskId, sampleKind, updateBy, deviceSerialNumber);
//        if (!createR.getSuccess()) {
//             
//            sendAndClose(session, createR.getMessage());
//            return;
//        }
//        task = tMeasureTaskService.getById(task.getId());

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        FMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());
        FRationParameter parameter = JSON.parseObject(byId.getMeasurementParamInfo(), FRationParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendError(deviceSerialNumber, "缺少波长， 无法测量");
            return;
        }

        if (di < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
        } else {
            digit = di;
        }
        
        try {

            R<FMeasureTaskAttachment> attachmentR = attachmentService.queryByTaskId(taskId);
            if (attachmentR.getSuccess()) {
                FMeasureTaskAttachment attachment = attachmentR.getData();
                if (attachment != null && StringUtils.hasText(attachment.getDataParam())) {
                    QueryDataDTO queryDataDTO = JSON.parseObject(attachment.getDataParam(), QueryDataDTO.class);
                    if (queryDataDTO.getDigit() != digit) {
                        reCount = true;
                    }
                }
            }

            // 检查任务中是否存在空白样本，如果存在则设置empty为true
            empty = taskSpecimen.stream()
                    .anyMatch(specimen ->
                            specimen.getSampleType() != null &&
                                    specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity()) &&
                                    specimen.getDeleteType() != null &&
                                    specimen.getDeleteType().equals("0")
                    );

            if (StringUtils.hasText(oneSampleId)) {
                FMeasureSample dealSample = tMeasureSampleService.getById(oneSampleId);
                if (dealSample == null) {
                    sendError(deviceSerialNumber, "样品不存在");
                    return;
                }
                sampleKind = dealSample.getSampleKind();

                // 设置参数
//                R<Boolean> setParams = setPreMeasureParams(deviceSerialNumber, parameter);
//                if (!setParams.getSuccess()) {
//                    sendError(deviceSerialNumber, setParams.getMessage());
//                    return;
//                }

                boolean repeat = parameter.getRepeatMode().equals("单池重复");
                dealSample.setSampleLocation(null);
                R<FRationDataVO> r;
                if (repeat) {
                    //重复测量，单独处理！
                    r = singleSampleRepeatMeasure(dealSample, parameter, task, deviceSerialNumber);

                } else {
                    r = singleSampleSimpleMeasure(dealSample, parameter, task, deviceSerialNumber);
                }

                if (!r.getSuccess()) {
                    sendError(deviceSerialNumber, r.getMessage());
                    return;
                }
                FRationDataVO data = r.getData();

                //保存数据
                R<FRationSaveCountDataInfo> saved = rationService.saveSampleMeasureData(data);
                if (!saved.getSuccess()) {
                    log.info("数据保存异常");
                    sendError(deviceSerialNumber, "数据保存异常");
                    return;
                }

                for (TaskSpecimen speciman : taskSpecimen) {

                    if (speciman.getSpecimenKey().equals(oneSampleId)) {
                        speciman.setIsTested(right);
                    }
                }
                task = tMeasureTaskService.getById(task.getId());
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
                task.setDeviceSerialNumber(deviceSerialNumber);
                tMeasureTaskService.updateById(task);


                if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {

                    rationService.changeStandSampleDataDigit(taskId, digit);
                    rationService.changeUnknowSampleDigit(taskId, digit);

                    JSONObject message = new JSONObject();
                    message.put("success", "测量完成");
                    sendMessage(deviceSerialNumber, message);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("result", "finish");
                    sendMessage(deviceSerialNumber, jsonObject);

                } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                    rationService.changeUnknowSampleDigit(taskId, digit);

                    JSONObject message = new JSONObject();
                    message.put("success", "测量完成");
                    sendMessage(deviceSerialNumber, message);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("result", "finish");
                    sendMessage(deviceSerialNumber, jsonObject);

                } else {
                    log.info("未知的样品种类");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("result", "finish");

                    sendMessage(deviceSerialNumber, jsonObject);
                }
                infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
                return;
            }

            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (!poolInfo.getSuccess()) {
                 
                sendError(deviceSerialNumber, "获取样品池信息失败");
                return;
            }
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();

            boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool() != null && poolInfoVO.getIsAutoChangePool();


            int usefulPoolNum = 1;
            int measureCount = 0;

            int currentPoolNum = -1;
            int maxPoolNum = 0;

            if (isAutoChangePool) {
                if (usefulLocation == null || usefulLocation.isEmpty()) {
                    log.info("无可用位置");
                     
                    sendError(deviceSerialNumber, "无可用样品池位置");
                    return;
                }
                usefulPoolNum = usefulLocation.size();
                maxPoolNum = Collections.max(usefulLocation);
            }

            task.setDeviceSerialNumber(deviceSerialNumber);

            //后续添加仪器状态检测

            // 设置参数
//            R<Boolean> setParams = setPreMeasureParams(deviceSerialNumber, parameter);
//            if (!setParams.getSuccess()) {
//
//                sendError(deviceSerialNumber, setParams.getMessage());
//                return;
//            }

            if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
                boolean repeat = "单池重复".equals(parameter.getRepeatMode());

                /*
                 * 循环所有样品
                 * 删除的过滤
                 * 测过的，要处理
                 */

                //参与曲线拟合的数据
                List<FRationCountDataInfo> fitCurveData = new ArrayList<>();
                //参与曲线拟合的参数
                List<String> fitCurveParams = new ArrayList<>();
                //测过的未知样，曲线拟合之后要重新计算
                List<TaskSpecimen> testUnknowSample = new ArrayList<>();

                for (TaskSpecimen specimen : taskSpecimen) {

                    if (!isAutoChangePool && measureCount == 1) {
                        log.info("不自动切换样品池，只测一个就结束");
                        break;
                    }

                    if (measureCount >= usefulPoolNum || currentPoolNum == maxPoolNum) {
                        break;
                    }

                    if (specimen.getDeleteType().equals(right)) {
                        System.out.println("删除的样品,不需要测量");
                        continue;
                    }

                    FMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                    if (!sample.getSampleKind().equals(sampleKind)) {

                        if (sample.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity()) && specimen.getIsTested().equals(right)) {
                            testUnknowSample.add(specimen);
                        }

                        log.info("不是需要的样品，跳过");
                        continue;
                    }

                    if (specimen.getIsTested().equals(right)) {
                    /*
                    测过的样品
                    处理空白样-----空白样统一处理，处理之后的放公共变量里
                    标准样需要返回分析结果，ABS或sumAbs
                    重新计算。遍历过程要保障，都是测过的样品，顺序不能乱
                    标准样，首先，取原始数据，计算abs值，计算sumAbs值
                    保存数据库？不对，测过的应该不会变，所以，直接取值
                    空白样一样，直接取值存即可
                     */
                        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                        queryWrapper.eq(FMeasureData::getTaskId, taskId)
                                .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                                .orderByDesc(FMeasureData::getSerialNumber)
                                .last("limit 1");
                        FMeasureData data = tMeasureDataService.getOne(queryWrapper);
                        if (data == null) {
                            data = new FMeasureData();
                        }
                        List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                        if (list != null && !list.isEmpty()) {
                            FRationCountDataInfo ansData;
                            FRationSaveCountDataInfo dealData = new FRationSaveCountDataInfo();
                            dealData.setSampleId(specimen.getSpecimenKey());
                            dealData.setTaskId(taskId);
                            dealData.setMeasureDataId(data.getId());

                            if (repeat) {
                                List<FRationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                                R<FRationCountDataInfo> absValue = rationService.countAbsValue(wavelengthDataInfos, parameter, digit);
                                if (!absValue.getSuccess()) {
                                    log.info("计算abs失败");
                                    continue;
                                }
                                ansData = absValue.getData();

                            } else {

                                R<FRationCountDataInfo> absValue = rationService.countAbsValue(list, parameter, digit);
                                if (!absValue.getSuccess()) {
                                    log.info("重新计算abs失败");
                                    continue;
                                }
                                ansData = absValue.getData();
                            }

                            if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                                emptySampleDeal(digit, ansData, dealData);
                                lastIsEmpty = true;
                            } else {
                                lastIsEmpty = false;
                                //测量过的数据，非空白样，添加标准浓度为了拟合曲线
                                //TODO 未来需要添加质量和浓度的区分
                                fitCurveParams.add(sample.getNormalConcentration());

                                if (empty) {
                                    FRationCountDataInfo sumAbsData = new FRationCountDataInfo();
                                    sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                                    sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                                    double v = Double.parseDouble(ansData.getColValue()) - minuend;
                                    sumAbsData.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                                    fitCurveData.add(sumAbsData);
                                } else {
                                    fitCurveData.add(ansData);
                                }
                            }
                        }
                        continue;
                    }

                    /*
                     * 测量
                     * 区分重复与不重复测，测量 + 计算出abs值一起
                     */
                    R<FRationDataVO> r;

                    if (!isAutoChangePool) {
                        sample.setSampleLocation(null);
                    }

                    if (repeat) {
                        //重复测量，单独处理！
                        r = singleSampleRepeatMeasure(sample, parameter, task, deviceSerialNumber);

                    } else {
                        r = singleSampleSimpleMeasure(sample, parameter, task, deviceSerialNumber);
                    }

                    if (isAutoChangePool) {
                        currentPoolNum = Integer.parseInt(sample.getSampleLocation());
                        fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.valueOf(sample.getSampleLocation()), param.getUpdateBy());
                    }
                    if (!r.getSuccess()) {

                        sendError(deviceSerialNumber, r.getMessage());
                        return;
                    }
                    measureCount++;
                    FRationDataVO data = r.getData();
                    List<FRationCountDataInfo> measureAbsDataList = data.getCountData();

                    specimen.setIsTested(right);
                    task.setSampleKey(JSON.toJSONString(taskSpecimen));
                    tMeasureTaskService.updateById(task);

                    FRationCountDataInfo absData = new FRationCountDataInfo();
                    for (FRationCountDataInfo dataInfo : measureAbsDataList) {
                        if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                            BeanUtils.copyProperties(dataInfo, absData);
                            break;
                        }
                    }
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                        R<FRationSaveCountDataInfo> saved = rationService.saveSampleMeasureData(data);
                        if (!saved.getSuccess()) {
                            log.info("数据保存异常");
                            sendError(deviceSerialNumber, saved.getMessage());
                            return;
                        }

                        FRationSaveCountDataInfo measureData = saved.getData();
                        String measureDataId = measureData.getMeasureDataId();

                        FRationSaveCountDataInfo dealData = new FRationSaveCountDataInfo();
                        dealData.setTaskId(taskId);
                        dealData.setSampleId(sample.getId());
                        dealData.setMeasureDataId(measureDataId);
                        emptySampleDeal(digit, absData, dealData);
                        for (FRationSaveCountDataInfo info : emptyDataList) {
                            String sampleId = info.getSampleId();
                            for (FRationCountDataInfo countDatum : info.getCountData()) {
                                if (countDatum.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                    sampleSend(deviceSerialNumber, sampleId, countDatum.getIdentity(), null, String.valueOf(countDatum.getColValue()));
                                }
                            }

                        }
                        lastIsEmpty = true;
                        specimen.setIsTested(right);
                        continue;
                    } else {
                        lastIsEmpty = false;
                        //测量成功后，非空白样，添加标准浓度为了拟合曲线
                        //TODO 未来需要添加质量和浓度的区分
                        fitCurveParams.add(sample.getNormalConcentration());

                        if (empty) {
                            FRationCountDataInfo sumAbsData = new FRationCountDataInfo();
                            sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                            sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                            double v = Double.parseDouble(absData.getColValue()) - minuend;
                            sumAbsData.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v), digit));
                            fitCurveData.add(sumAbsData);
                            measureAbsDataList.add(sumAbsData);

                            sampleSend(deviceSerialNumber, sample.getId(), sumAbsData.getIdentity(), null, sumAbsData.getColValue());
                            sendPoint(deviceSerialNumber, sample.getNormalConcentration(), sumAbsData.getColValue(), parameter);
                        } else {
                            fitCurveData.add(absData);
                            sendPoint(deviceSerialNumber, sample.getNormalConcentration(), absData.getColValue(), parameter);
                        }

                        data.setCountData(measureAbsDataList);
                        rationService.saveSampleMeasureData(data);
                        specimen.setIsTested(right);
                    }
                    specimen.setIsTested(right);
                }
                task = tMeasureTaskService.getById(taskId);
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
                tMeasureTaskService.updateById(task);

                //样品测完，移动样品池到1号
                if (isAutoChangePool) {

                    if (!usefulLocation.isEmpty()) {
                        DevOperationInputParam inputParam = new DevOperationInputParam();
                        inputParam.setDeviceSerialNumber(deviceSerialNumber);
                        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

                        SetParam setParam = new SetParam();
                        R<OperationResultEntity> operation = new R<>();
                        setParam.setCellPos(usefulLocation.get(0));

                        inputParam.setParam(setParam);

                        operation = operationUtilService.operation(inputParam);

                        if (!operation.getSuccess()) {
                            log.info("设置样品池位置出错");
                        }

                        JSONObject object = new JSONObject();
                        object.put("currentPoolNum", usefulLocation.get(0));
                        sendMessage(deviceSerialNumber, object);
                    }
                }


                //所有样品测完，拟合曲线
                //拟合曲线
                int minPointNum = Integer.MIN_VALUE + 1;

                switch (parameter.getCurveType()) {
                    case "1":
                        minPointNum = 2;
                        break;

                    case "2":
                        minPointNum = 3;
                        break;

                    case "3":
                        minPointNum = 4;
                        break;

                    case "4":
                        minPointNum = 5;
                        break;
                }

                if (parameter.getZeroMethod() != null && parameter.getZeroMethod().contains(RationCurveParamEnums.insert_zero.getIdentity())) {
                    minPointNum = minPointNum - 1;
                }

                if (fitCurveData.size() < minPointNum) {
                     
                    log.info("数据点数不足，不进行曲线拟合");
                    JSONObject message = new JSONObject();
                    message.put("success", "测量完成，请更换样品继续测量。");
                    sendMessage(deviceSerialNumber, message);
                    message.clear();
                    message.put("result", "finish");
                    sendMessage(deviceSerialNumber, message);

                    return;
                }

                R<FittingReceiveInfo> infoR = rationService.fittingCurve(fitCurveData, empty, task.getMethodKey(), fitCurveParams);

                if (!infoR.getSuccess()) {
                    log.info("曲线拟合异常");
                    sendError(deviceSerialNumber, "曲线拟合异常");
                    return;
                }
                FittingReceiveInfo fittingReceiveInfo = infoR.getData();
                String fitParam = JSON.toJSONString(fittingReceiveInfo);
                task.setFittingParam(fitParam);
                tMeasureTaskService.updateById(task);

                //绘图
                FRationFitCurveVO curveData = rationService.curveData(fittingReceiveInfo);


                JSONObject payload = new JSONObject();
                if (StringUtils.hasText(curveData.getR2())) {
                    payload.put("r2", curveData.getR2());
                }
                if (StringUtils.hasText(curveData.getR())) {
                    payload.put("r", curveData.getR());
                }
                payload.put("dataList", curveData.getDataList());
                payload.put("curveType", curveData.getCurveType());
                payload.put("equation", curveData.getEquation());
                payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
                payload.put("coefficientList", curveData.getCoefficientList());
                sendMessage(deviceSerialNumber, payload);


                if (!testUnknowSample.isEmpty()) {
                    reCountUnknowSampleData(deviceSerialNumber, testUnknowSample, taskId, fittingReceiveInfo, parameter);
                }

                JSONObject message = new JSONObject();
                message.put("success", "测量完成，请更换样品继续测量。");
                sendMessage(deviceSerialNumber, message);

                message.clear();
                message.put("result", "finish");
                sendMessage(deviceSerialNumber, message);

            } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                boolean repeat = "单池重复".equals(parameter.getRepeatMode());
                /*
                 * 循环所有样品
                 * 删除的过滤
                 * 测过的，要处理
                 */
                for (TaskSpecimen specimen : taskSpecimen) {

                    if (!isAutoChangePool && measureCount == 1) {
                        log.info("不自动切换样品池，只测一个就结束");
                        break;
                    }

                    if (measureCount >= usefulPoolNum || currentPoolNum == maxPoolNum) {
                        break;
                    }

                    if (specimen.getDeleteType().equals(right)) {
                        System.out.println("删除的样品,不需要测量");
                        continue;
                    }

                    FMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                    if (!sample.getSampleKind().equals(sampleKind)) {
                        log.info("不是需要的样品，跳过");
                        continue;
                    }

                    if (specimen.getIsTested().equals(right)) {
                    /*
                    测过的样品，未知样，只需要处理空白即可
                     */
                        if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                            //只处理空白样
                            LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                            queryWrapper.eq(FMeasureData::getTaskId, taskId)
                                    .eq(FMeasureData::getSampleKey, specimen.getSpecimenKey())
                                    .orderByDesc(FMeasureData::getSerialNumber)
                                    .last("limit 1");
                            FMeasureData data = tMeasureDataService.getOne(queryWrapper);
                            if (data == null) {
                                data = new FMeasureData();
                            }
                            List<FRationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), FRationWavelengthDataInfo.class);
                            if (list != null && !list.isEmpty()) {
                                FRationCountDataInfo ansData;

                                FRationSaveCountDataInfo dealData = new FRationSaveCountDataInfo();
                                dealData.setSampleId(specimen.getSpecimenKey());
                                dealData.setTaskId(taskId);
                                dealData.setMeasureDataId(data.getId());

                                if (repeat) {
                                    List<FRationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                                    R<FRationCountDataInfo> absValue = rationService.countAbsValue(wavelengthDataInfos, parameter, digit);
                                    if (!absValue.getSuccess()) {
                                        log.info("数据处理失败");
                                        continue;
                                    }
                                    ansData = absValue.getData();

                                } else {

                                    R<FRationCountDataInfo> absValue = rationService.countAbsValue(list, parameter, digit);
                                    if (!absValue.getSuccess()) {
                                        log.info("数据处理失败");
                                        continue;
                                    }
                                    ansData = absValue.getData();
                                }
                                emptySampleDeal(digit, ansData, dealData);
                            }
                        }
                        continue;
                    }

                    /*
                     * 测量
                     * 区分重复与不重复测，测量 + 计算出abs值一起
                     */
                    R<FRationDataVO> r;

                    if (!isAutoChangePool) {
                        sample.setSampleLocation(null);
                    }

                    if (repeat) {
                        //重复测量，单独处理！
                        r = singleSampleRepeatMeasure(sample, parameter, task, deviceSerialNumber);
                    } else {
                        r = singleSampleSimpleMeasure(sample, parameter, task, deviceSerialNumber);
                    }
                    if (isAutoChangePool) {
                        currentPoolNum = Integer.parseInt(sample.getSampleLocation());
                        fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), param.getUpdateBy());
                    }

                    if (!r.getSuccess()) {
                        sendError(deviceSerialNumber, r.getMessage());
                        return;
                    }

                    measureCount++;
                    FRationDataVO data = r.getData();

                    //保存数据
                    R<FRationSaveCountDataInfo> saved = rationService.saveSampleMeasureData(data);
                    if (!saved.getSuccess()) {
                        log.info("数据保存异常");
                        sendError(deviceSerialNumber, r.getMessage());
                        return;
                    }
                    FRationSaveCountDataInfo saveDataInfo = saved.getData();
                    specimen.setIsTested(right);
                    task.setSampleKey(JSON.toJSONString(taskSpecimen));

                    tMeasureTaskService.updateById(task);

                    List<FRationCountDataInfo> measureAbsDataList = saveDataInfo.getCountData();

                    FRationCountDataInfo absData = new FRationCountDataInfo();
                    for (FRationCountDataInfo dataInfo : measureAbsDataList) {
                        if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                            BeanUtils.copyProperties(dataInfo, absData);
                            break;
                        }
                    }
                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {

                        String measureDataId = saveDataInfo.getMeasureDataId();

                        FRationSaveCountDataInfo dealData = new FRationSaveCountDataInfo();
                        dealData.setTaskId(taskId);
                        dealData.setSampleId(sample.getId());
                        dealData.setMeasureDataId(measureDataId);

                        emptySampleDeal(digit, absData, dealData);
                        for (FRationSaveCountDataInfo info : emptyDataList) {
                            String sampleId = info.getSampleId();
                            for (FRationCountDataInfo countDatum : info.getCountData()) {
                                if (countDatum.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                    sampleSend(deviceSerialNumber, sampleId, countDatum.getIdentity(), null, String.valueOf(countDatum.getColValue()));
                                }
                            }
                        }
                        continue;
                    } else {
                        lastIsEmpty = false;
                        //TODO 未来需要添加质量和浓度的区分
                    /*
                    未知样处理----计算sumAbs值，计算拟合浓度，计算公式，保存结果
                    如果有空白样，和没有空白样
                    统一一个参数，然后根据有无空白，赋值
                    然后各类计算
                    最后保存。。。。应该先保存一次测量结果，在保存计算结果，
                     */
                        double countValue = 0.0;
                        if (empty) {
                            FRationCountDataInfo sumAbsData = new FRationCountDataInfo();
                            sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                            sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                            double v = Double.parseDouble(absData.getColValue()) - minuend;
                            Double value = DataCountUtil.formatValue(digit, v);
                            sumAbsData.setColValue(DataCountUtil.addZero(value, digit));
                            countValue = value;
                            measureAbsDataList.add(sumAbsData);
                            saveDataInfo.setCountData(measureAbsDataList);
                            rationService.saveAnalyzeData(saveDataInfo);
                            sampleSend(deviceSerialNumber, sample.getId(), sumAbsData.getIdentity(), null, String.valueOf(sumAbsData.getColValue()));
                        } else {
                            countValue = Double.parseDouble(absData.getColValue());
                        }
                        FittingReceiveInfo fittingInfo = null;
                        if (StringUtils.hasText(task.getFittingParam())) {
                            fittingInfo = JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
                        }
                        if (fittingInfo != null && fittingInfo.getRoundCoefficient() != null) {
                            String ans = rationService.valueCalculate(fittingInfo, countValue, parameter.getEquationType(), digit);

                            if (ans != null) {
                                FRationCountDataInfo valueData = new FRationCountDataInfo();
                                valueData.setOriginalValue(ans);
                                String s = rationService.fitDataRounding(ans, parameter);

                                valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                                valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                                valueData.setColValue(s);
                                measureAbsDataList.add(valueData);

                                measureAbsDataList = rationService.dataRounding(measureAbsDataList, parameter);

                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("sampleId", specimen.getSpecimenKey());
                                jsonObject.put("key", valueData.getIdentity());
                                jsonObject.put("value", valueData.getColValue());
                                sendMessage(deviceSerialNumber, jsonObject);

                                sendUnknowPoint(deviceSerialNumber, valueData, String.valueOf(countValue), parameter);

                            }

                        }
                        saveDataInfo.setCountData(measureAbsDataList);
                        rationService.saveAnalyzeData(saveDataInfo);
                        //定量的公式计算----待定
//                    rationService.formula()
//                        R<List<FRationCountDataInfo>> formula = rationService.formula(saveDataInfo.getAnalyzeDataId(), data.getWavelengthData(), sample, task, parameter, digit);
//
//                        if (!formula.getSuccess()) {
//                            log.info("公式计算异常");
//                             
//                            sendAndClose(session, formula.getMessage());
//                            return;
//                        }
//                        List<FRationCountDataInfo> formulaData = formula.getData();
//
//                        formulaData = rationService.dataRounding(formulaData, parameter);
//
//                        saveDataInfo.getCountData().addAll(formulaData);
//                        rationService.saveAnalyzeData(saveDataInfo);
                    }
                    specimen.setIsTested(right);
                }
                task = tMeasureTaskService.getById(taskId);
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
                tMeasureTaskService.updateById(task);
                //样品测完，移动样品池到1号
                if (isAutoChangePool) {

                    if (!usefulLocation.isEmpty()) {
                        DevOperationInputParam inputParam = new DevOperationInputParam();
                        inputParam.setDeviceSerialNumber(deviceSerialNumber);
                        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

                        SetParam setParam = new SetParam();
                        R<OperationResultEntity> operation = new R<>();
                        setParam.setCellPos(usefulLocation.get(0));

                        inputParam.setParam(setParam);

                        operation = operationUtilService.operation(inputParam);

                        if (!operation.getSuccess()) {
                            log.info("设置样品池位置出错");
                        }

                        JSONObject object = new JSONObject();
                        object.put("currentPoolNum", usefulLocation.get(0));
                        sendMessage(deviceSerialNumber, object);
                    }
                }

                JSONObject message = new JSONObject();
                message.put("success", "测量完成，请更换样品继续测量。");
                sendMessage(deviceSerialNumber, message);

                message.clear();
                message.put("result", "finish");
                sendMessage(deviceSerialNumber, message);
            } else {
                log.info("未知的样品种类");
                sendError(deviceSerialNumber, "未知的样品种类");
            }
        } catch (Exception e) {
            log.error("出现异常", e);
            sendError(deviceSerialNumber, "测量出错");

        } finally {
            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        }

    }

    public void reCountUnknowSampleData(String deviceSerialNumber, List<TaskSpecimen> dealSampleList, String taskId, FittingReceiveInfo curveData, FRationParameter parameter) throws IOException {

        boolean empty = dealSampleList.stream().anyMatch(specimen -> specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity()));

        for (TaskSpecimen specimen : dealSampleList) {

            List<FRationCountDataInfo> newData = new ArrayList<>();

            FRationCountDataInfo toCountData = new FRationCountDataInfo();

            FMeasureAnalysisData saveData = new FMeasureAnalysisData();
            String analysisDataId = null;

            LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                    .eq(FMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(FMeasureAnalysisData::getDataVersion);

            List<FMeasureAnalysisData> list = tMeasureAnalysisDataService.list(analysisDataQueryWrapper);
            for (FMeasureAnalysisData analysisData : list) {
                if (StringUtils.hasText(analysisData.getMeasureDataId()) && StringUtils.hasText(analysisData.getAnalysisResult())) {
                    List<FRationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), FRationCountDataInfo.class);
                    if (empty) {
                        for (FRationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                BeanUtils.copyProperties(dataInfo, toCountData);
                                newData.add(dataInfo);
                            }
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newData.add(dataInfo);
                            }
                        }
                    } else {
                        for (FRationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                BeanUtils.copyProperties(dataInfo, toCountData);
                                newData.add(dataInfo);
                            }
                        }
                    }
                    analysisDataId = analysisData.getId();
                    BeanUtils.copyProperties(analysisData, saveData);
                    break;
                }
            }
            if (!StringUtils.hasText(analysisDataId)) {
                log.info("该样品没有找到对应的测量数据");
                continue;
            }
            //拟合浓度计算
            String valued = rationService.valueCalculate(curveData, Double.valueOf(toCountData.getColValue()), parameter.getEquationType(), digit);

            if (valued != null) {
                FRationCountDataInfo valueData = new FRationCountDataInfo();
                valueData.setOriginalValue(valued);
                String s = rationService.fitDataRounding(valued, parameter);

                valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                valueData.setColValue(s);
                newData.add(valueData);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", specimen.getSpecimenKey());
                jsonObject.put("key", valueData.getIdentity());
                jsonObject.put("value", valueData.getColValue());
                sendMessage(deviceSerialNumber, jsonObject);
            }

            //公式计算---待定
            //TODO 公式计算

            saveData.setAnalysisResult(JSON.toJSONString(newData));
            tMeasureAnalysisDataService.update(saveData, analysisDataQueryWrapper);
        }
    }

    private static void initParams() {
        //初始化测量参数
        isAvg = false;
        isSd = false;
        isRsd = false;
        digit = 4;
        empty = false;
        emptyList.clear();
        emptyDataList.clear();
        lastIsEmpty = false;
        minuend = 0.0;
    }

    public void sendPoint(String deviceSerialNumber, String param, String absValue, FRationParameter parameter) {
        JSONObject point = new JSONObject();
        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            point.put("x", absValue);
            point.put("y", param);
        } else {
            point.put("x", param);
            point.put("y", absValue);
        }

        JSONObject payload = new JSONObject();
        payload.put("point", point);
        payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
        sendMessage(deviceSerialNumber, payload);

    }

    public void sendUnknowPoint(String deviceSerialNumber, FRationCountDataInfo param, String absValue, FRationParameter parameter) throws IOException {
        JSONObject point = new JSONObject();
        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            point.put("x", absValue);

            if (param.getIdentity().equals(SampleTableEnum.FIT_CONCENTRATION.getIdentity())) {
                point.put("y", DataCountUtil.addZero(Double.valueOf(param.getOriginalValue()), digit));
                point.put("yValue", param.getColValue());
                point.put("xValue", absValue);
            } else {
                point.put("y", DataCountUtil.addZero(Double.valueOf(param.getColValue()), digit));
            }
        } else {
            point.put("y", absValue);
            if (param.getIdentity().equals(SampleTableEnum.FIT_CONCENTRATION.getIdentity())) {
                point.put("x", DataCountUtil.addZero(Double.valueOf(param.getOriginalValue()), digit));
                point.put("xValue", param.getColValue());
                point.put("yValue", absValue);
            } else {
                point.put("x", DataCountUtil.addZero(Double.valueOf(param.getColValue()), digit));
            }
        }

        JSONObject payload = new JSONObject();
        payload.put("point", point);
        payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
        sendMessage(deviceSerialNumber,payload);
    }

    private void emptySampleDeal(Integer digit, FRationCountDataInfo ansData, FRationSaveCountDataInfo dealData){
        empty = true;

        FRationCountDataInfo yAbs = new FRationCountDataInfo();
        yAbs.setHeadName(SampleTableEnum.SUM_Abs.getName());
        yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
        yAbs.setColValue(DataCountUtil.addZero(0.0, digit));

        List<FRationCountDataInfo> currentList = new ArrayList<>();

        if (lastIsEmpty) {
            minuend = 0.0;
            emptyList.add(ansData);

            double sub = 0.0;
            //计算平均值
            for (FRationCountDataInfo dataInfo : emptyList) {
                sub += Double.parseDouble(dataInfo.getColValue());
            }
            minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
            //添加到空白列表
            currentList.add(ansData);
            double v = Double.parseDouble(ansData.getColValue()) - minuend;
            yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v),digit));
            currentList.add(yAbs);
            dealData.setCountData(currentList);
            emptyDataList.add(dealData);

            for (FRationSaveCountDataInfo data : emptyDataList) {
                List<FRationCountDataInfo> list = data.getCountData();
                List<FRationCountDataInfo> newList = new ArrayList<>();

                for (FRationCountDataInfo dataInfo : list) {
                    if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                        newList.add(dataInfo);
                        double v1 = Double.parseDouble(dataInfo.getColValue()) - minuend;
                        yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, v1), digit));
                        newList.add(yAbs);
                    }
                }
                data.setCountData(newList);
            }
            rationService.saveEmptySampleData(emptyDataList);
        } else {

            emptyList.clear();
            emptyDataList.clear();
            minuend = 0.0;
            emptyList.add(ansData);
            minuend = Double.parseDouble(ansData.getColValue());
            double sumAbs = Double.parseDouble(ansData.getColValue()) - minuend;
            yAbs.setColValue(DataCountUtil.addZero(DataCountUtil.formatValue(digit, sumAbs), digit));
            currentList.add(yAbs);
            currentList.add(ansData);
            dealData.setCountData(currentList);

            emptyDataList.add(dealData);
        }
        lastIsEmpty = true;
    }

    public List<FRationWavelengthDataInfo> countRepeatData(List<FRationWavelengthDataInfo> originData, Integer digit) {

        List<FRationWavelengthDataInfo> ans = new ArrayList<>();

        Map<String, List<Double>> absValue = new HashMap<>();
        Map<String, List<Double>> tPercentValue = new HashMap<>();
        Map<String, FRationWavelengthDataInfo> dataMap = new HashMap<>();
        Set<String> wavelengthSet = new HashSet<>();

        for (FRationWavelengthDataInfo data : originData) {
            if (data.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                dataMap.putIfAbsent(data.getWavelength(), data);
                continue;
            }
            double abs = Double.parseDouble(data.getAbsValue());
            double tPercent = Double.parseDouble(data.getTPercentValue());

            absValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(abs);
            tPercentValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(tPercent);
            wavelengthSet.add(data.getWavelength());
        }

        for (String wavelength : wavelengthSet) {
            FRationWavelengthDataInfo dataInfo = dataMap.get(wavelength);

            List<Double> allAbs = absValue.get(wavelength);
            double mean = DataCountUtil.calculateMean(allAbs, digit);

            List<Double> allPercent = tPercentValue.get(wavelength);
            double meanT = DataCountUtil.calculateMean(allPercent, digit);

            dataInfo.setAbsValue(String.valueOf(mean));
            dataInfo.setTPercentValue(String.valueOf(meanT));

            ans.add(dataInfo);
        }
        return ans;
    }



    //单个样品普通不微分测量
    public R<FRationDataVO> singleSampleSimpleMeasure(FMeasureSample sample, FRationParameter parameter, FMeasureTask task, String deviceSerialNumber) {


        List<FRationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<FRationCountDataInfo> countData = new ArrayList<>();
        FRationDataVO dataInfo = new FRationDataVO();

        List<String> waveLengths = parameter.getWaveLength();

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

        SetParam param = new SetParam();
        R<OperationResultEntity> operation = new R<>();
        if (StringUtils.hasText(sample.getSampleLocation())) {
            param.setCellPos(Integer.valueOf(sample.getSampleLocation()));
            JSONObject object = new JSONObject();
            object.put("currentPoolNum", sample.getSampleLocation());
            sendMessage(deviceSerialNumber, object);

            inputParam.setParam(param);

            operation = operationUtilService.operation(inputParam);

            if (!operation.getSuccess()) {
                log.info("设置样品池位置出错");
                return R.fail("设置样品池位置出错！");
            }
        }

        inputParam.setOperationType(OperationTypeEnums.SingWaveLengthMeasure.getType());
        LocalDateTime startTime = LocalDateTime.now();
        for (String waveLength : waveLengths) {

            inputParam.setSingleWavelength(waveLength);

            operation = operationUtilService.operation(inputParam);
            if (!operation.getSuccess()) {
                log.info("测量出错");
                return R.fail("测量出错！");
            }

            OperationResultEntity data = operation.getData();

            ChangeParam changeParam = data.getSingMeasureResult();


            machineData.add(changeParam);

//            ChangeParam originRsd = new ChangeParam();
//            if (StringUtils.hasText(task.getZeroParam())) {
//                List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
//                for (ChangeParam zeroParam : params) {
//                    if (zeroParam.getWl().equals(waveLength)) {
//                        BeanUtils.copyProperties(param, originRsd);
//                        break;
//                    }
//                }
//            }
//
//            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
//            Map<String, String> darkInfo = info.getDarkInfo();
//            double z = 1.0;
//            if (darkInfo != null && darkInfo.containsKey(waveLength)) {
//                z = Double.parseDouble(darkInfo.get(waveLength));
//            }
//            String tPercent = DataCountUtil.countTPercent(originRsd, changeParam, z);
//            String abs = DataCountUtil.changeTPercentOrAbs(null, tPercent);

            String abs = changeParam.getValue();
            String tPercent = changeParam.getOtherData();

            JSONObject currentInfo = new JSONObject();

            currentInfo.put("waveLength", waveLength + "nm");
            currentInfo.put("currentValue", DataCountUtil.formatValueWithBigDecimal(digit, abs) + WaveLengthEnum.abs.getName());
            sendMessage(deviceSerialNumber, currentInfo);

            FRationWavelengthDataInfo originDataInfo = new FRationWavelengthDataInfo();
            originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());
            originDataInfo.setWavelength(waveLength);

            originDataInfo.setAbsValue(abs);
            originDataInfo.setTPercentValue(tPercent);

            originDataList.add(originDataInfo);


            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sample.getId());
            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
            jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getAbsValue()));
            sendMessage(deviceSerialNumber, jsonObject);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
            jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getTPercentValue()));
            sendMessage(deviceSerialNumber, jsonObject);
        }


        LocalDateTime endTime = LocalDateTime.now();
        dataInfo.setMachineData(machineData);
        dataInfo.setWavelengthData(originDataList);
        dataInfo.setTaskId(task.getId());
        dataInfo.setSampleId(sample.getId());
        dataInfo.setSampleTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dataInfo.setStartTime(startTime);
        dataInfo.setEndTime(endTime);

        R<FRationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter,digit);
        if (!absValue.getSuccess()) {
            log.info("计算出错");
            sendError(deviceSerialNumber, absValue.getMessage());
            return R.fail("计算出错");
        }
        FRationCountDataInfo absValueData = absValue.getData();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sample.getId());
        jsonObject.put("key", absValueData.getIdentity());
        jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, String.valueOf( absValueData.getColValue())));
        sendMessage(deviceSerialNumber, jsonObject);

        jsonObject.put("key", "samplingTime");
        jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        sendMessage(deviceSerialNumber, jsonObject);

        countData.add(absValueData);
        dataInfo.setCountData(countData);

        return R.ok(dataInfo);
    }

    //微分测
    public R<Boolean> singleSampleDifferentialMeasure(){
        return R.ok();
    }

    public R<FRationDataVO> singleSampleRepeatMeasure(FMeasureSample sample, FRationParameter parameter, FMeasureTask task, String deviceSerialNumber) {
        int repeatCount = Integer.parseInt(parameter.getRepeatCount());


        List<FRationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<FRationCountDataInfo> countData = new ArrayList<>();
        FRationDataVO ans = new FRationDataVO();

        List<String> waveLengths = parameter.getWaveLength();

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

        SetParam param = new SetParam();
        R<OperationResultEntity> operation = new R<>();

        if (StringUtils.hasText(sample.getSampleLocation())) {
            param.setCellPos(Integer.valueOf(sample.getSampleLocation()));

            inputParam.setParam(param);

            operation = operationUtilService.operation(inputParam);

            if (!operation.getSuccess()) {
                log.info("设置样品池位置出错");
                return R.fail("设置样品池位置出错！");
            }

            JSONObject object = new JSONObject();
            object.put("currentPoolNum", sample.getSampleLocation());
            sendMessage(deviceSerialNumber, object);
        }

        double timeInterval = Double.parseDouble(parameter.getTimeInterval());
        timeInterval=timeInterval*1000;
        int interval= (int) Math.floor(timeInterval/5);
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime sampleTime = LocalDateTime.now();
        for (int i = 0; i < repeatCount; i++) {
            while (repeatMiddleTime != null && ChronoUnit.MILLIS.between(repeatMiddleTime, LocalDateTime.now()) < timeInterval) {
                log.info("时间间隔未到");
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    log.info("休眠异常");
                }
            }

            JSONObject object = new JSONObject();
            object.put("sampleId", sample.getId());
            object.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            object.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + i);
            object.put("value", sample.getSampleNumber() + "-" + (i + 1));
            sendMessage(deviceSerialNumber, object);
            R<ChangeParam> res = null;


            inputParam.setOperationType(OperationTypeEnums.SingWaveLengthMeasure.getType());

            for (String waveLength : waveLengths) {
                inputParam.setSingleWavelength(waveLength);
                operation = operationUtilService.operation(inputParam);

                if (!operation.getSuccess()) {
                    log.info("测量出错");
                    return R.fail("测量出错！");
                }

                OperationResultEntity data = operation.getData();
                ChangeParam changeParam = data.getSingMeasureResult();


                machineData.add(changeParam);

//                ChangeParam originRsd = new ChangeParam();
//                if (StringUtils.hasText(task.getZeroParam())) {
//                    List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
//                    for (ChangeParam zeroParam : params) {
//                        if (zeroParam.getWl().equals(waveLength)) {
//                            BeanUtils.copyProperties(param, originRsd);
//                            break;
//                        }
//                    }
//                }
//                DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
//                Map<String, String> darkInfo = info.getDarkInfo();
//                double z = 1.0;
//                if (darkInfo != null && darkInfo.containsKey(waveLength)) {
//                    z = Double.parseDouble(darkInfo.get(waveLength));
//                }
//                String tPercent = DataCountUtil.countTPercent(originRsd, changeParam, z);
//                String abs = DataCountUtil.changeTPercentOrAbs(null, tPercent);
                String abs = changeParam.getValue();
                String tPercent = changeParam.getOtherData();

                FRationWavelengthDataInfo originDataInfo = new FRationWavelengthDataInfo();
                originDataInfo.setIdentity(DataIdentityEnums.middle.getIdentity() + "-" + (i + 1));
                originDataInfo.setWavelength(waveLength);

                originDataInfo.setAbsValue(abs);
                originDataInfo.setTPercentValue(tPercent);
                originDataInfo.setMeasureTime(LocalDateTime.now());

                originDataList.add(originDataInfo);


                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sample.getId());
                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + i);
                jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getAbsValue()));
                sendMessage(deviceSerialNumber, jsonObject);

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getTPercentValue()));
                sendMessage(deviceSerialNumber, jsonObject);

            }
            repeatMiddleTime = LocalDateTime.now();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sample.getId());
            jsonObject.put("key", SampleTableEnum.SAMPLING_TIME.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + i);
            jsonObject.put("value", repeatMiddleTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sendMessage(deviceSerialNumber, jsonObject);

        }
        //重复测完，开始计算平均值

        Map<String, List<Double>> absValues = originDataList.stream()
                .collect(Collectors.groupingBy(
                        FRationWavelengthDataInfo::getWavelength,
                        Collectors.mapping(info -> Double.valueOf(info.getAbsValue()), Collectors.toList())
                ));

        Map<String, List<Double>> tPercentValues = originDataList.stream()
                .collect(Collectors.groupingBy(
                        FRationWavelengthDataInfo::getWavelength,
                        Collectors.mapping(info -> Double.valueOf(info.getTPercentValue()), Collectors.toList())
                ));
        for (String waveLength : waveLengths) {
            Map<String, FRepeatMeasureData> otherDataList = new HashMap<>();

            List<Double> absList = absValues.get(waveLength);
            double absMean = DataCountUtil.calculateMean(absList, digit);
            Double absSd = DataCountUtil.calculateSD(absList, digit);
            Double absRsd = DataCountUtil.calculateRSD(absList, digit);

            List<Double> tPercentList = tPercentValues.get(waveLength);
            double tPercentMean = DataCountUtil.calculateMean(tPercentList, digit);
            Double tPercentSd = DataCountUtil.calculateSD(tPercentList, digit);
            Double tPercentRsd = DataCountUtil.calculateRSD(tPercentList, digit);

            FRepeatMeasureData absData = new FRepeatMeasureData();
            absData.setSd(absSd == null ? "未检出" :DataCountUtil.addZero(absSd, digit));
            absData.setRsd(absRsd == null ? "未检出" :DataCountUtil.addZero(absRsd, digit));
            otherDataList.put(waveLength + "-" + WaveLengthEnum.abs.getCode(),absData);

            FRepeatMeasureData tPercentData = new FRepeatMeasureData();
            tPercentData.setSd(tPercentSd == null ? "未检出" :DataCountUtil.addZero(tPercentSd,digit));
            tPercentData.setRsd(tPercentRsd == null ? "未检出" :DataCountUtil.addZero(tPercentRsd, digit));
            otherDataList.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode() ,tPercentData);

            FRationWavelengthDataInfo info = new FRationWavelengthDataInfo();
            info.setWavelength(waveLength);
            info.setAbsValue(DataCountUtil.addZero(absMean, digit));
            info.setIdentity("result");
            info.setTPercentValue(DataCountUtil.addZero(tPercentMean,digit));
            info.setOtherData(otherDataList);
            info.setMeasureTime(LocalDateTime.now());


            repeatSend(deviceSerialNumber, absList, sample, repeatCount, waveLength + "-" + WaveLengthEnum.abs.getCode());
            repeatSend(deviceSerialNumber, tPercentList, sample, repeatCount, waveLength + "-" + WaveLengthEnum.tPercent.getCode());


            originDataList.add(info);
        }
        LocalDateTime endTime = LocalDateTime.now();

        R<FRationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter, digit);
        if (!absValue.getSuccess()) {
            return R.fail(absValue.getMessage());
        }
        FRationCountDataInfo data = absValue.getData();
        countData.add(data);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sample.getId());
        jsonObject.put("key", data.getIdentity());
        jsonObject.put("value", DataCountUtil.addZero(Double.parseDouble(data.getColValue()), digit));
        sendMessage(deviceSerialNumber, jsonObject);

        jsonObject.put("key", "samplingTime");
        jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        sendMessage(deviceSerialNumber, jsonObject);

        ans.setTaskId(task.getId());
        ans.setSampleId(sample.getId());
        ans.setStartTime(startTime);
        ans.setEndTime(endTime);
        ans.setCountData(countData);
        ans.setMachineData(machineData);
        ans.setSampleTime(endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        ans.setWavelengthData(originDataList);

        return R.ok(ans);

    }

    private R<Boolean> setPreMeasureParams(String deviceSerialNumber, FRationParameter parameter) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

        SetParam param = new SetParam();
        param.setPhotoMode(Integer.valueOf(WaveLengthEnum.abs.getCode()));


        param.setChangeLampWl(Double.parseDouble(parameter.getChangeLampWaveLength()));
        if (StringUtils.hasText(parameter.getSpectralBandwidth()) && infoService.isNeedSetSit(deviceSerialNumber)) {
            param.setSlit1(Double.parseDouble(parameter.getSpectralBandwidth()));
        }

        param.setResponseTime(Double.parseDouble(parameter.getResponseTime()));
        inputParam.setParam(param);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        }

        return R.ok(true);

    }


    public void sampleSend(String deviceSerialNumber, String sampleId, String key, String identity, String value) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sampleId);
        jsonObject.put("key", key);
        if (StringUtils.hasText(identity)) {
            jsonObject.put("identity", identity);
        }
        jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, value));
        sendMessage(deviceSerialNumber, jsonObject);
    }

    public void repeatSend(String deviceSerialNumber, List<Double> data, FMeasureSample sample, Integer repetitionTime, String key) {

        Double v = DataCountUtil.calculateMean(data, digit);
        Double sd = DataCountUtil.calculateSD(data, digit);
        Double rsd = DataCountUtil.calculateRSD(data, digit);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sample.getId());
//        repetitionTime = repetitionTime + 1;

        if (isAvg) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + repetitionTime);
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());
            sendMessage(deviceSerialNumber, jsonObject);

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.avg.getIdentity() + "-" + repetitionTime);
            jsonObject.put("value", DataCountUtil.addZero(v, digit));
            sendMessage(deviceSerialNumber, jsonObject);
        }

        if (isSd) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 1));
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
            sendMessage(deviceSerialNumber, jsonObject);

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.sd.getIdentity() + "-" + (repetitionTime + 1));
            jsonObject.put("value", sd == null ? "未检出" :DataCountUtil.addZero(sd, digit));
            sendMessage(deviceSerialNumber, jsonObject);
        }

        if (isRsd) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
            sendMessage(deviceSerialNumber, jsonObject);

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.rsd.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put("value", rsd == null ? "未检出" :DataCountUtil.addZero(rsd, digit));
            sendMessage(deviceSerialNumber, jsonObject);
        }
        jsonObject.remove("identity");
        jsonObject.put("key", key);
        jsonObject.put("value", DataCountUtil.addZero(v, digit));
        sendMessage(deviceSerialNumber, jsonObject);
    }

    private void sendMessage(String deviceSerialNumber,JSONObject object) {

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.RationMeasure.getOperation());
        vo.setData(object);

        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }

    private void sendError(String deviceSerialNumber, String message) {

        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.RationMeasure.getOperation(), message);

        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", "finish");

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.RationMeasure.getOperation());
        vo.setData(jsonObject);
        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }
}
