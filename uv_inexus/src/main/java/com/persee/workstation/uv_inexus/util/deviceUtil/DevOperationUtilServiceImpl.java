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
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceModelInfoDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceTypeInfoDAO;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.T800ComIdEnums;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceAttachmentKindEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceCalibrationInfoEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitPreCommandEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInitEntryInfo;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceUsageLogPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InitRecordPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.StepPoint;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.WorkTimeInfo;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceUsageLogService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.cmdEnums.ConnectEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.cmdEnums.DefaultStatusCmdEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.persee.workstation.uv_inexus.websocket.liveDetection.LiveDetectionCmdEnum;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.DetectionManyDevicesLampStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 仪器操作工具类，定义所有的仪器操作，根据业务层传参来组装发送报文，然后 从队列里取接收报文并处理
 * 将处理后的结果返给业务层
 */

@Service("DevOperationUtilService")
@Slf4j
public class DevOperationUtilServiceImpl implements DevOperationUtilService {

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    DeviceUsageLogService deviceUsageLogService;

    @Autowired
    InitRecordService initRecordService;

    @Autowired
    DeviceModelInfoDAO deviceModelInfoDAO;

    @Autowired
    DeviceTypeInfoDAO typeInfoDAO;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    DeviceXmlManage xmlManage;

    @Autowired
    UdpSendUtil udpSendUtil;

    @Autowired
    SamplePoolConfigService samplePoolConfigService;

    @Autowired
    SpectrumResultCalculator spectrumResultCalculator;

    private final static ConnectUtil connectUtil = ConnectUtil.getInstance();

    private final static String defaultIp = "255.255.255.255";

    private final static DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    private static final AtomicInteger newIpEnd = new AtomicInteger(253);
    private static final int ipEndMax = 254;
    private static final int ipEndMin = 1;
    private static final int size = ipEndMax - ipEndMin + 1;

    private final ExecutorService actionExecutor = Executors.newSingleThreadExecutor();

    ScanWavelengthConfig cfg = new ScanWavelengthConfig();

    @Override
    public void initXml() {

        try {
            Map<String, Map<String, Transition>> allXml = new HashMap<>();
            List<DevXmlEnums> all = DevXmlEnums.getAll();
            for (DevXmlEnums anEnum : all) {
                String path = anEnum.getXmlPath();
                Map<String, Transition> xml = protocolItemUtil.loadFromXml(path);
                allXml.put(anEnum.getDevType(), xml);
            }
            xmlManage.initAllXml(allXml);
        } catch (Exception e) {
            log.error("初始化XML异常！", e);
        }
    }

    @PostConstruct
    public void checkDeviceConnect() {

        initXml();

        List<DeviceInfoPO> lastConnectDevice = infoService.getLastConnectDevice();
        if (lastConnectDevice == null || lastConnectDevice.isEmpty()) {
            log.info("没有要恢复连接的仪器");
            return;
        }
//        DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

        String cmdId = T600ComIdEnums.GetSn.getComId();

        List<String> unConnectedDevice = new ArrayList<>();

        Integer port = null;
        String localIp = "";
        for (DeviceInfoPO po : lastConnectDevice) {
            String deviceSerialNumber = po.getDeviceSerialNumber();
            String type = deviceModelInfoDAO.getDevTypeByDevModel(po.getDeviceModel());
            Map<String, Transition> xmlInfo = xmlManage.getXmlInfo(type);

            Transition transition = xmlInfo.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            Object[] param = new Object[]{};

            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

            Integer connectType = po.getConnectType();
            if (connectType == null) {
                //默认网口连接
                connectType = 1;
            }

            if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
                //串口
                log.info("串口连接！");

            } else if (connectType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
                //wifi
                log.info("wifi连接！");
                if (port == null) {
                    DatagramSocket wifiSocket = connectUtil.getWifiSocket(po.getDevicePort());
                    if (wifiSocket == null) {
                        log.info("请检查wifi连接！");
                        unConnectedDevice.add(deviceSerialNumber);
                        continue;
                    }
                    port = connectUtil.getWifiPort();
                    localIp = connectUtil.getWifiLocalIp();
                }

            } else {
                //ethernet
                log.info("ethernet连接！");
                if (port == null) {
                    DatagramSocket etherSocket = connectUtil.getEtherSocket(po.getDevicePort());
                    if (etherSocket == null) {
                        log.info("请检查以太网连接！");
                        unConnectedDevice.add(deviceSerialNumber);
                        continue;
                    }
                    port = connectUtil.getEthernetPort();
                    localIp = connectUtil.getEthernetLocalIp();
                }
            }

            //共有数据库的情况下，不修改非自己ip的设备信息
            if (!po.getMachineIp().equals(localIp)) {
                continue;
            }

            String deviceId = deviceManager.getOrCreateDeviceId(po.getIp(), port, po.getConnectType());
            DevOperationUtilServiceImpl.deviceManager.updateDeviceInfo(deviceId, Constants.CONNECT_DEVICE_NO, "", type);

            R<Object> r = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
            if (r.getSuccess()) {
                OperationResultEntity result = (OperationResultEntity) r.getData();
                String sn = result.getDeviceSerialNumber();

                if (sn == null || !sn.equals(deviceSerialNumber)) {
                    log.info("当前ip的序列号以改变！");
                    unConnectedDevice.add(deviceSerialNumber);
                }

                String s = deviceManager.convertToPermanentByDeviceId(deviceId);
                deviceManager.updateDeviceInfo(s, Constants.CONNECT_DEVICE_SUCCESS, "", type);
                DeviceInfoPO newPO = new DeviceInfoPO();
                newPO.setDevicePort(po.getDevicePort());
                newPO.setIp(po.getIp());
                newPO.setDeviceSerialNumber(sn);
                newPO.setDeviceId(s);
                newPO.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
                newPO.setMachineIp(localIp);
                newPO.setIsDevOpened(Constants.DEVICE_IS_OPEN);
                newPO.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);

                DevOperationUtilServiceImpl.deviceManager.updateDeviceInfo(s, Constants.CONNECT_DEVICE_SUCCESS, "", type);

                DevOperationUtilServiceImpl.deviceManager.setDeviceIdleStatus(s, true);

                //判断初始状态
                cmdId = T600ComIdEnums.GetInitStatus.getComId();

                transition = xmlInfo.get(cmdId);
                timeout = transition.getTimeout();
                replyNumber = transition.getReplyNumber();
                headReceiveBytes = transition.getHeadReceiveBytes();

                param = new Object[]{};

                bytes = protocolItemUtil.calcSendBytes(param, transition);
                r = udpSendUtil.knowTotalReceiveSend(s, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
                if (r.getSuccess()) {

                    result = (OperationResultEntity) r.getData();
                    Integer isInited = result.getIsInited();
                    if (isInited.equals(Constants.INIT_STATUS_SUCCESS)) {
                        log.info("已初始化！");
                        newPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
                    } else {
                        log.info("未初始化！");
                        newPO.setIsInited(Constants.INIT_STATUS_NO);
                    }
                } else {
                    newPO.setIsInited(Constants.INIT_STATUS_FALSE);
                }

                xmlManage.setDevice(po.getIp(), type);

                infoService.saveOrUpdateInfo(newPO);

                setDeviceDefaultStatus(s, deviceSerialNumber);
            } else {
                log.error("获取SN失败！仪器未连接！");
                deviceManager.removeDevice(deviceId);
                unConnectedDevice.add(deviceSerialNumber);
            }
        }
//        connectUtil.closeAllSockets();

        for (String deviceSerialNumber : unConnectedDevice) {
            DeviceInfoPO po = infoService.getInfo(deviceSerialNumber);
            if (po != null) {
                po.setConnectStatus(Constants.CONNECT_DEVICE_NO);
                po.setIp("");
                po.setDeviceId("");
                po.setMachineIp("");
                po.setIsDevOpened(Constants.DEVICE_IS_NULL);
                po.setIsInited(Constants.INIT_STATUS_NO);
                po.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);
                Boolean b = infoService.saveOrUpdateInfo(po);
                System.out.println(b);
            }
        }
    }


    @Override
    public R<OperationResultEntity> operation(DevOperationInputParam inputParam) {

        if (inputParam == null) {
            return R.fail("00209001");
        }

        if (inputParam.getOperationType() == null) {
            return R.fail("00209002");
        }
        OperationTypeEnums anEnum = OperationTypeEnums.getEnum(inputParam.getOperationType());
        if (anEnum == null) {
            return R.fail("00209003");
        }

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();

        if (!StringUtils.hasText(deviceSerialNumber)) {
            if (!inputParam.getOperationType().equals(OperationTypeEnums.Find.getType()) && !inputParam.getOperationType().equals(OperationTypeEnums.Connect.getType())) {
                return R.fail("00209004");
            }
        } else {

            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null || !StringUtils.hasText(info.getDeviceId())) {
                return R.fail("00209004");
            }

            DeviceConnectInfo device = deviceManager.getDevice(info.getDeviceId());
            if (device == null) {
                return R.fail("00209004");
            }

            Integer devStatus = OperationTypeEnums.getEnum(inputParam.getOperationType()).getStatus();
            // [适配出厂链接多仪器]使用设备序列号的字符串锁，而不是全局锁，允许多个任务并行操作不同仪器
            synchronized (deviceSerialNumber.intern()) {
                if (anEnum != OperationTypeEnums.Stop) {
                    R<Boolean> r = infoService.newUseDevice(deviceSerialNumber, devStatus);
                    if (!r.getSuccess()) {
                        return R.fail(r.getMessage());
                    }
                }
            }

        }

        R<OperationResultEntity> result = R.ok();
        boolean release = true;
        try {
            switch (anEnum) {
                case Find:
                    result = find(inputParam);
                    break;
                case Connect:
                    result = connect(inputParam);
                    break;
                case Init:
                    synchronized (this) {
                        new Thread(() -> initDevice(deviceSerialNumber, inputParam.getIsInitFromHead())).start();
                        release = false;
                    }
                    break;
                case SetParam:
                    result = setParam(inputParam);
                    break;
                case Disconnect:
                    result = disconnect(inputParam);
                    release = false;
                    break;
                case LiveDetection:
                    result = liveDetection(deviceSerialNumber);
                    break;
                case ActionCmdEnum:
                    actionExecutor.submit(() -> executeAction(inputParam));
                    release = false;
                    break;
                case GetValue:
                    result = getValue(inputParam);
                    break;
                case Calibration:
                    result = calibration(inputParam);
                    release = false;
                    break;
                case Zero:
                    result = zero(inputParam);
                    break;
                case SingWaveLengthZero:
                    result = singleWavelengthZero(inputParam);
                    break;
                case Measure:
                    result = measure(inputParam);
                    break;
                case SingWaveLengthMeasure:
                    result = singWaveLengthMeasure(inputParam);
                    break;
                case Stop:
                    actionExecutor.submit(() -> stop(inputParam));
                    release = false;
                    break;
                case TimeMeasure:
                    actionExecutor.submit(() -> executeTimeMeasure(inputParam));
                    release = false;
                    break;
                case T800Connect:
                    t800Connect(inputParam);
                    break;
                case QueryLifeTime:
                    result = queryLifeTime(inputParam);
                    break;
                case MercuryLocate:
                    result = mercuryLocate(inputParam);
                    break;
                case SamplePoolCalibration:
                    result = samplePoolCalibration(inputParam);
                    break;
                case SetWlList:
                    result = setWlList(inputParam);
                    break;
                default:
                    infoService.newReleaseDevice(deviceSerialNumber);
                    break;
            }
        } catch (Exception e) {
            log.info("设备操作异常，设备序列号: {}, 操作类型: {}", deviceSerialNumber, anEnum, e);
        } finally {
            //释放仪器，部分操作自主释放
            if (release && StringUtils.hasText(deviceSerialNumber)) {
                infoService.newReleaseDevice(deviceSerialNumber);
            }
        }
        return result;
    }

    private R<OperationResultEntity> samplePoolCalibration(DevOperationInputParam inputParam) {
        OperationResultEntity resultEntity = new OperationResultEntity();
        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        List<String> wavelength = inputParam.getWavelength();
        Integer cellPos = inputParam.getCellPos();

        if (wavelength == null || wavelength.isEmpty()) {
            return R.fail("00209020");
        }

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();
        Object[] param;
        String cmdId = "";
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        byte[] bytes;
        R<Object> mesure = new R<>();
        List<ChangeParam> changeParamList = new ArrayList<>();

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            log.info("串口零点");
        } else {

            cmdId = T600ComIdEnums.SetCellPos.getComId();
            Transition transition = xml.get(cmdId);
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            param = new Object[]{cellPos};
            bytes = protocolItemUtil.calcSendBytes(param, transition);

            mesure = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);

            if (!mesure.getSuccess()) {
                return R.fail("00209022");
            }

            for (String s : wavelength) {

                cmdId = T600ComIdEnums.SetWl.getComId();
                transition = xml.get(cmdId);
                replyNumber = transition.getReplyNumber();
                timeout = transition.getTimeout();
                headReceiveBytes = transition.getHeadReceiveBytes();

                double v = formulaUtil.calculateStep(Double.parseDouble(s), deviceSerialNumber);
                param = new Object[]{v};
                bytes = protocolItemUtil.calcSendBytes(param, transition);

                mesure = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);

                if (!mesure.getSuccess()) {
                    return R.fail("00209022");
                }
                deviceManager.setCurrentWavelength(deviceId, s);

                cmdId = T600ComIdEnums.Measure.getComId();
                transition = xml.get(cmdId);
                replyNumber = transition.getReplyNumber();
                timeout = transition.getTimeout();
                param = new Object[]{};
                bytes = protocolItemUtil.calcSendBytes(param, transition);
                headReceiveBytes = transition.getHeadReceiveBytes();
                mesure = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
                if (!mesure.getSuccess()) {
                    return R.fail("00209022");
                } else {
                    Object data = mesure.getData();
                    resultEntity = (OperationResultEntity) data;
                    ChangeParam measureResult = resultEntity.getMeasureResult();

                    String mode = device.getLuminosityMode();

                    if (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode())) {

                        String value = DataCountUtil.countErOrEs(measureResult, mode);

                        if(Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                            value = DataCountUtil.count1960ErOrEs(measureResult, mode);
                        }

                        measureResult.setValue(value);

                    } else if (mode.equals(WaveLengthEnum.abs.getCode()) || mode.equals(WaveLengthEnum.tPercent.getCode())) {
                        Map<String, String> darkInfo = info.getDarkInfo();
                        String z = "1";
                        if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                            z = "0.1";
                        }
