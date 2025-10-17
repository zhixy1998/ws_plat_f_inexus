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

//    @Autowired
//    private ExperimentTestService experimentService;

    @Value("${device.default.T600xmlPath}")
    String T600xmlPath;

    @Value("${device.default.T800xmlPath}")
    String T800xmlPath;

    @Value("/topic/detection")
    String measureTopic;


    @MessageMapping("/detection/start")
    public void startMeasure(String deviceSerialNumber) {
        if (deviceSerialNumber == null || deviceSerialNumber.isEmpty()) {
            messagingTemplate.convertAndSend(measureTopic,
                    "缺少 deviceSerialNumber 参数");
            return;
        }

        String ip = infoService.getIp(deviceSerialNumber);
        if (ip == null) {
            messagingTemplate.convertAndSend(measureTopic,
                    "设备未连接: " + deviceSerialNumber);
            return;
        }

        // 启动发送接收线程
        this.startMeasureLoop(deviceSerialNumber);


        // 主动推送通知给前端
        messagingTemplate.convertAndSend(measureTopic,
                "开始测量: " + deviceSerialNumber + ", ip=" + ip);
    }


    public R<Boolean> startMeasureLoop(String deviceSerialNumber) {
        String deviceIp = infoService.getIp(deviceSerialNumber);
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.print("绑定 socket 失败: {}");
            return R.fail("绑定 socket 失败，请检查网线接口");
        }

        new Thread(() -> {
            try {
                // 获取 socket

                DatagramSocket socket = re.getData();

                // 循环发送
                while (Constants.Running) {

                    while (!DetectionConstants.isDeviceIdle) {
                        Thread.sleep(1000);
                    }
                    DetectionConstants.isDeviceIdle = false;

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
                                    vo.setX(measureParam.getWl());

                                    if (luminosity.equals(WaveLengthEnum.abs.getCode())) {
                                        ChangeParam zeroParam = DetectionConstants.zeroResult.get(measureParam.getWl());
                                        String s;
                                        if (!Objects.isNull(zeroParam)) {
                                             s = DataCountUtil.countTPercent(zeroParam, measureParam, 1.0);
                                        } else {
                                            s = DataCountUtil.countTPercent(null, measureParam, 1.0);
                                        }
                                        String abs = DataCountUtil.changeTPercentOrAbs(null, s);
                                        vo.setY(abs);
                                    } else if(luminosity.equals(WaveLengthEnum.es.getCode()) || luminosity.equals(WaveLengthEnum.er.getCode())) {
                                        String s = DataCountUtil.countErOrEs(measureParam, luminosity);
                                        vo.setY(s);
                                    } else if (luminosity.equals(WaveLengthEnum.tPercent.getCode())) {
                                        ChangeParam zeroParam = DetectionConstants.zeroResult.get(measureParam.getWl());
                                        String s;
                                        if (!Objects.isNull(zeroParam)) {
                                            s = DataCountUtil.countTPercent(zeroParam, measureParam, 1.0);
                                        } else {
                                            s = DataCountUtil.countTPercent(null, measureParam, 1.0);
                                        }
                                        vo.setY(s);
                                    }
                                }

                            } else if (comId.equals(LiveDetectionCmdEnum.GetLampPos.getComId())) {
                                List<DetectionLampStatus> dataList = getDetectionLampStatuses((int) parseResult);
                                vo.setDataList(dataList);
                            } else if (comId.equals(LiveDetectionCmdEnum.GetPhotoMode.getComId())) {
                                vo.setLuminosity(DetectionConstants.luminosity);
                            }
                        }

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
                        log.print("Measure/GetChangeLampWl/GetCellPos/GetLampPos 循环异常");
                        DetectionConstants.isDeviceIdle = true;
                        try {
                            Thread.sleep(2000); // 出错时延时重试
                        } catch (InterruptedException ignored) {
                            DetectionConstants.isDeviceIdle = true;
                        }
                    }
                }

            } catch (Exception e) {
                log.print("Measure/GetChangeLampWl/GetCellPos/GetLampPos 循环线程启动失败");
                DetectionConstants.isDeviceIdle = true;
                return;
            }
        }, "MeasureLoopThread").start();
        return R.ok();
    }

    @MessageMapping("/detection/stop")
    public void stopMeasure() {
        Constants.Running = false;
        messagingTemplate.convertAndSend(measureTopic, "停止测量");
    }

    private static List<DetectionLampStatus> getDetectionLampStatuses(int parseResult) {
        int pos = parseResult;
        List<DetectionLampStatus> dataList = new ArrayList<>();
        DetectionLampStatus status = new DetectionLampStatus();
        status.setLabel("氘灯");
        status.setValue(pos == 1 ? "亮" : "不亮");
        dataList.add(status);

        DetectionLampStatus status1 = new DetectionLampStatus();
        status1.setLabel("钨灯");
        status1.setValue(pos == 0 ? "亮" : "不亮");
        dataList.add(status1);
        return dataList;
    }

}
