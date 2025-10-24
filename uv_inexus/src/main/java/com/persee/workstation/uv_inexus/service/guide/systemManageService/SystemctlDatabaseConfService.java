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

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemCustomManageDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemDataBaseConfDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageUserConfVO;

public interface SystemctlDatabaseConfService {

    /**
     * 数据库配置(维修人员)
     */
    Boolean saveDatabaseConf(SystemDataBaseConfDTO dto);

    /**
     * 系统配置保存(用户)
     */
    Boolean saveCustomDatabaseConf(SystemCustomManageDTO dto);

    /**
     * 查询系统配置(用户)
     */
    SystemManageUserConfVO querySysManageUserConf(String updateBy);

    /**
     * 查询数据库(维修人员)
     */
    SystemDataBaseVO queryDatabaseConf(String updateBy);

    Boolean resetDatabaseConf(String updateBy);

    Boolean getDefaultClassicSwitch();

}
