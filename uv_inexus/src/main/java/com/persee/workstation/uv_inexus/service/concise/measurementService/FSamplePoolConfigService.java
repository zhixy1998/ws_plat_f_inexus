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
