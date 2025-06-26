package com.persee.workstation.uv_inexus.service.systemManageService;

import com.persee.workstation.uv_inexus.po.systemManagePo.SystemDatabaseConfPO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;

public interface SystemctlDatabaseConfService {

    Boolean saveDatabaseConf(SystemDatabaseConfPO po);

    SystemDataBaseVO queryDatabaseConf();
}
