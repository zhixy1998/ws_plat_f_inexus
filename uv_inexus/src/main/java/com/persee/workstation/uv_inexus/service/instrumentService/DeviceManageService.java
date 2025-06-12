package com.persee.workstation.uv_inexus.service.instrumentService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.vo.systemConfigVo.FindDeviceVO;

import java.util.List;

public interface DeviceManageService {

    DeviceInfoVO queryDeviceInfo(String deviceSerialNumber, String updateBy);

    Boolean init(DeviceInitDTO initDTO);

    Boolean startDevice(DeviceStartDTO startDTO);

    Boolean softClose(DeviceSoftCloseDTO closeDTO);

    List<FindDeviceVO> findDevice();

    /**
     * 仪器激活接口
     * 参数暂时未知 @param
     * @return
     */
    Boolean deviceActivation(String name);

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
    Boolean timedShutdown(String deviceSerialNumber, String closeTime);



    /**
     * 仪器校准接口
     * @param calibrationDTO
     * @return
     */
    Boolean deviceCalibration(DeviceCalibrationDTO calibrationDTO);


}
