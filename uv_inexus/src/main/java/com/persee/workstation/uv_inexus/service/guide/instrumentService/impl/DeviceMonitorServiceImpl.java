/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceHistoryTaskDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceQueryDTO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceUsageCountDTO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceMonitorEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceTaskStatusEnum;
import com.persee.workstation.uv_inexus.exception.BusinessException;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureProject;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceMonitorService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureProjectService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemLogSwitchService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemLogSwitchVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceMonitorServiceImpl implements DeviceMonitorService {

    @Autowired
    private TMeasureTaskMapper tMeasureTaskMapper;

    @Autowired
    private TMeasureTaskService tMeasureTaskService;


    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private TMeasureProjectService tMeasureProjectService;

    @Autowired
    SystemLogSwitchService logSwitchService;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;

    @Override
    public List<DeviceMonitorVO> queryStatus(DeviceQueryDTO queryDTO) {


        DeviceMonitorVO vo = new DeviceMonitorVO();
        vo.setType(DeviceMonitorEnum.getName("1"));
        DataNameValue value = new DataNameValue();
        value.setDataValue("09");
        value.setDataKey("s");

        List<DataNameValue> neeList = new ArrayList<>();
        neeList.add(value);

        vo.setData(neeList);
        ArrayList<DeviceMonitorVO> deviceMonitorVOS = new ArrayList<>();
        deviceMonitorVOS.add(vo);
        return deviceMonitorVOS;
    }

    @Override
    public DeviceComponentMonitorVO componentQuery(DeviceQueryDTO queryDTO) {
        return null;
    }

    @Override
    public List<DeviceUsageStatisticsVO> countUsageInfo(DeviceUsageCountDTO countDTO) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllHistoryUsage(LocalDateTime startTime, LocalDateTime endTime, String search) {
        return Collections.emptyList();
    }

    @Override
    public Result<Map<String, Object>> getTypeStatistics(String updateBy, String startTime, String endTime, String deviceNumber) {

        Map<String, Object> statisticsMap = new HashMap<>();
        statisticsMap.put("taskTypeCounts", new HashMap<String, Long>());
        if (StringUtils.isBlank(deviceNumber)) {
            log.warn("获取类型统计失败: 仪器编号为空");
            return Result.ok(statisticsMap);
        }

        if (StringUtils.isBlank(updateBy)) {
            log.warn("获取类型统计失败: 用户名为空");
            return Result.fail(YamlMessageSource.getMessageStatic("00205002"));
        }

        try {
            LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserPO::getUsername, updateBy);
            SysUserPO user = sysUserService.getOne(wrapper);

            if (user == null) {
                log.warn("获取类型统计失败: 用户 '{}' 不存在", updateBy);
                return Result.fail(YamlMessageSource.getMessageStatic("00205003"));
            }

            SystemLogSwitchVO switchVO = logSwitchService.querySystemLogSwitch();


            if (switchVO != null && Boolean.TRUE.equals(switchVO.getStatTypeEnabled())) {
                LambdaQueryWrapper<TMeasureTask> taskWrapper = new LambdaQueryWrapper<TMeasureTask>()
                        .eq(TMeasureTask::getDeviceSerialNumber, deviceNumber);
//                        .eq(TMeasureTask::getUserId, user.getId())
//                        .eq(TMeasureTask::getIsDelete, DeviceTaskStatusEnum.NOT_DELETED.getCode()); // 过滤已删除任务
                // 处理开始时间
                LocalDateTime start = parseDateTime(startTime, false);
                if (start != null) {
                    taskWrapper.ge(TMeasureTask::getCreateTime, start);
                }

                // 处理结束时间
                LocalDateTime end = parseDateTime(endTime, true);
                if (end != null) {
                    taskWrapper.le(TMeasureTask::getCreateTime, end);
                }
                if (start != null && end != null && start.isAfter(end)) {
                    return Result.fail(YamlMessageSource.getMessageStatic("00205008"));
                }
                List<TMeasureTask> tasks = tMeasureTaskService.list(taskWrapper);

                if (tasks != null && !tasks.isEmpty()) {
                    Map<String, Long> counts = tasks.stream()
                            .filter(t -> t.getMeasureType() != null)
                            .collect(Collectors.groupingBy(TMeasureTask::getMeasureType, Collectors.counting()));
                    statisticsMap.put("taskTypeCounts", counts);
                }
            } else {
                log.info("检测类型分项统计功能未开启");
                return Result.fail(YamlMessageSource.getMessageStatic("00205004"));
            }
            return Result.ok(statisticsMap);


        } catch (Exception e) {
            log.error("获取检测类型分项统计异常: {}", e.getMessage(), e);
            return Result.fail(YamlMessageSource.getMessageStatic("00205006"));
        }
    }

    @Override
    public Result<Map<String, Object>> getProjectStatistics(String updateBy, String startTime, String endTime, String deviceNumber) {

        Map<String, Object> statisticsMap = new HashMap<>();
        statisticsMap.put("projectCounts", new HashMap<String, Long>());

        if (StringUtils.isBlank(deviceNumber)) {
            log.warn("仪器编号为空");
            return Result.ok(statisticsMap);
        }
        if (StringUtils.isBlank(updateBy)) {
            log.warn("获取项目统计失败: 用户名为空");
            return Result.fail(YamlMessageSource.getMessageStatic("00205002"));
        }

        try {
            LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserPO::getUsername, updateBy);
            SysUserPO user = sysUserService.getOne(wrapper);

            if (user == null) {
                log.warn("获取项目统计失败: 用户 '{}' 不存在", updateBy);
                return Result.fail(YamlMessageSource.getMessageStatic("00000003"));
            }

            SystemLogSwitchVO switchVO = logSwitchService.querySystemLogSwitch();

            if (switchVO != null && Boolean.TRUE.equals(switchVO.getStatItemEnabled())) {
                LambdaQueryWrapper<TMeasureTask> taskWrapper = new LambdaQueryWrapper<TMeasureTask>()
                        .eq(TMeasureTask::getDeviceSerialNumber, deviceNumber);
//                        .eq(TMeasureTask::getUserId, user.getId())
//                        .eq(TMeasureTask::getIsDelete, DeviceTaskStatusEnum.NOT_DELETED.getCode());
                // 处理开始时间
                LocalDateTime start = parseDateTime(startTime, false);
                if (start != null) {
                    taskWrapper.ge(TMeasureTask::getCreateTime, start);
                }

                // 处理结束时间
                LocalDateTime end = parseDateTime(endTime, true);
                if (end != null) {
                    taskWrapper.le(TMeasureTask::getCreateTime, end);
                }
                if (start != null && end != null && start.isAfter(end)) {
                    return Result.fail(YamlMessageSource.getMessageStatic("00205008"));
                }

                List<TMeasureTask> tasks = tMeasureTaskService.list(taskWrapper);

                if (tasks != null && !tasks.isEmpty()) {
                    // 收集所有项目ID并批量查询项目名称
                    List<String> projectIds = tasks.stream()
                            .map(TMeasureTask::getProjectItemKey)
                            .filter(StringUtils::isNotBlank)
                            .distinct()
                            .collect(Collectors.toList());

                    Map<String, String> projectMap = new HashMap<>();
                    if (!projectIds.isEmpty()) {
                        List<TMeasureProject> projects = tMeasureProjectService.listByIds(projectIds);
                        projectMap = projects.stream().collect(Collectors.toMap(TMeasureProject::getId, TMeasureProject::getProjectName));
                    }

                    // 按项目名称进行分组计数
                    final Map<String, String> finalProjectMap = projectMap;
                    Map<String, Long> projectCounts = tasks.stream()
                            .map(task -> finalProjectMap.get(task.getProjectItemKey()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

                    statisticsMap.put("projectCounts", projectCounts);

                    long validTotal = projectCounts.values().stream().mapToLong(Long::longValue).sum();
                    statisticsMap.put("totalCount", validTotal);
                } else {
                    return Result.ok(statisticsMap);
                }

                return Result.ok(statisticsMap);
            }

            // 如果po为null或统计功能未开启
            log.info("检测项目分项统计功能未开启");
            return Result.fail(YamlMessageSource.getMessageStatic("00205005"));

        } catch (Exception e) {
            log.error("获取项目统计异常: {}", e.getMessage(), e);
            return Result.fail(YamlMessageSource.getMessageStatic("00205007"));
        }
    }

    /**
     * 兼容性解析时间字符串
     *
     * @param timeStr   原始时间字符串
     * @param isEndEdge 是否为结束边界（如果是，则补全为 23:59:59）
     */
    public LocalDateTime parseDateTime(String timeStr, boolean isEndEdge) {
        if (StringUtils.isBlank(timeStr)) return null;

        //处理ISO 8601 格式
        if (timeStr.contains("T") && timeStr.endsWith("Z")) {
            try {
                Instant instant = Instant.parse(timeStr);
                return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            } catch (Exception e) {
                timeStr = timeStr.split("T")[0];
            }
        }
        //处理普通日期格式 (yyyy-MM-dd)
        try {
            LocalDate date = LocalDate.parse(timeStr.substring(0, 10));
            return isEndEdge ? date.atTime(LocalTime.MAX) : date.atStartOfDay();

        } catch (Exception e) {
            log.warn("时间格式转换异常: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Page<HistoryTaskVO> getHistoryTask(DeviceHistoryTaskDTO queryDto) throws BusinessException {

        if (StringUtils.isBlank(queryDto.getDeviceSerialNumber())) {
            log.warn("查询历史任务失败：仪器编号为空");
            Page<HistoryTaskVO> emptyPage = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            emptyPage.setTotal(0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;

        }
        // 1. 初始化分页
        Page<HistoryTaskVO> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());

        //2026-03-10 00:00:00
        if (queryDto.getStartTime() != null) {
            queryDto.setStartDateTime(queryDto.getStartTime().atStartOfDay());
        }
        //2026-03-10 23:59:59
        if (queryDto.getEndTime() != null) {
            queryDto.setEndDateTime(queryDto.getEndTime().atTime(LocalTime.MAX));
        }
        if (queryDto.getStartDateTime() != null && queryDto.getEndDateTime() != null && queryDto.getStartDateTime().isAfter(queryDto.getEndDateTime())) {
            log.warn("查询历史任务失败：开始时间不能大于结束时间");
            throw new BusinessException(YamlMessageSource.getMessageStatic("00205008"));
        }

        IPage<HistoryTaskVO> resultPage = tMeasureTaskMapper.selectHistoryTaskPage(page, queryDto);

        DateTimeFormatter zhFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
        resultPage.convert(item -> {
            // 开始时间展示
            item.setStartTimeDisplay(item.getStartTime() != null ? item.getStartTime().format(zhFormatter) : "-");

            // 逻辑判断 taskStatus 是否结束
            if (DeviceTaskStatusEnum.COMPLETED.getCode().equals(item.getTaskStatus()) && item.getEndTime() != null) {
                item.setEndTimeDisplay(item.getEndTime().format(zhFormatter));
            } else {
                item.setEndTimeDisplay("-");
            }
            return item;
        });

        return (Page<HistoryTaskVO>) resultPage;
    }


}
