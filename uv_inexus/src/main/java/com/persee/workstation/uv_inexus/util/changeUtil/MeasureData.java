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
                        byte[] sendBytes = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get("Measure"));
                        byte[] sendBytes2 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get("GetChangeLampWl"));
                        byte[] sendBytes3 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get("GetCellPos"));
                        byte[] sendBytes4 = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get("GetLampPos"));

                        // 发送并接收
                        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, "Measure", sendBytes);
                        List<byte[]> responses2 = messageUtil.singleSendAndReceive(socket, deviceIp, "GetChangeLampWl", sendBytes2);
                        List<byte[]> responses3 = messageUtil.singleSendAndReceive(socket, deviceIp, "GetCellPos", sendBytes3);
                        List<byte[]> responses4 = messageUtil.singleSendAndReceive(socket, deviceIp, "GetLampPos", sendBytes4);

                        // 解析报文
                        Object parseResult = ResponseDeal.parse("Measure", responses, sendBytes);
                        Object parseResult2 = ResponseDeal.parse("GetChangeLampWl", responses2, sendBytes2);
                        Object parseResult3 = ResponseDeal.parse("GetCellPos", responses3, sendBytes3);
                        Object parseResult4 = ResponseDeal.parse("GetLampPos", responses4, sendBytes4);

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
