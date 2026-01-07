package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.guide.TSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.guide.TSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TSamplePoolConfigPO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TSamplePoolInfoVO;

import java.util.List;

public interface TSamplePoolConfigService extends IService<TSamplePoolConfigPO> {
    Boolean updateCurrentPoolNum(String deviceSerialNumber, Integer currentPoolNum, String updateBy);

    Boolean saveSamplePoolInfo(TSampleAttachTypeSaveDTO dto);


    R<Boolean> updateSamplePoolInfo(TSamplePoolInfoChangeDto changeDto);

    R<TSamplePoolInfoVO> querySamplePoolInfo(String deviceSerialNumber);

    List<Integer> getUsefulLocation(String deviceSerialNumber);

    R<Boolean> setCurrentPool(String deviceSerialNumber, Integer poolNum, String updateBy);

    R<TSamplePoolInfoVO> queryCalibrationSampleInfo(String deviceSerialNumber);
}
