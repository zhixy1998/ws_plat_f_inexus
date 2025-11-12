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
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.*;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.changeUtil.*;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceConfParam;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import com.persee.workstation.uv_inexus.websocket.liveDetection.LiveDetectionWebSocket;
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
    LiveDetectionWebSocket liveDetectionWebSocket;

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
            return R.fail("未查找到仪器信息");
        }

        List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceSerialNumber, deviceInfoPO.getId());
        if (initList.isEmpty()) {
            log.info("查询初始化状态异常");
            return R.fail(false, "查询初始化状态异常");
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
            return R.fail("未查找到仪器信息");
        }
//        log.info("查询仪器状态");
        return R.ok(info.getDeviceCurrentStatus());
    }

    @Override
    public R<Boolean> init(DeviceInitDTO initDTO) {
        if (initDTO.getDeviceSerialNumber().isEmpty()) {
            log.info("未传仪器序列号！");
            return R.fail("缺少序列号");
        }

        DeviceInfoPO infoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());
        if (infoPO == null || !StringUtils.hasText(infoPO.getId()) || !StringUtils.hasText(infoPO.getDeviceModel())) {
            log.info("未找到连接仪器信息！");
            return R.fail("未查找到仪器信息");
        }
        log.info("调用机器初始化接口");

        R<Boolean> r = infoService.useDevice(initDTO.getDeviceSerialNumber(), Constants.DEVICE_STATUS_INIT);
        if (!r.getSuccess()) {
            log.info(r.getMessage());
            return R.fail(r.getMessage());
        }

        LambdaQueryWrapper<DeviceModelInfoPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceModelInfoPO::getDevModel, infoPO.getDeviceModel());
        DeviceModelInfoPO modelInfoPO = deviceModelInfoDAO.selectOne(wrapper);

        List<DeviceInitEntryInfo> initEntry = modelInfoPO.getInitEntry();

        DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(initDTO.getDeviceSerialNumber());
        Integer kind = attachmentInfo.getAttachmentKind();

        if (kind.equals(DeviceAttachmentKindEnum.FIVE_POOL.getType())|| kind.equals(DeviceAttachmentKindEnum.EIGHT_POOL.getType())){
            List<DeviceInitEntryInfo> allInitEntry = addInitEntry(initEntry);
            initRecordService.initAllTask(initDTO.getDeviceSerialNumber(), infoPO.getId(), allInitEntry);
        } else {
            initRecordService.initAllTask(initDTO.getDeviceSerialNumber(), infoPO.getId(), initEntry);
        }

        infoPO.setIsInited(Constants.INIT_STATUS_RUNNING);
        infoService.saveOrUpdateInfo(infoPO);


        new Thread(() -> {
            try {
                stopInit.put(initDTO.getDeviceSerialNumber(), false);
                newStartInit(infoPO, attachmentInfo.getAttachmentKind());
            } catch (Exception e) {
                DetectionConstants.isDeviceIdle = true;
                throw new RuntimeException(e);
            }
        }).start();

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

        Command mode = new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()});
        devMessageUtil.wavelengthLocate(mode, ip);

        Double step = formulaUtil.calculateStep(660.0, infoPO.getDeviceSerialNumber());
        Command command = new Command("SetWl", new Object[]{step});
        devMessageUtil.wavelengthLocate(command, ip);

        devMessageUtil.setDeviceInitStatus(ip, 1);

        infoService.releaseDevice(infoPO.getDeviceSerialNumber());

        Constants.devConnectInfo.put(infoPO.getDeviceSerialNumber(), true);
        liveDetectionWebSocket.startMeasure(infoPO.getDeviceSerialNumber());

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
    public Boolean disConnect(String deviceSerialNumber, String updateBy) {
        DeviceInfoPO infoPO = infoService.getInfo(deviceSerialNumber);
        Constants.devConnectInfo.remove(deviceSerialNumber);
        devMessageUtil.disConnect(infoPO.getIp());

        infoPO.setConnectStatus(Constants.CONNECT_DEVICE_STOP);
        infoService.saveOrUpdateInfo(infoPO);

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
            if (po.getIsInited().equals(Constants.INIT_STATUS_SUCCESS)) {
                Constants.devConnectInfo.put(deviceVO.getDeviceSerialNumber(), true);
                liveDetectionWebSocket.startMeasure(deviceVO.getDeviceSerialNumber());
            }

            po.setIp(deviceVO.getIp());
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

            if (attachmentInfo == null) {
                attachmentInfo = new DeviceAttachmentTypeVO();
                attachmentInfo.setAttachmentNum(12);
                attachmentInfo.setAttachmentName("未识别");
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
            return R.fail("缺少序列号");
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
        } else{
            vo.setMessagePort(po.getMessagePort());
            vo.setAnnexSetting(po.getAnnexSetting());
            vo.setBandWidth(po.getBandWidth());
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
            return R.fail("缺少序列号");
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
            return R.fail("缺少序列号");
        }

        if (dto.getCalibrationType() == null || DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()) == null) {
            log.info("缺少校准类型");
            return R.fail("缺少校准类型");
        }

        String ip = infoService.getIp(dto.getDeviceSerialNumber());
        log.info("调用校准");

        if (dto.getCalibrationType() == 1) {
            //波长校正
            Command command = new Command(DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()), new Object[]{});
            try {
                devMessageUtil.deviceCalibration(command, ip);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else if (dto.getCalibrationType() == 2) {
            //暗电流校正
            Command command = new Command(DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()), new Object[]{});
            try {
                devMessageUtil.deviceCalibration(command, ip);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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

                CalibrationTempPO po = new CalibrationTempPO();
                po.setUpdateBy(dto.getUpdateBy());
                po.setParamValue(data.getDataValue());
                queryWrapper.eq(CalibrationTempPO::getParam, data.getDataKey());
                tempDao.update(po, queryWrapper);
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
            ans.setBandWidth(Arrays.asList("2"));
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
            return R.fail("未找到该设备");
        }

        String devModel = deviceSerialNumber.substring(3, 8);

        LambdaQueryWrapper<DeviceModelInfoPO> query = new LambdaQueryWrapper<>();
        query.eq(DeviceModelInfoPO::getDevModel, devModel);

        DeviceModelInfoPO deviceModelInfoPO = deviceModelInfoDAO.selectOne(query);
        if (deviceModelInfoPO == null) {
            return R.fail("未识别该型号");
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
        ChangeParam or = new ChangeParam("500", "128337", "103479", "128337");
        ChangeParam cu = new ChangeParam("500", "491765", "402424", "1716");

//        ChangeParam changeParam = new ChangeParam("0.01", "123456", "9852", "123456");
        String s = DataCountUtil.countTPercent(or, cu, 1.0);
        String s1 = DataCountUtil.changeTPercentOrAbs(null, s);
        System.out.println("+++" + s + "+++" + s1);


    }

}
