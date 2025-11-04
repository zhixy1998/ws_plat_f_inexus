package com.persee.workstation.uv_inexus.websocket.liveDetection;

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

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceConnectMessageEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@Controller
public class LiveDetectionWebSocket {

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    DeviceInfoService infoService; // 你现有的 service

    @Autowired
    private MessageUtil messageUtil;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    @Autowired
    FormulaUtil formulaUtil;

//    @Autowired
//    private ExperimentTestService experimentService;

    @Value("${device.default.T600xmlPath}")
    String T600xmlPath;

    @Value("${device.default.T800xmlPath}")
    String T800xmlPath;

    @Value("/topic/detection")
    String measureTopic;

    private static volatile boolean status = true;


    @MessageMapping("/detection/start")
    public void startMeasure(String deviceSerialNumber) {
        if (deviceSerialNumber == null || deviceSerialNumber.isEmpty()) {
            messagingTemplate.convertAndSend(measureTopic,
                    "缺少 deviceSerialNumber 参数");
            return;
        }
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);

        if (info == null || info.getIp() == null) {
            messagingTemplate.convertAndSend(measureTopic,
                    "设备未连接: " + deviceSerialNumber);
            return;
        }
        String ip = info.getIp();

        if (info.getIsActivated() == null || info.getIsActivated() == 0) {
            messagingTemplate.convertAndSend(measureTopic,
                    "设备未激活: " + deviceSerialNumber);
            return;
        }

        if (status) {
            // 启动发送接收线程
            this.startMeasureLoop(deviceSerialNumber);
            status = false;
        }

