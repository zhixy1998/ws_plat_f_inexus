/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceModelInfoDAO;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceAttachmentKindEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitPreCommandEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInitEntryInfo;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceModelInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InitRecordPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ProtocolItemUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.Transition;
import com.persee.workstation.uv_inexus.util.deviceUtil.cmdEnums.ConnectEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.cmdEnums.DefaultStatusCmdEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;

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
    InitRecordService initRecordService;

    @Autowired
    DeviceModelInfoDAO deviceModelInfoDAO;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    DeviceXmlManage xmlManage;

    @Autowired
    UdpSendUtil udpSendUtil;

    private final static ConnectUtil connectUtil = ConnectUtil.getInstance();

    private final static String defaultIp = "255.255.255.255";

    private final static DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

    private volatile static int newIpEnd = 253;

    @Override
    @PostConstruct
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

    @Override
    public void initDevice() {
//        infoService.activeDevice();

    }

    @Override
    public R<OperationResultEntity> operation(DevOperationInputParam inputParam) {

        if (inputParam == null) {
            return R.fail("缺少入参！");
        }

        if (inputParam.getOperationType() == null) {
            return R.fail("缺少操作类型！");
        }
        OperationTypeEnums anEnum = OperationTypeEnums.getEnum(inputParam.getOperationType());
        if (anEnum == null) {
            return R.fail("未找到该操作！");
        }

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();

        if (!StringUtils.hasText(deviceSerialNumber)) {

            if (!inputParam.getOperationType().equals(OperationTypeEnums.Find.getType()) && !inputParam.getOperationType().equals(OperationTypeEnums.Connect.getType()) ) {
                return R.fail("请先连接仪器！");
            }
        } else {
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info == null) {
                return R.fail("请先连接仪器！");
            }

            R<Boolean> r = infoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_INIT);
            if (!r.getSuccess()) {
                return R.fail(r.getMessage());
            }
        }

        R<OperationResultEntity> result = R.ok();
        boolean release = true;
        switch (anEnum) {
            case Find:
                result = find(inputParam);
                break;
            case Connect:
                result = connect(inputParam);
                break;
            case Init:
                synchronized (this) {
                    new Thread(() -> {
                        initDevice(inputParam.getDeviceSerialNumber());
                    }).start();
                    release = false;
                }
                break;
            case Disconnect:
                result =  disconnect(inputParam);
                break;
            default:
                infoService.releaseDevice(deviceSerialNumber);
                break;
        }
        //释放仪器，部分操作自主释放
        if (release) {
            infoService.releaseDevice(deviceSerialNumber);
        }
        return result;
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

        } else if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(inputParam.getConnectType())) {
            //wifi
            macAddress = connectUtil.getWifiMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("请检查wifi连接！");
            }

        } else {

            macAddress = connectUtil.getEthernetMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("请检查网线连接！");
            }

        }

        Object[] param = new Object[]{0,0,0,0,0,0};

        String[] macParts = macAddress.split(":");
        // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
        for (int i = 0; i < 6; i++) {

            int anInt = Integer.parseInt(macParts[i], 16);

            param[i] = anInt;  // 将每一部分从十六进制转换为十进制
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

            R<List<SearchInfo>> r = udpSendUtil.searchDevice(defaultIp, comId, bytes, Timeout, inputParam.getConnectType());
            if (r.getSuccess()) {
                List<SearchInfo> data = r.getData();
                allDevice.addAll(data);
                for (SearchInfo info : data) {
                    xmlManage.setDevice(info.getDeviceIp(), anEnum.getDevType());
                    xmlManage.setDevice(info.getDeviceMac(), anEnum.getDevType());
                    deviceManager.updateDeviceInfo(info.getDeviceId(), true, info.getDeviceMac(), anEnum.getDevType());

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
        if (deviceInfo == null) {
            return R.fail("请先查找仪器！");
        }

        boolean a = xmlManage.containsKeyInDeviceMap(deviceInfo.getDeviceIp());
        boolean b = xmlManage.containsKeyInDeviceMap(deviceInfo.getDeviceMac());

        if (!a && !b) {
            return R.fail("请先查找仪器！");
        }
        if (a) {
            devType = xmlManage.getDevType(deviceInfo.getDeviceIp());
        } else {
            devType = xmlManage.getDevType(deviceInfo.getDeviceMac());
        }
        String sendIp = deviceInfo.getDeviceIp();

        OperationResultEntity result = new OperationResultEntity();

        Integer connectType = inputParam.getConnectType();
        if (connectType == null) {
            //默认网口连接
            connectType = 1;
        }

        if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
            //串口
            return R.fail("串口连接暂未实现！");
        } else if (connectType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
            //wifi
            macAddress = connectUtil.getWifiMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("请检查wifi连接！");
            }
            connectPort = connectUtil.getWifiPort();
            localhostIp = connectUtil.getWifiLocalIp();

        } else {

            macAddress = connectUtil.getEthernetMacAddress();
            if (!StringUtils.hasText(macAddress)) {
                return R.fail("请检查网线连接！");
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
                    param[6 + i] = deviceMacParts[i];  // 将 MAC 地址的十进制值填充到 param 数组的相应位置
                }

                StringBuilder newIp = new StringBuilder();

                String[] deviceIpParts = ethernetIp.split("\\.");
                for (int i = 0; i <= 2; i++) {
                    param[12 + i] = Integer.parseInt(deviceIpParts[i]);  // 逆序插入 IP 地址的最后 3 个部分
                    newIp.append(deviceIpParts[i]).append(".");
                }

                param[param.length - 1] = String.valueOf(newIpEnd);
                newIp.append(param[param.length - 1]);

                String comId = T600ComIdEnums.NewIP.getComId();
                Map<String, Transition> xml = xmlManage.getXmlInfo(devType);
                Transition transition = xml.get(comId);
                int ReplyNumber = transition.getReplyNumber();
                int Timeout = transition.getTimeout();
                byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

                R<String> r = udpSendUtil.deviceNewIp(deviceInfo.getDeviceId(), comId, bytes, ReplyNumber, Timeout, connectType, newIp.toString());
                if (!r.getSuccess()) {
                    return R.fail("当前仪器无法连接！");
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
        for (ConnectEnum anEnum : ConnectEnum.values()) {
            String comId = anEnum.getComId();
            Map<String, Transition> xml = xmlManage.getXmlInfo(devType);
            Transition transition = xml.get(comId);
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();

            if (comId.equals(ConnectEnum.Connect.getComId())) {
                param = new Object[6];
                String[] macParts = macAddress.split(":");
                // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
                for (int i = 0; i < 6; i++) {

                    int anInt = Integer.parseInt(macParts[i], 16);

                    param[i] = anInt;  // 将每一部分从十六进制转换为十进制
                }
            }

            if (comId.equals(ConnectEnum.SetCellPos.getComId())) {
                if (result.getAttachmentType() == null) {
                    continue;
                }

                DeviceAttachmentTypeVO attachment = infoService.getAttachment(result.getAttachmentType());
                if (attachment == null || attachment.getAttachmentKind() == null) {
                    continue;
                }
                Integer kind = attachment.getAttachmentKind();

                if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType())|| kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())) {
                    param = new Object[]{1};
                } else {
                    continue;
                }
            }

            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);
            R<Object> r;
            if (replyNumber < 0) {
                r = udpSendUtil.unknownReceive(deviceId, comId, bytes, timeout, false, null, null);
            } else {
                 r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, replyNumber, timeout, connectType);

            }
            if (!r.getSuccess()) {
                return R.fail("当前仪器无法连接！");
            }

            if (comId.equals(ConnectEnum.Connect.getComId())) {

                String s = deviceManager.convertToPermanentByDeviceId(deviceInfo.getDeviceId());
                result.setIp(deviceInfo.getDeviceIp());
                result.setDeviceId(s);
                deviceId = s;
                result.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            }

            Object data = r.getData();

            if (comId.equals(ConnectEnum.GetWlList.getComId())) {
                result.setLinearSegment(formulaUtil.addFormula(data, result.getDeviceSerialNumber()));
                continue;
            }
            try {
                OperationResultEntity entity = (OperationResultEntity) data;
                if (!entity.getIsSuccess()) {
                    log.error("指令[{}]解析出错，错误原因：{}", comId, entity.getErrorMsg());
                    return R.fail("指令" + comId + "，解析出错！");
                }
                BeanUtils.copyProperties(entity, result, getNullPropertyNames(entity));
            } catch (Exception e) {
                log.error("指令{}，操作过程中数据转换错误，错误信息：{}",comId, e.getMessage());
                return R.fail("数据转换错误");
            }

            param = new Object[]{};
        }

        if (result.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
            setDeviceDefaultStatus(result.getDeviceId(), result.getDeviceSerialNumber());
        }

        return R.ok(result);
    }

    private void initDevice(String deviceSerialNumber) {


//        String deviceSerialNumber = inputParam.getDeviceSerialNumber();

        DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);

        LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceModelInfoPO::getDevModel, infoPO.getDeviceModel());
        DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

        List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

        DeviceAttachmentTypeVO attachmentInfo = infoService.getAttachmentInfo(deviceSerialNumber);
        Integer kind = attachmentInfo.getAttachmentKind();

        if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType())|| kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())){
            List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
            initRecordService.initAllTask(deviceSerialNumber, infoPO.getId(), allInitEntry);
        } else {
            initRecordService.initAllTask(deviceSerialNumber, infoPO.getId(), initEntry);
        }

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

                    //Todo 结束前缀
                    byte[] endBytes = new byte[1];

                    byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
                    R<Object> initDevice;
                    if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(connectType)) {
                        log.info("串口初始化");
                        //Todo 串口初始化待做
                        initDevice = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, timeout,false, deviceSerialNumber, endBytes);
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
                                    int intValue = Integer.parseInt(part); // 解析为 int（支持 0~255）
                                    endBytes[i] = (byte) (intValue & 0xFF); // 安全转为 byte（保留低8位）
                                    System.out.println(endBytes[i]);
                                }
                            }

                            initDevice = udpSendUtil.unknownReceive(deviceId, cmdId, bytes, timeout,false, deviceSerialNumber, endBytes);
                        } else {
                            initDevice = udpSendUtil.knowTotalReceiveSend(deviceId,cmdId, bytes,replyNumber,timeout,connectType);
                        }
                    }

                    if (!initDevice.getSuccess()) {
                        log.info("{}指令初始化失败", cmdId);
                        failDeal(infoPO, recordPO);
                        infoService.releaseDevice(infoPO.getDeviceSerialNumber());
                        return;
                    }
                    count++;
                    log.info("{}指令初始化成功", cmdId);
                } catch (Exception e) {
                    log.info("{}指令初始化异常", cmdId);
                    failDeal(infoPO, recordPO);
                    infoService.releaseDevice(infoPO.getDeviceSerialNumber());
                    return;
                }

            }
            if (count != cmdIdList.size()) {
                log.info("自检已停止");
                updateStop(recordPO, infoPO);
                infoService.releaseDevice(infoPO.getDeviceSerialNumber());
                return;
            }
            log.info("{}成功", recordPO.getInitCmdName());
            updateSuccess(recordPO);
        }
        log.info("自检结束");
        R<Boolean> r = setDeviceDefaultStatus(deviceId, deviceSerialNumber);

        infoPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);

    }

    private R<OperationResultEntity> disconnect(DevOperationInputParam inputParam) {

        String deviceSerialNumber = inputParam.getDeviceSerialNumber();
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        String deviceId = info.getDeviceId();
        DeviceConnectInfo device = deviceManager.getDevice(deviceId);
        Integer connectType = device.getConnectType();
        String xmlKey = device.getXmlKey();

        String comId = "DisConnect";

        Object[] param = new Object[]{};

        Map<String, Transition> xml = xmlManage.getXmlInfo(xmlKey);
        Transition transition = xml.get(comId);
        int ReplyNumber = transition.getReplyNumber();
        int Timeout = transition.getTimeout();

        byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

        R<Object> disconnect;
        if (DeviceConnectTypeEnum.SERIAL.getConnectType().equals(connectType)) {
            log.info("串口断开");
        } else {
            if (ReplyNumber < 0) {
                disconnect = udpSendUtil.unknownReceive(deviceId, comId, bytes, Timeout, false, deviceSerialNumber, null);
                if (!disconnect.getSuccess()) {
                    return R.fail("断开失败！");
                }
            } else {
                disconnect = udpSendUtil.knowTotalReceiveSend(deviceId,comId, bytes,ReplyNumber,Timeout,connectType);
                if (!disconnect.getSuccess()) {
                    return R.fail("断开失败！");
                }
            }
        }
        return R.ok();
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

            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);

            if (connectType.equals(DeviceConnectTypeEnum.SERIAL.getConnectType())) {
                log.info("串口默认");
                //Todo 串口初始化待做
                R<Object> r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, ReplyNumber, Timeout, connectType);
                if (!r.getSuccess()) {
                    return R.fail("处理失败！");
                }
            } else {
                R<Object> r = udpSendUtil.knowTotalReceiveSend(deviceId, comId, bytes, ReplyNumber, Timeout, connectType);
                if (!r.getSuccess()) {
                    return R.fail("处理失败！");
                }
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
        } else {
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
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        return emptyNames.toArray(new String[0]);
    }


}
