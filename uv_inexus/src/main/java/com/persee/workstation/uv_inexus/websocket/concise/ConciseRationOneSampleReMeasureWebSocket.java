/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRationParameter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.vo.concise.ration.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ConciseRationOneSampleReMeasureWebSocket extends TextWebSocketHandler {
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();

    private static volatile Boolean empty = false;
    private static volatile Boolean lastIsEmpty = false;
    private static volatile Double minuend = 0.0;
    //存放空白样数据（计算后的值）
    private static volatile List<FRationSaveCountDataInfo> emptyDataList = new ArrayList<>();
    //存放空白样计算出的Abs的值
    private static volatile List<FRationCountDataInfo> emptyList = new ArrayList<>();

    @Autowired
    private FMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;

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
    FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    FormulaUtil formulaUtil;

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

    /**
     * 前端连接成功 WebSocket 时触发
     *
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {


        if (status != 0) {
            sendAndClose(session, "已有连接，不要重复操作");
        }
        status = 1;
        // 记录当前连接
        sessions.put(session.getId(), session);
        initParams();

        // 解析参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId = getQueryParam(query, "sampleId");
        deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
        String updateBy = getQueryParam(query, "updateBy");
        int di = Integer.parseInt(getQueryParam(query, "digit"));
        isAvg = getQueryParam(query, "avg").equals("true");
        isSd = getQueryParam(query, "sd").equals("true");
        isRsd = getQueryParam(query, "rsd").equals("true");

        if (deviceSerialNumber == null) {
            sendAndClose(session, "未连接仪器");
            return;
        }

        if (taskId == null) {
            sendAndClose(session, "请创建任务后再测量");
            return;
        }

        FMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            sendAndClose(session, "任务或样品不存在");
            return;
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        FMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());
        FRationParameter parameter = JSON.parseObject(byId.getMeasurementParamInfo(), FRationParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendAndClose(session, "缺少波长， 无法测量");
            return;
        }

        if (di < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
        } else {
            digit = di;
        }

        R<Boolean> usedDevice = infoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
        if (!usedDevice.getSuccess()) {
            sendAndClose(session, usedDevice.getMessage());
            return;
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
                                    specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())
                    );

            FMeasureSample dealSample = tMeasureSampleService.getById(sampleId);
            if (dealSample == null) {
                infoService.releaseDevice(deviceSerialNumber);
                sendAndClose(session, "样品不存在");
                return;
            }
            String sampleKind = dealSample.getSampleKind();
            if (sampleKind == null) {
                infoService.releaseDevice(deviceSerialNumber);
                sendAndClose(session, "样品信息缺失");
                return;
            }


            boolean repeat = parameter.getRepeatMode().equals("单池重复");

            R<FRationDataVO> r;
            if (repeat) {
                //重复测量，单独处理！
                r = singleSampleRepeatMeasure(dealSample, parameter, task, deviceSerialNumber, session.getId());

            } else {
                r = singleSampleSimpleMeasure(dealSample, parameter, task, deviceSerialNumber, session.getId());
            }

            if (!r.getSuccess()) {
                infoService.releaseDevice(deviceSerialNumber);
                sendAndClose(session, r.getMessage());
                return;
            }
            FRationDataVO data = r.getData();

            //保存数据
            R<FRationSaveCountDataInfo> saved = rationService.saveSampleMeasureData(data);
            if (!saved.getSuccess()) {
                log.info("数据保存异常");
                infoService.releaseDevice(deviceSerialNumber);
                sendAndClose(session, saved.getMessage());
                return;
            }

            //后续添加仪器状态检测

            if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {

                rationService.changeStandSampleDataDigit(taskId, digit);
                rationService.changeUnknowSampleDigit(taskId, digit);

                JSONObject message = new JSONObject();
                message.put("success", "测量完成");
                session.sendMessage(new TextMessage(message.toJSONString()));

                status = 0;
                infoService.releaseDevice(deviceSerialNumber);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", "finish");
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                session.close();

            } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {

                rationService.changeUnknowSampleDigit(taskId, digit);

                JSONObject message = new JSONObject();
                message.put("success", "测量完成");
                session.sendMessage(new TextMessage(message.toJSONString()));

                status = 0;
                infoService.releaseDevice(deviceSerialNumber);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", "finish");
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                session.close();
            } else {
                log.info("未知的样品种类");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", "finish");
                status = 0;
                infoService.releaseDevice(deviceSerialNumber);
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                session.close();
            }

        } catch (Exception e) {
            log.info("连接异常{}", e.getMessage());
            status = 0;
            infoService.releaseDevice(deviceSerialNumber);
            sendAndClose(session, "测量出错");
        } finally {
            status = 0;
            infoService.releaseDevice(deviceSerialNumber);
            session.close();
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

    public void sendPoint(WebSocketSession session, String param, Double absValue, FRationParameter parameter) throws IOException {
        JSONObject point = new JSONObject();
        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            point.put("x", absValue);
            point.put("y", param);
        } else {
            point.put("x", param);
            point.put("y", absValue);
        }
        if (session.isOpen()) {
            JSONObject payload = new JSONObject();
            payload.put("point", point);
            payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
            session.sendMessage(new TextMessage(payload.toJSONString()));
        }
    }

    private void emptySampleDeal(Integer digit, FRationCountDataInfo ansData, FRationSaveCountDataInfo dealData) {
        empty = true;

        FRationCountDataInfo yAbs = new FRationCountDataInfo();
        yAbs.setHeadName(SampleTableEnum.SUM_Abs.getName());
        yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
        yAbs.setColValue("0.0");

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
            yAbs.setColValue(DataCountUtil.formatValueWithBigDecimal(digit, String.valueOf(v)));
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
                        yAbs.setColValue(String.valueOf(DataCountUtil.formatValue(digit, v1)));
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
            yAbs.setColValue(String.valueOf(DataCountUtil.formatValue(digit, sumAbs)));
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
    public R<FRationDataVO> singleSampleSimpleMeasure(FMeasureSample sample, FRationParameter parameter, FMeasureTask task, String deviceSerialNumber, String sessionId) throws IOException {

        WebSocketSession session = sessions.get(sessionId);

        List<FRationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<FRationCountDataInfo> countData = new ArrayList<>();
        FRationDataVO dataInfo = new FRationDataVO();

        List<String> waveLengths = parameter.getWaveLength();
        String ip = infoService.getIp(deviceSerialNumber);

        list.add(new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));
        if (StringUtils.hasText(sample.getSampleLocation())) {
            list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
            JSONObject object = new JSONObject();
            object.put("currentPoolNum", sample.getSampleLocation());
            session.sendMessage(new TextMessage(object.toJSONString()));
        }
        LocalDateTime startTime = LocalDateTime.now();
        for (String waveLength : waveLengths) {
            Double step = formulaUtil.calculateStep(Double.parseDouble(waveLength), deviceSerialNumber);

            list.add(new Command("SetWl", new Object[]{step}));
            list.add(new Command("Measure", new Object[]{}));

            R<ChangeParam> res = null;
            try {
                res = devMessageUtil.photometricAndRationMeasurement(list, ip);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!res.getSuccess()) {
                log.info("测量出错");
                return R.fail("测量出错");
            }
            ChangeParam response = res.getData();
            machineData.add(response);
            ChangeParam originRsd = new ChangeParam();
            if (StringUtils.hasText(task.getZeroParam())) {
                List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
                for (ChangeParam param : params) {
                    if (param.getWl().equals(waveLength)) {
                        BeanUtils.copyProperties(param, originRsd);
                        break;
                    }
                }
            }

            String tPercent = DataCountUtil.countTPercent(originRsd, response, 1.0);
            String data = DataCountUtil.changeTPercentOrAbs(null, tPercent);

            JSONObject currentInfo = new JSONObject();
            currentInfo.put("waveLength", waveLength + "nm");
            currentInfo.put("currentValue", DataCountUtil.formatValueWithBigDecimal(digit, data) + WaveLengthEnum.abs.getName());
            session.sendMessage(new TextMessage(currentInfo.toJSONString()));

            FRationWavelengthDataInfo originDataInfo = new FRationWavelengthDataInfo();
            originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());
            originDataInfo.setWavelength(waveLength);

            originDataInfo.setAbsValue(data);
            originDataInfo.setTPercentValue(tPercent);

            originDataList.add(originDataInfo);

            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sample.getId());
                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getAbsValue()));
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getTPercentValue()));
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            }
            list.clear();
        }
        LocalDateTime endTime = LocalDateTime.now();
        dataInfo.setMachineData(machineData);
        dataInfo.setWavelengthData(originDataList);
        dataInfo.setTaskId(task.getId());
        dataInfo.setSampleId(sample.getId());
        dataInfo.setSampleTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dataInfo.setStartTime(startTime);
        dataInfo.setEndTime(endTime);

        R<FRationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter, digit);
        if (!absValue.getSuccess()) {
            log.info("计算出错");
            sendAndClose(session, absValue.getMessage());
            return R.fail("计算出错");
        }
        FRationCountDataInfo data = absValue.getData();
        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sample.getId());
            jsonObject.put("key", data.getIdentity());
            jsonObject.put("value", data.getColValue());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }
        countData.add(data);
        dataInfo.setCountData(countData);

        return R.ok(dataInfo);
    }

    //微分测
    public R<Boolean> singleSampleDifferentialMeasure() {
        return R.ok();
    }

    public R<FRationDataVO> singleSampleRepeatMeasure(FMeasureSample sample, FRationParameter parameter, FMeasureTask task, String deviceSerialNumber, String sessionId) throws IOException {
        int repeatCount = Integer.parseInt(parameter.getRepeatCount());

        WebSocketSession session = sessions.get(sessionId);

        List<FRationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<FRationCountDataInfo> countData = new ArrayList<>();
        FRationDataVO ans = new FRationDataVO();

        List<String> waveLengths = parameter.getWaveLength();

        String ip = infoService.getIp(deviceSerialNumber);

        LocalDateTime startTime = LocalDateTime.now();
        list.add(new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));
        if (StringUtils.hasText(sample.getSampleLocation())) {
            list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
            JSONObject object = new JSONObject();
            object.put("currentPoolNum", sample.getSampleLocation());
            session.sendMessage(new TextMessage(object.toJSONString()));
        }

        for (int i = 0; i < repeatCount; i++) {
            for (String waveLength : waveLengths) {

                Double step = formulaUtil.calculateStep(Double.parseDouble(waveLength), deviceSerialNumber);
                list.add(new Command("SetWl", new Object[]{step}));
                list.add(new Command("Measure", new Object[]{}));

                R<ChangeParam> res = null;
                try {
                    res = devMessageUtil.photometricAndRationMeasurement(list, ip);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!res.getSuccess()) {
                    log.info("测量出错");
                    return R.fail("测量出错");
                }
                ChangeParam response = res.getData();
                response.setWl(waveLength + "-" + i);
                machineData.add(response);
                ChangeParam originRsd = new ChangeParam();
                List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
                for (ChangeParam param : params) {
                    if (param.getWl().equals(waveLength)) {
                        BeanUtils.copyProperties(param, originRsd);
                        break;
                    }
                }
                String tPercent = DataCountUtil.countTPercent(originRsd, response, 1.0);
                String data = DataCountUtil.changeTPercentOrAbs(null, tPercent);

                JSONObject currentInfo = new JSONObject();
                currentInfo.put("waveLength", waveLength + "nm");
                currentInfo.put("currentValue", DataCountUtil.formatValueWithBigDecimal(digit, data) + WaveLengthEnum.abs.getName());
                session.sendMessage(new TextMessage(currentInfo.toJSONString()));

                FRationWavelengthDataInfo originDataInfo = new FRationWavelengthDataInfo();
                originDataInfo.setIdentity(DataIdentityEnums.middle.getIdentity() + "-" + i);
                originDataInfo.setWavelength(waveLength);

                originDataInfo.setAbsValue(data);
                originDataInfo.setTPercentValue(tPercent);
                originDataInfo.setMeasureTime(LocalDateTime.now());

                originDataList.add(originDataInfo);

                if (session.isOpen()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("sampleId", sample.getId());
                    jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                    jsonObject.put("identity", waveLength + "-" + WaveLengthEnum.abs.getCode() + "-" + i);
                    jsonObject.put("value", originDataInfo.getAbsValue());
                    session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                    jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                    jsonObject.put("identity", waveLength + "-" + WaveLengthEnum.tPercent.getCode() + "-" + i);
                    jsonObject.put("value", originDataInfo.getTPercentValue());
                    session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                }

                list.clear();
            }
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
            absData.setSd(absSd == null ? "未检出" : DataCountUtil.addZero(absSd, digit));
            absData.setRsd(absRsd == null ? "未检出" : DataCountUtil.addZero(absRsd, digit));
            otherDataList.put(waveLength + "-" + WaveLengthEnum.abs.getCode(), absData);

            FRepeatMeasureData tPercentData = new FRepeatMeasureData();
            tPercentData.setSd(tPercentSd == null ? "未检出" : DataCountUtil.addZero(tPercentSd, digit));
            tPercentData.setRsd(tPercentRsd == null ? "未检出" : DataCountUtil.addZero(tPercentRsd, digit));
            otherDataList.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode(), tPercentData);

            FRationWavelengthDataInfo info = new FRationWavelengthDataInfo();
            info.setWavelength(waveLength);
            info.setAbsValue(DataCountUtil.addZero(absMean, digit));
            info.setIdentity("result");
            info.setTPercentValue(DataCountUtil.addZero(tPercentMean, digit));
            info.setOtherData(otherDataList);
            info.setMeasureTime(LocalDateTime.now());

            if (session.isOpen()) {
                repeatSend(session, absList, sample, repeatCount, waveLength + "-" + WaveLengthEnum.abs.getCode());
                repeatSend(session, tPercentList, sample, repeatCount, waveLength + "-" + WaveLengthEnum.tPercent.getCode());
            }

            originDataList.add(info);
        }
        LocalDateTime endTime = LocalDateTime.now();

        R<FRationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter, digit);
        if (!absValue.getSuccess()) {
            return R.fail(absValue.getMessage());
        }
        FRationCountDataInfo data = absValue.getData();
        countData.add(data);
        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sample.getId());
            jsonObject.put("key", data.getIdentity());
            jsonObject.put("value", DataCountUtil.addZero(Double.valueOf(data.getColValue()), digit));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }
        ans.setTaskId(task.getId());
        ans.setSampleId(sample.getId());
        ans.setStartTime(startTime);
        ans.setEndTime(endTime);
        ans.setCountData(countData);
        ans.setMachineData(machineData);
        ans.setSampleTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        ans.setWavelengthData(originDataList);

        return R.ok(ans);

    }

    public void sampleSend(WebSocketSession session, String sampleId, String key, String identity, String value) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sampleId);
        jsonObject.put("key", key);
        if (StringUtils.hasText(identity)) {
            jsonObject.put("identity", identity);
        }
        jsonObject.put("value", value);
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));
    }

    public void repeatSend(WebSocketSession session, List<Double> data, FMeasureSample sample, Integer repetitionTime, String key) throws IOException {

        Double v = DataCountUtil.calculateMean(data, digit);
        Double sd = DataCountUtil.calculateSD(data, digit);
        Double rsd = DataCountUtil.calculateRSD(data, digit);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sample.getId());

        if (isAvg) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + repetitionTime);
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.avg.getIdentity() + "-" + repetitionTime);
            jsonObject.put("value", DataCountUtil.addZero(v, digit));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        if (isSd) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 1));
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.sd.getIdentity() + "-" + (repetitionTime + 1));
            jsonObject.put("value", sd == null ? "未检出" : DataCountUtil.addZero(sd, digit));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        if (isRsd) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.rsd.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put("value", rsd == null ? "未检出" : DataCountUtil.addZero(rsd, digit));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        jsonObject.put("key", key);
        jsonObject.remove("identity");
        jsonObject.put("value", DataCountUtil.addZero(v, digit));

    }

    /**
     * 收到前端消息时触发
     *
     * @param session
     * @param message
     * @throws IOException
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String msg = message.getPayload();
        System.out.println("收到前端消息: " + msg);

        // 示例：如果前端发来"save"，就保存当前缓存
        if ("save".equalsIgnoreCase(msg)) {
            String deviceId = getQueryParam(session.getUri().getQuery(), "device_id");
            if (deviceId != null && deviceDataCache.containsKey(deviceId)) {
                List<String> toSave = new ArrayList<>(deviceDataCache.get(deviceId));
                System.out.println("保存数据[" + deviceId + "]: " + toSave.size() + "条");
                // TODO: 持久化保存到数据库
                deviceDataCache.get(deviceId).clear();
            }
        }
    }

    /**
     * 前端连接关闭时触发
     *
     * @param session
     * @param closeStatus
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        sessions.remove(session.getId());
        status = 0;
        System.out.println("连接关闭: " + session.getId());
    }

    /**
     * 连接异常时触发
     *
     * @param session
     * @param exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        try {
            session.close();
            infoService.releaseDevice(deviceSerialNumber);
        } catch (IOException e) {
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
        }finally {
            infoService.releaseDevice(deviceSerialNumber);
        }
        System.out.println("连接异常: " + exception.getMessage());
        status = 0;
        System.out.println("连接异常: " + exception.getMessage());
    }

    /**
     * 获取查询参数
     *
     * @param query
     * @param key
     * @return
     */
    private String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }

    /**
     * 发送消息并关闭连接
     *
     * @param session
     * @param msg
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            status = 0;
            JSONObject object = new JSONObject();
            object.put("error", msg);
            session.sendMessage(new TextMessage(object.toJSONString()));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "finish");
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            session.close();
        } catch (IOException ignored) {
        }
    }
}
