package com.persee.workstation.uv_inexus.service.systemManageService;

import com.persee.workstation.uv_inexus.po.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemLogSwitchVO;

public interface SystemLogSwitchService {

    SystemLogSwitchVO querySystemLogSwitch();

    Boolean changeSwitch(SystemLogSwitchPO po);

    Boolean resetLogSwitch(String updateBy);
}
