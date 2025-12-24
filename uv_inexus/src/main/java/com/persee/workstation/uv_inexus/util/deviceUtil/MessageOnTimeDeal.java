/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MessageOnTimeDeal {

    public static Object parse(String cmdId, String deviceId, String deviceSerialNumber, byte[] sendBytes, int timeout) {

        MessageStoreManager store = MessageStoreManager.getInstance();
        UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();

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
                        currentTimeout = 5000L; // 滑动超时
                    }
                    startTime = System.currentTimeMillis();

                    OperationResultEntity parsed =
                            UdpResponseDeal.parseScan(
                                    Collections.singletonList(message),
                                    cmdId,
                                    deviceSerialNumber
                            );

                    if (parsed != null
                            && parsed.getCurrentMeasure() != null
                            && parsed.getCurrentMeasure().getData() != null) {

                        ChangeParam data = parsed.getCurrentMeasure().getData();

                        // 获取缓存
                        ChangeParam temp = tempMap.computeIfAbsent(deviceSerialNumber, k -> new ChangeParam());
                        boolean hasFirstHalf = hasFirstHalfMap.getOrDefault(deviceSerialNumber, false);

                        if (!hasFirstHalf) {
                            // 第一条报文，缓存部分字段
                            temp.setWl(data.getWl());
                            temp.setS(data.getS());
                            hasFirstHalfMap.put(deviceSerialNumber, true);
                        } else {
                            // 第二条报文，补全字段
                            temp.setR(data.getR());
                            temp.setD(data.getD());

                            // 打印完整对象
                            log.info(
                                    "[SCAN-RECV-COMBINED] device={}, cmd={}, wl={}, s={}, r={}, d={}",
                                    deviceSerialNumber,
                                    cmdId,
                                    temp.getWl(),
                                    temp.getS(),
                                    temp.getR(),
                                    temp.getD()
                            );

                            // 放入队列
                            unknowCountMessageQueue.putMessage(deviceSerialNumber, cmdId, temp);

                            // 重置缓存，准备下一对报文
                            tempMap.put(deviceSerialNumber, new ChangeParam());
                            hasFirstHalfMap.put(deviceSerialNumber, false);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("parse error", e);
            }finally {
                ScanSessionManager.clear(deviceId);
                store.clearDeviceMessages(deviceId);
                log.info("[SCAN-END] device={} scan stopped", deviceId);
            }
        }).start();

        return tempMap;
    }
}

