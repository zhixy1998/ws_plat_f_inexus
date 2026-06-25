/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    // 设备序列号 -> (wl -> z)
    private static final Map<String, Map<String, String>> darkCache = new ConcurrentHashMap<>();


    @Autowired
    private SpectrumThreadManager spectrumThreadManager;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SpectrumResultCalculator calculator;

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

    public void parse(String cmdId, String deviceId, String deviceSerialNumber, byte[] sendBytes, int timeout, List<String> headReceiveBytes) {

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

            unknowCountMessageQueue.clearCommand(deviceSerialNumber, "Scan");
            unknowCountMessageQueue.clearCommand(deviceSerialNumber, "ScanFast");
//            unknowCountMessageQueue.clearCommand(deviceSerialNumber, "BaseLine");
//            unknowCountMessageQueue.clearCommand(deviceSerialNumber, "BaseLineFast");

            return; // 不需要继续创建新线程
        }

        boolean isScan = cmdId.equals("Scan") || cmdId.equals("ScanFast");
        if (isScan) {
            ScanSessionManager.startScan(deviceId);
        }
        boolean isDark = cmdId.equals("Dark");

        boolean isBaseline = cmdId.equals("BaseLine") || cmdId.equals("BaseLineFast");

        if (isScan || isBaseline) {
            unknowCountMessageQueue.clearIp(deviceSerialNumber);
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

                Map<String, ChangeParam> tempMap = new HashMap<>();

                long startTime = System.currentTimeMillis();
                long currentTimeout = timeout;
                int recvCount = 0;
                int flag = 0;
                boolean[] isFirstFlag = {true};// 标记首条光谱
                Constants.lastStepParam=null;
                try {
                    while (!Thread.currentThread().isInterrupted() && ScanSessionManager.isRunning(deviceId)
                            && System.currentTimeMillis() - startTime < currentTimeout) {

                        // ----------------- 1. 取消息 -----------------
                        byte[] message = store.peekMessage(deviceId);
                        if (message == null) {
                            Thread.sleep(5);
                            continue;
                        }
                        if (isStartMessage(message,deviceId)) {
                            log.info("07报文");
                            store.getMessage(deviceId);
                            startTime = System.currentTimeMillis();
                            continue;
                        }

                        if (message[2] == 0x00 && message[3] == 0x00 && message[4] == 0x00 && message[5] == 0x00){
                            flag = 3;
                            return;
                        }
                        if ((message[2] & 0xFF) == 0xFF && (message[3] & 0xFF) == 0x01 && (message[4] & 0xFF) == 0x09 && (message[5] & 0xFF) == 0x00){
                            flag = 4;
                            return;
                        }

                        StringBuilder s = new StringBuilder();
                        for (int j = 0; j < 4; j++) {
                            s.append(0XFF & message[j]).append(":");
                        }
                        String substring = s.substring(0, s.length() - 1);
                        if (!headReceiveBytes.contains(substring)) {
                            log.info("不是需要的报文头！");
                            store.getMessage(deviceId);
                            continue;
                        }
                        byte[] useMessage = store.getMessage(deviceId);

                        recvCount++;
                        if (recvCount >= 2) {
                            currentTimeout = 15000L;
                            if (cmdId.equals("ScanFast") || cmdId.equals("BaseLineFast")){
                                currentTimeout = 1000L;
                            }
                            startTime = System.currentTimeMillis();
                        }

                        // ----------------- 2. 解析报文 -----------------
                        OperationResultEntity parsed = UdpResponseDeal.parseScan(Collections.singletonList(useMessage));
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


                            double constrainedWl = spectrumDataProcessor.getConstrainedWavelength();
                            if (Double.isNaN(constrainedWl)){
                                log.info("[光谱扫描]:跳过最后一条");
                                break;
                            }

                            DevXmlEnums dev = DevXmlEnums.getEnum(device.getXmlKey());
                            if (dev.isNeedFit()) {
                                // 拟合实时处理，每生成一个拟合点就回调
                                spectrumDataProcessor.processStepAndPrintWavelength(temp, deviceSerialNumber, fittedParam -> {
                                    double wl = Double.parseDouble(fittedParam.getWl());
                                    SpectrumPoint sp = pointStore.getOrCreate(wl);

                                    if (isDark) {
                                        handleDarkRsd(temp, deviceSerialNumber, sp);
                                        deviceManager.setDarkWavelength(deviceId, String.valueOf(sp.getWavelength()));
                                        tempMap.remove(deviceSerialNumber);
                                    }

                                    if (isScan) {
                                        sp.setScanRsd(fittedParam);
                                        log.info("[光谱RSD] wl={}, r={}, s={}, d={}",
                                                sp.getWavelength(),
                                                sp.getScanRsdR(),
                                                sp.getScanRsdS(),
                                                sp.getScanRsdD());
                                        SpectrumCalcResult absResult = calculator.calculate(1, sp, RANGE, deviceSerialNumber);
                                        SpectrumCalcResult tResult   = calculator.calculate(2, sp, RANGE, deviceSerialNumber);
                                        SpectrumCalcResult esResult  = calculator.calculate(3, sp, RANGE, deviceSerialNumber);
                                        SpectrumCalcResult erResult  = calculator.calculate(4, sp, RANGE, deviceSerialNumber);
                                        SpectrumCalcResult rResult = calculator.calculate(2, sp, RANGE, deviceSerialNumber);
                                        rResult.setRPercent(rResult.getTPercent());

                                        sp.setAbsResult(absResult);
                                        sp.setTResult(tResult);
                                        sp.setEsResult(esResult);
                                        sp.setErResult(erResult);
                                        sp.setRResult(rResult);

                                        log.info("[光谱计算] wl={}, Abs={}, T%={}, Es={}, Er={} , Ms{}",
                                                sp.getWavelength(),
                                                absResult != null ? absResult.getAbs() : "-",
                                                tResult != null ? tResult.getTPercent() : "-",
                                                esResult != null ? esResult.getEs() : "-",
                                                erResult != null ? erResult.getEr() : "-",
                                                System.currentTimeMillis());

                                        if (Constants.firstDataTime != null && Constants.secondDataTime != null){
                                            Constants.pushInterval = (Constants.secondDataTime - Constants.firstDataTime) / 1000.0;
                                            log.info("计算得到推送间隔: {} 秒", Constants.pushInterval);
                                            Constants.firstDataTime= null;
                                            Constants.secondDataTime= null;
                                        }

                                        if (Constants.firstDataTime != null && Constants.secondDataTime == null){
                                            Constants.secondDataTime=System.currentTimeMillis();
                                        }
                                        if (Constants.firstDataTime == null){
                                            Constants.firstDataTime=System.currentTimeMillis();
                                        }
                                    } else {
                                        sp.setBaselineRsd(fittedParam);
                                        deviceManager.addZeroResult(deviceId,String.valueOf(sp.getWavelength()),fittedParam);
                                    }
;
                                    // ----------------- 放入未知队列 -----------------
                                    if (isScan || isBaseline) {
                                        if (isFirstFlag[0]) {
                                            log.info("[光谱首条,不存] device={}, cmd={}, wl={}",
                                                    deviceSerialNumber, cmdId, sp.getWavelength());
                                            isFirstFlag[0] = false;
                                        } else {
                                            unknowCountMessageQueue.putMessage(deviceSerialNumber, cmdId, sp);
                                            log.info("[光谱存入] device={}, cmd={}, wl={}",
                                                    deviceSerialNumber, cmdId, sp.getWavelength());
                                        }
                                    }
                                });
                            }
                            else {
                                // 不拟合，直接使用 temp
                                SpectrumPoint sp = pointStore.getOrCreate(constrainedWl);

                                if (isDark) {
                                    handleDarkRsdT1960(temp, deviceSerialNumber, sp);
                                    deviceManager.setDarkWavelength(deviceId, String.valueOf(sp.getWavelength()));
                                    tempMap.remove(deviceSerialNumber);
                                } else if (isScan) {
                                    sp.setScanRsd(temp);
                                    log.info("[T1960光谱RSD] wl={}, r={}, s={}, d={}",
                                            sp.getWavelength(),
                                            sp.getScanRsdR(),
                                            sp.getScanRsdS(),
                                            sp.getScanRsdD());
                                    SpectrumCalcResult absResult = calculator.calculate(5, sp, RANGE, deviceSerialNumber);
                                    SpectrumCalcResult tResult = calculator.calculate(6, sp, RANGE, deviceSerialNumber);
                                    SpectrumCalcResult esResult = calculator.calculate(7, sp, RANGE, deviceSerialNumber);
                                    SpectrumCalcResult erResult = calculator.calculate(8, sp, RANGE, deviceSerialNumber);
                                    SpectrumCalcResult rResult = calculator.calculate(6, sp, RANGE, deviceSerialNumber);
                                    rResult.setRPercent(rResult.getTPercent());

                                    sp.setAbsResult(absResult);
                                    sp.setTResult(tResult);
                                    sp.setEsResult(esResult);
                                    sp.setErResult(erResult);
                                    sp.setRResult(rResult);



                                log.info("[光谱计算] wl={}, Abs={}, T%={}, Es={}, Er={}",
                                        sp.getWavelength(),
                                        absResult != null ? absResult.getAbs() : "-",
                                        tResult != null ? tResult.getTPercent() : "-",
                                        esResult != null ? esResult.getEs() : "-",
                                        erResult != null ? erResult.getEr() : "-");

                                if (Constants.firstDataTime != null && Constants.secondDataTime != null){
                                    Constants.pushInterval = (Constants.secondDataTime - Constants.firstDataTime) / 1000.0;
                                    log.info("计算得到推送间隔: {} 秒", Constants.pushInterval);
                                    Constants.firstDataTime= null;
                                    Constants.secondDataTime= null;
                                }

                                if (Constants.firstDataTime != null && Constants.secondDataTime == null){
                                    Constants.secondDataTime=System.currentTimeMillis();
                                }
                                if (Constants.firstDataTime == null){
                                    Constants.firstDataTime=System.currentTimeMillis();
                                }

                            } else {
                                sp.setBaselineRsd(temp);
                                log.info("[光谱计算] wl={}, r={}, s={}, d={}",
                                        sp.getWavelength(),
                                        sp.getBaselineRsdR(),
                                        sp.getBaselineRsdS(),
                                        sp.getBaselineRsdD());
                            }
                                spectrumDataProcessor.dataPointIndex();

                                if (isScan || isBaseline) {
                                    if (isFirstFlag[0]) {
                                        log.info("[光谱首条,不存] device={}, cmd={}, wl={}",
                                                deviceSerialNumber, cmdId, sp.getWavelength());
                                        isFirstFlag[0] = false;
                                    } else {
                                        unknowCountMessageQueue.putMessage(deviceSerialNumber, cmdId, sp);
                                        log.info("[光谱存入] device={}, cmd={}, wl={}",
                                                deviceSerialNumber, cmdId, sp.getWavelength());
                                    }
                                }
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
                    } else if(flag == 3){
                        unknowCountMessageQueue.putMessage(
                                deviceSerialNumber,
                                cmdId,
                                EndMarker.NormalStop(deviceSerialNumber, cmdId)
                        );
                        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                    } else if (flag == 4){
                        unknowCountMessageQueue.putMessage(
                                deviceSerialNumber,
                                cmdId,
                                EndMarker.unNormalOverPower(deviceSerialNumber, cmdId)
                        );
                    }else{
                        unknowCountMessageQueue.putMessage(
                                deviceSerialNumber,
                                cmdId,
                                EndMarker.Normal(deviceSerialNumber, cmdId)
                        );
                    }
                    saveDarkInfoToDb(deviceSerialNumber);
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

    private boolean isStartMessage(byte[] message, String deviceId) {

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        DevXmlEnums anEnum = DevXmlEnums.getEnum(xmlKey);
        byte[] startMessage = anEnum.getStartMessage();

        // 验证字节数组长度
        if (message == null || startMessage == null || message.length < 12 || startMessage.length < 12) {
            return false;
        }

        // 比较两个字节数组的前12个字节
        for (int i = 0; i < 12; i++) {
            if (message[i] != startMessage[i]) {
                return false;
            }
        }
        return true;
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

    private void handleDarkRsd(ChangeParam rsd, String deviceSerialNumber,SpectrumPoint sp) {

        String dStr = rsd.getD();
        String sStr = rsd.getS();
        BigDecimal bigDecimal = BigDecimal.valueOf(sp.getWavelength());
        String wlStr = bigDecimal.stripTrailingZeros().toPlainString();
//        String wlStr = String.valueOf(sp.getWavelength());

        if (!StringUtils.hasText(dStr) || !StringUtils.hasText(sStr)) return;

        double d = Double.parseDouble(dStr);
        double z;

        if (d == 0.0) {
            z = 1.0;
        } else {
            double s = Double.parseDouble(sStr);
            z = s / d;
        }

        darkCache.computeIfAbsent(deviceSerialNumber, k -> new ConcurrentHashMap<>())
                .put(wlStr, String.valueOf(z));

        log.info("[Dark] device={},wl={}, d={}, s={}, Z={}",
                deviceSerialNumber, sp.getWavelength(),dStr, sStr, z);
    }

    private void handleDarkRsdT1960(ChangeParam rsd, String deviceSerialNumber, SpectrumPoint sp) {
        String dStr = rsd.getD();
        String sStr = rsd.getS();
        String rStr = rsd.getR();

        BigDecimal bigDecimal = BigDecimal.valueOf(sp.getWavelength());
        String wlStr = bigDecimal.stripTrailingZeros().toPlainString();

//        String wlStr = String.valueOf(sp.getWavelength());

        // 任一字符串为空则直接返回
        if (!StringUtils.hasText(dStr) || !StringUtils.hasText(sStr) || !StringUtils.hasText(rStr)) {
            return;
        }

        double D = Double.parseDouble(dStr);
        double S = Double.parseDouble(sStr);
        double R = Double.parseDouble(rStr);

        double f;
        double diffRS = R - S;
        if (Math.abs(diffRS) > 1.0e-10) {
            f = (D - S) / diffRS;
        } else {
            f = (D - S) / 1.0e-10;
        }
        double z = Math.min(f, 0.1);

        darkCache.computeIfAbsent(deviceSerialNumber, k -> new ConcurrentHashMap<>())
                .put(wlStr, String.valueOf(z));

        log.info("[Dark] device={}, wl={}, D={}, S={}, R={}, Z={}",
                deviceSerialNumber, sp.getWavelength(), dStr, sStr, rStr, z);
    }

    private void clearDarkZ(SpectrumPointStore store, String deviceSerialNumber) {

        if (store == null) return;

        for (SpectrumPoint sp : store.allPoints()) {
            sp.setDarkZ(null);
        }

        log.info("[CLEAR-DARK-Z] device={} 当前 pointStore 内 darkZ 已清空",
                deviceSerialNumber);
    }

    private void saveDarkInfoToDb(String deviceSerialNumber) {
        Map<String, String> map = darkCache.get(deviceSerialNumber);
        if (map == null || map.isEmpty()) return;

        DeviceInfoPO po = infoService.getInfo(deviceSerialNumber);
        if (po != null) {
            Map<String, String> darkInfo = po.getDarkInfo() == null ? new HashMap<>() : po.getDarkInfo();
            darkInfo.putAll(map);
            po.setDarkInfo(darkInfo);

//            po.setDarkInfo(map); // 保存整个Map
            Boolean b = infoService.saveOrUpdateInfo(po);
            if (b){
                log.info("[Dark] device={} darkInfo写入数据库成功", deviceSerialNumber);
            } else {
                log.warn("[Dark] device={} darkInfo写入数据库失败", deviceSerialNumber);
            }
        }

        // 清理缓存
        darkCache.remove(deviceSerialNumber);
    }
}