//                        if (darkInfo != null && StringUtils.hasText(darkInfo.get(s))) {
//                            z = darkInfo.get(s);
//                        }
                        Double zVal = spectrumResultCalculator.getZFromDatabase2(deviceSerialNumber, s);
                        if (zVal != null) {
                            z = String.valueOf(zVal);
                        }
                        String tPercent = DataCountUtil.countTPercent(null, measureResult, Double.parseDouble(z));

                        if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                            tPercent = DataCountUtil.calcTPercent1960(null, measureResult, Double.parseDouble(z));
                        }

                        String abs = DataCountUtil.changeTPercentOrAbs(null, tPercent);
                        if (mode.equals(WaveLengthEnum.abs.getCode())) {
                            measureResult.setValue(abs);
                            measureResult.setOtherData(tPercent);
                        } else {
                            measureResult.setValue(tPercent);
                            measureResult.setOtherData(abs);
                        }
                    }
                    deviceManager.setMeasureValue(deviceId, measureResult.getValue());

                    measureResult.setWl(s);
                    changeParamList.add(measureResult);
                }
            }
        }
        resultEntity.setOriginalMeasureResult(changeParamList);

        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> mercuryLocate(DevOperationInputParam inputParam) {
        OperationResultEntity resultEntity = new OperationResultEntity();

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);

        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();
        Object[] param = new Object[]{};

        String cmdId = T800ComIdEnums.GotoMLamp.getComId();
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(cmdId);
        int replyNumber = transition.getReplyNumber();
        int timeout = transition.getTimeout();
        byte[] endBytes = null;
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

        R<Object> calibration = new R<>();

        boolean isDealOnTime = false;
        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {

        } else {

            if (replyNumber < 0) {
                String[] readBytes = transition.getReadBytes();
                if (readBytes != null && readBytes.length > 0) {
                    endBytes = new byte[4];
                    String fullStr = readBytes[0];
                    String[] parts = fullStr.split(":");

                    for (int j = 0; j < 4 && j < parts.length; j++) {
                        String part = parts[j].trim();
                        // 跳过非数字部分（比如 "Check"）
                        if (!part.matches("\\d+")) {
                            break; // 或 continue，根据逻辑决定
                        }
                        // 解析为 int（支持 0~255）
                        int intValue = Integer.parseInt(part);
                        // 安全转为 byte（保留低8位）
                        endBytes[j] = (byte) (intValue & 0xFF);
                    }
                }

                calibration = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, timeout, isDealOnTime, deviceSerialNumber, endBytes, headReceiveBytes);

            } else {
                calibration = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
            }

            if (!calibration.getSuccess()) {
                //TODO 国际化待定

                return R.fail("00209005");
            }

            resultEntity = (OperationResultEntity) calibration.getData();

        }
        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> setWlList(DevOperationInputParam inputParam) {

        OperationResultEntity resultEntity = new OperationResultEntity();

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);

        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();
        Object[] param = new Object[4];

        String cmdId = T600ComIdEnums.SetWlList.getComId();
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(cmdId);
        int replyNumber = transition.getReplyNumber();
        int timeout = transition.getTimeout();
        byte[] endBytes = null;
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();


        int total = inputParam.getSetWlList().size();

        for (int i = 0; i < total+1; i++) {
            if (i < total) {
                StepPoint point = inputParam.getSetWlList().get(i);

                param[0] = i + 1;
                param[1] = total;
                param[2] = point.getTheoryStep();
                param[3] = point.getActualStep();
                if (device.getXmlKey().equals(DevXmlEnums.T800.getDevType())) {
                    param[2] = point.getActualStep();
                    param[3] = point.getTheoryStep();
                }
            } else {
                if (device.getXmlKey().equals(DevXmlEnums.T800.getDevType())) {
                    cmdId = T800ComIdEnums.DoIt.getComId();
                    xml = xmlManage.getXmlInfo(xmlKey);
                    transition = xml.get(cmdId);
                    replyNumber = transition.getReplyNumber();
                    timeout = transition.getTimeout();
                    endBytes = null;
                    headReceiveBytes = transition.getHeadReceiveBytes();
                    param = new Object[]{1};
                } else {
                    break;
                }
            }

            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

            R<Object> calibration = new R<>();

            boolean isDealOnTime = false;
            if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {

            } else {

                if (replyNumber < 0) {
                    String[] readBytes = transition.getReadBytes();
                    if (readBytes != null && readBytes.length > 0) {
                        endBytes = new byte[4];
                        String fullStr = readBytes[0];
                        String[] parts = fullStr.split(":");

                        for (int j = 0; j < 4 && i < parts.length; j++) {
                            String part = parts[j].trim();
                            // 跳过非数字部分（比如 "Check"）
                            if (!part.matches("\\d+")) {
                                break; // 或 continue，根据逻辑决定
                            }
                            // 解析为 int（支持 0~255）
                            int intValue = Integer.parseInt(part);
                            // 安全转为 byte（保留低8位）
                            endBytes[j] = (byte) (intValue & 0xFF);
                        }
                    }

                    calibration = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, timeout, isDealOnTime, deviceSerialNumber, endBytes, headReceiveBytes);

                } else {
                    calibration = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
                }
            }
            if (!calibration.getSuccess()) {
                //TODO 国际化待定

                return R.fail("00209006");
            }


            resultEntity = (OperationResultEntity) calibration.getData();

        }
        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> queryLifeTime(DevOperationInputParam inputParam) {

        OperationResultEntity result = new OperationResultEntity();
        result.setDeviceSerialNumber(inputParam.getDeviceSerialNumber());
        List<WorkTimeInfo> workTimeInfo = inputParam.getWorkTimeInfo();
        if (workTimeInfo == null || workTimeInfo.isEmpty()) {
            return R.fail("00209007");
        }


        List<WorkTimeInfo> entity = new ArrayList<>();


        for (WorkTimeInfo timeInfo : workTimeInfo) {

            String cmdId = timeInfo.getWorkTime();

            R<OperationResultEntity> r = sendTime(cmdId, inputParam.getDeviceSerialNumber());
            if (!r.getSuccess()) {
                return r;
            }

            OperationResultEntity data = r.getData();
            float useTime = data.getUseTime();
            timeInfo.setWorkTime(String.valueOf(useTime));

            cmdId = timeInfo.getTotalWorkTime();
            r = sendTime(cmdId, inputParam.getDeviceSerialNumber());
            if (!r.getSuccess()) {
                return r;
            }
            data = r.getData();
            float totalWorkTime = data.getUseTime();
            timeInfo.setTotalWorkTime(String.valueOf(totalWorkTime));
            timeInfo.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            entity.add(timeInfo);

        }

        result.setWorkTimeInfo(entity);
        return R.ok(result);
    }

    private R<OperationResultEntity> sendTime(String cmdId, String deviceSerialNumber) {


        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();
        Object[] param = new Object[]{};

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(cmdId);
        int ReplyNumber = transition.getReplyNumber();
        int Timeout = transition.getTimeout();
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

        R<Object> r;

        if (ReplyNumber < 0) {
            r = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, Timeout, false, deviceSerialNumber, null, headReceiveBytes);
        } else {
            r = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);
        }

        if (!r.getSuccess()) {
            return R.fail(r.getMessage());
        }
        OperationResultEntity data = (OperationResultEntity) r.getData();
        return R.ok(data);
    }

    private R<OperationResultEntity> t800Connect(DevOperationInputParam inputParam) {
        OperationResultEntity result = new OperationResultEntity();

        SearchInfo info = inputParam.getDeviceInfo();


        String deviceId = deviceManager.getOrCreateDeviceId(info.getDeviceIp(), Integer.valueOf(info.getConnectPort()), info.getConnectType());
//        deviceId = deviceManager.convertToPermanentByDeviceId(deviceId);

        xmlManage.setDevice(info.getDeviceIp(), DevXmlEnums.T800.getDevType());
        xmlManage.setDevice(info.getDeviceMac(), DevXmlEnums.T800.getDevType());
        deviceManager.updateDeviceInfo(deviceId, Constants.CONNECT_DEVICE_NO, info.getDeviceMac(), DevXmlEnums.T800.getDevType());

        info.setDeviceId(deviceId);
        List<SearchInfo> ans = new ArrayList<>();
        ans.add(info);
        result.setSearchInfo(ans);
        return R.ok(result);
    }

    private SearchInfo addLastDevice(DeviceInfoPO info) {
        String ip = info.getIp();
        Integer port = info.getDevicePort();

        String xmlKey = info.getXmlKey();
        String deviceMac = info.getDeviceMac();
        Integer connectType = info.getConnectType();

        String deviceId = deviceManager.getOrCreateDeviceId(ip, port, connectType);
        deviceManager.updateDeviceInfo(deviceId, Constants.CONNECT_DEVICE_NO, deviceMac, xmlKey);

        SearchInfo result = new SearchInfo();
        result.setDeviceIp(ip);
        result.setConnectPort(port.toString());
        result.setDeviceMac(deviceMac);
        result.setConnectType(connectType);
        result.setDeviceId(deviceId);
        result.setUpdateBy(info.getUpdateBy());
        return result;
    }

    private Boolean checkDeviceConnect(SearchInfo info) {
        String deviceId = info.getDeviceId();
        Integer connectType = info.getConnectType();

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String devType = device.getXmlKey();
        DevXmlEnums anEnum = DevXmlEnums.getEnum(devType);
        String comId = ConnectEnum.Connect.getComId();
        Map<String, Transition> xml = xmlManage.getXmlInfo(devType);

        Transition transition = xml.get(comId);
        int replyNumber = transition.getReplyNumber();
        int timeout = transition.getTimeout();
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        Object[] param = new Object[6];
        String[] macParts = info.getDeviceMac().split(":");
        // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
        for (int i = 0; i < 6; i++) {

            int anInt = Integer.parseInt(macParts[i], 16);

            param[i] = anInt;
        }

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);
        R<Object> r;
        if (replyNumber < 0) {
            r = udpSendUtil.unknownReceive(deviceId, comId, bytes, timeout, false, null, null, headReceiveBytes);
        } else {
            r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, replyNumber, timeout, connectType, headReceiveBytes);

        }
        if (!r.getSuccess()) {
            deviceManager.removeDevice(deviceId);
            return false;
        }

        return true;

    }

    private R<OperationResultEntity> stop(DevOperationInputParam inputParam) {

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();

        String cmdId = "Stop";

        Object[] param = new Object[]{};

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(cmdId);
        int ReplyNumber = transition.getReplyNumber();
        int Timeout = transition.getTimeout();
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

        R<Object> stop;

        boolean Stop = SpectrumDataProcessor.isNotNearEnd();
        if (Stop) {
            if (ReplyNumber < 0) {
                stop = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, Timeout, false, deviceSerialNumber, null, headReceiveBytes);
            } else {
                stop = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);
            }
            if (!stop.getSuccess()) {
                return R.fail("00209008");
            }
        }
        return R.ok();
    }

    private R<OperationResultEntity> getValue(DevOperationInputParam inputParam) {

        OperationResultEntity resultEntity = new OperationResultEntity();

        /* ================= 1. 设备上下文准备 ================= */

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();

        // 默认连接方式：网口
        if (inputParam.getConnectType() == null) {
            inputParam.setConnectType(DeviceConnectTypeEnum.ETHERNET.getConnectType());
        }

        // 校验连接状态
        String macAddress = "";
        if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(inputParam.getConnectType())) {
        } else if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(inputParam.getConnectType())) {
            macAddress = connectUtil.getWifiMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("00209009");
            }
        } else {
            macAddress = connectUtil.getEthernetMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("0020900A");
            }
        }

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        List<Command> commandList = new ArrayList<>();

        for (GetParam e : GetParam.values()) {
            Object value = getFieldValueByGetter(inputParam.getParam(), e.getParamField());
            if (value != null) {

                Object[] paramArray = new Object[]{value};
                commandList.add(new Command(e.getCmdId(), paramArray));
            }
        }

        if (commandList.isEmpty()) {
            return R.fail("0020900B");
        }


        for (Command command : commandList) {

            Transition transition = xml.get(command.getCmdId());
            if (transition == null) {
                return R.fail("0020900C", command.getCmdId());
            }

            byte[] sendBytes = protocolItemUtil.calcSendBytes(
                    command.getParams(), transition);

            R<Object> result;
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            if (replyNumber < 0) {
                result = udpSendUtil.unknownReceive(
                        deviceId,
                        command.getCmdId(),
                        sendBytes,
                        timeout,
                        false,
                        deviceSerialNumber,
                        null,
                        headReceiveBytes
                );
            } else {
                result = udpSendUtil.knowTotalReceiveSend(
                        deviceId,
                        command.getCmdId(),
                        sendBytes,
                        replyNumber,
                        timeout,
                        connectType,
                        headReceiveBytes
                );
            }

            if (!result.getSuccess()) {
                return R.fail("0020900D", command.getCmdId());
            }
        }

        resultEntity.setIsSuccess(true);
        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> executeAction(
            DevOperationInputParam inputParam
    ) {

        boolean isDealOnTime = false;

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();

        if (inputParam.getConnectType() == null) {
            inputParam.setConnectType(DeviceConnectTypeEnum.ETHERNET.getConnectType());
        }

        if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(connectType)) {
            if (!StringUtils.hasText(connectUtil.getWifiMacAddress())) {
                return R.fail("00209009");
            }
        } else if (DeviceConnectTypeEnum.ETHERNET.getConnectType().equals(connectType)) {
            if (!StringUtils.hasText(connectUtil.getEthernetMacAddress())) {
                return R.fail("0020900A");
            }
        }

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        /* ================= 2. 执行动作命令 ================= */

        String cmdId = inputParam.getAction().getCmdId();

        Transition transition = xml.get(cmdId);
        if (transition == null) {
            return R.fail("0020900C", cmdId);
        }

        // 无参数动作
        byte[] sendBytes = protocolItemUtil.calcSendBytes(
                new Object[]{}, transition);

        R<Object> result;
        int replyNumber = transition.getReplyNumber();
        int timeout = transition.getTimeout();
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        if (cmdId.equals("Scan") || cmdId.equals("BaseLine") || cmdId.equals("ScanFast") || cmdId.equals("BaseLineFast") || cmdId.equals("Dark")) {
            isDealOnTime = true;
        }

        if (replyNumber < 0) {
            result = udpSendUtil.unknownReceive(
                    deviceId,
                    cmdId,
                    sendBytes,
                    timeout,
                    isDealOnTime,
                    deviceSerialNumber,
                    null,
                    headReceiveBytes
            );
        } else {
            result = udpSendUtil.knowTotalReceiveSend(
                    deviceId,
                    cmdId,
                    sendBytes,
                    replyNumber,
                    timeout,
                    connectType,
                    headReceiveBytes
            );
        }

        if (!result.getSuccess()) {
            return R.fail(result.getMessage());
        }

        // ================= 先取解析结果（关键） =================

        OperationResultEntity resultEntity = (OperationResultEntity) result.getData();
        return R.ok(resultEntity);
    }


    private R<OperationResultEntity> setParam(
            DevOperationInputParam inputParam
    ) {

        OperationResultEntity resultEntity = new OperationResultEntity();
        SetParam setParam = inputParam.getParam();

        if (setParam == null) {
            return R.fail("0020900E");
        }

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();

        if (inputParam.getConnectType() == null) {
            inputParam.setConnectType(DeviceConnectTypeEnum.ETHERNET.getConnectType());
        }

        String macAddress = "";
        if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(connectType)) {
        } else if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(connectType)) {
            macAddress = connectUtil.getWifiMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("00209009");
            }
        } else {
            macAddress = connectUtil.getEthernetMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("0020900A");
            }
        }

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        /* ================= 2. 参数 → Command 自动转换 ================= */

        List<Command> commandList = new ArrayList<>();


        for (ParamCmdEnum e : ParamCmdEnum.values()) {
            Object value = getFieldValueByGetter(setParam, e.getParamField());
            if (xmlKey.equals(DevXmlEnums.T600AndT700.getDevType()) && e.equals(ParamCmdEnum.CORR_STATUS)) {
                log.warn("T600AndT700 不支持 CORR_STATUS 参数");
                continue;
            }
            if (value != null) {
                // 默认参数
                Object[] paramArray = new Object[]{value};

                // 针对特定字段做额外处理
                if (e == ParamCmdEnum.START_WL
                        || e == ParamCmdEnum.END_WL
                        || e == ParamCmdEnum.Set_Wl
                        || e == ParamCmdEnum.Change_LampWl) {

                    try {
                        // 假设 value 是数值类型，或者能转成 Double
                        double doubleValue = Double.parseDouble(value.toString());
                        double step = formulaUtil.calculateStep(doubleValue, deviceSerialNumber);

                        // 替换参数
                        paramArray = new Object[]{step};
                    } catch (Exception ex) {
                        log.warn("计算 step 失败，使用原始值: {}", value, ex);
                        // 保底
                        paramArray = new Object[]{value};
                    }
                }

                Double startWl = setParam.getStartWl();
                Double endWl = setParam.getEndWl();
                Double interval = setParam.getScanWlInterval();

                if (startWl != null && endWl != null && interval != null) {
                    // 直接操作 device 对象
                    cfg.setEndWavelength(endWl);
                    cfg.setStartWavelength(startWl);
                    cfg.setInterval(interval);
                    cfg.setDescending(startWl > endWl);
                    device.setScanWavelengthConfig(cfg);

                    log.info("[setParam] 保存扫描波长配置: device={} start={} end={} interval={}",
                            deviceSerialNumber, startWl, endWl, interval);
                }

                // 构造 Command
                commandList.add(new Command(e.getCmdId(), paramArray));
            }
        }


        if (commandList.isEmpty()) {
            return R.fail("0020900F");
        }

        for (Command command : commandList) {

            Transition transition = xml.get(command.getCmdId());
            if (transition == null) {
                return R.fail("0020900C", command.getCmdId());
            }

            byte[] sendBytes = protocolItemUtil.calcSendBytes(
                    command.getParams(), transition);

            R<Object> result;
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            if (replyNumber < 0) {
                result = udpSendUtil.unknownReceive(
                        deviceId,
                        command.getCmdId(),
                        sendBytes,
                        timeout,
                        false,
                        deviceSerialNumber,
                        null,
                        headReceiveBytes
                );
            } else {
                result = udpSendUtil.knowTotalReceiveSend(
                        deviceId,
                        command.getCmdId(),
                        sendBytes,
                        replyNumber,
                        timeout,
                        connectType,
                        headReceiveBytes
                );
            }

            if (!result.getSuccess()) {
                return R.fail("00209010", command.getCmdId());
            }
        }

        if (setParam.getPhotoMode() != null) {
            deviceManager.setLuminosityMode(deviceId, String.valueOf(setParam.getPhotoMode()));
        }

        if (setParam.getSetWl() != null) {

            BigDecimal wl = BigDecimal.valueOf(setParam.getSetWl());
            String wlStr = wl.stripTrailingZeros().toPlainString();

            deviceManager.setCurrentWavelength(deviceId, wlStr);
        }

        if (setParam.getCellPos() != null) {
            deviceManager.setCurrentPoolNum(deviceId, setParam.getCellPos());
        }

        resultEntity.setIsSuccess(true);
        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> find(DevOperationInputParam inputParam) {


        String macAddress = "";

        OperationResultEntity operationResultEntity = new OperationResultEntity();

        if (inputParam.getConnectType() == null) {
            //默认网口连接
            inputParam.setConnectType(DeviceConnectTypeEnum.ETHERNET.getConnectType());
        }

        if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(inputParam.getConnectType())) {
            //串口
            log.info("串口连接");
            return R.fail("00209011");

        } else if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(inputParam.getConnectType())) {
            macAddress = connectUtil.getWifiMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("00209012");
            }

        } else {

            macAddress = connectUtil.getEthernetMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("00209013");
            }

        }

        Object[] param = new Object[]{0, 0, 0, 0, 0, 0};

        String[] macParts = macAddress.split(":");
        // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
        for (int i = 0; i < 6; i++) {

            int anInt = Integer.parseInt(macParts[i], 16);

            // 将每一部分从十六进制转换为十进制
            param[i] = anInt;
        }

        List<SearchInfo> allDevice = new ArrayList<>();

        List<DevXmlEnums> all = DevXmlEnums.getAll();
        for (DevXmlEnums anEnum : all) {

            String comId = T600ComIdEnums.SearchAround.getComId();

            Map<String, Transition> xml = xmlManage.getXmlInfo(anEnum.getDevType());

            //发送报文组
            Transition transition = xml.get(comId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            R<List<SearchInfo>> r = udpSendUtil.searchDevice(defaultIp, comId, bytes, Timeout, inputParam.getConnectType(), anEnum.getPort());
            if (r.getSuccess()) {
                List<SearchInfo> data = r.getData();
                allDevice.addAll(data);
                for (SearchInfo info : data) {
                    xmlManage.setDevice(info.getDeviceIp(), anEnum.getDevType());
                    xmlManage.setDevice(info.getDeviceMac(), anEnum.getDevType());
                    deviceManager.updateDeviceInfo(info.getDeviceId(), Constants.CONNECT_DEVICE_NO, info.getDeviceMac(), anEnum.getDevType());
                }
            }
        }

        if (allDevice.isEmpty()) {
            //连接上次连接的仪器
                List<DeviceInfoPO> poList = infoService.getUserLastConnectDevice(inputParam.getUpdateBy());

            if (poList != null && !poList.isEmpty()) {

                for (DeviceInfoPO infoPO : poList) {

                    SearchInfo info = addLastDevice(infoPO);
                    Boolean b = checkDeviceConnect(info);
                    if (b) {
                        allDevice.add(info);
                        xmlManage.setDevice(info.getDeviceIp(), infoPO.getXmlKey());
                        xmlManage.setDevice(info.getDeviceMac(), infoPO.getXmlKey());
                    }


                }
            }

        }

        operationResultEntity.setSearchInfo(allDevice);

        return R.ok(operationResultEntity);
    }

    private R<OperationResultEntity> connect(DevOperationInputParam inputParam) {

        String macAddress = "";
        String devType = "";
        Integer connectPort = null;
        String localhostIp = "127.0.0.1";

        SearchInfo deviceInfo = inputParam.getDeviceInfo();
        if (deviceInfo == null || !StringUtils.hasText(deviceInfo.getDeviceId())) {
            return R.fail("00209014");
        }

        boolean a = xmlManage.containsKeyInDeviceMap(deviceInfo.getDeviceIp());
        boolean b = xmlManage.containsKeyInDeviceMap(deviceInfo.getDeviceMac());

        if (!a && !b) {
            return R.fail("00209014");
        }
        if (a) {
            devType = xmlManage.getDevType(deviceInfo.getDeviceIp());
        } else {
            devType = xmlManage.getDevType(deviceInfo.getDeviceMac());
        }
        String sendIp = deviceInfo.getDeviceIp();

        OperationResultEntity result = new OperationResultEntity();

        DeviceConnectInfo device = deviceManager.getDevice(deviceInfo.getDeviceId());
        if (device == null) {
            return R.fail("00209015");
        }
        synchronized (device) {
            if (device.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS) || device.getConnectStatus().equals(Constants.CONNECT_DEVICE_ING)) {
                return R.fail("00209015");
            }
            deviceManager.updateDeviceInfo(deviceInfo.getDeviceId(), Constants.CONNECT_DEVICE_ING, deviceInfo.getDeviceMac(), devType);

        }

        Integer connectType = device.getConnectType();

        if (connectType == null) {
            //默认网口连接
            connectType = 1;
        }

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            //串口
            return R.fail("00209016");
        } else if (connectType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
            macAddress = connectUtil.getWifiMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("00209017");
            }
            connectPort = connectUtil.getWifiPort();
            localhostIp = connectUtil.getWifiLocalIp();

        } else {

            macAddress = connectUtil.getEthernetMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("00209018");
            }
            connectPort = connectUtil.getEthernetPort();
            localhostIp = connectUtil.getEthernetLocalIp();

            String ethernetIp = connectUtil.getEthernetLocalIp();
            String[] split = ethernetIp.split("\\.");
            String[] deviceIp = sendIp.split("\\.");
            if (!split[0].equals(deviceIp[0]) || !split[1].equals(deviceIp[1]) || !split[2].equals(deviceIp[2])) {


                Object[] param = new Object[]{0, 0, 0, 0, 0, 0, 52, 218, 166, 101, 144, 86, 169, 254, 136, 253};

                String[] macParts = macAddress.split(":");
                // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
                for (int i = 0; i < 6; i++) {

                    int anInt = Integer.parseInt(macParts[i], 16);

                    param[i] = anInt;  // 将每一部分从十六进制转换为十进制
                }

                String deviceMac = deviceInfo.getDeviceMac();
                String[] deviceMacParts = deviceMac.split(":");
                for (int i = 0; i < deviceMacParts.length; i++) {
                    // 将 MAC 地址的十进制值填充到 param 数组的相应位置
                    param[6 + i] = deviceMacParts[i];
                }

                StringBuilder newIp = new StringBuilder();

                String[] deviceIpParts = ethernetIp.split("\\.");
                for (int i = 0; i <= 2; i++) {
                    // 逆序插入 IP 地址的最后 3 个部分
                    param[12 + i] = Integer.parseInt(deviceIpParts[i]);
                    newIp.append(deviceIpParts[i]).append(".");
                }


                String baseIp = newIp.toString();
                Integer s = allocateIp(baseIp, connectPort, deviceInfo.getDeviceMac());
                if (s == null) {
                    deviceManager.removeDevice(deviceInfo.getDeviceId());
                    return R.fail("00209019");
                }

                param[param.length - 1] = String.valueOf(s);
                newIp.append(param[param.length - 1]);

                String comId = T600ComIdEnums.NewIP.getComId();
                Map<String, Transition> xml = xmlManage.getXmlInfo(devType);
                Transition transition = xml.get(comId);
                int ReplyNumber = transition.getReplyNumber();
                int Timeout = transition.getTimeout();
                byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);
                List<String> headReceiveBytes = transition.getHeadReceiveBytes();

                R<String> r = udpSendUtil.deviceNewIp(deviceInfo.getDeviceId(), comId, bytes, ReplyNumber, Timeout, connectType, newIp.toString(), headReceiveBytes);
                if (!r.getSuccess()) {
                    deviceManager.removeDevice(deviceInfo.getDeviceId());
                    return R.fail("0020901A");
                }
                sendIp = newIp.toString();
                deviceInfo.setDeviceId(r.getData());
                deviceInfo.setDeviceIp(newIp.toString());

                xmlManage.removeDevice(deviceInfo.getDeviceIp());
                xmlManage.setDevice(sendIp, devType);
            }
        }

        result.setConnectType(connectType);
        result.setConnectPort(connectPort);
        result.setMachineIp(localhostIp);

        Object[] param = new Object[]{};
        String deviceId = deviceInfo.getDeviceId();


        DevXmlEnums devXmlEnums = DevXmlEnums.getEnum(devType);
        List<String> connectCommands = devXmlEnums.getConnectCommands();

        for (String comId : connectCommands) {
            Map<String, Transition> xml = xmlManage.getXmlInfo(devType);
            Transition transition = xml.get(comId);
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            try {
                if (comId.equals(ConnectEnum.Connect.getComId())) {
                    param = new Object[6];
                    String[] macParts = macAddress.split(":");
                    // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
                    for (int i = 0; i < 6; i++) {

                        int anInt = Integer.parseInt(macParts[i], 16);

                        param[i] = anInt;  // 将每一部分从十六进制转换为十进制
                    }
                }

                if (comId.equals(ConnectEnum.SetCellPos.getComId()) || comId.equals(ConnectEnum.GetCellPos.getComId())) {
                    if (result.getAttachmentType() == null) {
                        continue;
                    }

                    DeviceAttachmentTypeVO attachment = infoService.getAttachment(result.getAttachmentType(), result.getDeviceSerialNumber());
                    if (attachment == null || attachment.getAttachmentKind() == null) {
                        continue;
                    }
                    Integer kind = attachment.getAttachmentKind();

                    if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType()) || kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())) {
                        param = new Object[]{1};
                    } else {
                        result.setSamplePoolNum(1);
                        continue;
                    }
                }

                byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);
                R<Object> r;
                if (replyNumber < 0) {
                    r = udpSendUtil.unknownReceive(deviceId, comId, bytes, timeout, false, null, null, headReceiveBytes);
                } else {
//                r = udpSendUtil.knowTotalReceiveSend1(deviceId, comId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
                    r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, replyNumber, timeout, connectType, headReceiveBytes);

                }
                if (!r.getSuccess()) {

                    deviceManager.removeDevice(deviceId);

                    return R.fail("0020901A");
                }

                if (comId.equals(ConnectEnum.Connect.getComId()) || comId.equals(ConnectEnum.TestConnect.getComId())) {

                    String s = deviceManager.convertToPermanentByDeviceId(deviceInfo.getDeviceId());
                    result.setIp(deviceInfo.getDeviceIp());
                    result.setDeviceId(s);
                    deviceId = s;
                    result.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
                    deviceManager.setDeviceIdleStatus(deviceId, true);
                    deviceManager.updateDeviceInfo(deviceId, Constants.DEVICE_STATUS_IDLE, macAddress, devType);
                }

                Object data = r.getData();

                if (comId.equals(ConnectEnum.GetWlList.getComId()) || comId.equals(ConnectEnum.T800GetWlList.getComId())) {

                    OperationResultEntity entity = (OperationResultEntity) data;
                    List<StepPoint> stepPoints = entity.getStepPoints();
                    String s = formulaUtil.addFormula(stepPoints, result.getDeviceSerialNumber());
                    result.setStepPoints(stepPoints);

                    if (comId.equals(ConnectEnum.GetWlList.getComId())) {
                        result.setLinearSegment(s);
                    }

                    result.setLinearSegment(s);
                    continue;
                }

                OperationResultEntity entity = (OperationResultEntity) data;
                if (!entity.getIsSuccess()) {
                    log.error("指令[{}]解析出错，错误原因：{}", comId, entity.getErrorMsg());
                    deviceManager.removeDevice(deviceId);
                    return R.fail("0020901B", comId);
                }
                BeanUtils.copyProperties(entity, result, getNullPropertyNames(entity));
            } catch (Exception e) {
                log.error("指令{}，操作过程中数据转换错误，错误信息：{}", comId, e.getMessage());
                deviceManager.removeDevice(deviceId);
                return R.fail("0020901C");
            }

            param = new Object[]{};
        }

        if (result.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
            setDeviceDefaultStatus(result.getDeviceId(), result.getDeviceSerialNumber());
        }
        result.setXmlKey(devType);
        result.setDeviceMac(macAddress);

        return R.ok(result);
    }

    /**
     * 获取ip最后一位
     */
    private Integer allocateIp(String baseIp, Integer connectPort, String macAddress) {

        // 如果提供了 MAC 地址，优先使用 MAC 地址生成唯一 IP
        if (macAddress != null && !macAddress.isEmpty() && baseIp != null) {
            Integer candidate = generateIpFromMac(macAddress);
            if (candidate != null) {
                String ip = baseIp + candidate;
                String localIp = connectUtil.getEthernetLocalIp();

                // 检查是否与本地 IP 冲突
                if (!ip.equals(localIp)) {
                    // 检查设备是否存在
                    boolean exists = deviceManager.checkDeviceExist(ip, connectPort);
                    if (!exists) {
                        return candidate;
                    }
                }
            }
        }

        int attempts = 0;
        while (attempts < size) {
            // 原子地获取当前候选值，并更新计数器为下一个值
            int candidate = newIpEnd.getAndUpdate(prev ->
                    prev <= ipEndMin ? ipEndMax : prev - 1
            );

            String ip = baseIp + candidate;
            String localIp = connectUtil.getEthernetLocalIp();
            if (ip.equals(localIp)) {
                attempts++;
                continue;
            }

            // 检查设备是否存在（仅检查，不占用）
            boolean exists = deviceManager.checkDeviceExist(ip, connectPort);
            if (!exists) {
                return candidate;
            }
            attempts++;
        }
        return null;
    }

    /**
     * 根据 MAC 地址生成唯一的 IP 最后一位
     *
     * @param macAddress 设备的 MAC 地址
     * @return IP 最后一位 (1-254)
     */
    private Integer generateIpFromMac(String macAddress) {
        if (macAddress == null || macAddress.isEmpty()) {
            return null;
        }

        // 移除 MAC 地址中的分隔符（: 或 -）并转换为小写
        String cleanMac = macAddress.replace(":", "").replace("-", "").toLowerCase();

        // 使用哈希算法生成唯一值
        int hashCode = cleanMac.hashCode();

        // 确保为正数
        hashCode = Math.abs(hashCode);

        // 映射到 1-254 范围
        int ipLastDigit = (hashCode % 254) + 1;

        return ipLastDigit;
    }

    private void initDevice(String deviceSerialNumber, Boolean isInitFromHead) {


//        String deviceSerialNumber = inputParam.getDeviceSerialNumber();

        DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
        while (!infoPO.getIsInited().equals(Constants.INIT_STATUS_RUNNING)) {
            try {
                Thread.sleep(500);
                infoPO = infoService.getInfo(deviceSerialNumber);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

//        LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(DeviceModelInfoPO::getDevModel, infoPO.getDeviceModel());
//        DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);
//
//        List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();
//
//        DeviceAttachmentTypeVO attachmentInfo = infoService.getAttachmentInfo(deviceSerialNumber);
//        Integer kind = attachmentInfo.getAttachmentKind();
//
//        if (isInitFromHead) {
//            if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType())|| kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())){
//                List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
//                initRecordService.initAllTask(deviceSerialNumber, infoPO.getId(), allInitEntry);
//            } else {
//                initRecordService.initAllTask(deviceSerialNumber, infoPO.getId(), initEntry);
//            }
//        }

        log.info("自检开始");
        String deviceId = infoPO.getDeviceId();
        Integer attachmentType = infoPO.getAttachmentType();
        DeviceConstants.isStopInit.put(deviceSerialNumber, false);

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();

        String xmlKey = device.getXmlKey();
        Map<String, Transition> xmlInfo = xmlManage.getXmlInfo(xmlKey);

        List<InitRecordPO> list = initRecordService.searchInitStatus(deviceSerialNumber, infoPO.getId());
        for (InitRecordPO recordPO : list) {

            if (!isInitFromHead && recordPO.getInitStatus().equals(Constants.INIT_TASK_SUCCESS)) {
                continue;
            }
            updateRunning(recordPO);
            recordPO = initRecordService.selectById(recordPO.getId());

            List<String> cmdIdList = recordPO.getInitCmdIdList();

            int count = 0;
            while (count < cmdIdList.size() && !DeviceConstants.isStopInit.get(deviceSerialNumber)) {
                String cmdId = cmdIdList.get(count);
                Command command = dealCommand(attachmentType, cmdId);
                try {

                    Transition transition = xmlInfo.get(cmdId);
                    int timeout = transition.getTimeout();
                    int replyNumber = transition.getReplyNumber();
                    List<String> headReceiveBytes = transition.getHeadReceiveBytes();
                    //Todo 结束前缀
                    byte[] endBytes = new byte[4];

                    byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
                    R<Object> initDevice = new R<>();
                    if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(connectType)) {
                        log.info("串口初始化");
                        //Todo 串口初始化待做
                        initDevice = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, timeout, false, deviceSerialNumber, endBytes, headReceiveBytes);
                    } else {
                        if (replyNumber < 0) {
//
                            String[] readBytes = transition.getReadBytes();
                            if (readBytes != null && readBytes.length > 0) {
                                endBytes = new byte[4];
                                String fullStr = readBytes[0];
                                String[] parts = fullStr.split(":");

                                for (int i = 0; i < 4 && i < parts.length; i++) {
                                    String part = parts[i].trim();
                                    // 跳过非数字部分（比如 "Check"）
                                    if (!part.matches("\\d+")) {
                                        break; // 或 continue，根据逻辑决定
                                    }
                                    // 解析为 int（支持 0~255）
                                    int intValue = Integer.parseInt(part);
                                    // 安全转为 byte（保留低8位）
                                    endBytes[i] = (byte) (intValue & 0xFF);
//                                        System.out.println(endBytes[i]);
                                }
                            }

                            initDevice = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, timeout, false, deviceSerialNumber, endBytes, headReceiveBytes);
                        } else {
                            initDevice = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
                        }
                    }

                    if (!initDevice.getSuccess()) {
                        log.info("{}指令初始化失败", cmdId);

//                        setInitStatus(deviceId, 0);
                        infoService.newReleaseDevice(deviceSerialNumber);
                        failDeal(infoPO, recordPO);
                        return;
                    }
                    count++;
                    log.info("{}指令初始化成功", cmdId);
                } catch (Exception e) {
                    log.info("{}指令初始化异常", cmdId);
                    failDeal(infoPO, recordPO);
                    infoService.newReleaseDevice(deviceSerialNumber);
                    return;
                }

            }
            if (count != cmdIdList.size()) {
                log.info("自检已停止");
                updateStop(recordPO, infoPO);
                infoService.newReleaseDevice(deviceSerialNumber);
                return;
            }
            log.info("{}成功", recordPO.getInitCmdName());
            updateSuccess(recordPO);

        }
        log.info("自检结束");
        setInitStatus(deviceId, 1);
        R<Boolean> r = setDeviceDefaultStatus(deviceId, deviceSerialNumber);

        infoPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);
        infoService.newReleaseDevice(deviceSerialNumber);

    }

    private R<OperationResultEntity> liveDetection(String deviceSerialNumber) {

        OperationResultEntity resultEntity = new OperationResultEntity();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);

        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();

        Object[] param = new Object[]{};
        DevXmlEnums devXmlEnums = DevXmlEnums.getEnum(xmlKey);
        List<String> detectionCommands = devXmlEnums.getDetectionCommands();

        for (String comId : detectionCommands) {
            Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
            Transition transition = xml.get(comId);
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();
            byte[] endBytes = null;
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();


            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

            R<Object> liveDetection = new R<>();
            if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {

            } else {


                if (replyNumber < 0) {
                    String[] readBytes = transition.getReadBytes();
                    if (readBytes != null && readBytes.length > 0) {
                        endBytes = new byte[4];
                        String fullStr = readBytes[0];
                        String[] parts = fullStr.split(":");

                        for (int i = 0; i < 4 && i < parts.length; i++) {
                            String part = parts[i].trim();
                            // 跳过非数字部分（比如 "Check"）
                            if (!part.matches("\\d+")) {
                                break; // 或 continue，根据逻辑决定
                            }
                            int intValue = Integer.parseInt(part); // 解析为 int（支持 0~255）
                            endBytes[i] = (byte) (intValue & 0xFF); // 安全转为 byte（保留低8位）
                        }
                    }
                    liveDetection = udpSendUtil.unknownReceive(deviceId, comId, bytes, timeout, false, deviceSerialNumber, endBytes, headReceiveBytes);
                } else {
                    log.info("+++{}", LocalDateTime.now());
//                    liveDetection = udpSendUtil.knowTotalReceiveSend1(deviceId,comId, bytes,replyNumber,timeout,connectType, headReceiveBytes);
                    liveDetection = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
                    log.info("+++{}", LocalDateTime.now());
                }
            }

            if (!liveDetection.getSuccess()) {

                if (liveDetection.getMessage() != null && liveDetection.getMessage().equals(DeviceConstants.unCountResponse)) {
                    log.info("{}指令无响应", comId);

                    DeviceUsageLogPO logPO = new DeviceUsageLogPO();
                    logPO.setDeviceSerialNumber(deviceSerialNumber);
                    DeviceConnectionManager manager = DeviceConnectionManager.getInstance();
                    LocalDateTime connectTime = device.getConnectTime();
                    logPO.setStartTime(connectTime);

                    deviceUsageLogService.saveDeviceUsageLog(logPO);

                    deviceManager.removeDevice(deviceId);
                    DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
                    infoPO.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
                    infoService.saveOrUpdateInfo(infoPO);
                }
                //TODO 实时回显指令出错需要另外处理
                deviceManager.removeDevice(deviceId);

                log.error("{}指令执行失败", comId);
                return R.fail(liveDetection.getMessage());
            }

            Object data = liveDetection.getData();

            try {
                OperationResultEntity entity = (OperationResultEntity) data;
                if (!entity.getIsSuccess()) {
                    log.error("指令[{}]解析出错，错误原因：{}", comId, entity.getErrorMsg());
                    return R.fail("0020901B", comId);
                }
                BeanUtils.copyProperties(entity, resultEntity, getNullPropertyNames(entity));

                if (comId.equals(LiveDetectionCmdEnum.GetDLampStatus.getComId())) {
                    if (resultEntity.getDataList() == null) {
                        resultEntity.setDataList(new ArrayList<>());
                    }

                    DetectionManyDevicesLampStatus status = new DetectionManyDevicesLampStatus();
                    status.setLabel("氘灯");
                    status.setValue(entity.getLampStatus() ? "亮" : "不亮");

                    List<DetectionManyDevicesLampStatus> dataList = resultEntity.getDataList();
                    dataList.add(status);

                    resultEntity.setDataList(dataList);
                } else if (comId.equals(LiveDetectionCmdEnum.GetWLampStatus.getComId())) {
                    if (resultEntity.getDataList() == null) {
                        resultEntity.setDataList(new ArrayList<>());
                    }

                    DetectionManyDevicesLampStatus status = new DetectionManyDevicesLampStatus();
                    status.setLabel("钨灯");
                    status.setValue(entity.getLampStatus() ? "亮" : "不亮");
                    List<DetectionManyDevicesLampStatus> dataList = resultEntity.getDataList();
                    dataList.add(status);

                    resultEntity.setDataList(dataList);
                } else if (comId.equals(LiveDetectionCmdEnum.GetCellPos.getComId())) {
                    Integer currentPoolNum = resultEntity.getCurrentPoolNum();
                    deviceManager.setCurrentPoolNum(deviceId, currentPoolNum);
                }

                param = new Object[]{};
            } catch (Exception e) {
                log.error("指令{}，操作过程中数据转换错误，错误信息：{}", comId, e.getMessage());
                return R.fail("0020901C");
            }
        }

        device = deviceManager.getDevice(deviceId);

        Map<String, String> darkInfo = info.getDarkInfo();
        String z = "1";
        if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
            z = "0.1";
        }
