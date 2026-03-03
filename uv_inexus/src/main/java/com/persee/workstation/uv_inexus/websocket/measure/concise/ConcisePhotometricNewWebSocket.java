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
import com.persee.workstation.uv_inexus.enums.measurementEnums.DataIdentityEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.SampleTableEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPhotometricParameter;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
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
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricCountDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricOriginDataInfo;
import com.persee.workstation.uv_inexus.vo.concise.photometric.FPhotometricSampleAndDataVO;
import com.persee.workstation.uv_inexus.vo.concise.ration.FRepeatMeasureData;
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
public class ConcisePhotometricNewWebSocket {

    // 所有设备连接 <sessionId, deviceSerialNumber>
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
    SamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    private DevOperationUtilService operationUtilService;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    WebSocketSendUtil webSocketSendUtil;

    @Value("1")
    private String right;

    @Value("0")
    private String error;

    @Value("0")
    private String noSave;

    private static volatile boolean isAvg = false;
    private static volatile boolean isSd = false;
    private static volatile boolean isRsd = false;

    private static volatile List<String> taskIds = new ArrayList<>();

    /**
     * 前端连接成功 WebSocket 时触发
     *
     * @param param 前端参数
     */

    public void measureTask(PhotoAndRationMetricWebsocketParam param) {

        initParams();

        // 解析参数
        String taskId = param.getTaskId();
        String sampleId = param.getSampleId();
        String deviceSerialNumber = param.getDeviceSerialNumber();
        String di = param.getDigit();
        String updateBy = param.getUpdateBy();
        digit = di == null ? 4 : Integer.parseInt(di);
        isAvg = param.getIsAvg() != null && param.getIsAvg();
        isSd = param.getIsSd() != null && param.getIsSd();
        isRsd = param.getIsRsd() != null && param.getIsRsd();

        infoService.updateDeviceCurrentStatus(deviceSerialNumber, WebSocketOperationEnum.ConcisePhotoMetric.getOperation(), Constants.DEVICE_STATUS_MEASURE);

        if (taskId == null) {
            sendError(deviceSerialNumber, "请创建任务");
            return;
        }
        FMeasureTask task = tMeasureTaskService.getById(taskId);

        if (task == null) {
            sendError(deviceSerialNumber, "任务不存在");
            return;
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
        FPhotometricParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FPhotometricParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendError(deviceSerialNumber, "缺少波长， 无法测量");
            return;
        }

        if (StringUtils.hasText(di)) {
            try {
                digit = Integer.parseInt(di);
            } catch (Exception e) {
                digit = Constants.DEFAULT_DECIMAL_NUM;
            }
        } else {
            digit = Constants.DEFAULT_DECIMAL_NUM;
        }

        if (StringUtils.hasText(sampleId)) {

            for (TaskSpecimen specimen : taskSpecimen) {
                if (!specimen.getSpecimenKey().equals(sampleId)) {
                    continue;
                }

                if (specimen.getDeleteType().equals(right)) {
                    log.info("删除的样品,不需要测量");
                    sendError(deviceSerialNumber, "删除的样品,不需要测量");
                    return;
                }
                removeOldData(taskId, sampleId);

                FMeasureSample measureSample = tMeasureSampleService.getById(sampleId);
                if (measureSample == null) {
                    sendError(deviceSerialNumber, "样品不存在");
                    return;
                }
                measureSample.setSampleLocation(null);

                R<FPhotometricSampleAndDataVO> measure;
                R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
                if (!poolInfo.getSuccess()) {
                    sendError(deviceSerialNumber, "无样品池信息");
                    return;
                }
                FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
                Integer currentPoolNum = poolInfoVO.getCurrentPoolNum();

                boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool() != null && poolInfoVO.getIsAutoChangePool();

                if (isAutoChangePool) {
                    List<FSamplePoolStatus> list = poolInfoVO.getPoolStatusList();
                    boolean poolCanUse = false;
                    for (FSamplePoolStatus poolStatus : list) {
                        if (poolStatus.getPoolNum().equals(currentPoolNum)) {
                            poolCanUse = true;
                            break;
                        }
                    }
                    if (!poolCanUse) {
                        sendError(deviceSerialNumber, "当前定位样品池不可用，请移动样品池");
                        return;
                    }
                    measureSample.setSampleLocation(currentPoolNum.toString());
                    tMeasureSampleService.updateById(measureSample);
                }


                R<Boolean> setParams = setPreMeasureParams(deviceSerialNumber, parameter);
                if (!setParams.getSuccess()) {

                    sendError(deviceSerialNumber, "参数设置异常！");
                    return;
                }

                if (parameter.getRepetitionPattern().equals("单池重复")) {
                    measure = singleSampleRepeatMeasure(measureSample, parameter, task, deviceSerialNumber);
                } else {
                    measure = singleSampleMeasure(measureSample, parameter, task, deviceSerialNumber);
                }

                if (!measure.getSuccess()) {

                    sendError(deviceSerialNumber, "测量失败");
                    return;
                }
                FPhotometricSampleAndDataVO data = measure.getData();
                R<String> measureData = photometricService.savePhotometricMeasureData(data);
                String dataId = measureData.getData();
                if (!StringUtils.hasText(dataId)) {

                    sendError(deviceSerialNumber, "数据保存异常！");
                    return;
                }
                specimen.setIsTested(right);
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
                task.setDeviceSerialNumber(deviceSerialNumber);
                tMeasureTaskService.updateById(task);

                FMeasureSample sample = tMeasureSampleService.getById(sampleId);

                if (parameter.getResultParams() != null && !parameter.getResultParams().isEmpty()) {
                    R<FPhotometricSampleAndDataVO> formulaCount = formulaCont(deviceSerialNumber, data, sample, task, parameter);
                    if (!formulaCount.getSuccess()) {

                        sendError(deviceSerialNumber, "公式计算出错，请检查公式配置！");
                        return;
                    }
                    data = formulaCount.getData();

                    photometricService.saveCountData(data, dataId);
                }
            }
            infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
        } else {

            task = tMeasureTaskService.getById(task.getId());
            taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

            List<Integer> usefulLocation = fSamplePoolConfigService.getUsefulLocation(deviceSerialNumber);
            R<FSamplePoolInfoVO> poolInfo = fSamplePoolConfigService.querySamplePoolInfo(deviceSerialNumber);
            if (!poolInfo.getSuccess()) {

                sendError(deviceSerialNumber, "无样品池信息");
                return;
            }
            FSamplePoolInfoVO poolInfoVO = poolInfo.getData();
            Boolean isAutoChangePool = poolInfoVO.getIsAutoChangePool();

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

            R<Boolean> setParams = setPreMeasureParams(deviceSerialNumber, parameter);
            if (!setParams.getSuccess()) {

                sendError(deviceSerialNumber, "参数设置异常！");
                return;
            }

            for (TaskSpecimen specimen : taskSpecimen) {
                if (!isAutoChangePool && measureCount == 1) {
                    log.info("不自动切换样品池，只测一个就结束");
                    break;
                }

                if (measureCount >= usefulPoolNum || currentPoolNum == maxPoolNum) {
                    break;
                }
                if (specimen.getDeleteType().equals(right) || specimen.getIsTested().equals(right)) {
                    System.out.println("删除或测量过的样品,不需要测量");
                    continue;
                }
                FMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());

                if (!isAutoChangePool) {
                    sample.setSampleLocation(null);
                }

                R<FPhotometricSampleAndDataVO> measure;
                if (parameter.getRepetitionPattern().equals("单池重复")) {
                    measure = singleSampleRepeatMeasure(sample, parameter, task, deviceSerialNumber);
                } else {
                    measure = singleSampleMeasure(sample, parameter, task, deviceSerialNumber);
                }

                if (isAutoChangePool) {
                    currentPoolNum = Integer.parseInt(sample.getSampleLocation());
                    fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), updateBy);
                }

