package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemLogSwitchVO;

public interface SystemLogSwitchService {

    SystemLogSwitchVO querySystemLogSwitch();

    Boolean changeSwitch(SystemLogSwitchPO po);

    Boolean resetLogSwitch(String updateBy);
}
