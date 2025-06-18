package com.persee.workstation.uv_inexus.service.instrumentService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;

import java.util.List;

public interface DeviceManageService {

    R<DeviceInfoVO> queryDeviceInfo(String deviceSerialNumber, String updateBy);

    R<Boolean> init(DeviceInitDTO initDTO);

    Boolean startDevice(DeviceStartDTO startDTO);

    Boolean softClose(DeviceSoftCloseDTO closeDTO);

    List<FindDeviceVO> findDevice();

    /**
     * 仪器激活接口
     * 参数暂时未知 @param
     * @return
     */
    Boolean deviceActivation(String name);

    QueryDeviceConfigVO queryDeviceConfig(String deviceSerialNumber);

    List<Double> getBandWidth();

    /**
     * 仪器配置接口
     * @param po
     * @return
     */
    Boolean deviceConfig(InsConfigPO po);

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


    /**
     * 查询仪器校准方法接口
     * @param deviceSerialNumber
     * @return
     */
    List<DeviceCalibrationVO> queryCalibrationInfo(String deviceSerialNumber);

    void test();


}
