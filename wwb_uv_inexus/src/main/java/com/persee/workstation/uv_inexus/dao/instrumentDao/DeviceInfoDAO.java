package com.persee.workstation.uv_inexus.dao.instrumentDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.instrumentPo.DeviceInfoPO;
import org.apache.ibatis.annotations.Param;

public interface DeviceInfoDAO extends BaseMapper<DeviceInfoPO> {

    String getIp(@Param("device_serial_number") String deviceSerialNumber);
}
