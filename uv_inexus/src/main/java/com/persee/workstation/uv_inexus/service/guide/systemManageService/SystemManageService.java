package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemManageDTO;
import com.persee.workstation.uv_inexus.model.Result;
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
}