                if (!measure.getSuccess()) {

                    sendError(deviceSerialNumber, measure.getMessage());
                    return;
                }
                FPhotometricSampleAndDataVO data = measure.getData();
                R<String> measureData = photometricService.savePhotometricMeasureData(data);
                String dataId = measureData.getData();
                if (!StringUtils.hasText(dataId)) {

                    sendError(deviceSerialNumber, "数据保存异常！");
                    return;
                }
                specimen.setIsTested(right);
                task.setSampleKey(JSON.toJSONString(taskSpecimen));
                tMeasureTaskService.updateById(task);
                //公式计算
                measureCount++;
                if (parameter.getResultParams() != null && !parameter.getResultParams().isEmpty()) {
                    R<FPhotometricSampleAndDataVO> formulaCount = formulaCont(deviceSerialNumber, data, sample, task, parameter);
                    if (formulaCount.getSuccess()) {
                        data = formulaCount.getData();
                        photometricService.saveCountData(data, dataId);
                    }
                }
            }
            task = tMeasureTaskService.getById(task.getId());
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
        }

        JSONObject message = new JSONObject();
        message.put("success", "测量完成，请更换样品继续测量。");
        sendMessage(deviceSerialNumber, message);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", "finish");
        sendMessage(deviceSerialNumber, jsonObject);
        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);
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
            newData.setIsSave(noSave);
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
                newAnalysisData.setIsSave(noSave);
                tMeasureAnalysisDataService.save(newAnalysisData);
            }

            log.info("当前记录没有测量数据");
        }
    }

    public R<FPhotometricSampleAndDataVO> singleSampleMeasure(FMeasureSample sample, FPhotometricParameter parameter, FMeasureTask task, String deviceSerialNumber) {
        

        String sampleKey = sample.getId();

        List<FPhotometricOriginDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();

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
        LocalDateTime startTime = LocalDateTime.now();

        for (String length : waveLengths) {
            inputParam.setOperationType(OperationTypeEnums.SingWaveLengthMeasure.getType());
            inputParam.setSingleWavelength(length);

            operation = operationUtilService.operation(inputParam);
            if (!operation.getSuccess()) {
                log.info("测量出错");
                return R.fail("测量出错！");
            }
            OperationResultEntity data = operation.getData();
            ChangeParam singMeasureResult = data.getSingMeasureResult();

            String waveLength = singMeasureResult.getWl();

            machineData.add(singMeasureResult);
            ChangeParam originRsd = new ChangeParam();
            List<ChangeParam> params = new ArrayList<>();
            if (StringUtils.hasText(task.getZeroParam())) {
                params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
                if (params != null) {
                    for (ChangeParam zero : params) {
                        if (zero.getWl().equals(waveLength)) {
                            BeanUtils.copyProperties(zero, originRsd);
                            break;
                        }
                    }
                }
            }
            //处理数据
            FPhotometricOriginDataInfo dataInfo = dealAndSendMessage(deviceSerialNumber, waveLength, sampleKey, parameter.getLuminosity(), singMeasureResult, params, null, sample.getSampleNumber());
            dataInfo.setIdentity(DataIdentityEnums.result.getIdentity());

            originDataList.add(dataInfo);
        }


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sampleKey);
        jsonObject.put("key", "samplingTime");
        jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        sendMessage(deviceSerialNumber, jsonObject);


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

    public R<FPhotometricSampleAndDataVO> singleSampleRepeatMeasure(FMeasureSample sample, FPhotometricParameter parameter, FMeasureTask task, String deviceSerialNumber) {

        try {

            String sampleKey = sample.getId();

            List<FPhotometricOriginDataInfo> originDataList = new ArrayList<>();
            List<Command> list = new ArrayList<>();
            List<ChangeParam> machineData = new ArrayList<>();

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

            int repetitionTime = Integer.parseInt(parameter.getRepetitionTime());
            double timeInterval = Double.parseDouble(parameter.getTimeInterval());
            timeInterval=timeInterval*1000;
            int interval= (int) Math.floor(timeInterval/5);

            LocalDateTime samplingTime = LocalDateTime.now();
            LocalDateTime startTime = LocalDateTime.now();
            for (int i = 0; i < repetitionTime; i++) {
                while (repeatMiddleTime != null && ChronoUnit.MILLIS.between(repeatMiddleTime, LocalDateTime.now()) < timeInterval) {
                    log.info("时间间隔未到");
                    Thread.sleep(interval);
                }
                LocalDateTime time = LocalDateTime.now();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sampleKey);
                jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
                jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + i);
                jsonObject.put("value", sample.getSampleNumber() + "-" + (i + 1));
                sendMessage(deviceSerialNumber, jsonObject);

                inputParam.setOperationType(OperationTypeEnums.SingWaveLengthMeasure.getType());
                for (String waveLength : waveLengths) {
                    inputParam.setSingleWavelength(waveLength);

                    operation = operationUtilService.operation(inputParam);

                    if (!operation.getSuccess()) {
                        log.info("测量出错");
                        return R.fail("测量出错！");
                    }
                    OperationResultEntity data = operation.getData();
                    ChangeParam measureResult = data.getSingMeasureResult();


                    machineData.add(measureResult);
                    ChangeParam originRsd = new ChangeParam();
                    List<ChangeParam> params = new ArrayList<>();
                    if (StringUtils.hasText(task.getZeroParam())) {
                        params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
                        if (params != null) {
                            for (ChangeParam zero : params) {
                                if (zero.getWl().equals(waveLength)) {
                                    BeanUtils.copyProperties(zero, originRsd);
                                    break;
                                }
                            }
                        }
                    }
                    //处理数据
                    FPhotometricOriginDataInfo dataInfo = dealAndSendMessage(deviceSerialNumber, waveLength, sampleKey, parameter.getLuminosity(), measureResult, params, DataIdentityEnums.middle.getIdentity() + "-" + i, sample.getSampleNumber());
                    dataInfo.setMeasureTime(time);
                    dataInfo.setIdentity(DataIdentityEnums.middle.getIdentity() + "-" + (i + 1));

                    originDataList.add(dataInfo);

                }

                repeatMiddleTime = LocalDateTime.now();

                JSONObject object = new JSONObject();
                object.put("sampleId", sampleKey);
                object.put("key", SampleTableEnum.SAMPLING_TIME.getIdentity());
                object.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + i);
                object.put("value", repeatMiddleTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                sendMessage(deviceSerialNumber, object);
            }
            //循环测完，开始计算平均值
            String name = WaveLengthEnum.getName(parameter.getLuminosity());
            if (!StringUtils.hasText(name)) {
                name = "abs";
            }

            switch (name) {
                case "Abs":
                case "T%": {
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
                        map.put(waveLength + "-" + WaveLengthEnum.abs.getCode(), new FRepeatMeasureData(absSd == null ? "未检出" : String.valueOf(absSd), absRsd == null ? "未检出" : String.valueOf(absRsd)));
                        map.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode(), new FRepeatMeasureData(tPercentSd == null ? "未检出" : String.valueOf(tPercentSd), tPercentRsd == null ? "未检出" : String.valueOf(tPercentRsd)));

                        originDataInfo.setOtherData(map);
                        originDataInfo.setData(data);
                        originDataList.add(originDataInfo);
                        repeatSend(deviceSerialNumber, abs, sample, repetitionTime, waveLength + "-" + WaveLengthEnum.abs.getCode());
                        repeatSend(deviceSerialNumber, tPercent, sample, repetitionTime, waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                        originDataList.add(originDataInfo);
                    }
                }
                break;
                case "Es":
                case "Er": {
                    Map<String, List<Double>> erValues = originDataList.stream()
                            .collect(Collectors.groupingBy(
                                    FPhotometricOriginDataInfo::getWavelength,
                                    Collectors.mapping(info -> Double.parseDouble(info.getData().get(info.getWavelength() + "-" + info.getMode())), Collectors.toList())
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
                        repeatMeasureData.setSd(sd == null ? "未检出" : String.valueOf(sd));
                        repeatMeasureData.setRsd(rsd == null ? "未检出" : String.valueOf(rsd));

                        Map<String, FRepeatMeasureData> map = new HashMap<>();
                        map.put(waveLength + "-" + originDataInfo.getMode(), repeatMeasureData);

                        originDataInfo.setOtherData(map);
                        Map<String, String> data = new HashMap<>();
                        data.put(waveLength + "-" + originDataInfo.getMode(), String.valueOf(v));
                        originDataInfo.setData(data);
                        originDataList.add(originDataInfo);
                        repeatSend(deviceSerialNumber, doubles, sample, repetitionTime, waveLength + "-" + parameter.getLuminosity());
                    }
                }
                break;
                default:
                    break;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sampleKey);
            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", samplingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sendMessage(deviceSerialNumber, jsonObject);


            FPhotometricSampleAndDataVO sampleAndDataVO = new FPhotometricSampleAndDataVO();
            sampleAndDataVO.setSampleId(sampleKey);
            sampleAndDataVO.setTaskId(task.getId());
            sampleAndDataVO.setWavelengthData(originDataList);
            sampleAndDataVO.setMachineData(machineData);
            sampleAndDataVO.setStartTime(startTime);
            sampleAndDataVO.setSampleTime(samplingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sampleAndDataVO.setEndTime(LocalDateTime.now());

            return R.ok(sampleAndDataVO);
        } catch (Exception e) {
            status = 0;
            return R.fail("测量出错");
        }
    }

    private R<Boolean> setPreMeasureParams(String deviceSerialNumber, FPhotometricParameter parameter) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

        SetParam param = new SetParam();
        param.setPhotoMode(Integer.valueOf(parameter.getLuminosity()));

        param.setChangeLampWl(Double.parseDouble(parameter.getChangeLampWaveLength()));
        if (StringUtils.hasText(parameter.getSpectralBandwidth()) && infoService.isNeedSetSit(deviceSerialNumber)) {
            param.setSlit1(Double.valueOf(parameter.getSpectralBandwidth()));
        }

        if (parameter.getLuminosity().equals(WaveLengthEnum.er.getCode()) || parameter.getLuminosity().equals(WaveLengthEnum.es.getCode())) {

            param.setLampPos(Integer.valueOf(parameter.getEnergyLamp()));
            param.setGain(parameter.getGain());
        }
        param.setResponseTime(Double.valueOf(parameter.getResponseTime()));
        inputParam.setParam(param);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

        if (!operation.getSuccess()) {
            return R.fail("参数设置出错！");
        }

        return R.ok(true);

    }

    public void repeatSend(String deviceSerialNumber, List<Double> data, FMeasureSample sample, Integer repetitionTime, String key) throws IOException {

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
            jsonObject.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
            jsonObject.put("value", sd == null ? "未检出" : DataCountUtil.addZero(sd, digit));
            sendMessage(deviceSerialNumber, jsonObject);
        }
        if(isRsd) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
            sendMessage(deviceSerialNumber, jsonObject);

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.rsd.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put(SampleTableEnum.SAMPLE_NUMBER.getIdentity(), sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
            jsonObject.put("value", rsd == null ? "未检出" : DataCountUtil.addZero(rsd, digit));
            sendMessage(deviceSerialNumber, jsonObject);
        }

        jsonObject.clear();
        jsonObject.put("sampleId", sample.getId());
        jsonObject.put("key", key);
        jsonObject.remove("identity");
        jsonObject.put("value", DataCountUtil.addZero(v, digit));
        sendMessage(deviceSerialNumber, jsonObject);

    }

    private FPhotometricOriginDataInfo dealAndSendMessage(String deviceSerialNumber, String waveLength, String sampleKey, String luminosity, ChangeParam response, List<ChangeParam> params, String identity, String sampleNo) {

        FPhotometricOriginDataInfo originDataInfo = new FPhotometricOriginDataInfo();
        originDataInfo.setMode(luminosity);
        originDataInfo.setWavelength(waveLength);
        String currentValue = null;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sampleKey);
        if (identity != null) {
            jsonObject.put("identity", identity);
        }
        Map<String, String> measureData = new HashMap<>();

        if (luminosity.equals(WaveLengthEnum.er.getCode())) {
            String s = DataCountUtil.countErOrEs(response, WaveLengthEnum.er.getCode());
            measureData.put(waveLength + "-" + WaveLengthEnum.er.getCode(), s);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.er.getCode());

            Double v = DataCountUtil.formatValue(digit, Double.valueOf(s));
            currentValue = DataCountUtil.addZero(v, digit);

            jsonObject.put("value", currentValue);
            sendMessage(deviceSerialNumber, jsonObject);

        } else if (luminosity.equals(WaveLengthEnum.es.getCode())) {
            String s = DataCountUtil.countErOrEs(response, WaveLengthEnum.es.getCode());
            measureData.put(waveLength + "-" + WaveLengthEnum.es.getCode(), s);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.es.getCode());

            Double v = DataCountUtil.formatValue(digit, Double.valueOf(s));
            currentValue = DataCountUtil.addZero(v, digit);

            jsonObject.put("value", currentValue);
            sendMessage(deviceSerialNumber, jsonObject);

        } else {
            ChangeParam originRsd = new ChangeParam();
            if (params != null) {
                for (ChangeParam param : params) {
                    if (param.getWl().equals(waveLength)) {
                        BeanUtils.copyProperties(param, originRsd);
                        break;
                    }
                }
            }

            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            Map<String, String> darkInfo = info.getDarkInfo();
            double z = 1.0;
            try {
                if (darkInfo != null && darkInfo.containsKey(waveLength)) {
                    z = Double.parseDouble(darkInfo.get(waveLength));
                }
            } catch (Exception e) {
                log.info("获取z值出错");
            }



            String tPercent = DataCountUtil.countTPercent(originRsd, response, z);
            String data = DataCountUtil.changeTPercentOrAbs(null, tPercent);
            measureData.put(waveLength + "-" + WaveLengthEnum.abs.getCode(), data);
            measureData.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode(), tPercent);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());

            Double v = DataCountUtil.formatValue(digit, Double.valueOf(data));
            String absData = DataCountUtil.addZero(v, digit);
            jsonObject.put("value", absData);

            sendMessage(deviceSerialNumber, jsonObject);

            jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());

            Double tp = DataCountUtil.formatValue(digit, Double.valueOf(tPercent));
            String tPercentData = DataCountUtil.addZero(tp, digit);

            jsonObject.put("value", tPercentData);
            sendMessage(deviceSerialNumber, jsonObject);

            if (luminosity.equals(WaveLengthEnum.abs.getCode())) {
                currentValue = absData;
            } else {
                currentValue = tPercentData;
            }

        }

        JSONObject currentInfo = new JSONObject();
        currentInfo.put("waveLength", waveLength + "nm");
        currentInfo.put("currentValue", DataCountUtil.formatValueWithBigDecimal(digit, currentValue) + WaveLengthEnum.getName(luminosity));
        sendMessage(deviceSerialNumber, currentInfo);

        originDataInfo.setData(measureData);
        return originDataInfo;
    }

    private static void initParams() {
        //初始化测量参数
        isAvg = false;
        isSd = false;
        isRsd = false;
        digit = 4;
    }

    public R<FPhotometricSampleAndDataVO> formulaCont(String deviceSerialNumber, FPhotometricSampleAndDataVO vo, FMeasureSample sample, FMeasureTask task, FPhotometricParameter parameter) {
        List<FPhotometricOriginDataInfo> originDataList = vo.getWavelengthData();

        R<List<FPhotometricCountDataInfo>> countedData = photometricService.formula(originDataList, sample, task, parameter, digit);

        if (!countedData.getSuccess()) {
            log.info(countedData.getMessage());
            sendError(deviceSerialNumber, "请检查公式配置");
            return R.fail(countedData.getMessage());
        }
        List<FPhotometricCountDataInfo> countDataInfos = countedData.getData();
        countDataInfos = photometricService.dataRounding(countDataInfos, parameter);

        for (FPhotometricCountDataInfo countDataInfo : countDataInfos) {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sample.getId());
            jsonObject.put("key", countDataInfo.getResultIdentity() + "-" + countDataInfo.getFormulaText());
            jsonObject.put("value", countDataInfo.getCountValue());
            sendMessage(deviceSerialNumber, jsonObject);

        }
        vo.setCountData(countDataInfos);
        return R.ok(vo);
    }
    
    private void sendMessage(String deviceSerialNumber,JSONObject object) {
        
        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.ConcisePhotoMetric.getOperation());
        vo.setData(object);
        
        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }
    
    private void sendError(String deviceSerialNumber, String message) {

        webSocketSendUtil.sendErrorMessage(deviceSerialNumber, WebSocketOperationEnum.ConcisePhotoMetric.getOperation(), message);

        infoService.updateDeviceCurrentStatus(deviceSerialNumber, Constants.DEVICE_STATUS_IDLE);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", "finish");

        WebSocketResponseVO vo = new WebSocketResponseVO();
        vo.setOperation(WebSocketOperationEnum.ConcisePhotoMetric.getOperation());
        vo.setData(jsonObject);
        webSocketSendUtil.sendMessage(deviceSerialNumber, vo);
    }




}
