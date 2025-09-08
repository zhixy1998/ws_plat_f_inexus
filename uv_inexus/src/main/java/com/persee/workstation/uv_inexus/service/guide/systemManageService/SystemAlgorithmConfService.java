package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemAlgorithmConfDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;


public interface SystemAlgorithmConfService {

    SystemAlgorithmConfVO queryAlgorithmConf();

    Boolean saveAlgorithmConf(SystemAlgorithmConfDTO dto);

    Boolean resetAlgorithmConf();
}
