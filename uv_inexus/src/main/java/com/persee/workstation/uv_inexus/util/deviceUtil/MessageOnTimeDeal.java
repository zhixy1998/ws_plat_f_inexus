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
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.persee.workstation.uv_inexus.util.deviceUtil.SpectrumSourceType.RANGE;

@Slf4j
@Service
public class MessageOnTimeDeal {

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    FormulaUtil formulaUtil;

    public static UnknowCountMessageQueue unknowCountMessageQueue = UnknowCountMessageQueue.getInstance();

    private final Map<String, Thread> scanProcessorThreadMap = new ConcurrentHashMap<>();
    private final Map<String, Thread> baselineProcessorThreadMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, SpectrumPointStore> spectrumPointStoreMap = new ConcurrentHashMap<>();

    private final static DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    private final Map<String, SpectrumDataProcessor> spectrumProcessorMap =
            new ConcurrentHashMap<>();

    @Autowired
    private SpectrumThreadManager spectrumThreadManager;

//    public Object parse(String cmdId, String deviceId, String deviceSerialNumber, byte[] sendBytes, int timeout) {
//        boolean isScan;
//        if (cmdId.equals("Scan") || cmdId.equals("ScanFast")) {
//            ScanSessionManager.startScan(deviceId);
//            isScan = true;
//        } else {
//            isScan = false;
//        }
//        Map<String, Thread> targetThreadMap = isScan ? scanProcessorThreadMap : baselineProcessorThreadMap;
//
//        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
//        if (device == null) {
//            log.warn("[ON-TIME] device not found: {}", deviceId);
//            return null;
//        }
//
//        ScanWavelengthConfig cfg = device.getScanWavelengthConfig();
//
//        MessageStoreManager store = MessageStoreManager.getInstance();
//
//        // 用 Map 缓存每个设备半成品 ChangeParam
//        Map<String, ChangeParam> tempMap = new ConcurrentHashMap<>();
//        Map<String, Boolean> hasFirstHalfMap = new ConcurrentHashMap<>();
//
//        new Thread(() -> {
//            long startTime = System.currentTimeMillis();
//            long currentTimeout = timeout;
//            int recvCount = 0;
//
//            try {
//                while (ScanSessionManager.isRunning(deviceId)
//                        && System.currentTimeMillis() - startTime < currentTimeout) {
//
//                    if (store.getQueueSize(deviceId) == 0) {
//                        Thread.sleep(5);
//                        continue;
//                    }
//
//                    byte[] message = store.getMessage(deviceId);
//                    if (message == null) {
//                        continue;
//                    }
//
//                    recvCount++;
//                    if (recvCount >= 2) {
//                        currentTimeout = 5000L;
//                    }
//                    startTime = System.currentTimeMillis();
//
//                    OperationResultEntity parsed =
//                            UdpResponseDeal.parseScan(Collections.singletonList(message));
//
//                    if (parsed == null || parsed.getCurrentMeasure() == null) {
//                        continue;
//                    }
//
//                    ChangeParam data = parsed.getCurrentMeasure();
//
//                    // 永远只维护一个“正在拼的对象”
//                    ChangeParam temp =
//                            tempMap.computeIfAbsent(deviceSerialNumber, k -> new ChangeParam());
//
//                    // 谁不为 null，就补谁
//                    if (data.getWl() != null) temp.setWl(data.getWl());
//                    if (data.getS() != null)  temp.setS(data.getS());
//                    if (data.getR() != null)  temp.setR(data.getR());
//                    if (data.getD() != null)  temp.setD(data.getD());
//
//                    log.debug(
//                            "[SCAN-RECV-PART] device={}, wl={}, s={}, r={}, d={}",
//                            deviceSerialNumber,
//                            temp.getWl(),
//                            temp.getS(),
//                            temp.getR(),
//                            temp.getD()
//                    );
//
//                    // 只有真正“齐全”才入队
//                    if (temp.getWl() != null
//                            && temp.getS() != null
//                            && temp.getR() != null
//                            && temp.getD() != null) {
//
//                        log.info(
//                                "[SCAN-RECV-COMBINED] device={}, cmd={}, wl={}, s={}, r={}, d={}",
//                                deviceSerialNumber,
//                                cmdId,
//                                temp.getWl(),
//                                temp.getS(),
//                                temp.getR(),
//                                temp.getD()
//                        );
//
//                        unknowCountMessageQueue.putMessage(deviceSerialNumber, cmdId, temp);
//
//                        // 清空，准备下一组
//                        tempMap.remove(deviceSerialNumber);
//                    }
//                }
//            } catch (Exception e) {
//                log.error("parse error", e);
//            } finally {
//                store.clearDeviceMessages(deviceId);
//                log.info("[SCAN-END] device={} scan stopped", deviceId);
//                infoService.newReleaseDevice(deviceSerialNumber);
//            }
//        }).start();
//
//
//        targetThreadMap.compute(deviceSerialNumber, (k, existingThread) -> {
//            // 如果已有线程在运行，先停止旧线程
//            if (existingThread != null && existingThread.isAlive()) {
//                log.info("[光谱测试] 停止旧线程，重新创建，device={}, cmdId={}", deviceSerialNumber, cmdId);
//                existingThread.interrupt();
//                try {
//                    existingThread.join(1000); // 等待最多1秒让旧线程结束
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//
//            Thread t = new Thread(() -> {
//                log.info("[光谱测试] 开始处理光谱数据，设备={}，操作类型={}",
//                        deviceSerialNumber, isScan ? "Scan" : "BaseLine");
//
//                SpectrumDataProcessor spectrumDataProcessor =
//                        new SpectrumDataProcessor(formulaUtil, device);
//
//                // 只在真正启动新线程时 reset
//                spectrumDataProcessor.reset();
//
//                SpectrumPointStore pointStore =
//                        spectrumPointStoreMap.computeIfAbsent(deviceSerialNumber, deviceKey  -> new SpectrumPointStore());
//
//                spectrumProcessorMap.put(deviceSerialNumber, spectrumDataProcessor);
//
//                try {
//                    while (ScanSessionManager.isRunning(deviceId)) {
//                        ChangeParam param =
//                                (ChangeParam) unknowCountMessageQueue
//                                        .takeMessage(deviceSerialNumber, cmdId);
//
//                        if (param == null) {
//                            Thread.sleep(5);
//                            continue;
//                        }
//
//                        // 处理光谱数据
//                        spectrumDataProcessor.processStepAndPrintWavelength(
//                                param, deviceSerialNumber
//                        );
//
//                        double constrainedWl =
//                                spectrumDataProcessor.getConstrainedWavelength();
//
//                        SpectrumPoint sp = pointStore.getOrCreate(constrainedWl);
//
//                        if (isScan) {
//                            sp.setScanRsd(param);
//
//                            SpectrumCalcResult absResult =
//                                    SpectrumResultCalculator.calculate(1, sp, RANGE);
//                            SpectrumCalcResult tResult =
//                                    SpectrumResultCalculator.calculate(2, sp, RANGE);
//                            SpectrumCalcResult esResult =
//                                    SpectrumResultCalculator.calculate(3, sp, RANGE);
//                            SpectrumCalcResult erResult =
//                                    SpectrumResultCalculator.calculate(4, sp, RANGE);
//
//                            log.info("[光谱计算] wl={}, Abs={}, T%={}, Es={}, Er={}",
//                                    sp.getWavelength(),
//                                    absResult != null ? absResult.getAbs() : "-",
//                                    tResult != null ? tResult.getTPercent() : "-",
//                                    esResult != null ? esResult.getEs() : "-",
//                                    erResult != null ? erResult.getEr() : "-"
//                            );
//                        } else {
//                            sp.setBaselineRsd(param);
//                        }
//                    }
//
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } finally {
//                    spectrumProcessorMap.remove(deviceSerialNumber);
//                    // 从正确的映射中移除
//                    if (isScan) {
//                        scanProcessorThreadMap.remove(deviceSerialNumber);
//                    } else {
//                        baselineProcessorThreadMap.remove(deviceSerialNumber);
//                    }
//                    infoService.newReleaseDevice(deviceSerialNumber);
//
//                    log.info("[光谱测试] 停止处理光谱数据，设备={}，操作类型={}",
//                            deviceSerialNumber, isScan ? "Scan" : "BaseLine");
//                }
//            });
//
//            t.start();
//            return t;
//        });
//
//        return tempMap;
//    }

    public void parse(String cmdId, String deviceId, String deviceSerialNumber, byte[] sendBytes, int timeout) {

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        if (device == null) {
            log.warn("[ON-TIME] device not found: {}", deviceId);
            return;
        }

        if ("Stop".equals(cmdId)) {
            log.info("[STOP-SCAN] device={} request stop scan", deviceId);

            // 停掉解析线程
            ScanSessionManager.stopScan(deviceId);

            // 清空队列，避免残包
            MessageStoreManager.getInstance().clearDeviceMessages(deviceId);

            return; // 不需要继续创建新线程
        }

        boolean isScan = cmdId.equals("Scan") || cmdId.equals("ScanFast");
        if (isScan) {
            ScanSessionManager.startScan(deviceId);
        }

        MessageStoreManager store = MessageStoreManager.getInstance();
        Map<String, Thread> targetThreadMap = isScan ? scanProcessorThreadMap : baselineProcessorThreadMap;

        targetThreadMap.compute(deviceSerialNumber, (k, existingThread) -> {
            // 停止旧线程
            if (existingThread != null && existingThread.isAlive()) {
                log.info("[光谱测试] 停止旧线程，重新创建，device={}, cmdId={}", deviceSerialNumber, cmdId);
                existingThread.interrupt();
                try {
                    existingThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 创建并启动新线程
            Thread t = new Thread(() -> {
                log.info("[光谱测试] 开始处理光谱数据，设备={}，操作类型={}", deviceSerialNumber, isScan ? "Scan" : "BaseLine");

                SpectrumDataProcessor spectrumDataProcessor = new SpectrumDataProcessor(formulaUtil, device);
                spectrumDataProcessor.reset();

                SpectrumPointStore pointStore = spectrumPointStoreMap.computeIfAbsent(deviceSerialNumber, key -> new SpectrumPointStore());
                spectrumProcessorMap.put(deviceSerialNumber, spectrumDataProcessor);

                // ----------------- 半成品 map，用于拼装完整 ChangeParam -----------------
                Map<String, ChangeParam> tempMap = new HashMap<>();

                long startTime = System.currentTimeMillis();
                long currentTimeout = timeout;
                int recvCount = 0;
                int flag = 0;
                boolean isFirst = true; // 标记第一条完整光谱


                try {
                    while (!Thread.currentThread().isInterrupted() && ScanSessionManager.isRunning(deviceId) && System.currentTimeMillis() - startTime < currentTimeout) {

                        // ----------------- 1. 取消息 -----------------
                        byte[] message = store.getMessage(deviceId);
                        if (message == null) {
                            Thread.sleep(5);
                            continue;
                        }
                        if (message[2] == 0x00 && message[3] == 0x00 && message[4] == 0x00 && message[5] == 0x00){
                            unknowCountMessageQueue.putMessage(
                                    deviceSerialNumber,
                                    cmdId,
                                    EndMarker.Normal(deviceSerialNumber, cmdId)
                            );
                            spectrumProcessorMap.remove(deviceSerialNumber);
                            if (isScan) scanProcessorThreadMap.remove(deviceSerialNumber);
                            else baselineProcessorThreadMap.remove(deviceSerialNumber);

                            store.clearDeviceMessages(deviceId);
                            infoService.newReleaseDevice(deviceSerialNumber);
                            log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                            return;
                        }

                        recvCount++;
                        if (recvCount >= 2) currentTimeout = 5000L;
                        startTime = System.currentTimeMillis();

                        // ----------------- 2. 解析报文 -----------------
                        OperationResultEntity parsed = UdpResponseDeal.parseScan(Collections.singletonList(message));
                        if (parsed == null || parsed.getCurrentMeasure() == null) continue;

                        ChangeParam partial = parsed.getCurrentMeasure();

                        // ----------------- 3. 拼装半成品 -----------------
                        ChangeParam temp = tempMap.computeIfAbsent(deviceSerialNumber, z -> new ChangeParam());
                        if (partial.getWl() != null) temp.setWl(partial.getWl());
                        if (partial.getS() != null)  temp.setS(partial.getS());
                        if (partial.getR() != null)  temp.setR(partial.getR());
                        if (partial.getD() != null)  temp.setD(partial.getD());

                        // ----------------- 4. 如果完整，再计算光谱 -----------------
                        if (temp.getWl() != null && temp.getS() != null && temp.getR() != null && temp.getD() != null) {

                            // 处理光谱数据
                            spectrumDataProcessor.processStepAndPrintWavelength(temp, deviceSerialNumber);

                            double constrainedWl = spectrumDataProcessor.getConstrainedWavelength();
                            SpectrumPoint sp = pointStore.getOrCreate(constrainedWl);

                            if (isScan) {
                                sp.setScanRsd(temp);

                                SpectrumCalcResult absResult = SpectrumResultCalculator.calculate(1, sp, RANGE);
                                SpectrumCalcResult tResult   = SpectrumResultCalculator.calculate(2, sp, RANGE);
                                SpectrumCalcResult esResult  = SpectrumResultCalculator.calculate(3, sp, RANGE);
                                SpectrumCalcResult erResult  = SpectrumResultCalculator.calculate(4, sp, RANGE);

                                sp.setAbsResult(absResult);
                                sp.setTResult(tResult);
                                sp.setEsResult(esResult);
                                sp.setErResult(erResult);


                                log.info("[光谱计算] wl={}, Abs={}, T%={}, Es={}, Er={}",
                                        sp.getWavelength(),
                                        absResult != null ? absResult.getAbs() : "-",
                                        tResult != null ? tResult.getTPercent() : "-",
                                        esResult != null ? esResult.getEs() : "-",
                                        erResult != null ? erResult.getEr() : "-");

                            } else {
                                sp.setBaselineRsd(temp);
                            }


                            // ----------------- 5. 放入未知队列 -----------------
                            if (isFirst) {
                                log.info("[光谱首条] device={}, cmd={}, wl={}", deviceSerialNumber, cmdId, sp.getWavelength());
                                isFirst = false;
                            } else {
                                unknowCountMessageQueue.putMessage(deviceSerialNumber, cmdId, sp);
                            }

                            // 清空半成品准备下一条报文
                            tempMap.remove(deviceSerialNumber);
                        }

                        // ----------------- 6. 超时检查 -----------------
                        if (System.currentTimeMillis() - startTime > currentTimeout) {
                            log.info("[光谱测试] 超时退出 device={}", deviceSerialNumber);
                            flag = 1;
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    flag = 2;
                } finally {
                    if (flag == 1 || flag == 2){
                        unknowCountMessageQueue.putMessage(
                                deviceSerialNumber,
                                cmdId,
                                EndMarker.unNormal(deviceSerialNumber, cmdId)
                        );
                    }else{
                        unknowCountMessageQueue.putMessage(
                                deviceSerialNumber,
                                cmdId,
                                EndMarker.Normal(deviceSerialNumber, cmdId)
                        );
                    }
                    spectrumProcessorMap.remove(deviceSerialNumber);
                    if (isScan) scanProcessorThreadMap.remove(deviceSerialNumber);
                    else baselineProcessorThreadMap.remove(deviceSerialNumber);

                    store.clearDeviceMessages(deviceId);
                    infoService.newReleaseDevice(deviceSerialNumber);

                    log.info("[光谱测试] 停止处理光谱数据，设备={}，操作类型={}", deviceSerialNumber, isScan ? "Scan" : "BaseLine");
                }
            });

            t.start();
            return t;
        });
    }

    public void stop(String deviceSerialNumber, String deviceId) {

        // 中断 scan / baseline 线程
        Thread scanThread = scanProcessorThreadMap.remove(deviceSerialNumber);
        if (scanThread != null) {
            scanThread.interrupt();
        }

        Thread baselineThread = baselineProcessorThreadMap.remove(deviceSerialNumber);
        if (baselineThread != null) {
            baselineThread.interrupt();
        }

        // 再次确保状态关闭
        ScanSessionManager.stopScan(deviceId);

        log.info("[ON-TIME] stop parser thread, device={}", deviceSerialNumber);
    }

}



