package com.persee.workstation.uv_inexus.dao.instrumentDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.instrumentPo.InsConfigPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DataNameValue;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceCalibrationParams;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceCalibrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InsConfigDAO extends BaseMapper<InsConfigPO> {

    List<DeviceCalibrationParams> getCalibrationParams(@Param("calibration_type") Integer type);

    List<DeviceCalibrationVO> getCalibrationInfo(@Param("device_type") String deviceType);

    List<DataNameValue> getCalibrationValue(@Param("param_id") String paramId);
}
