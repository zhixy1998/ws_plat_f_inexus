package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemManageDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageVO;

public interface SystemManageService {
    SystemManageVO getSystemManage();

    Boolean saveSystemManage(SystemManageDTO dto);

    Boolean resetSystemManage(String updateBy);
}
