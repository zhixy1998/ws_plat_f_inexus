package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
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
public class RationWebSocket extends TextWebSocketHandler {
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();

    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    RationService rationService;

    @Autowired
    TMeasureSampleService tMeasureSampleService;

    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    DeviceInfoService infoService;

    @Value("1")
    private String right;

    @Value("0")
    private String error;

    private static volatile int status = 0;
    /**
     * 前端连接成功 WebSocket 时触发
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
        String sampleId= getQueryParam(query, "sampleId");
        String deviceSerialNumber= getQueryParam(query, "deviceSerialNumber");
        String sampleKind= getQueryParam(query, "sampleKind");

        if (deviceSerialNumber == null) {
            sendAndClose(session, "未连接仪器");
            return;
        }

        if (taskId == null) {
            sendAndClose(session, "请创建任务后再测量");
            return;
        }
        if(!StringUtils.hasText(sampleKind)){
            sendAndClose(session, "请选择样品类型");
            return;
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            sendAndClose(session, "任务或样品不存在");
            return;
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        TMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());
        RationParameter parameter = JSON.parseObject(byId.getMeasurementParamInfo(), RationParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendAndClose(session, "缺少波长， 无法测量");
            return;
        }

        String ip = infoService.getIp(deviceSerialNumber);


        if (StringUtils.hasText(sampleId)) {
            //单样品重测
            LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TMeasureData::getSampleKey, sampleId)
                    .eq(TMeasureData::getTaskId, taskId);
            List<TMeasureData> list = tMeasureDataService.list(queryWrapper);
            if (list != null || !list.isEmpty()) {
                //认为是重测，覆盖数据
                log.info("有数据，重测");
            }

            return;
        } else {

            if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {

                List<RationDataVO> data = new ArrayList<>();
                List<Double> emptyData = new ArrayList<>();

                List<String> inputParam = new ArrayList<>();

                Map<String, String> points = new HashMap<>();



                boolean empty = false;
                double minuend = 0;
                List<RationCountDataInfo> countData = new ArrayList<>();
                for (TaskSpecimen specimen : taskSpecimen) {

                    if (specimen.getDeleteType().equals(right)) {
                        System.out.println("删除的样品,不需要测量");
                        continue;
                    }

                    TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

                    if (specimen.getIsTested().equals(right)) {
                        log.info("已测的样品，获取之前测量数据，为了拟合曲线");
                        LambdaQueryWrapper<TMeasureAnalysisData> queryWrapper = new LambdaQueryWrapper<>();
                        queryWrapper.eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                                .eq(TMeasureAnalysisData::getTaskId, taskId)
                                .orderByDesc(TMeasureAnalysisData::getAnalysisName).last("limit 1");
                        TMeasureAnalysisData one = tMeasureAnalysisDataService.getOne(queryWrapper);
                        if (one != null) {
                            List<RationCountDataInfo> array = JSON.parseArray(one.getAnalysisResult(), RationCountDataInfo.class);
                            if (array != null && !array.isEmpty()) {
                                List<RationCountDataInfo> absDataList = array.stream()
                                        .filter(arr -> SampleTableEnum.Abs.getIdentity().equals(arr.getIdentity())
                                                || SampleTableEnum.sumAbs.getIdentity().equals(arr.getIdentity()))
                                        .collect(Collectors.toList());
                                countData.addAll(absDataList);
                            }
                        }
                        continue;
                    }

                    if (!sample.getSampleKind().equals(sampleKind)) {
                        log.info("不是需要的样品，跳过");
                        continue;
                    }
                    R<RationDataVO> r;

                    if (parameter.getRepeatMode().equals("单池重复")) {
                        //重复测量，单独处理！
                        r = singleSampleRepeatMeasure(sample, parameter, task, ip, session.getId());

                    } else {
                        r = singleSampleSimpleMeasure(sample, parameter, task, ip, session.getId());
                    }

                    if (!r.getSuccess()) {
                        sendAndClose(session, r.getMessage());
                        return;
                    }
                    inputParam.add(sample.getNormalConcentration());
                    points.put(sample.getId(), sample.getNormalConcentration());

                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                        empty = true;
                        minuend = r.getData().getCountData().get(0).getColValue();
                        emptyData.add(minuend);
                    }
                    specimen.setIsTested(right);
                    data.add(r.getData());
                }

                if (empty) {
                    for (RationDataVO dataVO : data) {
                        RationCountDataInfo yAbs = new RationCountDataInfo();
                        yAbs.setIdentity(SampleTableEnum.sumAbs.getIdentity());

                        double value = dataVO.getCountData().get(0).getColValue();

                        double sum = 0;
                        if (emptyData.size() > 1) {
                            for (Double emptyDatum : emptyData) {
                                sum += emptyDatum;
                            }
                            minuend = sum / (emptyData.size() - 1);
                        }

                        yAbs.setColValue(value - minuend);
                        dataVO.getCountData().add(yAbs);
                        countData.add(yAbs);
                        JSONObject point = new JSONObject();
                        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
                            point.put("x", yAbs.getColValue());
                            point.put("y", points.get(dataVO.getSampleId()));
                        } else {
                            point.put("x", points.get(dataVO.getSampleId()));
                            point.put("y", yAbs.getColValue());
                        }
                        if (session.isOpen()) {
                            JSONObject payload = new JSONObject();
                            payload.put("point", point);
                            payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
                            session.sendMessage(new TextMessage(payload.toJSONString()));
                        }

                        rationService.saveStandSampleData(dataVO);

                    }
                } else {
                    for (RationDataVO dataVO : data) {
                        countData.addAll(dataVO.getCountData());
                        JSONObject point = new JSONObject();
                        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
                            point.put("x", dataVO.getCountData().get(0).getColValue());
                            point.put("y", points.get(dataVO.getSampleId()));
                        } else {
                            point.put("x", points.get(dataVO.getSampleId()));
                            point.put("y", dataVO.getCountData().get(0).getColValue());
                        }
                        if (session.isOpen()) {
                            JSONObject payload = new JSONObject();
                            payload.put("point", point);
                            payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
                            session.sendMessage(new TextMessage(payload.toJSONString()));
                        }
                        rationService.saveStandSampleData(dataVO);
                    }
                }
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
//                tMeasureTaskService.updateById(task);
                //拟合曲线
                R<FittingReceiveInfo> infoR = rationService.fittingCurve(countData, empty, task.getMethodKey(), inputParam);

                if (!infoR.getSuccess()) {
                    sendAndClose(session, infoR.getMessage());
                    return;
                }
                FittingReceiveInfo fittingReceiveInfo = infoR.getData();
                String fitParam = JSON.toJSONString(fittingReceiveInfo);
                task.setFittingParam(fitParam);
                tMeasureTaskService.updateById(task);

                //绘图
                RationFitCurveVO curveData = rationService.curveData(fittingReceiveInfo);

                if (session.isOpen()) {
                    JSONObject payload = new JSONObject();
                    payload.put("R2", curveData.getR2());
                    payload.put("dataList", curveData.getDataList());
                    payload.put("curveType", curveData.getCurveType());
                    payload.put("equation", curveData.getEquation());
                    payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
                    payload.put("coefficientList", curveData.getCoefficientList());
                    session.sendMessage(new TextMessage(payload.toJSONString()));
                }


            } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
                List<RationDataVO> data = new ArrayList<>();
                List<Double> emptyData = new ArrayList<>();

                boolean empty = false;
                double minuend = 0;
                for (TaskSpecimen specimen : taskSpecimen) {

                    if (specimen.getDeleteType().equals(right) || specimen.getIsTested().equals(right)) {
                        System.out.println("删除的样品,不需要测量");
                        continue;
                    }

                    TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

                    if (!sample.getSampleKind().equals(sampleKind)) {
                        log.info("不是需要的样品，跳过");
                        continue;
                    }
                    //未知样品普通测
                    R<RationDataVO> r;
                    if (parameter.getRepeatMode().equals("单池重复")) {
                        //重复测量，单独处理！
                        r = singleSampleRepeatMeasure(sample, parameter, task, ip, session.getId());

                    } else {
                        r = singleSampleSimpleMeasure(sample, parameter, task, ip, session.getId());
                    }
                    if (!r.getSuccess()) {
                        sendAndClose(session, r.getMessage());
                        return;
                    }

                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                        empty = true;
                        minuend = r.getData().getCountData().get(0).getColValue();
                        emptyData.add(minuend);
                    }
                    specimen.setIsTested(right);
                    data.add(r.getData());
                }
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
//                tMeasureTaskService.updateById(task);
                List<RationCountDataInfo> countData = new ArrayList<>();
                FittingReceiveInfo fittingInfo = null;
                if (StringUtils.hasText(task.getFittingParam())) {
                     fittingInfo= JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
                }

                if (empty) {
                    for (RationDataVO dataVO : data) {
                        RationCountDataInfo yAbs = new RationCountDataInfo();
                        yAbs.setIdentity(SampleTableEnum.sumAbs.getIdentity());

                        double value = dataVO.getCountData().get(0).getColValue();
                        double sum = 0;
                        if (emptyData.size() > 1) {
                            for (Double emptyDatum : emptyData) {
                                sum += emptyDatum;
                            }
                            minuend = sum / (emptyData.size() - 1);
                        }

                        yAbs.setColValue(value - minuend);
                        dataVO.getCountData().add(yAbs);
                        String ans = null;
                        if (fittingInfo != null) {
                            ans = rationService.valueCalculate(fittingInfo, yAbs.getColValue(), parameter.getEquationType());
                        }

                        RationCountDataInfo valueData = new RationCountDataInfo();
                        valueData.setHeadName(SampleTableEnum.fitConcentration.getName());
                        valueData.setIdentity(SampleTableEnum.fitConcentration.getIdentity());
                        valueData.setColValue(ans==null? null : Double.valueOf(ans));
                        dataVO.getCountData().add(valueData);
                        rationService.saveUnknowSampleData(dataVO);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("sampleId", dataVO.getSampleId());
                        jsonObject.put("key", valueData.getIdentity());
                        jsonObject.put("value", valueData.getColValue());
                        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                        JSONObject point = new JSONObject();
                        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
                            point.put("x", yAbs.getColValue());
                            point.put("y", ans);
                        } else {
                            point.put("x", ans);
                            point.put("y", yAbs.getColValue());
                        }
                        if (session.isOpen()) {
                            JSONObject payload = new JSONObject();
                            payload.put("point", point);
                            payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
                            session.sendMessage(new TextMessage(payload.toJSONString()));
                        }

                    }
                } else {
                    for (RationDataVO dataVO : data) {
                        countData.addAll(dataVO.getCountData());
                        double value = dataVO.getCountData().get(0).getColValue();
                        String ans = null;
                        if (fittingInfo != null) {
                            ans = rationService.valueCalculate(fittingInfo, value, parameter.getEquationType());
                        }

                        RationCountDataInfo valueData = new RationCountDataInfo();
                        valueData.setHeadName(SampleTableEnum.fitConcentration.getName());
                        valueData.setIdentity(SampleTableEnum.fitConcentration.getIdentity());
                        valueData.setColValue(ans==null? null : Double.valueOf(ans));
                        dataVO.getCountData().add(valueData);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("sampleId", dataVO.getSampleId());
                        jsonObject.put("key", valueData.getIdentity());
                        jsonObject.put("value", valueData.getColValue());
                        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                        JSONObject point = new JSONObject();
                        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
                            point.put("x", value);
                            point.put("y", ans);
                        } else {
                            point.put("x", ans);
                            point.put("y", value);
                        }
                        if (session.isOpen()) {
                            JSONObject payload = new JSONObject();
                            payload.put("point", point);
                            payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
                            session.sendMessage(new TextMessage(payload.toJSONString()));
                        }
                        rationService.saveStandSampleData(dataVO);
                    }
                }



            } else {
                sendAndClose(session, "请选择样品类型");
                return;
            }

            List<RationDataVO> data = new ArrayList<>();


            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", "finish");
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            }
            sessions.remove(session.getId());
            session.close();
            status = 0;
        }
    }



    //单个样品普通不微分测量
    public R<RationDataVO> singleSampleSimpleMeasure(TMeasureSample sample, RationParameter parameter, TMeasureTask task, String ip, String sessionId) throws IOException {

        WebSocketSession session = sessions.get(sessionId);

        List<RationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<RationCountDataInfo> countData = new ArrayList<>();
        RationDataVO dataInfo = new RationDataVO();

        List<String> waveLengths = parameter.getWaveLength();

        list.add(new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));
        LocalDateTime startTime = LocalDateTime.now();
        for (String waveLength : waveLengths) {
            list.add(new Command("SetWl", new Object[]{waveLength}));
            list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
            list.add(new Command("Measure", new Object[]{}));

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
            String tPercent = DataCountUtil.countTPercent(originRsd, response, 1.0);
            String data = DataCountUtil.changeTPercentOrAbs(null, tPercent);

            RationWavelengthDataInfo originDataInfo = new RationWavelengthDataInfo();
            originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());
            originDataInfo.setWavelength(waveLength);

            originDataInfo.setAbsValue(data);
            originDataInfo.setTPercentValue(tPercent);

            originDataList.add(originDataInfo);

            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sample.getId());
                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                jsonObject.put("value", originDataInfo.getAbsValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                jsonObject.put("value", originDataInfo.getTPercentValue());
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

        R<RationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter);
        if (!absValue.getSuccess()) {
            log.info("计算出错");
            sendAndClose(session, absValue.getMessage());
            return R.fail("计算出错");
        }
        RationCountDataInfo data = absValue.getData();
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
    public R<Boolean> singleSampleDifferentialMeasure(){
        return R.ok();
    }

    public R<RationDataVO> singleSampleRepeatMeasure(TMeasureSample sample, RationParameter parameter, TMeasureTask task, String ip, String sessionId) throws IOException {
        int repeatCount = Integer.parseInt(parameter.getRepeatCount());

        WebSocketSession session = sessions.get(sessionId);

        List<RationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<RationCountDataInfo> countData = new ArrayList<>();
        RationDataVO ans = new RationDataVO();

        List<String> waveLengths = parameter.getWaveLength();

        LocalDateTime startTime = LocalDateTime.now();
        list.add(new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));

        for (int i = 0; i < repeatCount; i++) {
            for (String waveLength : waveLengths) {
                list.add(new Command("SetWl", new Object[]{waveLength}));
                list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
                list.add(new Command("Measure", new Object[]{}));

                R<ChangeParam> res = devMessageUtil.photometricAndRationMeasurement(list, ip);
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

                RationWavelengthDataInfo originDataInfo = new RationWavelengthDataInfo();
                originDataInfo.setIdentity(DataIdentityEnums.middle.getIdentity() + "-" + i);
                originDataInfo.setWavelength(waveLength);

                originDataInfo.setAbsValue(data);
                originDataInfo.setTPercentValue(tPercent);

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
        Map<String, Double> avgAbsValues = originDataList.stream()
                .collect(Collectors.groupingBy(
                        RationWavelengthDataInfo::getWavelength,
                        Collectors.averagingDouble(info -> Double.parseDouble(info.getAbsValue()))
                ));
        Map<String, Double> tPercentValues = originDataList.stream()
                .collect(Collectors.groupingBy(
                        RationWavelengthDataInfo::getWavelength,
                        Collectors.averagingDouble(info -> Double.parseDouble(info.getTPercentValue()))
                ));
        for (String waveLength : waveLengths) {
            RationWavelengthDataInfo info = new RationWavelengthDataInfo();
            info.setWavelength(waveLength);
            info.setAbsValue(String.valueOf(avgAbsValues.get(waveLength)));
            info.setIdentity("result");
            info.setTPercentValue(String.valueOf(tPercentValues.get(waveLength)));

            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sample.getId());
                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                jsonObject.put("value", info.getAbsValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                jsonObject.put("value", info.getTPercentValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            }

            originDataList.add(info);
        }
        LocalDateTime endTime = LocalDateTime.now();

        R<RationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter);
        if (!absValue.getSuccess()) {
            return R.fail(absValue.getMessage());
        }
        RationCountDataInfo data = absValue.getData();
        countData.add(data);
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
        ans.setTaskId(task.getId());
        ans.setSampleId(sample.getId());
        ans.setStartTime(task.getStartTime());
        ans.setEndTime(task.getEndTime());
        ans.setCountData(countData);
        ans.setEndTime(endTime);
        ans.setMachineData(machineData);
        ans.setWavelengthData(originDataList);

        return R.ok(ans);

    }

    public List<RationEmptyInfo> emptySampleCheck(List<TaskSpecimen> specimenList, String taskId, String deviceIp, String sampleKind, RationParameter parameter) {
        //查找空白样，然后测量，计算，最后返回一个空白样的信息，id，结果，，空白样测量完直接存放，区分标准和未知样，标准样的数据对未知样影响

        if (parameter.getRepeatMode().equals("重复")) {
            //重复单独处理
        }
        if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
            for (TaskSpecimen specimen : specimenList) {

                if (specimen.getDeleteType().equals(right)) {
                    log.info("跳过删除的样品");
                    continue;
                }

                if (!specimen.getSampleKind().equals(sampleKind) || !specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity()) ) {
                    log.info("跳过非空白样和非标准样品");
                    continue;
                }

//                R<RationDataVO> res = singleSampleSimpleMeasure(specimen, parameter, tMeasureTaskService.getById(taskId), deviceIp, session.getId());

            }
        } else {

        }

        return null;

    }

    /**
     * 收到前端消息时触发
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
        } catch (IOException ignored) {}
    }
}
