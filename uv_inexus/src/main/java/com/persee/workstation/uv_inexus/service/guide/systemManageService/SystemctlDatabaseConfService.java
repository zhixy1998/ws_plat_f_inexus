package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.InitSystemConf;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemDataBaseConfDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;

public interface SystemctlDatabaseConfService {

    Boolean saveDatabaseConf(SystemDataBaseConfDTO dto);

    SystemDataBaseVO queryDatabaseConf();

    Boolean resetDatabaseConf(String updateBy);

    Boolean changeFastLoginSwitch(Boolean switchStatus, String updateBy);

    Result<InitSystemConf> updateSystemDatabaseConf(InitSystemConf initSystemConf);

    Result<InitSystemConf> initSystemConf();

}
