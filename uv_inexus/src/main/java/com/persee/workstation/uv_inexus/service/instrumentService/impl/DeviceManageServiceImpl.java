package com.persee.workstation.uv_inexus.service.instrumentService.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.instrumentDao.CalibrationTempDAO;
import com.persee.workstation.uv_inexus.dao.instrumentDao.InsConfigDAO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceCalibrationDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceInitDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceSoftCloseDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceStartDTO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceConnectMessageEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitInfoEnum;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.instrumentPo.CalibrationTempPO;
import com.persee.workstation.uv_inexus.po.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.instrumentPo.InitRecordPO;
import com.persee.workstation.uv_inexus.po.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.instrumentService.InitRecordService;
import com.persee.workstation.uv_inexus.util.ByteUse;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.DeviceInitCommandEnum;
import com.persee.workstation.uv_inexus.util.changeUtil.FindDevEnum;
import com.persee.workstation.uv_inexus.vo.ScanParamInfo;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import lombok.extern.slf4j.Slf4j;
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
    InitRecordService initRecordService;

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    DeviceMessageUtils messageUtils;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Value("${device.default.sendPort}")
    private Integer sendPort;

    @Value("${device.default.receivePort}")
    private Integer receivePort;

    @Override
    public R<DeviceInfoVO> queryDeviceInfo(String deviceSerialNumber, String updateBy) {
        log.info("查询仪器信息");
        DeviceInfoVO vo = new DeviceInfoVO();

        List<DeviceInitInfo> infoList = new ArrayList<>();

        List<InitRecordPO> initList = initRecordService.searchInitStatus(deviceSerialNumber);
        if (initList.isEmpty()) {
            log.info("查询初始化状态异常");
            return R.fail(false, "查询初始化状态异常");
        }

        DeviceInfoPO deviceInfoPO = infoService.getInfo(deviceSerialNumber);

        vo.setIsInited(deviceInfoPO.getIsInited());
        vo.setConnectStatus(deviceInfoPO.getConnectStatus());
        vo.setUserName(updateBy);
        vo.setIsDevOpened(deviceInfoPO.getIsDevOpened());
        vo.setDeviceSerialNumber(deviceSerialNumber);

        String connect = DeviceConnectMessageEnum.getName(deviceInfoPO.getConnectStatus()) + " IP="
                + deviceInfoPO.getIp() + " port=" + sendPort;

        vo.setConnectMessage(connect);

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
            newStartInit(infoPO);
        }).start();

        return R.ok(true);
    }


    private void newStartInit(DeviceInfoPO infoPO) {
        log.info("自检开始");
        R<String> ret;
        String ip = infoPO.getIp();

        InitRecordPO recordPO = new InitRecordPO();
        recordPO.setDeviceInfoId(infoPO.getId());
        recordPO.setDeviceSerialNumber(infoPO.getDeviceSerialNumber());

        for (int i = 0; i < DeviceInitCommandEnum.getLength(); i++) {
            Command command;
            if (DeviceInitCommandEnum.getComId(i).equals("SetSlit1")) {
                command = new Command(DeviceInitCommandEnum.getComId(i), new Object[]{20});
            } else {
                command = new Command(DeviceInitCommandEnum.getComId(i), new Object[]{});
            }

            R<String> initDevice = devMessageUtil.initDevice(command);
            if (!initDevice.getSuccess()) {
                log.info("{}指令初始化异常", DeviceInitCommandEnum.getComId(i));
                infoPO.setConnectStatus(Constants.CONNECT_DEVICE_FALSE);
                recordPO.setInitTaskNum(getInitTaskNum(i));
                failDeal(infoPO, recordPO);
                return;
            }
            log.info("{}指令初始化成功", DeviceInitCommandEnum.getComId(i));
            infoPO.setIsDevOpened(Constants.DEVICE_IS_OPEN);
            infoPO.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            infoService.saveOrUpdateInfo(infoPO);
            recordPO.setInitTaskNum(getInitTaskNum(i));
            updateSuccess(recordPO);
        }
        infoPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);
    }

    private Integer getInitTaskNum(Integer i) {
        switch (i) {
            case 0:
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
            case 11:
                return 8;
            default:
                return 8;
        }

    }


    private void startInit(DeviceInfoPO infoPO) {
        log.info("开始自检~~~");
        R<String> ret;
        String ip = infoPO.getIp();

        InitRecordPO recordPO = new InitRecordPO();
        recordPO.setDeviceInfoId(infoPO.getId());
        recordPO.setDeviceSerialNumber(infoPO.getDeviceSerialNumber());

        log.info("获取连接状态");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.LinkMachineSend, 2);
        if (!ret.getSuccess()) {
            log.info("仪器连接失败");

            infoPO.setConnectStatus(Constants.CONNECT_DEVICE_FALSE);

            recordPO.setInitTaskNum(DeviceInitInfoEnum.WAVELENGTH_LIST.getCode());

            failDeal(infoPO, recordPO);
            return;
        }
        log.info("连接成功，");

        infoPO.setIsDevOpened(Constants.DEVICE_IS_OPEN);
        infoPO.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);

        ret = messageUtils.initUtil(ip, sendPort, ByteUse.ReadSamplepoolSend, 4);
        if (!ret.getSuccess()) {
            log.info("读取样品池失败");
            log.info(ret.getMessage());

            recordPO.setInitTaskNum(DeviceInitInfoEnum.WAVELENGTH_LIST.getCode());
            recordPO.setInitStatus(Constants.INIT_STATUS_FALSE);
            failDeal(infoPO, recordPO);
            return;
        }
        log.info("获取初始化状态");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.InitStatusSend, 5);

            /*
            未知指令，uvwin7发送的初始化有，不过excel里未记录
            ret = messageUtils.initUtil(ip, sendPort, ByteUse.BeforeInit, 999);
             */

