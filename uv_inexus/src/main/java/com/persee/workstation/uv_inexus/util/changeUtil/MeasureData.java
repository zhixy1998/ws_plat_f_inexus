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

    @Value("${device.default.xmlPath}")
    String xmlPath;

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
                        Map<String, Transition> xml = protocolItemUtil.loadFromXml(xmlPath);
                        byte[] sendBytes = protocolItemUtil.calcSendBytes(new Object[]{}, xml.get("Measure"));

                        // 发送并接收
                        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, "Measure", sendBytes);

                        // 解析报文
                        Object parseResult = ResponseDeal.parse("Measure", responses, sendBytes);

                        if (parseResult == null || "false".equals(parseResult.toString())) {
                            log.print("Measure 指令解析失败");
                        } else {
                            log.print("Measure 数据解析成功: {}");
                        }

                        // 休眠2秒
                        Thread.sleep(2000);

                    } catch (Exception e) {
                        log.print("Measure 循环异常");
                        try {
                            Thread.sleep(2000); // 出错时延时重试
                        } catch (InterruptedException ignored) {}
                    }
                }


            } catch (Exception e) {
                log.print("Measure 循环线程启动失败");
            }
        }, "MeasureLoopThread").start();
    }

}
