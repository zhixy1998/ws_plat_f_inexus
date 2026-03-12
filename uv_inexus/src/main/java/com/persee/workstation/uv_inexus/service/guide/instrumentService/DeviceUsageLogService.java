/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceUsageLogPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceUsageLogVO;

import java.util.List;

public interface DeviceUsageLogService {

    /**
     * 保存仪器使用开关机日志
     */
    Boolean saveDeviceUsageLog(DeviceUsageLogPO po);

    /**
     * 删除仪器使用开关机日志
     */
    Boolean deleteDeviceUsageLog(String deviceSerialNumber);

    /**
     * 清空仪器使用开关机日志
     */
    Boolean clearDeviceUsageLog();

    /**
     * 统计仪器使用率
     */
    List<DeviceUsageLogVO> deviceUsageLogStatistics(String startTime, String endTime);

    /**
     * 统计用户使用率
     */
    JSONObject userUsageLogStatistics(String startTime, String endTime);
}
