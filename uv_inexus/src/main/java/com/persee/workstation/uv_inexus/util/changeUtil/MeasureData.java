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

package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.model.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramSocket;
import java.util.List;
import java.util.Map;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;


@Service("MeasureData")
public class MeasureData {

    @Autowired
    private MessageUtil messageUtil;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

//    @Autowired
//    private ExperimentTestService experimentService;

    @Value("${device.default.T600xmlPath}")
    String T600xmlPath;

    @Value("${device.default.T800xmlPath}")
    String T800xmlPath;

    /**
     * 启动后台线程，循环发送 Measure 指令（参数固定为 0），并解析返回数据
     */
    public void startMeasureLoop(String deviceIp) {
        new Thread(() -> {
            try {
                // 获取 socket
                R<DatagramSocket> re = messageUtil.getSocket();
                if (!re.getSuccess()) {
                    log.print("绑定 socket 失败: {}");
                    return;
                }
                DatagramSocket socket = re.getData();

                // 循环发送
                while (Constants.Running) {
                    try {
                        Map<String, Transition> xml = protocolItemUtil.loadFromXml(T600xmlPath);
                        byte[] sendBytes = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get(T600ComIdEnums.Measure.getComId()));
                        byte[] sendBytes2 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get(T600ComIdEnums.GetChangeLampWl.getComId()));
                        byte[] sendBytes3 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get(T600ComIdEnums.GetCellPos.getComId()));
                        byte[] sendBytes4 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get(T600ComIdEnums.GetLampPos.getComId()));
                        byte[] sendBytes5 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get(T600ComIdEnums.GetAccessoryType.getComId()));
                        byte[] sendBytes6 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get(T600ComIdEnums.GetPhotoMode.getComId()));

                        // 发送并接收
                        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, T600ComIdEnums.Measure.getComId(), sendBytes, xml.get(T600ComIdEnums.Measure.getComId()).getReplyNumber(), xml.get(T600ComIdEnums.Measure.getComId()).getTimeout());
                        List<byte[]> responses2 = messageUtil.singleSendAndReceive(socket, deviceIp, T600ComIdEnums.GetChangeLampWl.getComId(), sendBytes2, xml.get(T600ComIdEnums.GetChangeLampWl.getComId()).getReplyNumber(), xml.get(T600ComIdEnums.GetChangeLampWl.getComId()).getTimeout());
                        List<byte[]> responses3 = messageUtil.singleSendAndReceive(socket, deviceIp, T600ComIdEnums.GetCellPos.getComId(), sendBytes3, xml.get(T600ComIdEnums.GetCellPos.getComId()).getReplyNumber(), xml.get(T600ComIdEnums.GetCellPos.getComId()).getTimeout());
                        List<byte[]> responses4 = messageUtil.singleSendAndReceive(socket, deviceIp, T600ComIdEnums.GetLampPos.getComId(), sendBytes4, xml.get(T600ComIdEnums.GetLampPos.getComId()).getReplyNumber(), xml.get(T600ComIdEnums.GetLampPos.getComId()).getTimeout());
                        List<byte[]> responses5 = messageUtil.singleSendAndReceive(socket, deviceIp, T600ComIdEnums.GetAccessoryType.getComId(), sendBytes5, xml.get(T600ComIdEnums.GetAccessoryType.getComId()).getReplyNumber(), xml.get(T600ComIdEnums.GetAccessoryType.getComId()).getTimeout());
                        List<byte[]> responses6 = messageUtil.singleSendAndReceive(socket, deviceIp, T600ComIdEnums.GetPhotoMode.getComId(), sendBytes6, xml.get(T600ComIdEnums.GetPhotoMode.getComId()).getReplyNumber(), xml.get(T600ComIdEnums.GetPhotoMode.getComId()).getTimeout());


                        // 解析报文
                        Object parseResult = ResponseDeal.parse(T600ComIdEnums.Measure.getComId(), responses, sendBytes);
                        Object parseResult2 = ResponseDeal.parse(T600ComIdEnums.GetChangeLampWl.getComId(), responses2, sendBytes2);
                        Object parseResult3 = ResponseDeal.parse(T600ComIdEnums.GetCellPos.getComId(), responses3, sendBytes3);
                        Object parseResult4 = ResponseDeal.parse(T600ComIdEnums.GetLampPos.getComId(), responses4, sendBytes4);
                        Object parseResult5 = ResponseDeal.parse(T600ComIdEnums.GetAccessoryType.getComId(), responses5, sendBytes5);
                        Object parseResult6 = ResponseDeal.parse(T600ComIdEnums.GetPhotoMode.getComId(), responses6, sendBytes6);

                        if (parseResult == null || "false".equals(parseResult.toString())) {
                            log.print("Measure 指令解析失败");
                        } else {
                            log.print("Measure 数据解析成功: {}");
                        }

                        if (parseResult2 == null || "false".equals(parseResult2.toString())) {
                            log.print("GetChangeLampWl 指令解析失败");
                        } else {
                            log.print("GetChangeLampWl 数据解析成功: {}");
                        }

                        if (parseResult3 == null || "false".equals(parseResult3.toString())) {
                            log.print("GetCellPos 指令解析失败");
                        } else {
                            log.print("GetCellPos 数据解析成功: {}");
                        }

                        if (parseResult4 == null || "false".equals(parseResult4.toString())) {
                            log.print("GetLampPos 指令解析失败");
                        } else {
                            log.print("GetLampPos 数据解析成功: {}");
                        }

                        if (parseResult5 == null || "false".equals(parseResult5.toString())) {
                            log.print("GetAccessoryType 指令解析失败");
                        } else {
                            log.print("GetAccessoryType 数据解析成功: {}");
                        }

                        if (parseResult6 == null || "false".equals(parseResult6.toString())) {
                            log.print("GetPhotoMode 指令解析失败");
                        } else {
                            log.print("GetPhotoMode 数据解析成功: {}");
                        }

                        // 休眠2秒
                        Thread.sleep(3500);

                    } catch (Exception e) {
                        log.print("Measure/GetChangeLampWl/GetCellPos/GetLampPos 循环异常");
                        try {
                            Thread.sleep(2000); // 出错时延时重试
                        } catch (InterruptedException ignored) {}
                    }
                }


            } catch (Exception e) {
                log.print("Measure/GetChangeLampWl/GetCellPos/GetLampPos 循环线程启动失败");
            }
        }, "MeasureLoopThread").start();
    }

}
