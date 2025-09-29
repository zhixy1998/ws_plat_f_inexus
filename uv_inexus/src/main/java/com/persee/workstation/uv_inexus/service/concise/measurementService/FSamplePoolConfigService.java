package com.persee.workstation.uv_inexus.service.concise.measurementService;


import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FSamplePoolConfigPO;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;

import java.util.List;

public interface FSamplePoolConfigService  extends IService<FSamplePoolConfigPO> {

    Boolean updateCurrentPoolNum(String deviceSerialNumber, Integer currentPoolNum, String updateBy);

    Boolean saveSamplePoolInfo(FSampleAttachTypeSaveDTO dto);


    R<Boolean> updateSamplePoolInfo(FSamplePoolInfoChangeDto samplePoolInfoChangeDto);

    R<FSamplePoolInfoVO> querySamplePoolInfo(String deviceSerialNumber);

    List<Integer> getUsefulLocation(String deviceSerialNumber);

    R<Boolean> setCurrentPool(String deviceSerialNumber, Integer poolNum, String updateBy);
}
