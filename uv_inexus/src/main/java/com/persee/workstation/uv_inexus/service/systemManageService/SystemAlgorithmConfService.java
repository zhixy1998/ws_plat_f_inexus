package com.persee.workstation.uv_inexus.service.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemAlgorithmConfDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;

import java.util.List;

public interface SystemAlgorithmConfService {

    List<SystemAlgorithmConfVO> queryAlgorithmConf();

    Boolean saveAlgorithmConf(List<SystemAlgorithmConfDTO> list);

    Boolean resetAlgorithmConf();
}
