package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.PhotometricParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.DataIdentityEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.measurementPo.*;
import com.persee.workstation.uv_inexus.po.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.measurementService.*;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.PhotometricSampleAndDataVO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PhotometricWebSocket extends TextWebSocketHandler {

    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();

    private static volatile int status = 0;

    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    private PhotometricService photometricService;

    @Autowired
    private TMeasureSampleService tMeasureSampleService;

    @Autowired
    private TMeasureAnalysisDataService tMeasureAnalysisDataService;

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
        if (deviceSerialNumber == null) {
            sendAndClose(session, "未连接仪器");
            return;
        }

        if (taskId == null) {
            sendAndClose(session, "请创建任务");
            return;
        }
        TMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            sendAndClose(session, "任务或样品不存在");
            return;
        }
        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        PhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), PhotometricParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendAndClose(session, "缺少波长， 无法测量");
            return;
        }

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

            singleSampleMeasure(specimen, parameter, task, deviceSerialNumber, session.getId());

        } else {
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(right) || specimen.getIsTested().equals(right)) {
                    System.out.println("删除的样品,不需要测量");
                    continue;
                }
                R<Boolean> r = singleSampleMeasure(specimen, parameter, task, deviceSerialNumber, session.getId());
                if (!r.getSuccess()) {
                    sendAndClose(session, "测量失败");
                    return;
                }
                specimen.setIsTested(right);
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
        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureData::getTaskId, taskId).eq(TMeasureData::getSampleKey, sampleId)
                .orderByDesc(TMeasureData::getSerialNumber).last("limit 1");
        TMeasureData data = tMeasureDataService.getOne(queryWrapper);
        if (data != null) {
            TMeasureData newData = new TMeasureData();

            newData.setSampleKey(sampleId);
            newData.setTaskId(taskId);
            newData.setIsDelete(error);
            newData.setRemark(data.getRemark());

            newData.setSerialNumber(data.getSerialNumber() + 1);
            tMeasureDataService.save(newData);
        }

        LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
        analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                .eq(TMeasureAnalysisData::getSampleKey, sampleId);
        TMeasureAnalysisData analysisData = tMeasureAnalysisDataService.getOne(analysisDataQueryWrapper);
        if (analysisData != null) {
            tMeasureAnalysisDataService.remove(analysisDataQueryWrapper);
            System.out.println("删除数据");
        }
    }

    public R<Boolean> singleSampleMeasure(TaskSpecimen specimen, PhotometricParameter parameter, TMeasureTask task, String deviceSerialNumber, String sessionId) throws IOException {

        WebSocketSession session = sessions.get(sessionId);

        String sampleKey = specimen.getSpecimenKey();
        TMeasureSample sample = tMeasureSampleService.getById(sampleKey);

        List<PhotometricOriginDataInfo> originDataList = new ArrayList<>();
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
            PhotometricOriginDataInfo dataInfo = dealAndSendMessage(session, waveLength, sampleKey, parameter.getLuminosity(), response, params, null);
            originDataList.add(dataInfo);
            list.clear();
        }


        R<List<PhotometricCountDataInfo>> countedData = photometricService.formula(originDataList, sample, task, parameter);

        if (!countedData.getSuccess()) {
            log.info("计算出错");
            sendAndClose(session, "计算出错");
            return R.fail("计算出错");
        }
        List<PhotometricCountDataInfo> countDataInfos = countedData.getData();

        for (PhotometricCountDataInfo countDataInfo : countDataInfos) {
            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sampleKey);
                jsonObject.put("key", countDataInfo.getResultIdentity() + "-" + countDataInfo.getFormulaText());
                jsonObject.put("value", countDataInfo.getCountValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            }
        }

        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sampleKey);
            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        LocalDateTime endTime = LocalDateTime.now();

        PhotometricSampleAndDataVO sampleAndDataVO = new PhotometricSampleAndDataVO();
        sampleAndDataVO.setTaskId(task.getId());
        sampleAndDataVO.setSampleId(sampleKey);
        sampleAndDataVO.setWavelengthData(originDataList);
        sampleAndDataVO.setCountData(countDataInfos);
        sampleAndDataVO.setMachineData(machineData);
        sampleAndDataVO.setStartTime(startTime);
        sampleAndDataVO.setEndTime(endTime);
        sampleAndDataVO.setSampleTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        photometricService.savePhotometricMeasureData(sampleAndDataVO);
        return R.ok();
    }



    public R<PhotometricSampleAndDataVO> singleSampleRepeatMeasure(TaskSpecimen specimen, PhotometricParameter parameter, TMeasureTask task, String deviceSerialNumber, String sessionId) throws IOException {

        WebSocketSession session = sessions.get(sessionId);

        String sampleKey = specimen.getSpecimenKey();
        TMeasureSample sample = tMeasureSampleService.getById(sampleKey);

        List<PhotometricOriginDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();

        List<String> waveLengths = parameter.getWaveLength();

        list.add(new Command("SetPhotoMode", new Object[]{parameter.getLuminosity()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));
        LocalDateTime startTime = LocalDateTime.now();
        for (int i = 0; i < Integer.parseInt(parameter.getRepetitionTime()); i++) {

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

                PhotometricOriginDataInfo dataInfo = dealAndSendMessage(session, waveLength, sampleKey, parameter.getLuminosity(), response, params, DataIdentityEnums.middle.getIdentity()+ i);

                originDataList.add(dataInfo);
                list.clear();
            }
        }
        //循环测完，开始计算平均值
        if (parameter.getLuminosity().equals(WaveLengthEnum.er.getCode())) {
            Map<String, Double> erValues = originDataList.stream()
                    .collect(Collectors.groupingBy(
                            PhotometricOriginDataInfo::getWavelength,
                            Collectors.averagingDouble(info -> Double.parseDouble(info.getErValue()))
                    ));
            for (String waveLength : waveLengths) {
                PhotometricOriginDataInfo originDataInfo = new PhotometricOriginDataInfo();
                originDataInfo.setMode(parameter.getLuminosity());
                originDataInfo.setWavelength(waveLength);
                originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());
                originDataInfo.setErValue(String.valueOf(erValues.get(waveLength)));

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sampleKey);

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.er.getCode());
                jsonObject.put("value", originDataInfo.getErValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                originDataList.add(originDataInfo);
            }

        } else if (parameter.getLuminosity().equals(WaveLengthEnum.es.getCode())) {
            Map<String, Double> esValues = originDataList.stream()
                    .collect(Collectors.groupingBy(
                            PhotometricOriginDataInfo::getWavelength,
                            Collectors.averagingDouble(info -> Double.parseDouble(info.getEsValue()))
                    ));
            for (String waveLength : waveLengths) {
                PhotometricOriginDataInfo originDataInfo = new PhotometricOriginDataInfo();
                originDataInfo.setMode(parameter.getLuminosity());
                originDataInfo.setWavelength(waveLength);
                originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());
                originDataInfo.setEsValue(String.valueOf(esValues.get(waveLength)));

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sampleKey);

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.es.getCode());
                jsonObject.put("value", originDataInfo.getEsValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                originDataList.add(originDataInfo);
            }
        } else {
            Map<String, Double> avgAbsValues = originDataList.stream()
                    .collect(Collectors.groupingBy(
                            PhotometricOriginDataInfo::getWavelength,
                            Collectors.averagingDouble(info -> Double.parseDouble(info.getAbsValue()))
                    ));
            Map<String, Double> tPercentValues = originDataList.stream()
                    .collect(Collectors.groupingBy(
                            PhotometricOriginDataInfo::getWavelength,
                            Collectors.averagingDouble(info -> Double.parseDouble(info.getTPercentValue()))
                    ));

            for (String waveLength : waveLengths) {
                PhotometricOriginDataInfo originDataInfo = new PhotometricOriginDataInfo();
                originDataInfo.setMode(parameter.getLuminosity());
                originDataInfo.setWavelength(waveLength);
                originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());
                originDataInfo.setAbsValue(String.valueOf(avgAbsValues.get(waveLength)));
                originDataInfo.setTPercentValue(String.valueOf(tPercentValues.get(waveLength)));

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sampleKey);

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                jsonObject.put("value", originDataInfo.getAbsValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                jsonObject.put("value", originDataInfo.getTPercentValue());
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                originDataList.add(originDataInfo);
            }
        }
        PhotometricSampleAndDataVO sampleAndDataVO = new PhotometricSampleAndDataVO();
        sampleAndDataVO.setSampleId(sampleKey);
        sampleAndDataVO.setTaskId(task.getId());
        sampleAndDataVO.setWavelengthData(originDataList);
        sampleAndDataVO.setMachineData(machineData);
        sampleAndDataVO.setStartTime(startTime);
        sampleAndDataVO.setEndTime(LocalDateTime.now());

        return R.ok(sampleAndDataVO);
    }

    private PhotometricOriginDataInfo dealAndSendMessage(WebSocketSession session, String waveLength, String sampleKey, String luminosity, ChangeParam response ,List<ChangeParam> params, String identity) throws IOException {

        PhotometricOriginDataInfo originDataInfo = new PhotometricOriginDataInfo();
        originDataInfo.setMode(luminosity);
        originDataInfo.setWavelength(waveLength);
        originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sampleKey);
        if (identity != null) {
            jsonObject.put("identity",  identity);
        }

        if (luminosity.equals(WaveLengthEnum.er.getCode())) {
            String s = DataCountUtil.countErOrEs(response, WaveLengthEnum.er.getCode());
            originDataInfo.setErValue(s);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.er.getCode());
            jsonObject.put("value", originDataInfo.getErValue());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

        } else if (luminosity.equals(WaveLengthEnum.es.getCode())) {
            String s = DataCountUtil.countErOrEs(response, WaveLengthEnum.es.getCode());
            originDataInfo.setEsValue(s);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.es.getCode());
            jsonObject.put("value", originDataInfo.getEsValue());
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
            originDataInfo.setAbsValue(data);
            originDataInfo.setTPercentValue(tPercent);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
            jsonObject.put("value", originDataInfo.getAbsValue());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
            jsonObject.put("value", originDataInfo.getTPercentValue());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }
        return originDataInfo;
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