        // 主动推送通知给前端
        messagingTemplate.convertAndSend(measureTopic,
                "开始测量: " + deviceSerialNumber + ", ip=" + ip);
    }


    public R<Boolean> startMeasureLoop(String deviceSerialNumber) {
        String deviceIp = infoService.getIp(deviceSerialNumber);
//        R<DatagramSocket> re = messageUtil.getSocket();
//        if (!re.getSuccess()) {
//            log.print("绑定 socket 失败: {}");
//            return R.fail("绑定 socket 失败，请检查网线接口");
//        }

        Thread measureThread = new Thread(() -> {
            try {
                // 获取 socket
                // 循环发送
                while (Constants.Running) {

                    while (!DetectionConstants.isDeviceIdle) {
                        Thread.sleep(1000);
                    }
                    DetectionConstants.isDeviceIdle = false;
                    R<DatagramSocket> news = messageUtil.getSocket();
                    if (!news.getSuccess()) {
                        log.print("绑定 socket 失败: {}");
                        messagingTemplate.convertAndSend(measureTopic, "请检查网口连接！");
                        throw new RuntimeException("绑定 socket 失败，请检查网线接口");
                    }
                    DatagramSocket socket = news.getData();

                    try {
                        Map<String, Transition> xml = protocolItemUtil.loadFromXml(T600xmlPath);
                        DetectionResponseInfoVO vo = new DetectionResponseInfoVO();

                        DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
                        vo.setAttachmentName(attachmentInfo.getAttachmentName());
                        vo.setAttachmentKind(attachmentInfo.getAttachmentKind());
                        vo.setTotalPoolNum(attachmentInfo.getSamplePoolNum());

                        for (int i = 1; i <= LiveDetectionCmdEnum.getLength(); i++) {
                            LiveDetectionCmdEnum anEnum = LiveDetectionCmdEnum.getEnum(i);

                            String comId = anEnum.getComId();

                            Object[] objects =anEnum.getParam() == null ? new Object[]{} : new Object[] {anEnum.getParam()};

                            Integer count = anEnum.getCount();

                            byte[] sendBytes = protocolItemUtil.calcSendBytes(objects, xml.get(comId));

                            List<byte[]> responses = messageUtil.detectionSendAndReceive(socket, deviceIp, comId, sendBytes, count);
                            Object parseResult = ResponseDeal.parse(comId, responses, sendBytes);
                            if (comId.equals(LiveDetectionCmdEnum.Connect.getComId())) {
                                String ans = parseResult.toString();
                                if (ans.equals("true")) {
                                    vo.setConnectStatus(DeviceConnectMessageEnum.CONNECT_SUCCESS.getName());
                                    continue;
                                } else {
                                    //重连
                                }
                            } else if (comId.equals(LiveDetectionCmdEnum.Measure.getComId())) {
                                ChangeParam measureParam = (ChangeParam) parseResult;
                                if (!Objects.isNull(measureParam)) {
                                    String luminosity = DetectionConstants.luminosity;

                                    Double wl = formulaUtil.calculateWl(Double.parseDouble(measureParam.getWl()), deviceSerialNumber);
                                    vo.setX(wl + "nm");
                                    measureParam.setWl(String.valueOf(wl));

                                    if (luminosity.equals(WaveLengthEnum.abs.getCode())) {
                                        ChangeParam zeroParam = DetectionConstants.zeroResult.get(Double.valueOf(measureParam.getWl()));
                                        String s;
                                        if (!Objects.isNull(zeroParam)) {
                                             s = DataCountUtil.countTPercent(zeroParam, measureParam, 1.0);
                                        } else {
                                            s = DataCountUtil.countTPercent(null, measureParam, 1.0);
                                        }
                                        String abs = DataCountUtil.changeTPercentOrAbs(null, s);
                                        vo.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, abs));
                                    } else if(luminosity.equals(WaveLengthEnum.es.getCode()) || luminosity.equals(WaveLengthEnum.er.getCode())) {
                                        String s = DataCountUtil.countErOrEs(measureParam, luminosity);
                                        vo.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, s));
                                    } else if (luminosity.equals(WaveLengthEnum.tPercent.getCode())) {
                                        ChangeParam zeroParam = DetectionConstants.zeroResult.get(Double.valueOf(measureParam.getWl()));
                                        String s;
                                        if (!Objects.isNull(zeroParam)) {
                                            s = DataCountUtil.countTPercent(zeroParam, measureParam, 1.0);
                                        } else {
                                            s = DataCountUtil.countTPercent(null, measureParam, 1.0);
                                        }
                                        vo.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, s));
                                    }
                                }

                            } else if (comId.equals(LiveDetectionCmdEnum.GetLampPos.getComId())) {
                                List<DetectionLampStatus> dataList = getDetectionLampStatuses((int) parseResult);
                                vo.setDataList(dataList);
                            } else if (comId.equals(LiveDetectionCmdEnum.GetPhotoMode.getComId())) {
                                vo.setLuminosity(WaveLengthEnum.getName(DetectionConstants.luminosity));
                            }
                        }

                        vo.setY(vo.getY() + WaveLengthEnum.getName(DetectionConstants.luminosity));
                        Map<String, Object> map = vo.toMap();
                        ConcurrentHashMap<String, Object> detectionInfo = DetectionConstants.detectionInfo;
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.putAll(map);
                        jsonObject.putAll(detectionInfo);

                        messagingTemplate.convertAndSend(measureTopic, jsonObject);
                        DetectionConstants.isDeviceIdle = true;

                        // 休眠2秒
                        Thread.sleep(DetectionConstants.interval);

                    } catch (Exception e) {
                        log.print("监测线程异常" + e.getMessage());
                        DetectionConstants.isDeviceIdle = true;
                        try {
                            Thread.sleep(DetectionConstants.interval); // 出错时延时重试
                        } catch (InterruptedException ignored) {
                            DetectionConstants.isDeviceIdle = true;
                        }
                    }
                }

            } catch (Exception e) {
                log.print("Measure/GetChangeLampWl/GetCellPos/GetLampPos 循环线程启动失败");
                DetectionConstants.isDeviceIdle = true;
            }  finally {
                status = true;
                DetectionConstants.isDeviceIdle = true;
            }
        }, "MeasureLoopThread");

        // 添加线程结束监听器
        measureThread.setUncaughtExceptionHandler((thread, exception) -> {
            log.print("测量线程异常终止: " + exception.getMessage());
            status = true;
            DetectionConstants.isDeviceIdle = true;
        });

        measureThread.start();

        return R.ok();
    }

    @MessageMapping("/detection/stop")
    public void stopMeasure() {
        Constants.Running = false;
        status = true;
        messagingTemplate.convertAndSend(measureTopic, "停止测量");
    }

    private static List<DetectionLampStatus> getDetectionLampStatuses(int parseResult) {
        List<DetectionLampStatus> dataList = new ArrayList<>();
        DetectionLampStatus status = new DetectionLampStatus();
        status.setLabel("氘灯");
        status.setValue(parseResult == 1 ? "亮" : "不亮");
        dataList.add(status);

        DetectionLampStatus status1 = new DetectionLampStatus();
        status1.setLabel("钨灯");
        status1.setValue(parseResult == 0 ? "亮" : "不亮");
        dataList.add(status1);
        return dataList;
    }

}
