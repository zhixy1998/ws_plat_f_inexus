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

package com.persee.workstation.uv_inexus.dao.guide.instrumentDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InsConfigPO;
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
