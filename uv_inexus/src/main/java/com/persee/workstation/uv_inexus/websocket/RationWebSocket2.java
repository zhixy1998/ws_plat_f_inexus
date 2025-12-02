/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureData;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureSample;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationCountDataInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationFitCurveVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationWavelengthDataInfo;
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

@Component
@Slf4j
public class RationWebSocket2 extends TextWebSocketHandler {
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
    private RationService rationService;

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

        TMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());
        RationParameter parameter = JSON.parseObject(byId.getMeasurementParamInfo(), RationParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendAndClose(session, "缺少波长， 无法测量");
            return;
        }

        if (sampleId != null) {
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
                List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                List<RationDataVO> data = new ArrayList<>();

                if (parameter.getRepeatMode().equals("重复")) {
                    //重复测量，单独处理！
                    return;
                }

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
                    R<RationDataVO> r;
                    //标准样品
                    if (RationCurveParamEnums.no_differential.getIdentity().equals(parameter.getDifferentialMode())){
                        r = singleSampleSimpleMeasure(specimen, parameter, taskId, task.getMethodKey(), deviceSerialNumber, session.getId());
                    }else {
                        r = singleSampleDifferentialMeasure(specimen, parameter, taskId, task.getMethodKey(), deviceSerialNumber, session.getId());
                    }


                    if (!r.getSuccess()) {
                        sendAndClose(session, r.getMessage());
                        return;
                    }

                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                        empty = true;
                        minuend = r.getData().getCountData().get(0).getColValue();
                    }
                    specimen.setIsTested(right);
                    data.add(r.getData());
                }
                List<RationCountDataInfo> countData = new ArrayList<>();
                if (empty) {
                    for (RationDataVO dataVO : data) {
                        RationCountDataInfo yAbs = new RationCountDataInfo();
                        yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());

                        double value = dataVO.getCountData().get(0).getColValue();
                        yAbs.setColValue(value - minuend);
                        dataVO.getCountData().add(yAbs);
                        countData.add(yAbs);
                        rationService.saveRationSampleMeasureData(dataVO);

                    }
                } else {
                    for (RationDataVO dataVO : data) {
                        countData.addAll(dataVO.getCountData());
                        rationService.saveRationSampleMeasureData(dataVO);
                    }
                }
                //拟合曲线
                R<FittingReceiveInfo> infoR = rationService.fittingCurve(countData, empty, task.getMethodKey(), parameter.getWaveLengthParams());
                if (!infoR.getSuccess()) {
                    sendAndClose(session, infoR.getMessage());
                    return;
                }
                FittingReceiveInfo fittingReceiveInfo = infoR.getData();

                String fitParam = JSON.toJSONString(fittingReceiveInfo);
                task.setFittingParam(fitParam);

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
                List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                List<RationDataVO> data = new ArrayList<>();

                if (parameter.getRepeatMode().equals("重复")) {
                    //重复测量，单独处理！
                    return;
                }

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

                    R<RationDataVO> r;
                    //未知样品普通测
                    if (RationCurveParamEnums.no_differential.getIdentity().equals(parameter.getDifferentialMode())){
                        r = singleSampleSimpleMeasure(specimen, parameter, taskId, task.getMethodKey(), deviceSerialNumber, session.getId());
                    }else {
                        r = singleSampleDifferentialMeasure(specimen, parameter, taskId, task.getMethodKey(), deviceSerialNumber, session.getId());
                    }

                    if (!r.getSuccess()) {
                        sendAndClose(session, r.getMessage());
                        return;
                    }

                    if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                        empty = true;
                        minuend = r.getData().getCountData().get(0).getColValue();
                    }
                    specimen.setIsTested(right);
                    data.add(r.getData());
                }
                List<RationCountDataInfo> countData = new ArrayList<>();
                FittingReceiveInfo fittingInfo = JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
                if (empty) {
                    for (RationDataVO dataVO : data) {
                        RationCountDataInfo yAbs = new RationCountDataInfo();
                        yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());

                        double value = dataVO.getCountData().get(0).getColValue();
                        yAbs.setColValue(value - minuend);
                        dataVO.getCountData().add(yAbs);
                        String ans = rationService.valueCalculate(fittingInfo, yAbs.getColValue(), parameter.getEquationType());

                        RationCountDataInfo valueData = new RationCountDataInfo();
                        valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                        valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                        valueData.setColValue(Double.valueOf(ans));
                        dataVO.getCountData().add(valueData);
                        rationService.saveRationSampleMeasureData(dataVO);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("sampleId", dataVO.getSampleId());
                        jsonObject.put("key", valueData.getIdentity());
                        jsonObject.put("value", valueData.getColValue());
                        session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                    }
                } else {
                    for (RationDataVO dataVO : data) {
                        countData.addAll(dataVO.getCountData());
                        double value = dataVO.getCountData().get(0).getColValue();
                        String ans = rationService.valueCalculate(fittingInfo, value, parameter.getEquationType());

                        RationCountDataInfo valueData = new RationCountDataInfo();
                        valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                        valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                        valueData.setColValue(Double.valueOf(ans));
                        dataVO.getCountData().add(valueData);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("sampleId", dataVO.getSampleId());
                        jsonObject.put("key", valueData.getIdentity());
                        jsonObject.put("value", valueData.getColValue());
                        session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                        rationService.saveRationSampleMeasureData(dataVO);
                    }
                }



            } else {
                sendAndClose(session, "请选择样品类型");
                return;
            }

            List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            List<RationDataVO> data = new ArrayList<>();




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
    }



    //单个样品普通不微分测量
    public R<RationDataVO> singleSampleSimpleMeasure(TaskSpecimen specimen, RationParameter parameter, String taskId, String methodId, String deviceSerialNumber, String sessionId) throws IOException {

        WebSocketSession session = sessions.get(sessionId);

        String sampleKey = specimen.getSpecimenKey();
        TMeasureSample sample = tMeasureSampleService.getById(sampleKey);
        TMeasureTask task = tMeasureTaskService.getById(taskId);

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

            String ip = infoService.getIp(deviceSerialNumber);
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
                jsonObject.put("sampleId", sampleKey);
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
        dataInfo.setTaskId(taskId);
        dataInfo.setSampleId(sampleKey);
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
            jsonObject.put("sampleId", sampleKey);
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
    public R<RationDataVO> singleSampleDifferentialMeasure(TaskSpecimen specimen, RationParameter parameter, String taskId, String methodId, String deviceSerialNumber, String sessionId){
        // 根据sessionId获取session
        WebSocketSession session = sessions.get(sessionId);

        String specimenKey = specimen.getSpecimenKey();
        TMeasureSample sample = tMeasureSampleService.getById(specimenKey);
        TMeasureTask task = tMeasureTaskService.getById(taskId);
        List<ChangeParam> zeroParams = JSON.parseArray(task.getZeroParam(), ChangeParam.class);



        // 设置参数
        List<Command> list = new ArrayList<>();
        // 设置光度模式
        list.add(new Command(SetParamEnum.SetPhotoMode.getComId(), new Object[]{WaveLengthEnum.abs.getCode()}));
        list.add(new Command(SetParamEnum.SetResponseTime.getComId(), new Object[]{parameter.getResponseTime()}));
        list.add(new Command(SetParamEnum.SetCellPos.getComId(), new Object[]{sample.getSampleLocation()}));
        list.add(new Command(SetParamEnum.SetScanWlInterval.getComId(), new Object[]{1}));

        List<List<String>> originDataList = new ArrayList<>();
        for (String waveLength : parameter.getWaveLength()) {
            List<String> params = new ArrayList<>();
            list.add(new Command(SetParamEnum.SetStartWl.getComId(), new Object[]{Double.valueOf(waveLength)+5}));
            list.add(new Command(SetParamEnum.SetEndWl.getComId(), new Object[]{Double.valueOf(waveLength)-5}));
            String ip = infoService.getIp(deviceSerialNumber);
            R<String> stringR = devMessageUtil.timeSweepSet(list, ip);
            if (!stringR.getSuccess()) {
                log.info("设置出错");
                return R.fail("设置出错");
            }

            List<ChangeParam> changeParams = devMessageUtil.getQuantitativeDifferentialMeasureData(new Command(SetParamEnum.Scan.getComId(), new Object[]{}), ip);

            if (changeParams == null) {
                log.info("测量出错");
                return R.fail("测量出错");
            }
            for (int i = 0; i < changeParams.size(); i++) {
                String tPercent = DataCountUtil.countTPercent(zeroParams.get(0),changeParams.get(0), 1.0);
                String data = DataCountUtil.changeTPercentOrAbs2(null, tPercent);
                params.add(data);
                zeroParams.remove(0);
                changeParams.remove(0);
            }


        }

        return R.ok();
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
     * @param status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
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
            session.close();
        } catch (IOException ignored) {}
    }
}
