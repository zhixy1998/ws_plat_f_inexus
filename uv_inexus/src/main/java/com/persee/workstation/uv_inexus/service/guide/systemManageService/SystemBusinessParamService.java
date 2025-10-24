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

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAutoLogoutConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageMeasureConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemStandCurveConfVO;

public interface SystemBusinessParamService {

    SystemBusinessParamVO queryBusinessParam();

    R<SystemManageMeasureConfVO> queryMeasureConf();

    R<SystemStandCurveConfVO> queryStandCurveConf();

    R<SystemAutoLogoutConfVO> queryAutoLogoutConf();

    Boolean saveBusinessParam(SystemBusinessParamDTO dto);

    Boolean resetBusinessParam();

}
