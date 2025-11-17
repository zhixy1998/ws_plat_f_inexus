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

package com.persee.workstation.uv_inexus.service.guide.instrumentService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceCalibrationDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceInitDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceSoftCloseDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceStartDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceCalibrationVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceInfoVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.QueryDeviceConfigVO;

import java.util.List;

public interface DeviceManageService {

    R<DeviceInfoVO> queryDeviceInfo(String deviceSerialNumber, String updateBy);

    R<Integer> queryDeviceStatus(String deviceSerialNumber);

    R<Boolean> init(DeviceInitDTO initDTO);

    R<Boolean> stopInit(String deviceSerialNumber, String updateBy);

    Boolean startDevice(DeviceStartDTO startDTO);

    Boolean softClose(DeviceSoftCloseDTO closeDTO);

    R<Boolean> closeOrOpenLamp(String lampType, Integer status);

    /**
     * 断开连接
     */
    Boolean disConnect(String deviceSerialNumber, String updateBy);


    R<List<FindDeviceVO>> findDevice() throws Exception;

    /**
     * 仪器激活接口
     * 参数暂时未知 @param
     * @return
     */
    Boolean deviceActivation(String name);

    /**
     * 查询仪器配置接口
     * @param deviceSerialNumber
     * @return
     */
    R<QueryDeviceConfigVO> queryDeviceConfig(String deviceSerialNumber);

    /**
     * 查询光谱带宽接口
     * @return
     */
    List<Double> getBandWidth(String deviceSerialNumber);

    /**
     * 仪器配置接口
     * @param po
     * @return
     */
    R<Boolean> deviceConfig(InsConfigPO po);

    /**
     * 自动进样器配置
     */
//    R<Boolean> autoInjectorConfig();

    /**
     * 定时关机设置
     * @param closeTime
     * @return
     */
    Boolean timedStart(String deviceSerialNumber, String closeTime);

    /**
     * 仪器校准接口
     * @param calibrationDTO
     * @return
     */
    R<Boolean> deviceCalibration(DeviceCalibrationDTO calibrationDTO);


    /**
     * 查询仪器校准方法接口
     * @param deviceSerialNumber
     * @return
     */
    List<DeviceCalibrationVO> queryCalibrationInfo(String deviceSerialNumber);

    /**
     * 检查仪器状态
     */
    R<Boolean> checkDeviceStatus(String deviceSerialNumber);

    /**
     * 更新仪器当前状态
     */
    R<Boolean> updateDeviceStatus(String deviceSerialNumber, String status);

    R<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber);


    void test();


}
