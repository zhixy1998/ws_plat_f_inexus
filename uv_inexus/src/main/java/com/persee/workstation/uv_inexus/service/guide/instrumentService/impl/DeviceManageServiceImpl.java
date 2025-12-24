/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.*;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceCalibrationDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceInitDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceSoftCloseDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceStartDTO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceAttachmentKindEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceCalibrationInfoEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceConnectMessageEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitPreCommandEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.ScanSpeedEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.*;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceConfParam;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.LiveDetectionManyDevicesWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
public class DeviceManageServiceImpl implements DeviceManageService {

    @Autowired
    InsConfigDAO configDAO;

    @Autowired
    CalibrationTempDAO tempDao;

    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    @Autowired
    InitRecordService initRecordService;

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    DeviceMessageUtils messageUtils;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    FormulaUtil formulaUtil;

    @Autowired
    FSamplePoolConfigService samplePoolConfigService;

    @Autowired
    DeviceModelInfoDAO deviceModelInfoDAO;

    @Autowired
    DeviceTypeInfoDAO deviceTypeInfoDAO;

    @Autowired
    LiveDetectionManyDevicesWebSocket liveDetectionWebSocket;

//    @Autowired
//    LiveDetectionWebSocket liveDetectionWebSocket;

    @Autowired
    DevOperationUtilService operationUtilService;

    @Value("${device.default.sendPort}")
    Integer sendPort;

    @Value("${device.default.receivePort}")
    Integer receivePort;

    private static volatile Map<String, Boolean> stopInit = new HashMap<>();

    @Override
    public R<DeviceInfoVO> queryDeviceInfo(String deviceSerialNumber, String updateBy) {
//        log.info("查询仪器信息");
        DeviceInfoVO vo = new DeviceInfoVO();

        List<DeviceInitInfo> infoList = new ArrayList<>();

        DeviceInfoPO deviceInfoPO = infoService.getInfo(deviceSerialNumber);
        if (deviceInfoPO == null || deviceInfoPO.getId() == null) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            return R.fail(YamlMessageSource.getMessageStatic("00203001"));
        }

        List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceSerialNumber, deviceInfoPO.getId());
        if (initList.isEmpty()) {
            log.info("查询初始化状态异常");
            //查询初始化状态异常
            return R.fail(false,YamlMessageSource.getMessageStatic("00203002"));
        }

        vo.setIsInited(deviceInfoPO.getIsInited());
        vo.setConnectStatus(deviceInfoPO.getConnectStatus());
        vo.setUserName(updateBy);
        vo.setIsDevOpened(deviceInfoPO.getIsDevOpened());
        vo.setDeviceSerialNumber(deviceSerialNumber);
        vo.setDeviceName(deviceInfoPO.getDeviceName());
        vo.setIsActivated(deviceInfoPO.getIsActivated() == null ? 0 : deviceInfoPO.getIsActivated());

        String connect = DeviceConnectMessageEnum.getName(deviceInfoPO.getConnectStatus()) + " IP="
                + deviceInfoPO.getIp() + " port=" + sendPort;

        vo.setConnectMessage(connect);

        String attachmentType = infoService.getAttachmentType(deviceSerialNumber);
        vo.setAppendMessage(attachmentType);

        for (InitRecordPO recordPO : initList) {
            DeviceInitInfo info = new DeviceInitInfo();
            info.setInitTaskNum(recordPO.getInitTaskNum());
            info.setInitProject(recordPO.getInitCmdName());
            info.setInitStatus(recordPO.getInitStatus());

            infoList.add(info);
        }
        vo.setIntMessage(infoList);


        return R.ok(vo);
    }

    @Override
    public R<Integer> queryDeviceStatus(String deviceSerialNumber) {

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null || info.getDeviceCurrentStatus() == null) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            return R.fail(YamlMessageSource.getMessageStatic("00203001"));
        }
