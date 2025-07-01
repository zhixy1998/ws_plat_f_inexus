package com.persee.workstation.uv_inexus.service.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;

public interface SystemBusinessParamService {

    SystemBusinessParamVO queryBusinessParam();

    Boolean saveBusinessParam(SystemBusinessParamDTO dto);

    Boolean resetBusinessParam();

}