//            if (!ret.getSuccess()) {
//                log.info("获取初始化状态异常");
//                log.info(ret.getMessage());
//                failDeal(infoPO, null);
//                return;
//            }
        log.info("初始化样品池");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.InitSamplepoolSend, 6);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.WAVELENGTH_LIST.getCode());
        if (!ret.getSuccess()) {
            log.info("初始化样品池异常");
            log.info(ret.getMessage());

            failDeal(infoPO, recordPO);
            return;
        }

        log.info("样品池初始化成功，开始样品池定位");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.SamplepoolLocationSend, 7);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.WAVELENGTH_LIST.getCode());
        if (!ret.getSuccess()) {
            log.info("样品池定位异常");
            log.info(ret.getMessage());

            failDeal(infoPO, recordPO);
            return;
        }
        log.info("样品池初始化完成，更新状态");
        updateSuccess(recordPO);

        log.info("样品池定位完成，开始初始化滤色片电机");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.InitColorFilterSend, 8);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.COLOR_FILTER.getCode());
        if (!ret.getSuccess()) {
            log.info("初始化滤色片电机异常");
            log.info(ret.getMessage());

            failDeal(infoPO, recordPO);
            return;
        }
        log.info("滤色片电机初始化完成");
        updateSuccess(recordPO);

        log.info("滤色片电机初始化完成，开始初始化狭缝电机");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.InitSlitMotorSend, 9);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.INIT_SLIT_MOTOR.getCode());
        if (!ret.getSuccess()) {
            log.info("狭缝电机初始化异常");
            log.info(ret.getMessage());

            failDeal(infoPO, recordPO);
            return;
        }
        log.info("狭缝电机初始化成功");
        updateSuccess(recordPO);

        log.info("狭缝电机初始化完成，开始设置狭缝宽度");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.SetSlitWidthSend, 10);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.INIT_SLIT_MOTOR.getCode());
        if (!ret.getSuccess()) {
            log.info("设置狭缝宽度异常");
            log.info(ret.getMessage());

            failDeal(infoPO, recordPO);
            return;
        }
        log.info("狭缝宽度初始化成功");
        updateSuccess(recordPO);

        log.info("狭缝宽度设置完成，开始波长电机初始化");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.InitWavelengthSend, 11);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.SCANNING_MOTOR.getCode());
        if (!ret.getSuccess()) {
            log.info("初始化波长异常");
            log.info(ret.getMessage());

            failDeal(infoPO, recordPO);
            return;
        }
        log.info("波长电机初始化成功");
        updateSuccess(recordPO);

        log.info("初始化波长完成，开始灯电机初始化");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.InitLampMotorSend, 12);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.LIGHT_SOURCE_MOTOR.getCode());
        if (!ret.getSuccess()) {
            log.info("灯电机初始化异常");
            log.info(ret.getMessage());
            failDeal(infoPO, recordPO);
            return;
        }
        log.info("灯电机初始成功");
        updateSuccess(recordPO);

        log.info("灯电机初始化完成，开始钨灯能量检查");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.WuDengPowerSend, 13);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.TUNGSTEN_LAMP_ENERGY.getCode());
        if (!ret.getSuccess()) {
            log.info("钨灯检查异常");
            log.info(ret.getMessage());
            failDeal(infoPO, recordPO);
            return;
        }
        log.info("钨灯检测成功");
        updateSuccess(recordPO);

        log.info("钨灯能量检查完成，开始氘灯能量检查");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.DaoDengPowerSend, 14);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.DEUTERIUM_LAMP_ENERGY.getCode());
        if (!ret.getSuccess()) {
            log.info("氘灯检测异常");
            log.info(ret.getMessage());
            failDeal(infoPO, recordPO);
            return;
        }
        log.info("氘灯检测成功");
        updateSuccess(recordPO);

        log.info("氘灯能量检查完成,开始波长检测");
        ret = messageUtils.initUtil(ip, sendPort, ByteUse.CheckWavelengthSend, 15);
        recordPO.setInitTaskNum(DeviceInitInfoEnum.WAVELENGTH.getCode());
        if (!ret.getSuccess()) {
            log.info("波长检测异常");
            log.info(ret.getMessage());
            failDeal(infoPO, recordPO);
            return;
        }

        log.info("波长检测成功");
        updateSuccess(recordPO);

        infoPO.setIsInited(Constants.INIT_STATUS_SUCCESS);
        infoService.saveOrUpdateInfo(infoPO);
    }

    private void updateSuccess(InitRecordPO recordPO) {
        recordPO.setInitStatus(Constants.INIT_TASK_STATUS_SUCCESS);
        initRecordService.saveOrUpdateInitTask(recordPO);
    }

    private void failDeal(DeviceInfoPO po, InitRecordPO recordPO) {

        po.setIsInited(Constants.INIT_STATUS_FALSE);
        infoService.saveOrUpdateInfo(po);

        if (recordPO != null) {
            for (int i = recordPO.getInitTaskNum(); i <= DeviceInitInfoEnum.getLength(); i++) {
                recordPO.setInitTaskNum(i);
                recordPO.setInitStatus(Constants.INIT_STATUS_FALSE);
                initRecordService.saveOrUpdateInitTask(recordPO);
            }

        }
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
    public List<FindDeviceVO> findDevice() {
        log.info("发送广播查找设备");
        FindDeviceVO deviceVO = messageUtils.searchDevice();

        if (deviceVO != null) {
            log.info("查找到设备");

            DeviceInfoPO po = new DeviceInfoPO();
            po.setIsInited(deviceVO.getIsInited());
            po.setIp(deviceVO.getIp());
            po.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
            po.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            po.setIsDevOpened(1);
            po.setDevicePort(sendPort);
            deviceVO.setIsDevOpened(1);

            infoService.saveOrUpdateInfo(po);

            List<FindDeviceVO> list = new ArrayList<>();
            list.add(deviceVO);
            return list;
        }
        log.info("未查找到设备");
        return null;
    }

    @Override
    public List<FindDeviceVO> newFindDevice() {
        List<Command> list = new ArrayList<>();
        list.add(new Command(FindDevEnum.FindDevice.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.NewIP.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.Connect.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetSn.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetAccessoryType.getComId(), new Object[]{}));
        list.add(new Command(FindDevEnum.GetInitStatus.getComId(), new Object[]{}));


        R<FindDeviceVO> connected = devMessageUtil.connectDevice(list);
        if (connected.getSuccess()) {
            log.info("找到设备");
            FindDeviceVO deviceVO = connected.getData();
            DeviceInfoPO po = new DeviceInfoPO();
            po.setIsInited(deviceVO.getIsInited());
            po.setIp(deviceVO.getIp());
            po.setDeviceSerialNumber(deviceVO.getDeviceSerialNumber());
            po.setConnectStatus(Constants.CONNECT_DEVICE_SUCCESS);
            po.setIsDevOpened(1);
            po.setDevicePort(sendPort);
            deviceVO.setIsDevOpened(1);

            infoService.saveOrUpdateInfo(po);

            List<FindDeviceVO> devices = new ArrayList<>();
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
        String ip = infoService.getIp(dto.getDeviceSerialNumber());
        log.info("调用校准");


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
                param.setLastValue(tempPO.getParamValue());


            }


            vo.setParams(params);
        }


        return info;
    }

    @Override
    public void test() {
        String ip = "169.254.110.254";
        ScanParamInfo info = new ScanParamInfo();
        info.setWaveLengthInterval("0.5");
        info.setStartWaveLength("400");
        info.setEndWaveLength("700");
//        messageUtils.searchDevice();
//        messageUtils.initUtil(ip, sendPort, ByteUse.LinkMachineSend, 2);
        messageUtils.setScanParam(ip, sendPort, ByteUse.SpectrumScanAllConfig);

    }

}