//        if (darkInfo != null && StringUtils.hasText(darkInfo.get(device.getCurrentWavelength()))) {
//            z = darkInfo.get(device.getCurrentWavelength());
//        }

        Double zVal = spectrumResultCalculator.getZFromDatabase2(deviceSerialNumber, device.getCurrentWavelength());
        if (zVal != null) {
            z = String.valueOf(zVal);
        }

        ChangeParam measureParam = resultEntity.getMeasureResult();
        if (!Objects.isNull(measureParam)) {

            String luminosity = device.getLuminosityMode();
            if (!StringUtils.hasText(luminosity)) {
                luminosity = WaveLengthEnum.abs.getCode();
            }

            String wl = device.getCurrentWavelength();
            resultEntity.setX(wl + "nm");
            measureParam.setWl(wl);

            String poolValue = null;

            Integer currentPoolNum = deviceManager.getCurrentPoolNum(deviceId);
            FSamplePoolStatus poolStatus = samplePoolConfigService.getPoolCorrectionData(deviceSerialNumber, currentPoolNum);
            if (poolStatus != null) {
                if (poolStatus.getLuminosity() != null && poolStatus.getLuminosity().equals(luminosity)) {
                    Map<String, String> poolData = poolStatus.getPoolData();
                    poolValue = poolData.get(wl);
                }
            }

            if (luminosity.equals(WaveLengthEnum.abs.getCode())) {

                ChangeParam zeroParam = device.getZeroResult(measureParam.getWl());

                String s;
                if (!Objects.isNull(zeroParam)) {
                    s = DataCountUtil.countTPercent(zeroParam, measureParam, Double.valueOf(z));
                    if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                        s = DataCountUtil.calcTPercent1960(zeroParam, measureParam, Double.parseDouble(z));
                    }
                } else {
                    s = DataCountUtil.countTPercent(null, measureParam, Double.valueOf(z));
                    if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                        s = DataCountUtil.calcTPercent1960(null, measureParam, Double.parseDouble(z));
                    }
                }
                String abs = DataCountUtil.changeTPercentOrAbs(null, s);
                if (poolValue != null) {
                    abs = samplePoolConfigService.applyPoolCorrection(abs, poolValue, luminosity);
                }

                resultEntity.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, abs));
            } else if (luminosity.equals(WaveLengthEnum.es.getCode()) || luminosity.equals(WaveLengthEnum.er.getCode())) {
                String s = DataCountUtil.countErOrEs(measureParam, luminosity);

                if(Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                    s = DataCountUtil.count1960ErOrEs(measureParam, luminosity);
                }

                resultEntity.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, s));
            } else if (luminosity.equals(WaveLengthEnum.tPercent.getCode())) {
                ChangeParam zeroParam = device.getZeroResult(measureParam.getWl());
                String s;

                if (!Objects.isNull(zeroParam)) {
                    s = DataCountUtil.countTPercent(zeroParam, measureParam, Double.valueOf(z));

                    if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                        s = DataCountUtil.calcTPercent1960(zeroParam, measureParam, Double.parseDouble(z));
                    }
                } else {
                    s = DataCountUtil.countTPercent(null, measureParam, Double.valueOf(z));
                    if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                        s = DataCountUtil.calcTPercent1960(null, measureParam, Double.parseDouble(z));
                    }
                }

                if (poolValue != null) {
                    s = samplePoolConfigService.applyPoolCorrection(s, poolValue, luminosity);
                }
                resultEntity.setY(DataCountUtil.formatValueWithBigDecimal(Constants.DEFAULT_DECIMAL_NUM, s));
            }
        }
        deviceManager.setMeasureValue(deviceId, resultEntity.getY());

        resultEntity.setLampPos(EnergyLampEnums.getName(resultEntity.getLampPos()));

        resultEntity.setLuminosity(WaveLengthEnum.getName(device.getLuminosityMode()));

        Double v = formulaUtil.calculateWl(Double.parseDouble(resultEntity.getChangeLampWl()), deviceSerialNumber);
        resultEntity.setChangeLampWl(String.valueOf(v));

        if (resultEntity.getDataList() == null) {
            resultEntity.setDataList(new ArrayList<>());
        }

        device = deviceManager.getDevice(deviceId);
        resultEntity.setY(resultEntity.getY() + WaveLengthEnum.getName(device.getLuminosityMode()));
        if (!StringUtils.hasText(resultEntity.getBandwidth())) {
            resultEntity.setBandwidth("2.0");
        }
        return R.ok(resultEntity);

    }


    private R<OperationResultEntity> calibration(DevOperationInputParam inputParam) {
        OperationResultEntity resultEntity = new OperationResultEntity();

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);

        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();

        Object[] param = new Object[]{};

        String cmdId = DeviceCalibrationInfoEnum.getComId(inputParam.getCalibrationType());
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(cmdId);
        int replyNumber = transition.getReplyNumber();
        int timeout = transition.getTimeout();
        byte[] endBytes = null;
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

        R<Object> calibration = new R<>();

        boolean isDealOnTime = false;
        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {

        } else {

            if (replyNumber < 0) {
                String[] readBytes = transition.getReadBytes();
                if (readBytes != null && readBytes.length > 0) {
                    endBytes = new byte[4];
                    String fullStr = readBytes[0];
                    String[] parts = fullStr.split(":");

                    for (int i = 0; i < 4 && i < parts.length; i++) {
                        String part = parts[i].trim();
                        // 跳过非数字部分（比如 "Check"）
                        if (!part.matches("\\d+")) {
                            break; // 或 continue，根据逻辑决定
                        }
                        // 解析为 int（支持 0~255）
                        int intValue = Integer.parseInt(part);
                        // 安全转为 byte（保留低8位）
                        endBytes[i] = (byte) (intValue & 0xFF);
                    }
                }

//                isDealOnTime = xmlKey.equals(DevXmlEnums.T800.getDevType());
                isDealOnTime = inputParam.getCalibrationType().equals(DeviceCalibrationInfoEnum.T800Dark.getType());

                calibration = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, timeout, isDealOnTime, deviceSerialNumber, endBytes, headReceiveBytes);

            } else {
                calibration = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
            }
        }
        if (!calibration.getSuccess()) {
            //TODO 国际化待定
            String description = DeviceCalibrationInfoEnum.getDescription(inputParam.getCalibrationType());
            infoService.newReleaseDevice(deviceSerialNumber);
            return R.fail(description + "失败");
        }

        resultEntity = (OperationResultEntity) calibration.getData();

        if (!isDealOnTime) {
            infoService.newReleaseDevice(deviceSerialNumber);
        }
        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> zero(DevOperationInputParam inputParam) {

        OperationResultEntity resultEntity = new OperationResultEntity();
        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        List<String> wavelength = inputParam.getWavelength();

        if (wavelength == null || wavelength.isEmpty()) {
//            return R.fail("请选择要零点的波长");
            wavelength = Arrays.asList("660.0");
        }

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();
        Object[] param;
        String cmdId = "";
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        byte[] bytes;
        R<Object> zero = new R<>();
        List<ChangeParam> changeParamList = new ArrayList<>();

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            log.info("串口零点");
        } else {
            for (String s : wavelength) {

                cmdId = T600ComIdEnums.SetWl.getComId();
                Transition transition = xml.get(cmdId);
                int ReplyNumber = transition.getReplyNumber();
                int Timeout = transition.getTimeout();
                List<String> headReceiveBytes = transition.getHeadReceiveBytes();

                Double v = formulaUtil.calculateStep(Double.parseDouble(s), deviceSerialNumber);
                param = new Object[]{v};
                bytes = protocolItemUtil.calcSendBytes(param, transition);

                zero = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);

                if (!zero.getSuccess()) {
                    return R.fail("00209021");
                }
                deviceManager.setZeroWavelength(deviceId, s);
                deviceManager.setCurrentWavelength(deviceId, s);

                cmdId = T600ComIdEnums.Zero.getComId();
                transition = xml.get(cmdId);
                ReplyNumber = transition.getReplyNumber();
                Timeout = transition.getTimeout();
                param = new Object[]{};
                bytes = protocolItemUtil.calcSendBytes(param, transition);
                headReceiveBytes = transition.getHeadReceiveBytes();
                zero = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);
                if (!zero.getSuccess()) {
                    return R.fail("00209021");
                } else {
                    Object data = zero.getData();
                    resultEntity = (OperationResultEntity) data;
                    ChangeParam measureResult = resultEntity.getMeasureResult();
                    measureResult.setWl(s);
                    changeParamList.add(measureResult);
                    deviceManager.addZeroResult(deviceId, s, measureResult);
                }
            }
        }
        resultEntity.setZeroResult(changeParamList);

        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> singleWavelengthZero(DevOperationInputParam inputParam) {

        OperationResultEntity resultEntity = new OperationResultEntity();
        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        String wavelength = inputParam.getSingleWavelength();


        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();
        Object[] param;
        String cmdId = "";
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        byte[] bytes;
        R<Object> zero = new R<>();
        ChangeParam measureResult = new ChangeParam();

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            log.info("串口零点");
        } else {


            cmdId = T600ComIdEnums.SetWl.getComId();
            Transition transition = xml.get(cmdId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            Double v = formulaUtil.calculateStep(Double.parseDouble(wavelength), deviceSerialNumber);
            param = new Object[]{v};
            bytes = protocolItemUtil.calcSendBytes(param, transition);

            zero = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);

            if (!zero.getSuccess()) {
                return R.fail("00209021");
            }

            deviceManager.setZeroWavelength(deviceId, wavelength);
            deviceManager.setCurrentWavelength(deviceId, wavelength);


            cmdId = T600ComIdEnums.Zero.getComId();
            transition = xml.get(cmdId);
            ReplyNumber = transition.getReplyNumber();
            Timeout = transition.getTimeout();
            param = new Object[]{};
            bytes = protocolItemUtil.calcSendBytes(param, transition);
            headReceiveBytes = transition.getHeadReceiveBytes();
            zero = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);
            if (!zero.getSuccess()) {
                return R.fail("00209021");
            } else {
                Object data = zero.getData();
                resultEntity = (OperationResultEntity) data;
                measureResult = resultEntity.getMeasureResult();
                measureResult.setWl(wavelength);
                deviceManager.addZeroResult(deviceId, wavelength, measureResult);
            }
        }
        resultEntity.setSingZeroResult(measureResult);

        return R.ok(resultEntity);
    }

    private R<OperationResultEntity> measure(DevOperationInputParam inputParam) {
        OperationResultEntity resultEntity = new OperationResultEntity();
        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        List<String> wavelength = inputParam.getWavelength();

        if (wavelength == null || wavelength.isEmpty()) {
            return R.fail("00209020");
        }

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();
        Object[] param;
        String cmdId = "";
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        byte[] bytes;
        R<Object> mesure = new R<>();
        List<ChangeParam> changeParamList = new ArrayList<>();

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            log.info("串口零点");
        } else {
            for (String s : wavelength) {

                cmdId = T600ComIdEnums.SetWl.getComId();
                Transition transition = xml.get(cmdId);
                int replyNumber = transition.getReplyNumber();
                int timeout = transition.getTimeout();
                List<String> headReceiveBytes = transition.getHeadReceiveBytes();

                Double v = formulaUtil.calculateStep(Double.parseDouble(s), deviceSerialNumber);
                param = new Object[]{v};
                bytes = protocolItemUtil.calcSendBytes(param, transition);

                mesure = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);

                if (!mesure.getSuccess()) {
                    return R.fail("00209022");
                }
                deviceManager.setCurrentWavelength(deviceId, s);

                cmdId = T600ComIdEnums.Measure.getComId();
                transition = xml.get(cmdId);
                replyNumber = transition.getReplyNumber();
                timeout = transition.getTimeout();
                param = new Object[]{};
                bytes = protocolItemUtil.calcSendBytes(param, transition);
                headReceiveBytes = transition.getHeadReceiveBytes();
                mesure = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
                if (!mesure.getSuccess()) {
                    return R.fail("00209022");
                } else {
                    Object data = mesure.getData();
                    resultEntity = (OperationResultEntity) data;
                    ChangeParam measureResult = resultEntity.getMeasureResult();


                    ChangeParam zeroResult = device.getZeroResult(s);

                    String mode = device.getLuminosityMode();

                    if (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode())) {

                        String value = DataCountUtil.countErOrEs(measureResult, mode);

                        if(Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                            value = DataCountUtil.count1960ErOrEs(measureResult, mode);
                        }

                        measureResult.setValue(value);

                    } else if (mode.equals(WaveLengthEnum.abs.getCode()) || mode.equals(WaveLengthEnum.tPercent.getCode())) {
                        Map<String, String> darkInfo = info.getDarkInfo();
                        String z = "1";
                        if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                            z = "0.1";
                        }
//                        if (darkInfo != null && StringUtils.hasText(darkInfo.get(s))) {
//                            z = darkInfo.get(s);
//                        }
                        Double zVal = spectrumResultCalculator.getZFromDatabase2(deviceSerialNumber, s);
                        if (zVal != null) {
                            z = String.valueOf(zVal);
                        }
                        String tPercent = DataCountUtil.countTPercent(zeroResult, measureResult, Double.parseDouble(z));

                        if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                            tPercent = DataCountUtil.calcTPercent1960(zeroResult, measureResult, Double.parseDouble(z));
                        }

                        String abs = DataCountUtil.changeTPercentOrAbs(null, tPercent);
                        if (mode.equals(WaveLengthEnum.abs.getCode())) {
                            measureResult.setValue(abs);
                            measureResult.setOtherData(tPercent);
                        } else {
                            measureResult.setValue(tPercent);
                            measureResult.setOtherData(abs);
                        }
                    }
                    deviceManager.setMeasureValue(deviceId, measureResult.getValue());

                    measureResult.setWl(s);
                    changeParamList.add(measureResult);
                }
            }
        }
        resultEntity.setOriginalMeasureResult(changeParamList);

        return R.ok(resultEntity);
    }


    private R<OperationResultEntity> singWaveLengthMeasure(DevOperationInputParam inputParam) {
        OperationResultEntity resultEntity = new OperationResultEntity();
        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        String wavelength = inputParam.getSingleWavelength();


        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();
        Object[] param;
        String cmdId = "";
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);

        byte[] bytes;
        R<Object> mesure = new R<>();

        ChangeParam measureResult = new ChangeParam();

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            log.info("串口零点");
        } else {


            cmdId = T600ComIdEnums.SetWl.getComId();
            Transition transition = xml.get(cmdId);
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            Double v = formulaUtil.calculateStep(Double.parseDouble(wavelength), deviceSerialNumber);
            param = new Object[]{v};
            bytes = protocolItemUtil.calcSendBytes(param, transition);

            mesure = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);

            if (!mesure.getSuccess()) {
                return R.fail("00209022");
            }
            deviceManager.setCurrentWavelength(deviceId, wavelength);
            device = deviceManager.getDevice(deviceId);

            cmdId = T600ComIdEnums.Measure.getComId();
            transition = xml.get(cmdId);
            replyNumber = transition.getReplyNumber();
            timeout = transition.getTimeout();
            param = new Object[]{};
            bytes = protocolItemUtil.calcSendBytes(param, transition);
            headReceiveBytes = transition.getHeadReceiveBytes();
            mesure = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, replyNumber, timeout, connectType, headReceiveBytes);
            if (!mesure.getSuccess()) {
                return R.fail("00209022");
            } else {
                Object data = mesure.getData();
                resultEntity = (OperationResultEntity) data;
                measureResult = resultEntity.getMeasureResult();

                ChangeParam zeroResult = device.getZeroResult(wavelength);

                String mode = device.getLuminosityMode();

                String poolValue = null;

                Integer currentPoolNum = deviceManager.getCurrentPoolNum(deviceId);
                FSamplePoolStatus poolStatus = samplePoolConfigService.getPoolCorrectionData(deviceSerialNumber, currentPoolNum);
                if (poolStatus != null) {

                    if (poolStatus.getLuminosity() != null && poolStatus.getLuminosity().equals(mode)) {
                        Map<String, String> poolData = poolStatus.getPoolData();
                        poolValue = poolData.get(wavelength);
                    }
                }

                if (mode.equals(WaveLengthEnum.es.getCode()) || mode.equals(WaveLengthEnum.er.getCode())) {

                    String value = DataCountUtil.countErOrEs(measureResult, mode);
                    if(Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                        value = DataCountUtil.count1960ErOrEs(measureResult, mode);
                    }

                    measureResult.setValue(value);

                } else if (mode.equals(WaveLengthEnum.abs.getCode()) || mode.equals(WaveLengthEnum.tPercent.getCode())) {
                    Map<String, String> darkInfo = info.getDarkInfo();
                    String z = "1";
                    if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                        z = "0.1";
                    }
//                    if (darkInfo != null && StringUtils.hasText(darkInfo.get(wavelength))) {
//                        z = darkInfo.get(wavelength);
//                    }
                    Double zVal = spectrumResultCalculator.getZFromDatabase2(deviceSerialNumber, device.getCurrentWavelength());
                    if (zVal != null) {
                        z = String.valueOf(zVal);
                    }

                    String tPercent = DataCountUtil.countTPercent(zeroResult, measureResult, Double.parseDouble(z));

                    if (Objects.equals(DevXmlEnums.T800.getDevType(), device.getXmlKey())) {
                        tPercent = DataCountUtil.calcTPercent1960(zeroResult, measureResult, Double.parseDouble(z));
                    }
                    String abs = DataCountUtil.changeTPercentOrAbs(null, tPercent);

                    if (mode.equals(WaveLengthEnum.abs.getCode())) {
                        if (poolValue != null) {
                            abs = samplePoolConfigService.applyPoolCorrection(abs, poolValue, mode);

                        }

                        measureResult.setValue(abs);
                        measureResult.setOtherData(tPercent);
                    } else {
                        if (poolValue != null) {
                            tPercent = samplePoolConfigService.applyPoolCorrection(tPercent, poolValue, mode);
                        }
                        measureResult.setValue(tPercent);
                        measureResult.setOtherData(abs);
                    }
                }

                measureResult.setWl(wavelength);
                deviceManager.setMeasureValue(deviceId, measureResult.getValue());

            }
        }
        resultEntity.setSingMeasureResult(measureResult);

        return R.ok(resultEntity);
    }

