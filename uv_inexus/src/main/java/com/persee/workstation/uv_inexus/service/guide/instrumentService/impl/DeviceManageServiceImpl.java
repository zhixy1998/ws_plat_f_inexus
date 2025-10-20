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
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.CalibrationTempDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.InsConfigDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceCalibrationDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceInitDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceSoftCloseDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceStartDTO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceCalibrationInfoEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceConnectMessageEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitInfoEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.CalibrationTempPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InitRecordPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.ByteUse;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.DeviceInitCommandEnum;
import com.persee.workstation.uv_inexus.util.changeUtil.FindDevEnum;
import com.persee.workstation.uv_inexus.vo.ScanParamInfo;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
    FSamplePoolConfigService samplePoolConfigService;

    @Value("${device.default.sendPort}")
    Integer sendPort;

    @Value("${device.default.receivePort}")
    Integer receivePort;

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

        String connect = DeviceConnectMessageEnum.getName(deviceInfoPO.getConnectStatus()) + " IP="
                + deviceInfoPO.getIp() + " port=" + sendPort;

        vo.setConnectMessage(connect);

        String attachmentType = infoService.getAttachmentType(deviceSerialNumber);
        vo.setAppendMessage(attachmentType);

        for (InitRecordPO recordPO : initList) {
            DeviceInitInfo info = new DeviceInitInfo();
            info.setInitTaskNum(recordPO.getInitTaskNum());
            info.setInitProject(DeviceInitInfoEnum.getName(recordPO.getInitTaskNum()));
            info.setInitStatus(recordPO.getInitStatus());

            infoList.add(info);
        }
        vo.setIntMessage(infoList);


        return R.ok(vo);
    }

    @Override
    public R<Boolean> init(DeviceInitDTO initDTO) {
        if (initDTO.getDeviceSerialNumber().isEmpty()) {
            log.info("未传仪器序列号！");
            return R.fail("缺少序列号");
        }

        DeviceInfoPO infoPO = infoService.getInfo(initDTO.getDeviceSerialNumber());
        if (infoPO == null || infoPO.getId() == null || infoPO.getDeviceSerialNumber() == null) {
            log.info("未找到连接仪器信息！");
            return R.fail("未查找到仪器信息");
        }
        log.info("调用机器初始化接口");

        initRecordService.initAllTask(initDTO.getDeviceSerialNumber(), infoPO.getId());

        infoPO.setIsInited(Constants.INIT_STATUS_RUNNING);
        infoService.saveOrUpdateInfo(infoPO);

//        new Thread(() -> {
//            startInit(infoPO);
//        }).start();

        new Thread(() -> {
            try {
                newStartInit(infoPO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        return R.ok(true);
    }


    private void newStartInit(DeviceInfoPO infoPO) throws Exception {
        log.info("自检开始");
        String ip = infoPO.getIp();

        while (!DetectionConstants.isDeviceIdle) {
            log.info("等待设备空闲");
            Thread.sleep(10);
        }
        DetectionConstants.isDeviceIdle = false;

        infoService.updateDeviceCurrentStatus(infoPO.getDeviceSerialNumber(), Constants.DEVICE_STATUS_INIT);

        InitRecordPO recordPO = new InitRecordPO();
        recordPO.setDeviceInfoId(infoPO.getId());
        recordPO.setDeviceSerialNumber(infoPO.getDeviceSerialNumber());
        int i = 1;
        while (i <= DeviceInitCommandEnum.getLength()) {
            recordPO.setInitTaskNum(getInitTaskNum(i));
            updateRunning(recordPO);
            Command command;
            if (DeviceInitCommandEnum.getComId(i).equals("SetSlit1")) {
                command = new Command(DeviceInitCommandEnum.getComId(i), new Object[]{2});
            } else if (DeviceInitCommandEnum.getComId(i).equals("SetCellPos")) {
                command = new Command(DeviceInitCommandEnum.getComId(i), new Object[]{1});
            } else {
                command = new Command(DeviceInitCommandEnum.getComId(i), new Object[]{});
            }

            R<String> initDevice = devMessageUtil.initDevice(command, ip);
            if (!initDevice.getSuccess()) {
                log.info("{}指令初始化异常", DeviceInitCommandEnum.getComId(i));
                DetectionConstants.isDeviceIdle = true;
                if (i == 1) {
                    infoPO.setConnectStatus(Constants.CONNECT_DEVICE_FALSE);
                }
                recordPO.setInitTaskNum(getInitTaskNum(i));
                failDeal(infoPO, recordPO);
                infoService.updateDeviceCurrentStatus(infoPO.getDeviceSerialNumber(), Constants.DEVICE_STATUS_IDLE);
                devMessageUtil.setDeviceInitStatus(ip, 1);
                return;
            }
            log.info("{}指令初始化成功", DeviceInitCommandEnum.getComId(i));
            if (i < 3) {
                infoPO.setIsDevOpened(Constants.DEVICE_IS_OPEN);
                infoPO.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
                infoService.saveOrUpdateInfo(infoPO);
            }
            recordPO.setInitTaskNum(getInitTaskNum(i));
            updateSuccess(recordPO);
            i++;
        }

        DetectionConstants.isDeviceIdle = true;
        infoPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);

        devMessageUtil.setDeviceInitStatus(ip, 1);

        infoService.updateDeviceCurrentStatus(infoPO.getDeviceSerialNumber(), Constants.DEVICE_STATUS_IDLE);

    }

    private Integer getInitTaskNum(Integer i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
                return 1;
            case 4:
                return 2;
            case 5:
            case 6:
                return 3;
            case 7:
                return 4;
            case 8:
                return 5;
            case 9:
                return 6;
            case 10:
                return 7;
            default:
                return 8;
        }

    }

    private void updateSuccess(InitRecordPO recordPO) {
        recordPO.setInitStatus(Constants.INIT_TASK_STATUS_SUCCESS);
        initRecordService.saveOrUpdateInitTask(recordPO);
    }

    private void updateRunning(InitRecordPO recordPO) {
        recordPO.setInitStatus(Constants.INIT_TASK_STATUS_ING);
        initRecordService.saveOrUpdateInitTask(recordPO);
    }

    private void failDeal(DeviceInfoPO po, InitRecordPO recordPO) {


        recordPO.setInitStatus(Constants.INIT_STATUS_FALSE);
        initRecordService.saveOrUpdateInitTask(recordPO);

        po.setIsInited(Constants.INIT_STATUS_FALSE);
        infoService.saveOrUpdateInfo(po);
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
    @DS("guide")
    public List<FindDeviceVO> findDevice() throws Exception {
        List<Command> list = new ArrayList<>();
        list.add(new Command(FindDevEnum.FindDevice.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.NewIP.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.Connect.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetSn.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetAccessoryType.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetInitStatus.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetCellPos.getComId(), new Object[]{}));

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

            if (deviceVO.getIsInited() == 0) {
                po.setIsInited(deviceVO.getIsInited());
            }

            if (po.getIsInited() == null) {
                po.setIsInited(Constants.INIT_STATUS_FALSE);
            }

            po.setIp(deviceVO.getIp());
            po.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
            po.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            po.setIsDevOpened(1);
            po.setAttachmentType(deviceVO.getAttachmentType());
            po.setDevicePort(sendPort);
            po.setDeviceCurrentStatus(Constants.DEVICE_STATUS_IDLE);

            //TODO 根据仪器序列号，获取型号信息，还需要调整
            String deviceModel = deviceVO.getDeviceSerialNumber().substring(3, 8);
            po.setDeviceModel(deviceModel);

            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceVO.getDeviceSerialNumber());

            FSampleAttachTypeSaveDTO dto = new FSampleAttachTypeSaveDTO();

            BeanUtils.copyProperties(deviceVO, dto);
            dto.setAttachmentTypeName(attachmentInfo.getAttachmentName());
            samplePoolConfigService.saveSamplePoolInfo(dto);

            po.setAttachmentName(attachmentInfo.getAttachmentName());
            infoService.saveOrUpdateInfo(po);

            initRecordService.initAllTask(deviceVO.getDeviceSerialNumber(), po.getId());

            BeanUtils.copyProperties(po, deviceVO);
            deviceVO.setAttachmentKind(attachmentInfo.getAttachmentKind());
            devices.add(deviceVO);

            return devices;
        }
        return null;
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
            log.info("查询仪器配置--未找到配置");
            return R.fail("未找到配置");
        }
        vo.setMessagePort(po.getMessagePort());
        vo.setAnnexSetting(po.getAnnexSetting());
        vo.setBandWidth(po.getBandWidth());
        return R.ok(vo);
    }

    @Override
    public List<Double> getBandWidth() {
        List<Double> result = new ArrayList<>();
        result.add(0.2);
        result.add(0.5);
        result.add(1.0);
        result.add(2.0);
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
            devMessageUtil.deviceCalibration(command, ip);
        } else if (dto.getCalibrationType() == 2) {
            //暗电流校正
            Command command = new Command(DeviceCalibrationInfoEnum.getComId(dto.getCalibrationType()), new Object[]{});
            devMessageUtil.deviceCalibration(command, ip);
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

        //根据序列号，去查找仪器类型，根据仪器类型，去获取参数列表
        log.info("检测仪器类型");
        String type = "T600"; //暂时默认用T600
        List<DeviceCalibrationVO> info = configDAO.getCalibrationInfo(type);
        for (DeviceCalibrationVO vo : info) {
            List<DeviceCalibrationParams> params = configDAO.getCalibrationParams(vo.getCalibrationType());
            for (DeviceCalibrationParams param : params) {
                List<DataNameValue> value = configDAO.getCalibrationValue(param.getId());
                param.setData(value);
                LambdaQueryWrapper<CalibrationTempPO> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(CalibrationTempPO::getCalibrationType, vo.getCalibrationType())
                        .eq(CalibrationTempPO::getDeviceSerialNumber, deviceSerialNumber)
                        .eq(CalibrationTempPO::getParam, param.getParam()).orderByDesc(CalibrationTempPO::getId);

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
    public void test() {
        String ip = "169.254.110.254";
        ScanParamInfo info = new ScanParamInfo();
        info.setWaveLengthInterval("0.5");
        info.setStartWaveLength("400");
        info.setEndWaveLength("700");
//        messageUtils.searchDevice();+
//        messageUtils.initUtil(ip, sendPort, ByteUse.LinkMachineSend, 2);
        messageUtils.setScanParam(ip, sendPort, ByteUse.SpectrumScanAllConfig);

    }

}
