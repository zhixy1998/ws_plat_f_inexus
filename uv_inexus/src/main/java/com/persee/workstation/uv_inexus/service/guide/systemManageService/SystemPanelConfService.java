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
