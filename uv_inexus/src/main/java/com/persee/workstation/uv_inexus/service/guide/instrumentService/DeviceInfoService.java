package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;

public interface DeviceInfoService {

    DeviceInfoPO getInfo(String deviceSerialNumber);

    Boolean saveOrUpdateInfo(DeviceInfoPO po);

    String getIp(String deviceSerialNumber);

}