//private R<OperationResultEntity> executeTimeMeasure(DevOperationInputParam inputParam) throws InterruptedException {
//
//    OperationResultEntity resultEntity = new OperationResultEntity();
//
//    String deviceSerialNumber = inputParam.getDeviceSerialNumber();
//    Double intervalSeconds = inputParam.getIntervalSeconds();
//
//    if (intervalSeconds == null || intervalSeconds <= 0) {
//        return R.fail("时间间隔必须大于 0");
//    }
//
//    long intervalMillis = (long) (intervalSeconds * 1000);
//    long nextTime = System.currentTimeMillis();
//
//    DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
//    String deviceId = info.getDeviceId();
//    DeviceConnectInfo device = deviceManager.getDevice(deviceId);
//    String xmlKey = device.getXmlKey();
//    Integer connectType = device.getConnectType();
//
//    Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
//
//    List<ChangeParam> resultList = new ArrayList<>();
//
//    if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
//        return R.fail("暂不支持串口");
//    }
//
//    // 获取当前波长（假设时间扫描每次波长不变）
//    String wl = deviceManager.getCurrentWavelength(deviceId);
//    if (wl == null) {
//        return R.fail("当前波长未设置，请先校零");
//    }
//
//    // 获取对应 zeroRsd
//    ChangeParam zeroRsd = deviceManager.getZeroResult(deviceId, wl);
//    if (zeroRsd == null) {
//        return R.fail("未找到波长 " + wl + " 的校零结果，请先执行 zero");
//    }
//
//    /* ================= 2. 按时间循环 Measure ================= */
//    Transition measureTransition = xml.get(T600ComIdEnums.Measure.getComId());
//    byte[] measureBytes = protocolItemUtil.calcSendBytes(new Object[]{}, measureTransition);
//
//    while (!Thread.currentThread().isInterrupted()) {
//
//        nextTime += intervalMillis;
//
//        long start = System.currentTimeMillis();
//
//        R<Object> rsp = udpSendUtil.knowTotalReceiveSend(
//                deviceId,
//                T600ComIdEnums.Measure.getComId(),
//                measureBytes,
//                measureTransition.getReplyNumber(),
//                measureTransition.getTimeout(),
//                connectType
//        );
//
//        if (!rsp.getSuccess()) {
//            return R.fail("时间扫描测量失败");
//        }
//
//        OperationResultEntity temp = (OperationResultEntity) rsp.getData();
//        ChangeParam measureRsd = temp.getMeasureResult();
//        measureRsd.setWl(wl);
//        resultList.add(measureRsd);
//
//        // ========== 调用 SpectrumResultCalculator 计算 Abs/T%/Es/Er ==========
//        SpectrumPoint point = new SpectrumPoint(Double.parseDouble(wl));
//        point.setZeroRsd(zeroRsd);
//        point.setMeasureRsd(measureRsd);
//
//        // ===== 统一结果对象 =====
//        SpectrumCalcResult calcResult = new SpectrumCalcResult();
//        calcResult.setWavelength(point.getWavelength());
//
//        SpectrumCalcResult absResult = SpectrumResultCalculator.calculate(1, point, SpectrumSourceType.SINGLE);
//        SpectrumCalcResult tResult   = SpectrumResultCalculator.calculate(2, point, SpectrumSourceType.SINGLE);
//        SpectrumCalcResult esResult  = SpectrumResultCalculator.calculate(3, point, SpectrumSourceType.SINGLE);
//        SpectrumCalcResult erResult  = SpectrumResultCalculator.calculate(4, point, SpectrumSourceType.SINGLE);
//
//
//        // 可选：把计算结果保存到 measureRsd 对应的对象中
//        point.setAbsResult(absResult);
//        point.setTResult(tResult);
//        point.setEsResult(esResult);
//        point.setErResult(erResult);
//
//
//        point.setTimestamp(System.currentTimeMillis());
//        log.info("[时间计算] wl={}, Abs={}, T%={}, Es={}, Er={}, time={}",
//                point.getWavelength(),
//                absResult != null ? absResult.getAbs() : "-",
//                tResult != null ? tResult.getTPercent() : "-",
//                esResult != null ? esResult.getEs() : "-",
//                erResult != null ? erResult.getEr() : "-",
//                point.getTimestamp());
//
//
//        unknowCountMessageQueue.putMessage(deviceSerialNumber, "Measure", point);
//        long cost = System.currentTimeMillis() - start;
//        long sleep = nextTime - System.currentTimeMillis();
//
//        if (sleep > 0) {
//            Thread.sleep(sleep);
//        } else {
//            log.warn("[时间测量] 已超时 {} ms，跳过 sleep", -sleep);
//        }
//        try {
//            Thread.sleep(intervalMillis);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            unknowCountMessageQueue.putMessage(
//                    deviceSerialNumber,
//                    "Measure",
//                    EndMarker.timeout(deviceSerialNumber, "Measure")
//            );
//            infoService.newReleaseDevice(deviceSerialNumber);
//            break;
//        }
//    }
//
//    resultEntity.setOriginalMeasureResult(resultList);
//    return R.ok(resultEntity);
//}

    private R<OperationResultEntity> executeTimeMeasure(DevOperationInputParam inputParam) {

        OperationResultEntity resultEntity = new OperationResultEntity();

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        Double intervalSeconds = inputParam.getIntervalSeconds();
        Double totalTime = inputParam.getTotalTime();

        if (intervalSeconds == null || intervalSeconds <= 0) {
            return R.fail("0020901D");
        }

        if (totalTime == null || totalTime <= 0) {
            return R.fail("0020901E");
        }

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        String xmlKey = device.getXmlKey();
        Integer connectType = device.getConnectType();

        // ========= 当前波长 & zero =========
        String wl = deviceManager.getCurrentWavelength(deviceId);
        if (wl == null) {
            return R.fail("0020901F");
        }

        ChangeParam zeroRsd = deviceManager.getZeroResult(deviceId, wl);
//        if (zeroRsd == null) {
//            return R.fail("未找到波长 " + wl + " 的校零结果");
//        }

        // 默认连接方式：网口
        if (inputParam.getConnectType() == null) {
            inputParam.setConnectType(DeviceConnectTypeEnum.ETHERNET.getConnectType());
        }
        // 校验连接状态
        String macAddress = "";
        if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(connectType)) {
        } else if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(connectType)) {
            macAddress = connectUtil.getWifiMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("00209009");
            }
        } else {
            macAddress = connectUtil.getEthernetMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("0020900A");
            }
        }
        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        DevXmlEnums dev = DevXmlEnums.getEnum(xmlKey);
        Transition measureTransition;
        if (dev.getDevType().equals("T800")){
            measureTransition = xml.get(T800ComIdEnums.T800TimeMeasure.getComId());
        }else{
            measureTransition = xml.get(T600ComIdEnums.Measure.getComId());
        }
        byte[] measureBytes = protocolItemUtil.calcSendBytes(new Object[]{}, measureTransition);

        udpSendUtil.TimeMeasure(
                deviceId,
                T600ComIdEnums.Measure.getComId(),
                measureBytes,
                measureTransition.getReplyNumber(),
                measureTransition.getTimeout(),
                connectType,
                intervalSeconds,
                totalTime,
                deviceSerialNumber,
                measureTransition.getHeadReceiveBytes()
        );

        // ================== 计算线程 ==================
