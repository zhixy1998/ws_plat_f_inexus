package com.persee.workstation.uv_inexus.service.concise.measurementService;


import com.persee.workstation.uv_inexus.dto.concise.common.FSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;

import java.util.List;

public interface FSamplePoolConfigService {

    Boolean updateCurrentPoolNum(String deviceSerialNumber, Integer currentPoolNum, String updateBy);

    Boolean saveSamplePoolInfo(String deviceSerialNumber, List<FSamplePoolStatus> poolInfo);


    Boolean updateSamplePoolInfo(FSamplePoolInfoChangeDto samplePoolInfoChangeDto);



}