//        log.info("查询仪器状态");
        return R.ok(info.getDeviceCurrentStatus());
    }

    @Override
    public R<Boolean> init(DeviceInitDTO initDTO) {
        if (initDTO.getDeviceSerialNumber().isEmpty()) {
            log.info("未传仪器序列号！");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }

        DeviceInfoPO infoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());
        if (infoPO == null || !StringUtils.hasText(infoPO.getId()) || !StringUtils.hasText(infoPO.getDeviceModel())) {
            log.info("未找到连接仪器信息！");
            //未查找到仪器信息
            return R.fail(YamlMessageSource.getMessageStatic("00203001"));
        }
        log.info("调用机器初始化接口");

        R<Boolean> r = infoService.useDevice(initDTO.getDeviceSerialNumber(), Constants.DEVICE_STATUS_INIT);
        if (!r.getSuccess()) {
            log.info(r.getMessage());
            return R.fail(r.getMessage());
        }
        infoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());

        LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceModelInfoPO::getDevModel, infoPO.getDeviceModel());
        DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

        List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

        DeviceAttachmentTypeVO attachmentInfo = infoService.getAttachmentInfo(initDTO.getDeviceSerialNumber());
        Integer kind = attachmentInfo.getAttachmentKind();

        if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType())|| kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())){
            List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
            initRecordService.initAllTask(initDTO.getDeviceSerialNumber(), infoPO.getId(), allInitEntry);
        } else {
            initRecordService.initAllTask(initDTO.getDeviceSerialNumber(), infoPO.getId(), initEntry);
        }

        infoPO.setIsInited(Constants.INIT_STATUS_RUNNING);
        infoService.saveOrUpdateInfo(infoPO);


        Thread initThread = new Thread(() -> {
            try {
                DeviceInfoPO finalInfoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());
                stopInit.put(initDTO.getDeviceSerialNumber(), false);
                newStartInit(finalInfoPO, attachmentInfo.getAttachmentKind());
            } catch (Exception e) {
                DetectionConstants.isDeviceIdle = true;
                throw new RuntimeException(e);
            }
        });
        initThread.start();

        return R.ok(true);
    }

    private void newStartInit(DeviceInfoPO infoPO, Integer attachmentType) throws Exception {
        log.info("自检开始");
        String ip = infoPO.getIp();

        List<InitRecordPO> list = initRecordService.searchInitStatus(infoPO.getDeviceSerialNumber(), infoPO.getId());
        for (InitRecordPO recordPO : list) {
            updateRunning(recordPO);
            recordPO = initRecordService.selectById(recordPO.getId());

            List<String> cmdIdList = recordPO.getInitCmdIdList();

            int count = 0;
            while (count < cmdIdList.size() && !stopInit.get(infoPO.getDeviceSerialNumber())) {
                String cmdId = cmdIdList.get(count);
                Command command = dealCommand(attachmentType, cmdId);
                try {
                    R<String> initDevice = devMessageUtil.initDevice(command, ip);
                    if (!initDevice.getSuccess()) {
                        log.info("{}指令初始化异常", cmdId);
                        failDeal(infoPO, recordPO);
                        devMessageUtil.setDeviceInitStatus(ip, 1);
                        infoService.releaseDevice(infoPO.getDeviceSerialNumber());
                        return;
                    }
                    count++;
                    log.info("{}指令初始化成功", cmdId);
                } catch (Exception e) {
                    log.info("{}指令初始化异常", cmdId);
                    failDeal(infoPO, recordPO);
                    devMessageUtil.setDeviceInitStatus(ip, 1);
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


        infoPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);
        synchronized (infoPO) {
            Command mode = new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()});
            Boolean b = devMessageUtil.wavelengthLocate(mode, ip);

            Double step = formulaUtil.calculateStep(660.0, infoPO.getDeviceSerialNumber());
            Command command = new Command("SetWl", new Object[]{step});
            Boolean locate = devMessageUtil.wavelengthLocate(command, ip);
            DetectionConstants.currentWavelength = String.valueOf(660);

            R<String> stringR = devMessageUtil.setDeviceInitStatus(ip, 1);

            infoService.releaseDevice(infoPO.getDeviceSerialNumber());

            Constants.devConnectInfo.put(infoPO.getDeviceSerialNumber(), true);
            liveDetectionWebSocket.startMeasure(infoPO.getDeviceSerialNumber());
        }

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

    @Override
    public R<Boolean> stopInit(String deviceSerialNumber, String updateBy) {
        stopInit.put(deviceSerialNumber, true);

        return R.ok();
    }


    @Override
    public Boolean startDevice(DeviceStartDTO startDTO) {
        return null;
    }

    @Override
    public Boolean softClose(DeviceSoftCloseDTO closeDTO) {
        return null;
    }

    @Override
    public R<Boolean> closeOrOpenLamp(String lampType, Integer status, String deviceSerialNumber) {

//        SetWLampStatus  （设置钨灯开关：1开0关）
//        SetDLampStatus  （设置氘灯开关：1开0关）
//        SetLampPos （设置灯位置：0钨灯1氘灯）
        Command command = new Command();
        if (lampType.equals(EnergyLampEnums.Tungsten.getIdentity())) {
            command = new Command("SetWLampStatus", new Object[]{status});
        }
        if (lampType.equals(EnergyLampEnums.Deuterium.getIdentity())) {
            command = new Command("SetDLampStatus", new Object[]{status});
        }

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null || !StringUtils.hasText(info.getIp()) || !info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            return R.fail("设备未连接");
        }
        String ip = info.getIp();
        R<Boolean> r = infoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_OTHER);
        if (!r.getSuccess()) {
            return R.fail(r.getMessage());
        }
        Boolean b = devMessageUtil.changeLamp(command, ip);
        infoService.releaseDevice(deviceSerialNumber);
        if (!b) {
            return R.fail("操作失败");
        }

        return R.ok(true);
    }

    @Override
    public R<Boolean> changeLamp(String currentLampType, String deviceSerialNumber) {
        Command command = new Command();
        if (currentLampType.equals(EnergyLampEnums.Tungsten.getIdentity())) {
            command = new Command("SetLampPos", new Object[]{1});
        } else if (currentLampType.equals(EnergyLampEnums.Deuterium.getIdentity())) {
            command = new Command("SetLampPos", new Object[]{0});
        }
        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null || !StringUtils.hasText(info.getIp()) || !info.getConnectStatus().equals(Constants.CONNECT_DEVICE_SUCCESS)) {
            return R.fail("设备未连接");
        }
        String ip = info.getIp();
        R<Boolean> r = infoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_OTHER);
        if (!r.getSuccess()) {
            return R.fail(r.getMessage());
        }
        Boolean b = devMessageUtil.changeLamp(command, ip);
        infoService.releaseDevice(deviceSerialNumber);
        if (!b) {
            return R.fail("操作失败");
        }

        return R.ok(true);
    }

    @Override
    public Boolean disConnect(String deviceSerialNumber, String updateBy) {
        DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
        Constants.devConnectInfo.remove(deviceSerialNumber);
        devMessageUtil.disConnect(infoPO.getIp());

        infoPO.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
        infoService.saveOrUpdateInfo(infoPO);
        ResponseDeal.Sn = null;
        return true;
    }

    @Override
    @DS("guide")
    public R<List<FindDeviceVO>> findDevice() throws Exception {


        List<Command> list = new ArrayList<>();
        list.add(new Command(FindDevEnum.FindDevice.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.NewIP.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.Connect.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetSn.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetAccessoryType.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetInitStatus.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetCellPos.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetWlList.getComId(), new Object[]{}));

        List<FindDeviceVO> devices = new ArrayList<>();

        R<FindDeviceVO> connected = devMessageUtil.connectDevice(list);
        DetectionConstants.isDeviceIdle = true;
        if (connected.getSuccess()) {
            log.info("找到设备");
            FindDeviceVO deviceVO = connected.getData();
            String ip = deviceVO.getIp();

            DeviceInfoPO po = infoService.getInfo(deviceVO.getDeviceSerialNumber());
            if (po == null) {
                po = new DeviceInfoPO();
            }

            if (deviceVO.getIsInited() == null || deviceVO.getIsInited() == 0) {
                po.setIsInited(Constants.INIT_STATUS_NO);
            }

            if (po.getIsInited() == null) {
                po.setIsInited(Constants.INIT_STATUS_NO);
            }
            Constants.devConnectInfo.put(deviceVO.getDeviceSerialNumber(), true);
            if (po.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                liveDetectionWebSocket.startMeasure(deviceVO.getDeviceSerialNumber());

                Command mode = new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()});
                Boolean b = devMessageUtil.wavelengthLocate(mode, ip);

                Double step = formulaUtil.calculateStep(660.0, deviceVO.getLinearSegment());
                Command command = new Command("SetWl", new Object[]{step});
                Boolean locate = devMessageUtil.wavelengthLocate(command, ip);

                DetectionConstants.currentWavelength = "660";
            }

            po.setIp(ip);
            po.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
            po.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            po.setIsDevOpened(1);
            po.setAttachmentType(deviceVO.getAttachmentType());
            po.setDevicePort(sendPort);
            po.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);
            po.setLinearSegment(deviceVO.getLinearSegment());

            //TODO 根据仪器序列号，获取型号信息，还需要调整
            String deviceModel = deviceVO.getDeviceSerialNumber().substring(3, 8);
            po.setDeviceModel(deviceModel);

            LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceModelInfoPO::getDevModel, deviceModel);
            DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

            deviceVO.setDeviceModelName(modelInfoPO.getDevName());
            po.setDeviceModelName(modelInfoPO.getDevName());

            if (StringUtils.hasText(po.getDeviceName())) {
                deviceVO.setDeviceName(po.getDeviceName());
            } else {
                deviceVO.setDeviceName(modelInfoPO.getDevName());
                po.setDeviceName(modelInfoPO.getDevName());
            }

            Integer i = infoService.queryDeviceActivationStatus(deviceVO.getDeviceSerialNumber());
            po.setIsActivated(i);

            List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

            //附件处理
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(deviceVO.getAttachmentType());
            LambdaQueryWrapper<InsConfigPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InsConfigPO::getDeviceSerialNumber, deviceVO.getDeviceSerialNumber());
            InsConfigPO configPO = configDAO.selectOne(queryWrapper);
            if (attachmentInfo == null) {
                attachmentInfo = new DeviceAttachmentTypeVO();
                attachmentInfo.setAttachmentNum(12);
                attachmentInfo.setAttachmentName("未识别");
            }

            if (configPO == null) {
                configPO = new InsConfigPO();
                configPO.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
                configPO.setAnnexSetting(attachmentInfo.getAttachmentKind() == 0 ? -1 : attachmentInfo.getAttachmentKind());
                configPO.setMessagePort(1);
                configPO.setMessagePortChange(false);
                configPO.setAnnexSettingChange(false);
                configDAO.insert(configPO);
            } else {
                configPO.setAnnexSetting(attachmentInfo.getAttachmentKind() == 0 ? -1 : attachmentInfo.getAttachmentKind());
                configPO.setMessagePort(1);
                configPO.setMessagePortChange(false);
                configPO.setAnnexSettingChange(false);
                configDAO.updateById(configPO);
            }

            attachmentInfo.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
            FSampleAttachTypeSaveDTO dto = new FSampleAttachTypeSaveDTO();

            BeanUtils.copyProperties(deviceVO, dto);
            dto.setAttachmentTypeName(attachmentInfo.getAttachmentName());
            samplePoolConfigService.saveSamplePoolInfo(dto);

            po.setAttachmentName(attachmentInfo.getAttachmentName());
            infoService.saveOrUpdateInfo(po);
            boolean initRecord = true;

            List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceVO.getDeviceSerialNumber(), po.getId());
            if (initList == null || initList.isEmpty()) {
                initRecord = false;
            }

            if (deviceVO.getIsInited() == 0 || !initRecord) {
                if (attachmentInfo.getAttachmentKind().equals(1)){
                    List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
                    initRecordService.initAllTask(deviceVO.getDeviceSerialNumber(), po.getId(), allInitEntry);
                }
                initRecordService.initAllTask(deviceVO.getDeviceSerialNumber(), po.getId(), initEntry);
            }

            BeanUtils.copyProperties(po, deviceVO);
            deviceVO.setAttachmentKind(attachmentInfo.getAttachmentKind());
            deviceVO.setLinearSegment(null);
            devices.add(deviceVO);

            return R.ok(devices);
        }
        return R.fail(connected.getMessage());
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


    @Override
    public Boolean deviceActivation(String name) {
        return null;
    }

    @Override
    public R<QueryDeviceConfigVO> queryDeviceConfig(String deviceSerialNumber) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("查询仪器配置--缺少序列号");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }

        QueryDeviceConfigVO vo = new QueryDeviceConfigVO();

        LambdaQueryWrapper<InsConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(InsConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        InsConfigPO po = configDAO.selectOne(query);
        if (po == null) {
            log.info("查询仪器配置--未找到配置，给默认值");
            vo.setMessagePort(1);
            vo.setAnnexSetting(1);
            vo.setBandWidth(2.0);
            vo.setAnnexSettingChange(false);
            vo.setMessagePortChange(false);
        } else{
            vo.setMessagePort(po.getMessagePort());
            vo.setAnnexSetting(po.getAnnexSetting());
            vo.setBandWidth(po.getBandWidth());
            vo.setAnnexSettingChange(false);
            vo.setMessagePortChange(false);
        }

        return R.ok(vo);
    }

    @Override
    public List<Double> getBandWidth(String deviceSerialNumber) {
        List<Double> result = new ArrayList<>();
        R<DeviceMeasureVO> r = queryDeviceTypeInfo(deviceSerialNumber);
        if (!r.getSuccess()) {
            result.add(2.0);
            return result;
        }
        DeviceMeasureVO vo = r.getData();
        if (vo == null || vo.getBandWidth() == null || vo.getBandWidth().isEmpty()) {
            result.add(2.0);
            return result;
        }
        List<String> bandWidth = vo.getBandWidth();
        for (String s : bandWidth) {
            result.add(Double.parseDouble(s));
        }
        return result;
    }

    @Override
    public R<Boolean> deviceConfig(InsConfigPO po) {
//        if (po.getRestoreFactorySetting()) {
//            log.info("恢复出厂设置");
//        }
        if (!StringUtils.hasText(po.getDeviceSerialNumber())) {
            log.info("设置系统配置--缺少序列号");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }
        log.info("设置仪器配置");


        log.info("设置仪器配置成功！");

        LambdaQueryWrapper<InsConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(InsConfigPO::getDeviceSerialNumber, po.getDeviceSerialNumber());
        if (configDAO.selectCount(query) == 0) {
            configDAO.insert(po);
        } else {
            configDAO.update(po, query);
        }
        return R.ok(true);
    }

    @Override
    public Boolean timedStart(String deviceSerialNumber, String closeTime) {
        log.info("定时开机");
        return null;
    }

    @Override
    public R<Boolean> deviceCalibration(DeviceCalibrationDTO dto) {

        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            log.info("缺少序列号");
            //缺少序列号
            return R.fail(YamlMessageSource.getMessageStatic("00203003"));
        }

        if (dto.getCalibrationType() == null || DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()) == null) {
            log.info("缺少校准类型");
            //缺少校准类型
            return R.fail(YamlMessageSource.getMessageStatic("00203004"));
        }

        String ip = infoService.getIp(dto.getDeviceSerialNumber());
        log.info("调用校准");

        R<Boolean> r = infoService.useDevice(dto.getDeviceSerialNumber(), Constants.DEVICE_STATUS_OTHER);
        if (!r.getSuccess()) {
            log.info("设备被占用");
            return R.fail(r.getMessage());
        }

        if (dto.getCalibrationType() == 1) {
            //波长校正
            Command command = new Command(DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()), new Object[]{});
            try {
                R<String> stringR = devMessageUtil.deviceCalibration(command, ip);
                if (!stringR.getSuccess()) {
                    return R.fail("波长校正失败，请检查仪器!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }finally {
                infoService.releaseDevice(dto.getDeviceSerialNumber());
            }
        } else if (dto.getCalibrationType() == 2) {
            //暗电流校正
            Command command = new Command(DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()), new Object[]{});
            try {
                R<String> stringR = devMessageUtil.deviceCalibration(command, ip);
                if (!stringR.getSuccess()) {
                    return R.fail("暗电流校正失败，请检查仪器!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                infoService.releaseDevice(dto.getDeviceSerialNumber());
            }
        }

        LambdaQueryWrapper<CalibrationTempPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

        List<CalibrationTempPO> poList = tempDao.selectList(queryWrapper);
        if (poList.isEmpty()) {
            for (DataNameValue data : dto.getData()) {
                CalibrationTempPO po = new CalibrationTempPO();
                po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
                po.setCreateBy(dto.getUpdateBy());
                po.setCalibrationType(dto.getCalibrationType());
                po.setParam(data.getDataKey());
                po.setParamValue(data.getDataValue());
                tempDao.insert(po);
            }
        } else {
            for (DataNameValue data : dto.getData()) {
                LambdaQueryWrapper<CalibrationTempPO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CalibrationTempPO::getDeviceSerialNumber, dto.getDeviceSerialNumber())
                        .eq(CalibrationTempPO::getCalibrationType, dto.getCalibrationType());

                wrapper.eq(CalibrationTempPO::getParam, data.getDataKey());
                CalibrationTempPO calibrationTempPO = tempDao.selectOne(wrapper);
                if (calibrationTempPO == null) {
                    CalibrationTempPO po = new CalibrationTempPO();
                    po.setDeviceSerialNumber(dto.getDeviceSerialNumber());
                    po.setCreateBy(dto.getUpdateBy());
                    po.setCalibrationType(dto.getCalibrationType());
                    po.setParam(data.getDataKey());
                    po.setParamValue(data.getDataValue());
                    tempDao.insert(po);
                } else {
                    calibrationTempPO.setUpdateBy(dto.getUpdateBy());
                    calibrationTempPO.setParamValue(data.getDataValue());
                    tempDao.update(calibrationTempPO, wrapper);
                }
            }
        }

        return R.ok();
    }

    @Override
    public List<DeviceCalibrationVO> queryCalibrationInfo(String deviceSerialNumber) {
        String type = "T600/T700";
        if (StringUtils.hasText(deviceSerialNumber)) {
            //根据序列号，去查找仪器类型，根据仪器类型，去获取参数列表
            log.info("检测仪器类型");
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            if (info != null) {
                LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
                query.eq(DeviceModelInfoPO::getDevModel, info.getDeviceModel());

                DeviceModelInfoPO deviceModelInfoPO = deviceModelInfoDAO.selectOne(query);
                if (deviceModelInfoPO != null) {
                    DeviceTypeInfoPO deviceTypeInfoPO = deviceTypeInfoDAO.selectById(deviceModelInfoPO.getDevTypeId());
                    if (deviceTypeInfoPO != null && StringUtils.hasText(deviceTypeInfoPO.getDevType())) {
                        type = deviceTypeInfoPO.getDevType();
                    }
                }
            }
        }

        R<DeviceMeasureVO> deviceMeasureVOR = queryDeviceTypeInfo(deviceSerialNumber);
        if (!deviceMeasureVOR.getSuccess()) {
            log.info("查询仪器类型失败");
            return null;
        }
        DeviceMeasureVO measureVO = deviceMeasureVOR.getData();
        List<String> bandWidth = measureVO.getBandWidth();
        List<DataNameValue> band = new ArrayList<>();
        for (String s : bandWidth) {
            band.add(new DataNameValue("", s));
        }


        List<DeviceCalibrationVO> info = configDAO.getCalibrationInfo(type);
        for (DeviceCalibrationVO vo : info) {
            List<DeviceCalibrationParams> params = configDAO.getCalibrationParams(vo.getCalibrationType());
            for (DeviceCalibrationParams param : params) {

                if (param.getParamName().equals("光谱带宽")) {
                    param.setData(band);
                    param.setLastValue("2.0");
                } else {
                    List<DataNameValue> value = configDAO.getCalibrationValue(param.getId());
                    param.setData(value);
                }

                if (param.getParamName().equals("扫描速度")) {
                    param.setLastValue("1");
                }

                LambdaQueryWrapper<CalibrationTempPO> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(CalibrationTempPO::getCalibrationType, vo.getCalibrationType())
                        .eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber)
                        .eq(CalibrationTempPO::getParam, param.getParam())
                        .orderByDesc(CalibrationTempPO::getId);

                CalibrationTempPO tempPO = tempDao.selectOne(queryWrapper);
                if (tempPO != null) {
                    param.setLastValue(tempPO.getParamValue());
                }


            }
            vo.setParams(params);
        }


        return info;
    }

    @Override
    public R<Boolean> checkDeviceStatus(String deviceSerialNumber) {
        return null;
    }

    @Override
    public R<Boolean> updateDeviceStatus(String deviceSerialNumber, String status) {

        return null;
    }

    @Override
    public R<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber) {

        if (!StringUtils.hasText(deviceSerialNumber)) {
            DeviceMeasureVO ans = new DeviceMeasureVO();
            ans.setConnectMode(1);
            ans.setMinWavelength("190");
            ans.setMaxWavelength("1100");
            ans.setBandWidth(Collections.singletonList("2"));
            ans.setGain(Arrays.asList(1, 2, 3, 4));
            ans.setScanInterval(Arrays.asList("0.1", "0.2", "0.5", "1.0", "2.0", "5.0"));
            ans.setResponseTime(Arrays.asList("0.01", "0.02", "0.05", "0.1", "0.2", "0.5", "1", "2"));
            List<DeviceConfParam> list = new ArrayList<>();
            list.add(new DeviceConfParam(WaveLengthEnum.abs.getName(), WaveLengthEnum.abs.getCode()));
            list.add(new DeviceConfParam(WaveLengthEnum.tPercent.getName(), WaveLengthEnum.tPercent.getCode()));
            list.add(new DeviceConfParam(WaveLengthEnum.es.getName(), WaveLengthEnum.es.getCode()));
            list.add(new DeviceConfParam(WaveLengthEnum.er.getName(), WaveLengthEnum.er.getCode()));
            ans.setLuminosity(list);
            list = new ArrayList<>();
            list.add(new DeviceConfParam(EnergyLampEnums.Tungsten.getName(), EnergyLampEnums.Tungsten.getIdentity()));
            list.add(new DeviceConfParam(EnergyLampEnums.Deuterium.getName(), EnergyLampEnums.Deuterium.getIdentity()));
            ans.setLampType(list);
            list = new ArrayList<>();
            list.add(new DeviceConfParam(ScanSpeedEnum.SLOW.getName(), ScanSpeedEnum.SLOW.getCode()));
            list.add(new DeviceConfParam(ScanSpeedEnum.MEDIUM.getName(), ScanSpeedEnum.MEDIUM.getCode()));
            list.add(new DeviceConfParam(ScanSpeedEnum.FAST.getName(), ScanSpeedEnum.FAST.getCode()));
            list.add(new DeviceConfParam(ScanSpeedEnum.SUPER_FAST.getName(), ScanSpeedEnum.SUPER_FAST.getCode()));
            ans.setScanSpeed(list);
            return R.ok(ans);
        }

        DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
        if (info == null) {
            //未找到该设备
            return R.fail(YamlMessageSource.getMessageStatic("00203005"));
        }

        String devModel = deviceSerialNumber.substring(3, 8);

        LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
        query.eq(DeviceModelInfoPO::getDevModel, devModel);

        DeviceModelInfoPO deviceModelInfoPO = deviceModelInfoDAO.selectOne(query);
        if (deviceModelInfoPO == null) {
            //未识别该型号
            return R.fail(YamlMessageSource.getMessageStatic("00203006"));
        }

        DeviceMeasureVO ans = new DeviceMeasureVO();

        ans.setBandWidth(deviceModelInfoPO.getSpacingWidth() == null ? null : deviceModelInfoPO.getSpacingWidth());

        Integer devTypeId = deviceModelInfoPO.getDevTypeId();

        DeviceTypeInfoPO deviceTypeInfoPO = deviceTypeInfoDAO.selectById(devTypeId);
        ans = changeType(deviceTypeInfoPO);
        ans.setBandWidth(deviceModelInfoPO.getSpacingWidth());

        return R.ok(ans);
    }

    private DeviceMeasureVO changeType(DeviceTypeInfoPO po) {

        DeviceMeasureVO ans = new DeviceMeasureVO();
        List<DeviceConfParam> middle = new ArrayList<>();
        BeanUtils.copyProperties(po, ans);
        po.getLuminosity().forEach(i -> {
            DeviceConfParam param = new DeviceConfParam();
            param.setParamValue(String.valueOf(i + 1));
            param.setParamName(WaveLengthEnum.getName(String.valueOf(i + 1)));
            middle.add(param);
        });
        ans.setLuminosity(middle);

        List<DeviceConfParam> lamp = new ArrayList<>();
        po.getLampType().forEach(i -> {
            DeviceConfParam param = new DeviceConfParam();
            param.setParamValue(i.toString());
            param.setParamName(EnergyLampEnums.getName(i.toString()));
            lamp.add(param);
        });
        ans.setLampType(lamp);

        List<DeviceConfParam> scanSpeed = new ArrayList<>();
        po.getScanSpeed().forEach(i -> {
            DeviceConfParam param = new DeviceConfParam();
            param.setParamName(i);
            param.setParamValue(ScanSpeedEnum.getCodeByName(i));
            scanSpeed.add(param);
        });
        ans.setScanSpeed(scanSpeed);

        return ans;

    }

    @Override
    public void test() {
//        List<DeviceTypeInfoPO> deviceTypeInfoPOS = deviceTypeInfoDAO.selectList(null);
//        for (DeviceTypeInfoPO po : deviceTypeInfoPOS) {
//            log.info("{}", po);
//
//            LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
//            query.eq(DeviceModelInfoPO::getDevTypeId, po.getId());
//            List<DeviceModelInfoPO> modelInfoPOS = deviceModelInfoDAO.selectList(query);
//            for (DeviceModelInfoPO modelInfoPO : modelInfoPOS) {
//                log.info("{}", modelInfoPO);
//            }
//
//        }
//
//        try {
//            devMessageUtil.zero(new Command("GetWlList", null), "169.254.180.253");
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//        ChangeParam(wl=500.0, r=491765, s=402424, d=1716)


//        ChangeParam(wl=500, r=128337, s=103479, d=128337)
//        ChangeParam or = new ChangeParam("500", "128337", "103479", "128337");
//        ChangeParam cu = new ChangeParam("500", "491765", "402424", "1716");

////        ChangeParam changeParam = new ChangeParam("0.01", "123456", "9852", "123456");
//        String s = DataCountUtil.countTPercent(or, cu, 1.0);
//        String s1 = DataCountUtil.changeTPercentOrAbs(null, s);
//        System.out.println("+++" + s + "+++" + s1);


    }

    @Override
    public R<List<SearchInfo>> searchDevice(Integer connectType) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Find.getType());
        inputParam.setConnectType(connectType);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            OperationResultEntity data = operation.getData();
            List<SearchInfo> searchInfo = data.getSearchInfo();
            return R.ok(searchInfo);
        }
    }

    @Override
    public R<FindDeviceVO> connect(SearchInfo param) {
        Integer connectType = param.getConnectType();

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Connect.getType());
        inputParam.setConnectType(connectType);
        inputParam.setDeviceInfo(param);

        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            OperationResultEntity data = operation.getData();
            FindDeviceVO vo = new FindDeviceVO();

            DeviceInfoPO info = new DeviceInfoPO();

            BeanUtils.copyProperties(data, vo);
            BeanUtils.copyProperties(data, info);

            info.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            info.setIsDevOpened(1);

            info.setDevicePort(data.getConnectPort());
            info.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);

            String deviceModel = vo.getDeviceSerialNumber().substring(3, 8);
            info.setDeviceModel(deviceModel);

            LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceModelInfoPO::getDevModel, deviceModel);
            DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

            vo.setDeviceModelName(modelInfoPO.getDevName());
            info.setDeviceModelName(modelInfoPO.getDevName());

            if (StringUtils.hasText(info.getDeviceName())) {
                vo.setDeviceName(info.getDeviceName());
            } else {
                vo.setDeviceName(modelInfoPO.getDevName());
                info.setDeviceName(modelInfoPO.getDevName());
            }

            Integer i = infoService.queryDeviceActivationStatus(vo.getDeviceSerialNumber());
            info.setIsActivated(i);

            List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

            //附件处理
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(vo.getAttachmentType());
            LambdaQueryWrapper<InsConfigPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InsConfigPO::getDeviceSerialNumber, vo.getDeviceSerialNumber());
            InsConfigPO configPO = configDAO.selectOne(queryWrapper);
            if (attachmentInfo == null) {
                attachmentInfo = new DeviceAttachmentTypeVO();
                attachmentInfo.setAttachmentNum(12);
                attachmentInfo.setAttachmentName("未识别");
            }

            if (configPO == null) {
                configPO = new InsConfigPO();
                configPO.setDeviceSerialNumber(vo.getDeviceSerialNumber());
                configPO.setAnnexSetting(attachmentInfo.getAttachmentKind() == 0 ? -1 : attachmentInfo.getAttachmentKind());
                configPO.setMessagePort(1);
                configPO.setMessagePortChange(false);
                configPO.setAnnexSettingChange(false);
                configDAO.insert(configPO);
            } else {
                configPO.setAnnexSetting(attachmentInfo.getAttachmentKind() == 0 ? -1 : attachmentInfo.getAttachmentKind());
                configPO.setMessagePort(1);
                configPO.setMessagePortChange(false);
                configPO.setAnnexSettingChange(false);
                configDAO.updateById(configPO);
            }

            attachmentInfo.setDeviceSerialNumber(vo.getDeviceSerialNumber());
            FSampleAttachTypeSaveDTO dto = new FSampleAttachTypeSaveDTO();

            BeanUtils.copyProperties(vo, dto);
            dto.setAttachmentTypeName(attachmentInfo.getAttachmentName());
            samplePoolConfigService.saveSamplePoolInfo(dto);

            info.setAttachmentName(attachmentInfo.getAttachmentName());
            infoService.saveOrUpdateInfo(info);
            boolean initRecord = true;

            List<InitRecordPO> initList = initRecordService.searchInitStatus(vo.getDeviceSerialNumber(), info.getId());
            if (initList == null || initList.isEmpty()) {
                initRecord = false;
            }

            if (vo.getIsInited() == 0 || !initRecord) {
                if (attachmentInfo.getAttachmentKind().equals(1)){
                    List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
                    initRecordService.initAllTask(vo.getDeviceSerialNumber(), info.getId(), allInitEntry);
                }
                initRecordService.initAllTask(vo.getDeviceSerialNumber(), info.getId(), initEntry);
            }

            BeanUtils.copyProperties(info, vo);
            vo.setAttachmentKind(attachmentInfo.getAttachmentKind());
            vo.setLinearSegment(null);

            infoService.saveOrUpdateInfo(info);
            if (info.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                liveDetectionWebSocket.startMeasure(info.getDeviceSerialNumber());
            }

            return R.ok(vo);
        }
    }

    @Override
    public R<Boolean> disconnectDevice(String deviceSerialNumber) {
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.Disconnect.getType());
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            OperationResultEntity data = operation.getData();
            DeviceInfoPO info = infoService.getInfo(deviceSerialNumber);
            info.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
            info.setDeviceId("");
            info.setConnectType(-1);
            info.setIp("");
            info.setIsInited(0);
            info.setMachineIp("");
            info.setDevicePort(-1);
            infoService.saveOrUpdateInfo(info);
            return R.ok(true);
        }
    }

    @Override
    public R<Boolean> startInitDevice(String deviceSerialNumber) {
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Init.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setIsInitFromHead(true);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            log.info("初始化启动失败: {}", operation.getMessage());
            return R.fail("初始化启动失败");
        } else {
            liveDetectionWebSocket.startMeasure(deviceSerialNumber);
            return R.ok(true);
        }
    }

    @Override
    public R<Boolean> continueInitDevice(String deviceSerialNumber) {

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setOperationType(OperationTypeEnums.Init.getType());
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setIsInitFromHead(false);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            log.info("继续初始化启动失败: {}", operation.getMessage());
            return R.fail("初始化启动失败");
        } else {
            return R.ok(true);
        }
    }

    @Override
    public R<String> getConnectDevice(String updateBy) {

        LambdaQueryWrapper<DeviceInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceInfoPO::getConnectStatus, Constants.CONNECT_DEVICE_SUCCESS)
                .eq(DeviceInfoPO::getUpdateBy, updateBy)
                .orderByDesc(DeviceInfoPO::getUpdated);
        //TODO 校验用户权限，是否可以连多个
        int max = 1;
        List<DeviceInfoPO> pos = deviceInfoDAO.selectList(queryWrapper);
        if (pos != null && !pos.isEmpty()) {
            DeviceInfoPO infoPO = pos.remove(0);

            try {
                pos.forEach(po -> disconnectDevice(po.getDeviceSerialNumber()));
            } catch (Exception e) {
                log.error("断开连接失败: {}", e.getMessage());
            }

            if (infoPO.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                DeviceConnectionManager connectionManager = DeviceConnectionManager.getInstance();
                DeviceConnectInfo device = connectionManager.getDevice(infoPO.getDeviceId());
                if (device != null) {
                    liveDetectionWebSocket.startMeasure(infoPO.getDeviceSerialNumber());
                }
            }
            return R.ok(infoPO.getDeviceSerialNumber());
        }
        return R.ok(null);
    }


    @Override
    public R<OperationResultEntity> setParamOneDevice(
            DevOperationInputParam param
    ) {

        param.setOperationType(OperationTypeEnums.SetParam.getType());
//        param.setDeviceSerialNumber(deviceSerialNumber);

        R<OperationResultEntity> operation =
                operationUtilService.operation(param);

        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            return R.ok(operation.getData());
        }
    }

    @Override
    public R<OperationResultEntity> getParamOneDevice(
            DevOperationInputParam param
    ) {

        // 1️⃣ 设置操作类型为“读取参数”
        param.setOperationType(OperationTypeEnums.GetValue.getType());

        // 2️⃣ 调用统一操作入口
        R<OperationResultEntity> operation =
                operationUtilService.operation(param);

        // 3️⃣ 返回结果
        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            return R.ok(operation.getData());
        }
    }



    @Override
    public R<OperationResultEntity> Action(
            DevOperationInputParam param
    ) {

        // 明确这是 Action 类型
        param.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());

        R<OperationResultEntity> operation =
                operationUtilService.operation(param);

        if (!operation.getSuccess()) {
            return R.fail(operation.getMessage());
        } else {
            new Thread(() -> {
                UnknowCountMessageQueue queue = UnknowCountMessageQueue.getInstance();
                while (true) {
                    if (queue.getQueueSize(param.getDeviceSerialNumber(), "Measure") <= 0) {
                        continue;
                    } else {
                        Object scan = queue.pollMessage(param.getDeviceSerialNumber(), "Measure");
                        System.out.println(scan.toString());
                    }
                }
            }).start();
            return R.ok(operation.getData());
        }
    }

}