//        Thread calcThread = new Thread(() -> {
//
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    log.info("[计算线程] 等待队列数据...");
//                    MeasurePacket packet = queue.take();
//                    System.out.println("取到 packet: " + packet);
//
//                    if (packet.measureRsd == null || packet.wl == null) {
//                        log.warn("[计算线程] measureRsd 或 wl 为空，跳过计算");
//                        continue;
//                    }
//
//                    double wlVal;
//                    try {
//                        wlVal = Double.parseDouble(packet.wl);
//                    } catch (NumberFormatException e) {
//                        log.error("[计算线程] 波长非法: {}", packet.wl);
//                        continue;
//                    }
//
//                    SpectrumPoint point = new SpectrumPoint(Double.parseDouble(packet.measureRsd.getWl()));
//                    point.setZeroRsd(zeroRsd);
//                    point.setMeasureRsd(packet.measureRsd);
//                    point.setTimestamp(packet.timestamp);
//
//                    point.setAbsResult(SpectrumResultCalculator.calculate(1, point, SpectrumSourceType.SINGLE));
//                    point.setTResult(SpectrumResultCalculator.calculate(2, point, SpectrumSourceType.SINGLE));
//                    point.setEsResult(SpectrumResultCalculator.calculate(3, point, SpectrumSourceType.SINGLE));
//                    point.setErResult(SpectrumResultCalculator.calculate(4, point, SpectrumSourceType.SINGLE));
//
//                    log.info("[时间计算] wl={}, Abs={}, T%={}, Es={}, Er={}, time={}",
//                            point.getWavelength(),
//                            point.getAbsResult() != null ? point.getAbsResult().getAbs() : "-",
//                            point.getTResult() != null ? point.getTResult().getTPercent() : "-",
//                            point.getEsResult() != null ? point.getEsResult().getEs() : "-",
//                            point.getErResult() != null ? point.getErResult().getEr() : "-",
//                            point.getTimestamp());
//
//                    unknowCountMessageQueue.putMessage(deviceSerialNumber, "Measure", point);
//
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } catch (Exception e) {
//                    log.error("[计算线程异常]", e);
//                }
//            }
//        }, "Measure-Calc-" + deviceSerialNumber);
//        calcThread.start();

        return R.ok(resultEntity);
    }


    private R<OperationResultEntity> disconnect(DevOperationInputParam inputParam) {

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();

        String cmdId = "DisConnect";

        Object[] param = new Object[]{};

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(cmdId);
        int ReplyNumber = transition.getReplyNumber();
        int Timeout = transition.getTimeout();
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

        R<Object> disconnect;
        if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(connectType)) {
            log.info("串口断开");
        } else {
            if (ReplyNumber < 0) {
                disconnect = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, Timeout, false, deviceSerialNumber, null, headReceiveBytes);
            } else {
                disconnect = udpSendUtil.knowTotalReceiveSend(deviceId, cmdId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);
            }
            if (!disconnect.getSuccess()) {
                MessageStoreManager.getInstance().clearDevice(deviceId);

                deviceManager.removeDevice(deviceId);
                return R.fail("00209023");
            }
        }
        MessageStoreManager.getInstance().clearDevice(deviceId);

        deviceManager.removeDevice(deviceId);

        return R.ok();
    }

    private void setInitStatus(String deviceId, Integer initStatus) {
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();
        String comId = T600ComIdEnums.SetInitStatus.getComId();

        Object[] param = new Object[]{initStatus};

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(comId);
        int ReplyNumber = transition.getReplyNumber();
        int Timeout = transition.getTimeout();
        List<String> headReceiveBytes = transition.getHeadReceiveBytes();

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            log.info("串口默认");
            //Todo 串口初始化待做

        } else {
            R<Object> r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);

        }

    }

    private R<Boolean> setDeviceDefaultStatus(String deviceId, String deviceSerialNumber) {

        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();

        for (DefaultStatusCmdEnum anEnum : DefaultStatusCmdEnum.values()) {
            String comId = anEnum.getComId();

            String value = anEnum.getValue();
            Object[] param = new Object[]{value};

            if (comId.equals(DefaultStatusCmdEnum.SetWl.getComId())) {
                Double step = formulaUtil.calculateStep(Double.parseDouble(value), deviceSerialNumber);
                param = new Object[]{step};
            }

            Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
            Transition transition = xml.get(comId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            List<String> headReceiveBytes = transition.getHeadReceiveBytes();

            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

            if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
                log.info("串口默认");
                R<Object> r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);
                if (!r.getSuccess()) {
                    return R.fail("00209024");
                }
            } else {
                R<Object> r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, ReplyNumber, Timeout, connectType, headReceiveBytes);
                if (!r.getSuccess()) {
                    return R.fail("00209024");
                }
            }
            if (comId.equals(DefaultStatusCmdEnum.SetPhotoMode.getComId())) {
                deviceManager.setLuminosityMode(deviceId, value);
            }
            if (comId.equals(DefaultStatusCmdEnum.SetWl.getComId())) {
                deviceManager.setCurrentWavelength(deviceId, value);
            }
        }
        return R.ok();
    }

    private static Command dealCommand(Integer attachmentType, String cmdId) {
        Command command;
        if (cmdId.equals("SetSlit1")) {
            if (attachmentType.equals(DeviceAttachmentKindEnum.INTEGRAL_BALL.getType())) {
                command = new Command(cmdId, new Object[]{5});
            } else {
                command = new Command(cmdId, new Object[]{2});
            }
        } else if (cmdId.equals("SetCellPos")) {
            command = new Command(cmdId, new Object[]{1});
        }else if(cmdId.equals(T800ComIdEnums.DoIt.getComId())) {
            command = new Command(cmdId, new Object[]{1});
        }else {
            command = new Command(cmdId, new Object[]{});
        }
        return command;
    }

    public List<DeviceInitEntryInfo> addInitEntry(List<DeviceInitEntryInfo> initEntry) {
        List<DeviceInitEntryInfo> ans = new ArrayList<>();

        for (int i = 0; i < DeviceInitPreCommandEnum.getLength(); i++) {
            DeviceInitEntryInfo entry = new DeviceInitEntryInfo();
            DeviceInitPreCommandEnum anEnum = DeviceInitPreCommandEnum.getEnum(i);
            entry.setInitComId(anEnum.getComIdList());
            entry.setInitComName(anEnum.getDescription());
            entry.setInitComValue(anEnum.getComValue());
            ans.add(entry);
        }
        ans.addAll(initEntry);
        return ans;
    }

    private void updateSuccess(InitRecordPO recordPO) {
        recordPO.setInitStatus(Constants.INIT_TASK_SUCCESS);
        initRecordService.saveOrUpdateInitTask(recordPO);
    }

    private void updateStop(InitRecordPO recordPO, DeviceInfoPO po) {
        recordPO.setInitStatus(Constants.INIT_TASK_STOP);
        initRecordService.saveOrUpdateInitTask(recordPO);

        po.setIsInited(Constants.INIT_STATUS_STOP);
        infoService.saveOrUpdateInfo(po);
    }

    private void updateRunning(InitRecordPO recordPO) {
        recordPO.setInitStatus(Constants.INIT_TASK_ING);
        initRecordService.saveOrUpdateInitTask(recordPO);
    }

    private void failDeal(DeviceInfoPO po, InitRecordPO recordPO) {

        recordPO.setInitStatus(Constants.INIT_TASK_FALSE);
        initRecordService.saveOrUpdateInitTask(recordPO);

        po.setIsInited(Constants.INIT_STATUS_FALSE);
        infoService.saveOrUpdateInfo(po);
    }

    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        return emptyNames.toArray(new String[0]);
    }

    //方法
    private static Object getFieldValueByGetter(Object obj, String fieldName) {
        try {
            String getterName = "get" +
                    Character.toUpperCase(fieldName.charAt(0)) +
                    fieldName.substring(1);

            Method method = obj.getClass().getMethod(getterName);
            return method.invoke(obj);

        } catch (NoSuchMethodException e) {
            // 没有 getter，直接返回 null
            return null;
        } catch (Exception e) {
            throw new RuntimeException("读取属性失败: " + fieldName, e);
        }
    }

    @Data
    static class MeasurePacket {
        private ChangeParam measureRsd;
        private long timestamp;
        public String wl;

        public MeasurePacket(ChangeParam measureRsd, long timestamp, String wl) {
            this.measureRsd = measureRsd;
            this.timestamp = timestamp;
            this.wl = wl;
        }
    }

}
