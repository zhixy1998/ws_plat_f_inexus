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

package com.persee.workstation.uv_inexus.dao.guide.instrumentDao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import org.apache.ibatis.annotations.Param;

@DS("guide")
public interface DeviceInfoDAO extends BaseMapper<DeviceInfoPO> {

    String getIp(@Param("device_serial_number") String deviceSerialNumber);

    String getAttachmentType(@Param("device_serial_number") String deviceSerialNumber);
    DeviceAttachmentTypeVO getAttachmentInfo(@Param("device_serial_number") String deviceSerialNumber);
    //修改计算公式
    int updateFormula(@Param("deviceSerialNumber") String deviceSerialNumber, @Param("linearSegment") String linearSegment);
    //根据序列号获取计算公式
    String getFormula(@Param("deviceSerialNumber") String deviceSerialNumber);

    Integer queryDeviceActivateStatus(@Param("deviceSerialNumber") String deviceSerialNumber);

    void updateDeviceActivateStatus(@Param("deviceSerialNumber") String deviceSerialNumber, @Param("activateStatus") Integer activateStatus);
}
