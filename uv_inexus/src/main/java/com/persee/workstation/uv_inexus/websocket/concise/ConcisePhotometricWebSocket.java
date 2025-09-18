package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.enums.measurementEnums.DataIdentityEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPhotometricParameter;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRepeatMeasureData;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ConcisePhotometricWebSocket extends TextWebSocketHandler {

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();

    private static volatile int status = 0;

    private static volatile int digit = 4;

    private static LocalDateTime repeatMiddleTime;

    @Autowired
    private FMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private FMeasureMethodService tMeasureMethodService;

    @Autowired
    private FMeasureDataService tMeasureDataService;

    @Autowired
    private PhotometricConciseService photometricService;

    @Autowired
    private FMeasureSampleService tMeasureSampleService;

    @Autowired
    private FMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    DeviceInfoService infoService;

    @Value("1")
    private String right;

    @Value("0")
    private String error;


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

        // 解析参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String sampleId = getQueryParam(query, "sampleId");
        String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
        String di = getQueryParam(query, "digit");
        if (deviceSerialNumber == null) {
            sendAndClose(session, "未连接仪器");
            return;
        }

        if (taskId == null) {
            sendAndClose(session, "请创建任务");
            return;
        }
        FMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            sendAndClose(session, "任务或样品不存在");
            return;
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendAndClose(session, "缺少波长， 无法测量");
            return;
        }

        digit = StringUtils.hasText(di) ? Integer.parseInt(di) : Constants.DEFAULT_DECIMAL_NUM;

        if (sampleId != null) {
            TaskSpecimen specimen = new TaskSpecimen();
            for (TaskSpecimen s : taskSpecimen) {
                if (s.getSpecimenKey().equals(sampleId)) {
                    BeanUtils.copyProperties(s, specimen);
                }
            }
            if (specimen.getDeleteType().equals(right)) {
                log.info("删除的样品,不需要测量");
                session.sendMessage(new TextMessage("已删除的样品, 无法测量"));
                return;
            }
            removeOldData(taskId, sampleId);

            R<FPhotometricSampleAndDataVO> measure;
            if (parameter.getRepetitionPattern().equals("单池重复")) {
                measure = singleSampleRepeatMeasure(specimen, parameter, task, deviceSerialNumber, session.getId());
            } else{
                measure = singleSampleMeasure(specimen, parameter, task, deviceSerialNumber, session.getId());
            }

            if (!measure.getSuccess()) {
                sendAndClose(session, "测量失败");
                return;
            }
            FPhotometricSampleAndDataVO data = measure.getData();
            R<String> measureData = photometricService.savePhotometricMeasureData(data);
            String dataId = measureData.getData();
            if (!StringUtils.hasText(dataId)) {
                sendAndClose(session, "数据保存异常！");
                return;
            }
            specimen.setIsTested(right);
            task.setSampleKey(JSON.toJSONString(taskSpecimen));
            tMeasureTaskService.updateById(task);

            FMeasureSample sample = tMeasureSampleService.getById(sampleId);
            R<FPhotometricSampleAndDataVO> formulaCount = formulaCount(session, data, sample, task, parameter);
            if (!formulaCount.getSuccess()) {
                sendAndClose(session, "公式计算失败");
                return;
            }
            data = formulaCount.getData();

            photometricService.saveCountData(data, dataId);

        } else {
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(right) || specimen.getIsTested().equals(right)) {
                    System.out.println("删除或测量过的样品,不需要测量");
                    continue;
                }
                R<FPhotometricSampleAndDataVO> measure;
                if (parameter.getRepetitionPattern().equals("单池重复")) {
                    measure = singleSampleRepeatMeasure(specimen, parameter, task, deviceSerialNumber, session.getId());
                } else{
                    measure = singleSampleMeasure(specimen, parameter, task, deviceSerialNumber, session.getId());
                }

                if (!measure.getSuccess()) {
                    sendAndClose(session, measure.getMessage());
                    return;
                }
                FPhotometricSampleAndDataVO data = measure.getData();
                R<String> measureData = photometricService.savePhotometricMeasureData(data);
                String dataId = measureData.getData();
                if (!StringUtils.hasText(dataId)) {
                    sendAndClose(session, "数据保存异常！");
                    return;
                }
                specimen.setIsTested(right);
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
                tMeasureTaskService.updateById(task);
                //公式计算
                FMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                R<FPhotometricSampleAndDataVO> formulaCount = formulaCount(session, data, sample, task, parameter);
                if (!formulaCount.getSuccess()) {
                    sendAndClose(session, "公式计算失败");
                    return;
                }
                data = formulaCount.getData();
                photometricService.saveCountData(data, dataId);
            }
        }
        task.setSampleKey(JSON.toJSONString(taskSpecimen));
        tMeasureTaskService.updateById(task);
        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "finish");
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }
        sessions.remove(session.getId());
        session.close();
        status = 0;

    }

    private void removeOldData(String taskId, String sampleId) {
        LambdaQueryWrapper<FMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureData::getTaskId, taskId).eq(FMeasureData::getSampleKey, sampleId)
                .orderByDesc(FMeasureData::getSerialNumber).last("limit 1");
        FMeasureData data = tMeasureDataService.getOne(queryWrapper);
        if (data != null) {
            FMeasureData newData = new FMeasureData();

            newData.setSampleKey(sampleId);
            newData.setTaskId(taskId);
            newData.setIsDelete(error);
            newData.setRemark(data.getRemark());

            newData.setSerialNumber(data.getSerialNumber() + 1);
            tMeasureDataService.save(newData);
        }

        LambdaQueryWrapper<FMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(FMeasureAnalysisData::getTaskId, taskId)
                .eq(FMeasureAnalysisData::getSampleKey, sampleId)
                .orderByDesc(FMeasureAnalysisData::getDataVersion)
                .last("limit 1");
        FMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null) {
            if (StringUtils.hasText(analysisData.getMeasureDataId()) || StringUtils.hasText(analysisData.getAnalysisResult())) {

                FMeasureAnalysisData newAnalysisData = new FMeasureAnalysisData();
                newAnalysisData.setSampleKey(sampleId);
                newAnalysisData.setTaskId(taskId);
                newAnalysisData.setDataVersion(analysisData.getDataVersion() + 1);
                newAnalysisData.setIsDelete(error);
                tMeasureAnalysisDataService.save(newAnalysisData);
            }

            log.info("当前记录没有测量数据");
        }
    }

    public R<FPhotometricSampleAndDataVO> singleSampleMeasure(TaskSpecimen specimen, FPhotometricParameter parameter, FMeasureTask task, String deviceSerialNumber, String sessionId) throws IOException {

        WebSocketSession session = sessions.get(sessionId);

        String sampleKey = specimen.getSpecimenKey();
        FMeasureSample sample = tMeasureSampleService.getById(sampleKey);

        List<FPhotometricOriginDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();

        List<String> waveLengths = parameter.getWaveLength();

        list.add(new Command("SetPhotoMode", new Object[]{parameter.getLuminosity()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));
        LocalDateTime startTime = LocalDateTime.now();
        for (String waveLength : waveLengths) {
            list.add(new Command("SetWl", new Object[]{waveLength}));
            list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
            list.add(new Command("Measure", new Object[]{}));

            String ip = infoService.getIp(deviceSerialNumber);
            R<ChangeParam> res = devMessageUtil.photometricAndRationMeasurement(list, ip);
            if (!res.getSuccess()) {
                log.info("测量出错");
                return R.fail("测量出错");
            }
            ChangeParam response = res.getData();
            machineData.add(response);
            ChangeParam originRsd = new ChangeParam();
            List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
            for (ChangeParam param : params) {
                if (param.getWl().equals(waveLength)) {
                    BeanUtils.copyProperties(param, originRsd);
                    break;
                }
            }
            //处理数据
            FPhotometricOriginDataInfo dataInfo = dealAndSendMessage(session, waveLength, sampleKey, parameter.getLuminosity(), response, params, null, sample.getSampleNumber());
            dataInfo.setIdentity(DataIdentityEnums.result.getIdentity());

            originDataList.add(dataInfo);
            list.clear();
        }
        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sampleKey);
            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        LocalDateTime endTime = LocalDateTime.now();

        FPhotometricSampleAndDataVO sampleAndDataVO = new FPhotometricSampleAndDataVO();
        sampleAndDataVO.setTaskId(task.getId());
        sampleAndDataVO.setSampleId(sampleKey);
        sampleAndDataVO.setWavelengthData(originDataList);
        sampleAndDataVO.setMachineData(machineData);
        sampleAndDataVO.setStartTime(startTime);
        sampleAndDataVO.setEndTime(endTime);
        sampleAndDataVO.setSampleTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return R.ok(sampleAndDataVO);
    }

    public R<FPhotometricSampleAndDataVO> singleSampleRepeatMeasure(TaskSpecimen specimen, FPhotometricParameter parameter, FMeasureTask task, String deviceSerialNumber, String sessionId) throws IOException {

        WebSocketSession session = sessions.get(sessionId);

        String sampleKey = specimen.getSpecimenKey();
        FMeasureSample sample = tMeasureSampleService.getById(sampleKey);

        List<FPhotometricOriginDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();

        List<String> waveLengths = parameter.getWaveLength();


        list.add(new Command("SetPhotoMode", new Object[]{parameter.getLuminosity()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));
        int repetitionTime = Integer.parseInt(parameter.getRepetitionTime());
        int timeInterval = Integer.parseInt(parameter.getTimeInterval());
        LocalDateTime samplingTime = LocalDateTime.now();
        LocalDateTime startTime = LocalDateTime.now();
        for (int i = 0; i < repetitionTime; i++) {

            while(repeatMiddleTime !=null && ChronoUnit.SECONDS.between(repeatMiddleTime, LocalDateTime.now()) < timeInterval) {
                log.info("时间间隔未到");
            }
            LocalDateTime time = LocalDateTime.now();
            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sampleKey);
                jsonObject.put("key", SampleTableEnum.sampleNumber.getIdentity());
                jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + i);
                jsonObject.put("value", sample.getSampleNumber() + "-" + (i + 1));
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            }
            for (String waveLength : waveLengths) {
                list.add(new Command("SetWl", new Object[]{waveLength}));
                list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
                list.add(new Command("Measure", new Object[]{}));

                String ip = infoService.getIp(deviceSerialNumber);
                R<ChangeParam> res = devMessageUtil.photometricAndRationMeasurement(list, ip);
                if (!res.getSuccess()) {
                    log.info("测量出错");
                    return R.fail("测量出错");
                }
                ChangeParam response = res.getData();
                response.setWl(waveLength + "-" + i);
                machineData.add(response);
                List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);

                FPhotometricOriginDataInfo dataInfo = dealAndSendMessage(session, waveLength, sampleKey, parameter.getLuminosity(), response, params, DataIdentityEnums.middle.getIdentity() + "-" + i, sample.getSampleNumber() + "-" + i);
                dataInfo.setMeasureTime(time);
                dataInfo.setIdentity(DataIdentityEnums.middle.getIdentity() + "-" + i);
                originDataList.add(dataInfo);
                list.clear();
                repeatMiddleTime = LocalDateTime.now();
            }
        }
        //循环测完，开始计算平均值
        String name = WaveLengthEnum.getName(parameter.getLuminosity());
        if (!StringUtils.hasText(name)){
            name = "abs";
        }

        switch (name) {
            case "Abs":
            case "T%":
            {
                Map<String, List<Double>> absValues = originDataList.stream()
                        .collect(Collectors.groupingBy(
                                FPhotometricOriginDataInfo::getWavelength,
                                Collectors.mapping(info -> Double.parseDouble(info.getData().get(info.getWavelength() + "-" + WaveLengthEnum.abs.getCode())), Collectors.toList())
                        ));

                Map<String, List<Double>> tPercentValues = originDataList.stream()
                        .collect(Collectors.groupingBy(
                                FPhotometricOriginDataInfo::getWavelength,
                                Collectors.mapping(info -> Double.parseDouble(info.getData().get(info.getWavelength() + "-" + WaveLengthEnum.tPercent.getCode())), Collectors.toList())
                        ));

                for (String waveLength : waveLengths) {
                    FPhotometricOriginDataInfo originDataInfo = new FPhotometricOriginDataInfo();
                    originDataInfo.setMode(parameter.getLuminosity());
                    originDataInfo.setWavelength(waveLength);
                    originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());

                    List<Double> abs = absValues.get(waveLength);
                    Double absV = DataCountUtil.calculateMean(abs, digit);
                    samplingTime = LocalDateTime.now();
                    Double absSd = DataCountUtil.calculateSD(abs, digit);
                    Double absRsd = DataCountUtil.calculateRSD(abs, digit);

                    List<Double> tPercent = tPercentValues.get(waveLength);
                    Double tPercentV = DataCountUtil.calculateMean(tPercent, digit);
                    Double tPercentSd = DataCountUtil.calculateSD(tPercent, digit);
                    Double tPercentRsd = DataCountUtil.calculateRSD(tPercent, digit);

                    Map<String, String> data = new HashMap<>();
                    data.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode(), String.valueOf(tPercentV));
                    data.put(waveLength + "-" + WaveLengthEnum.abs.getCode(), String.valueOf(absV));

                    Map<String, FRepeatMeasureData> map = new HashMap<>();
                    map.put(waveLength + "-" + WaveLengthEnum.abs.getCode(), new FRepeatMeasureData(absSd == null? "未检出" :String.valueOf(absSd), absRsd == null? "未检出" :String.valueOf(absRsd)));
                    map.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode(), new FRepeatMeasureData(tPercentSd == null? "未检出" : String.valueOf(tPercentSd), tPercentRsd == null ? "未检出" :String.valueOf(tPercentRsd)));

                    originDataInfo.setOtherData(map);
                    originDataInfo.setData(data);
                    originDataList.add(originDataInfo);
                    repeatSend(session, abs, sample, repetitionTime, waveLength + "-" + WaveLengthEnum.abs.getCode());
                    repeatSend(session, tPercent, sample, repetitionTime, waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                    originDataList.add(originDataInfo);
                }
            }
            break;
            case "Es":
            case "Er":
            {
                Map<String, List<Double>> erValues = originDataList.stream()
                        .collect(Collectors.groupingBy(
                                FPhotometricOriginDataInfo::getWavelength,
                                Collectors.mapping(info -> Double.parseDouble(info.getData().get(info.getWavelength() + "-" +info.getMode())), Collectors.toList())
                        ));
                for (String waveLength : waveLengths) {
                    FPhotometricOriginDataInfo originDataInfo = new FPhotometricOriginDataInfo();

                    originDataInfo.setMode(parameter.getLuminosity());
                    originDataInfo.setWavelength(waveLength);
                    originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());

                    List<Double> doubles = erValues.get(waveLength);
                    Double v = DataCountUtil.calculateMean(doubles, digit);
                    samplingTime = LocalDateTime.now();
                    Double sd = DataCountUtil.calculateSD(doubles, digit);
                    Double rsd = DataCountUtil.calculateRSD(doubles, digit);

                    FRepeatMeasureData repeatMeasureData = new FRepeatMeasureData();
                    repeatMeasureData.setSd(sd == null ? "未检出" :String.valueOf(sd));
                    repeatMeasureData.setRsd(rsd == null ? "未检出" :String.valueOf(rsd));

                    Map<String, FRepeatMeasureData> map = new HashMap<>();
                    map.put(waveLength + "-" + originDataInfo.getMode(), repeatMeasureData);

                    originDataInfo.setOtherData(map);
                    Map<String, String> data = new HashMap<>();
                    data.put(waveLength + "-" + originDataInfo.getMode(), String.valueOf(v));
                    originDataInfo.setData(data);
                    originDataList.add(originDataInfo);
                    repeatSend(session, doubles, sample, repetitionTime, waveLength + "-" + WaveLengthEnum.er.getCode());
                }
            }
            break;
            default:
                break;
        }

        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sampleKey);
            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", samplingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        FPhotometricSampleAndDataVO sampleAndDataVO = new FPhotometricSampleAndDataVO();
        sampleAndDataVO.setSampleId(sampleKey);
        sampleAndDataVO.setTaskId(task.getId());
        sampleAndDataVO.setWavelengthData(originDataList);
        sampleAndDataVO.setMachineData(machineData);
        sampleAndDataVO.setStartTime(startTime);
        sampleAndDataVO.setSampleTime(samplingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        sampleAndDataVO.setEndTime(LocalDateTime.now());

        return R.ok(sampleAndDataVO);
    }

    public void repeatSend(WebSocketSession session, List<Double> data, FMeasureSample sample, Integer repetitionTime, String key) throws IOException {

        Double v = DataCountUtil.calculateMean(data, digit);
        Double sd = DataCountUtil.calculateSD(data, digit);
        Double rsd = DataCountUtil.calculateRSD(data, digit);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sample.getId());
        repetitionTime = repetitionTime + 1;

        jsonObject.put("key", SampleTableEnum.sampleNumber.getIdentity());
        jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + repetitionTime);
        jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        jsonObject.put("key", key);
        jsonObject.put("identity", DataIdentityEnums.avg.getIdentity() + "-" + repetitionTime);
        jsonObject.put("value", String.valueOf(v));
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        jsonObject.put("key", SampleTableEnum.sampleNumber.getIdentity());
        jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 1));
        jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        jsonObject.put("key", key);
        jsonObject.put("identity", DataIdentityEnums.sd.getIdentity() + "-" + (repetitionTime + 1));
        jsonObject.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber()+ "-" + DataIdentityEnums.sd.getName());
        jsonObject.put("value", sd == null ? "未检出" :String.valueOf(sd));
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        jsonObject.put("key", SampleTableEnum.sampleNumber.getIdentity());
        jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 2));
        jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        jsonObject.put("key", key);
        jsonObject.put("identity", DataIdentityEnums.rsd.getIdentity() + "-" + (repetitionTime + 2));
        jsonObject.put(SampleTableEnum.sampleNumber.getIdentity(), sample.getSampleNumber()+ "-" + DataIdentityEnums.rsd.getName());
        jsonObject.put("value", rsd == null ? "未检出" :String.valueOf(rsd));
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        jsonObject.put("key", key);
        jsonObject.remove("identity");
        jsonObject.put("value", String.valueOf(v));

    }

    private FPhotometricOriginDataInfo dealAndSendMessage(WebSocketSession session, String waveLength, String sampleKey, String luminosity, ChangeParam response ,List<ChangeParam> params, String identity, String sampleNo) throws IOException {

        FPhotometricOriginDataInfo originDataInfo = new FPhotometricOriginDataInfo();
        originDataInfo.setMode(luminosity);
        originDataInfo.setWavelength(waveLength);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sampleKey);
        if (identity != null) {
            jsonObject.put("identity",  identity);
        }
        Map<String, String> measureData = new HashMap<>();

        if (luminosity.equals(WaveLengthEnum.er.getCode())) {
            String s = DataCountUtil.countErOrEs(response, WaveLengthEnum.er.getCode());
            measureData.put(waveLength + "-" + WaveLengthEnum.er.getCode(), s);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.er.getCode());
            jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, s));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        } else if (luminosity.equals(WaveLengthEnum.es.getCode())) {
            String s = DataCountUtil.countErOrEs(response, WaveLengthEnum.es.getCode());
            measureData.put(waveLength + "-" + WaveLengthEnum.es.getCode(), s);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.es.getCode());
            jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, s));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        } else {
            ChangeParam originRsd = new ChangeParam();
            for (ChangeParam param : params) {
                if (param.getWl().equals(waveLength)) {
                    BeanUtils.copyProperties(param, originRsd);
                    break;
                }
            }
            String tPercent = DataCountUtil.countTPercent(originRsd, response, 1.0);
            String data = DataCountUtil.changeTPercentOrAbs(null, tPercent);
            measureData.put(waveLength + "-" + WaveLengthEnum.abs.getCode(), data);
            measureData.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode(), tPercent);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
            jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, data));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
            jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, tPercent));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }
        originDataInfo.setData(measureData);
        return originDataInfo;
    }

    public R<FPhotometricSampleAndDataVO> formulaCount(WebSocketSession session, FPhotometricSampleAndDataVO vo, FMeasureSample sample, FMeasureTask task, FPhotometricParameter parameter) throws IOException {
        List<FPhotometricOriginDataInfo> originDataList = vo.getWavelengthData();

        R<List<FPhotometricCountDataInfo>> countedData = photometricService.formula(originDataList, sample, task, parameter, digit);

        if (!countedData.getSuccess()) {
            log.info(countedData.getMessage());
            sendAndClose(session, countedData.getMessage());
            return R.fail(countedData.getMessage());
        }
        List<FPhotometricCountDataInfo> countDataInfos = countedData.getData();

        for (FPhotometricCountDataInfo countDataInfo : countDataInfos) {
            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sample.getId());
                jsonObject.put("key", countDataInfo.getResultIdentity() + "-" + countDataInfo.getFormulaText());
                jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, countDataInfo.getCountValue()));
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            }
        }
        vo.setCountData(countDataInfos);
        return R.ok(vo);
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
            session.sendMessage(new TextMessage(msg));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "finish");
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            status = 0;
            session.close();
        } catch (IOException ignored) {
        }
    }

}
