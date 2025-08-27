package com.persee.workstation.uv_inexus.service.guide.instrumentService;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;

import java.time.LocalDateTime;
import java.util.List;

public interface DeviceMonitorService {

    /**
     * 查询监控信息，各种参数
     * @param queryDTO
     * @return
     */
    List<DeviceMonitorVO> queryStatus(DeviceQueryDTO queryDTO);

    /**
     * 查询器件状态信息
     * @param queryDTO
     * @return
     */
    DeviceComponentMonitorVO componentQuery(DeviceQueryDTO queryDTO);

    /**
     * 查询仪器统计信息
     * @param countDTO
     * @return
     */
    List<DeviceUsageStatisticsVO> countUsageInfo(DeviceUsageCountDTO countDTO);

    /**
     * 查询历史使用情况接口
     * @param startTime
     * 返回值待定，应该是一个po类，传值类型也可以做更改，后面根据需求调整。
     * @return
     */
    List<String> getAllHistoryUsage(LocalDateTime startTime, LocalDateTime endTime, String search);

    /**
     * 查询历史任务详细信息
     * @param id 任务id，任务id应该唯一
     */
//    void getHistoryDetailed(String id);




}
