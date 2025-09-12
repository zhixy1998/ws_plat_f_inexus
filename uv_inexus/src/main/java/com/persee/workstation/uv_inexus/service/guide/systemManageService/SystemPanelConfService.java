package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelOrderDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemPanelConfVO;

import java.util.List;

public interface SystemPanelConfService {
    Boolean savePanelConf(SystemPanelConfDTO dto);

    SystemPanelConfVO queryPanelConf();

    Boolean resetPanelConf(String updateBy);

    List<String> queryPanelOrder();

    R<Boolean> savePanelOrder(SystemPanelOrderDTO dto);
}
