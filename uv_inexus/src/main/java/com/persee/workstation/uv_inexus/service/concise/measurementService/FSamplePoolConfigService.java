package com.persee.workstation.uv_inexus.service.concise.measurementService;


import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;

import java.util.List;

public interface FSamplePoolConfigService {

    Boolean updateCurrentPoolNum(String deviceSerialNumber, Integer currentPoolNum, String updateBy);

    Boolean saveSamplePoolInfo(FSampleAttachTypeSaveDTO dto);


    R<Boolean> updateSamplePoolInfo(FSamplePoolInfoChangeDto samplePoolInfoChangeDto);

    R<FSamplePoolInfoVO> querySamplePoolInfo(String deviceSerialNumber);

    List<Integer> getUsefulLocation(String deviceSerialNumber);

    R<Boolean> setCurrentPool(String deviceSerialNumber, Integer poolNum, String updateBy);
}
