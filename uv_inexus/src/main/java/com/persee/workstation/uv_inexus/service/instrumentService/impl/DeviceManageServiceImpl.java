package com.persee.workstation.uv_inexus.service.instrumentService.impl;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceInitInfoEnum;
import com.persee.workstation.uv_inexus.service.instrumentService.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceInfoVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceInitInfo;
import com.persee.workstation.uv_inexus.vo.systemConfigVo.FindDeviceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class DeviceManageServiceImpl implements DeviceManageService {
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
        vo.setDeviceSerialNumber("111111");
        vo.setIsInited(0);
        List<FindDeviceVO> list = new ArrayList<>();
        list.add(vo);
        return list;
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
    public Boolean timedShutdown(String deviceSerialNumber, String closeTime) {
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
