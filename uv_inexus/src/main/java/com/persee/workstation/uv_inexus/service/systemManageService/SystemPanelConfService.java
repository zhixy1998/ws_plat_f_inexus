package com.persee.workstation.uv_inexus.service.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelConfDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemPanelConfVO;

public interface SystemPanelConfService {
    Boolean savePanelConf(SystemPanelConfDTO dto);

    SystemPanelConfVO queryPanelConf();

    Boolean resetPanelConf(String updateBy);
}
