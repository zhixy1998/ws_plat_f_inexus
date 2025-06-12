package com.persee.workstation.uv_inexus.service.instrumentService.impl;


import com.persee.workstation.uv_inexus.dao.instrumentDao.InsConfigDAO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitInfoEnum;
import com.persee.workstation.uv_inexus.po.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.service.instrumentService.*;
import com.persee.workstation.uv_inexus.util.DeviceMessageUtils;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.vo.systemConfigVo.FindDeviceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class DeviceManageServiceImpl implements DeviceManageService {

    @Autowired
    InsConfigDAO configDAO;

    @Autowired
    DeviceMessageUtils messageUtils;

    @Override
    public DeviceInfoVO queryDeviceInfo(String deviceSerialNumber, String updateBy) {
        log.info("查询仪器信息");
        DeviceInfoVO vo = new DeviceInfoVO();

        vo.setDeviceSerialNumber("PX-DEVICE-T800-001");
        vo.setIsInited(0);
        vo.setConnectMessage("connect to device：ip=1.1.1.2 port=30500，连接中");
        vo.setConnectStatus(1);
        List<DeviceInitInfo> list  = new ArrayList<>();
        Integer length = DeviceInitInfoEnum.getLength();

        for (int i = 1; i < length + 1; i++) {
            DeviceInitInfo info = new DeviceInitInfo();
            info.setInitTaskNum(i);
            info.setInitProject(DeviceInitInfoEnum.getName(i));
            Random rand = new Random();
            int randomNumber = rand.nextInt(3);
            info.setInitStatus(randomNumber);

            list.add(info);

        }

        vo.setIntMessage(list);
        vo.setUserName(updateBy);
        vo.setIsDevOpened(1);
        vo.setAppendMessage("附件信息");

        return vo;
    }

    @Override
    public Boolean init(DeviceInitDTO initDTO) {
        if (initDTO.getDeviceSerialNumber().isEmpty()) {
            log.info("未传仪器序列号！");
            return false;
        }
        log.info("调用机器初始化接口");
        Random random = new Random();
        boolean b = random.nextBoolean();
        if (!b) {
            return false;
        }
        return true;
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



        log.info("查找到设备");
        FindDeviceVO vo = new FindDeviceVO();
        vo.setDeviceSerialNumber("01-1850-01-0000");
        vo.setIsInited(0);
        vo.setIsDevOpened(2);
        List<FindDeviceVO> list = new ArrayList<>();
        list.add(vo);

        new Thread(()-> {messageUtils.connectDevice();}).start();
        return list;
    }

    @Override
    public Boolean deviceActivation(String name) {
        return null;
    }

    @Override
    public QueryDeviceConfigVO queryDeviceConfig(String deviceSerialNumber) {

        QueryDeviceConfigVO vo = new QueryDeviceConfigVO();

        vo.setAnnexSetting(0);
        vo.setBandWidth(0.2);
        vo.setMessagePort(1);
        return vo;
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
    public Boolean deviceConfig(InsConfigPO po) {
//        if (po.getRestoreFactorySetting()) {
//            log.info("恢复出厂设置");
//        }
        log.info("设置仪器配置");


        log.info("设置仪器配置成功！");

        return configDAO.insert(po) > 0;
    }

    @Override
    public Boolean timedShutdown(String deviceSerialNumber, String closeTime) {
        log.info("定时关机");
        return null;
    }

    @Override
    public Boolean deviceCalibration(DeviceCalibrationDTO calibrationDTO) {
        return null;
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
                param.setLastValue("6");
            }
            vo.setParams(params);
        }


        return info;
    }

    private Boolean checkUser(String userName, String password){
        if (!StringUtils.hasLength(userName)){
            log.info("用户信息为空，使用默认用户");
            userName = "default";
            password = "123456";
            return true;
        }
        log.info("检测用户信息");
        //具体方法检测用户信息；
        return false;
    }

}
