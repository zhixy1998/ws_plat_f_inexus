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

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemManageDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageVO;

public interface SystemManageService {
    SystemManageVO getSystemManage();

    Boolean saveSystemManage(SystemManageDTO dto);

    Boolean resetSystemManage(String updateBy);

    Boolean changeLanguage(String language);

    String queryLanguage();

}
