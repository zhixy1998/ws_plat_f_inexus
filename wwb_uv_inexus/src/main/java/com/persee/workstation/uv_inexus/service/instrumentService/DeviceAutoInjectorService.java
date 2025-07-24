package com.persee.workstation.uv_inexus.service.instrumentService;

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.instrumentPo.DeviceAutoInjectorPO;
import com.persee.workstation.uv_inexus.po.instrumentPo.InjectionTrayParamPO;

import java.util.List;

public interface DeviceAutoInjectorService {

    R<Boolean> setInjectorConf(DeviceAutoInjectorPO po);

    R<Boolean> setInjectorTray(List<InjectionTrayParamPO> poList);

    R<DeviceAutoInjectorPO> queryInjectorConf(String deviceSerialNumber);

    List<InjectionTrayParamPO> selectInjectorTray(String deviceSerialNumber);

    R<Integer> checkConnectStatus(String deviceSerialNumber);

}
