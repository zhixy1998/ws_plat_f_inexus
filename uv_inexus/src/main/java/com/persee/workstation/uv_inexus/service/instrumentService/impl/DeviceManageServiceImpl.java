package com.persee.workstation.uv_inexus.service.instrumentService.impl;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.service.instrumentService.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Random;

@Slf4j
@Service
public class DeviceManageServiceImpl implements DeviceManageService {
    @Override
    public DeviceInfoVO queryDeviceInfo(DeviceQueryDTO queryDTO) {
        log.info("查询仪器信息");

        DeviceInfoVO infoVO = new DeviceInfoVO();

        infoVO.setIsInited("1");

        return null;
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
    public Boolean deviceActivation(String name) {
        return null;
    }

    @Override
    public Boolean deviceConfig(DeviceLocalConfigDTO dto) {
        if (dto.getRestoreFactorySetting()) {
            log.info("恢复出厂设置");
        }
        log.info("设置仪器配置");
        return null;
    }

    @Override
    public Boolean timedShutdown(String closeTime) {
        log.info("定时关机");
        return null;
    }

    @Override
    public Boolean deviceCalibration(DeviceCalibrationDTO calibrationDTO) {
        return null;
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
