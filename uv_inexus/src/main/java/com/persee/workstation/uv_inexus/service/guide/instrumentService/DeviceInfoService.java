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

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;

public interface DeviceInfoService {

    DeviceInfoPO getInfo(String deviceSerialNumber);

    Boolean saveOrUpdateInfo(DeviceInfoPO po);

    String getIp(String deviceSerialNumber);

    //查找附件类型
    String getAttachmentType(String deviceSerialNumber);

    void updateDeviceCurrentStatus(String deviceSerialNumber, Integer currentStatus);

    Boolean changeDeviceName(String deviceSerialNumber, String newDeviceName, String updateBy);

    //占用仪器
    R<Boolean> useDevice(String deviceSerialNumber, Integer status);

    //释放仪器
    void releaseDevice(String deviceSerialNumber);

    //激活仪器
    void activeDevice(String deviceSerialNumber);

    //查询仪器激活状态
    Integer queryDeviceActivationStatus(String deviceSerialNumber);
}
