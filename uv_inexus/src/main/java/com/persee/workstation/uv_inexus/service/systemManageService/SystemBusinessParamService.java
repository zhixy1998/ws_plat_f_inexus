package com.persee.workstation.uv_inexus.service.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;

import java.util.List;

public interface SystemBusinessParamService {

    List<SystemBusinessParamVO> queryBusinessParam();

    Boolean saveBusinessParam(List<SystemBusinessParamDTO> list);

    Boolean resetBusinessParam();

}
