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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemManageDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagerConf;
import com.persee.workstation.uv_inexus.vo.systemManageVo.ConfigVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.LongDistanceConfigVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageVO;

public interface SystemManageService {
    SystemManageVO getSystemManage();

    Boolean saveSystemManage(SystemManageDTO dto);

    Boolean resetSystemManage(String updateBy);

    Result<String> changeLanguage(String language);

    Result<Boolean> setConfiguration(ConfigVO configVO) throws JsonProcessingException;

    Result<Boolean> longDistanceConfig(LongDistanceConfigVO longDistanceConfigVO);

    Result<SystemManagerConf> changeSystemManagerConf(SystemManagerConf systemManagerConf) throws JsonProcessingException;

    Result<SystemManagerConf> getSystemManagerConf() throws JsonProcessingException;

}
