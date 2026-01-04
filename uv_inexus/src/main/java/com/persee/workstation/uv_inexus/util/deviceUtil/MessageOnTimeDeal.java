/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MessageOnTimeDeal {

    @Autowired
    static DeviceInfoService infoService;

    static UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();
    public static Object parse(String cmdId, String deviceId, String deviceSerialNumber, byte[] sendBytes, int timeout) {

        MessageStoreManager store = MessageStoreManager.getInstance();

        // 用 Map 缓存每个设备半成品 ChangeParam
        Map<String, ChangeParam> tempMap = new ConcurrentHashMap<>();
        Map<String, Boolean> hasFirstHalfMap = new ConcurrentHashMap<>();

        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            long currentTimeout = timeout;
            int recvCount = 0;

            try {
                while (ScanSessionManager.isRunning(deviceId)
                        && System.currentTimeMillis() - startTime < currentTimeout) {

                    if (store.getQueueSize(deviceId) == 0) {
                        Thread.sleep(5);
                        continue;
                    }

                    byte[] message = store.getMessage(deviceId);
                    if (message == null) {
                        continue;
                    }

                    recvCount++;
                    if (recvCount >= 2) {
                        currentTimeout = 5000L;
                    }
                    startTime = System.currentTimeMillis();

                    OperationResultEntity parsed =
                            UdpResponseDeal.parseScan(Collections.singletonList(message));

                    if (parsed == null || parsed.getCurrentMeasure() == null) {
                        continue;
                    }

                    ChangeParam data = parsed.getCurrentMeasure();

                    // 永远只维护一个“正在拼的对象”
                    ChangeParam temp =
                            tempMap.computeIfAbsent(deviceSerialNumber, k -> new ChangeParam());

                    // 谁不为 null，就补谁
                    if (data.getWl() != null) temp.setWl(data.getWl());
                    if (data.getS() != null)  temp.setS(data.getS());
                    if (data.getR() != null)  temp.setR(data.getR());
                    if (data.getD() != null)  temp.setD(data.getD());

                    log.debug(
                            "[SCAN-RECV-PART] device={}, wl={}, s={}, r={}, d={}",
                            deviceSerialNumber,
                            temp.getWl(),
                            temp.getS(),
                            temp.getR(),
                            temp.getD()
                    );

                    // 只有真正“齐全”才入队
                    if (temp.getWl() != null
                            && temp.getS() != null
                            && temp.getR() != null
                            && temp.getD() != null) {

                        log.info(
                                "[SCAN-RECV-COMBINED] device={}, cmd={}, wl={}, s={}, r={}, d={}",
                                deviceSerialNumber,
                                cmdId,
                                temp.getWl(),
                                temp.getS(),
                                temp.getR(),
                                temp.getD()
                        );

                        unknowCountMessageQueue.putMessage(deviceSerialNumber, cmdId, temp);

                        // 清空，准备下一组
                        tempMap.remove(deviceSerialNumber);
                    }
                }
            } catch (Exception e) {
                log.error("parse error", e);
            } finally {
                store.clearDeviceMessages(deviceId);
                log.info("[SCAN-END] device={} scan stopped", deviceId);
                infoService.newReleaseDevice(deviceSerialNumber);
            }
        }).start();


        return tempMap;
    }
}

