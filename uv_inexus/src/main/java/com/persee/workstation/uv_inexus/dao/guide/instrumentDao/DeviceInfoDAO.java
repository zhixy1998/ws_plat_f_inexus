package com.persee.workstation.uv_inexus.dao.guide.instrumentDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import org.apache.ibatis.annotations.Param;

public interface DeviceInfoDAO extends BaseMapper<DeviceInfoPO> {

    String getIp(@Param("device_serial_number") String deviceSerialNumber);

    String getAttachmentType(@Param("device_serial_number") String deviceSerialNumber);
    DeviceAttachmentTypeVO getAttachmentInfo(@Param("device_serial_number") String deviceSerialNumber);
}
