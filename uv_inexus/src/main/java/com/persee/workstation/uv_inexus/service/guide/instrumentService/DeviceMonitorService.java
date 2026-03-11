/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * 检测类型分项统计
     */
    Result<Map<String, Object>> getTypeStatistics(String updateBy, String startTime, String endTime);

    /**
     * 检测项目分项统计
     */
    Result<Map<String, Object>> getProjectStatistics(String updateBy, String startTime, String endTime);


    /**
     * 查询历史任务
     */
    Page<HistoryTaskVO> getHistoryTask(DeviceHistoryTaskDTO queryDto);




}
