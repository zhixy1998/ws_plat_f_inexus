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

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import org.apache.ibatis.annotations.Param;

public interface DeviceInfoDAO extends BaseMapper<DeviceInfoPO> {

    String getIp(@Param("device_serial_number") String deviceSerialNumber);

    String getAttachmentType(@Param("device_serial_number") String deviceSerialNumber);
    DeviceAttachmentTypeVO getAttachmentInfo(@Param("device_serial_number") String deviceSerialNumber);
}
