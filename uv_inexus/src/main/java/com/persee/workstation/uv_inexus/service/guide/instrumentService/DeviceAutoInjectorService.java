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
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceAutoInjectorPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InjectionTrayParamPO;

import java.util.List;

public interface DeviceAutoInjectorService {

    R<Boolean> setInjectorConf(DeviceAutoInjectorPO po);

    R<Boolean> setInjectorTray(List<InjectionTrayParamPO> poList);

    R<DeviceAutoInjectorPO> queryInjectorConf(String deviceSerialNumber);

    List<InjectionTrayParamPO> selectInjectorTray(String deviceSerialNumber);

    R<Integer> checkConnectStatus(String deviceSerialNumber);

}
