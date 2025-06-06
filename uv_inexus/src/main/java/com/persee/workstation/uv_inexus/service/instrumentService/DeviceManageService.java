package com.persee.workstation.uv_inexus.service.instrumentService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;

public interface DeviceManageService {

    DeviceInfoVO queryDeviceInfo(DeviceQueryDTO queryDTO);

    Boolean init(DeviceInitDTO initDTO);

    Boolean startDevice(DeviceStartDTO startDTO);

    Boolean softClose(DeviceSoftCloseDTO closeDTO);

    /**
     * 仪器激活接口
     * 参数暂时未知 @param
     * @return
     */
    Boolean deviceActivation(String name);

    /**
     * 仪器配置
     * 1，定时开关机
     * 2，基础配置
     */

    /**
     * 仪器配置接口
     * @param dto
     * @return
     */
    Boolean deviceConfig(DeviceLocalConfigDTO dto);

    /**
     * 定时关机设置
     * @param closeTime
     * @return
     */
    Boolean timedShutdown(String closeTime);



    /**
     * 仪器校准接口
     * @param calibrationDTO
     * @return
     */
    Boolean deviceCalibration(DeviceCalibrationDTO calibrationDTO);


}
