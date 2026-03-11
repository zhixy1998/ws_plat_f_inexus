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
import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemLogSwitchDAO;
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
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemLogSwitchPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceMonitorService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureProjectService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    SystemLogSwitchDAO dao;

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
        if (StringUtils.isBlank(deviceNumber)) {
            log.warn("获取类型统计失败: 仪器编号为空");
            return Result.fail(YamlMessageSource.getMessageStatic("00205001"));
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

            SystemLogSwitchPO po = dao.selectById(userId);

            Map<String, Object> statisticsMap = new HashMap<>();

            if (po != null && Boolean.TRUE.equals(po.getStatTypeEnabled())) {

                LambdaQueryWrapper<TMeasureTask> taskWrapper = new LambdaQueryWrapper<TMeasureTask>()
                        .eq(TMeasureTask::getDeviceSerialNumber, deviceNumber);
//                        .eq(TMeasureTask::getUserId, user.getId())
//                        .eq(TMeasureTask::getIsDelete, DeviceTaskStatusEnum.NOT_DELETED.getCode()); // 过滤已删除任务
                if (StringUtils.isNotBlank(startTime)) {
                    taskWrapper.ge(TMeasureTask::getCreateTime, startTime + " 00:00:00");
                }
                if (StringUtils.isNotBlank(endTime)) {
                    taskWrapper.le(TMeasureTask::getCreateTime, endTime + " 23:59:59");
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

        if (StringUtils.isBlank(deviceNumber)) {
            log.warn("获取项目统计失败: 仪器编号为空");
            return Result.fail(YamlMessageSource.getMessageStatic("00205001"));
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

            SystemLogSwitchPO po = dao.selectById(userId);
            Map<String, Object> statisticsMap = new HashMap<>();

            if (po != null && Boolean.TRUE.equals(po.getStatItemEnabled())) {
                LambdaQueryWrapper<TMeasureTask> taskWrapper = new LambdaQueryWrapper<TMeasureTask>()
                        .eq(TMeasureTask::getDeviceSerialNumber, deviceNumber);
//                        .eq(TMeasureTask::getUserId, user.getId())
//                        .eq(TMeasureTask::getIsDelete, DeviceTaskStatusEnum.NOT_DELETED.getCode());
                if (StringUtils.isNotBlank(startTime)) {
                    taskWrapper.ge(TMeasureTask::getCreateTime, startTime + " 00:00:00");
                }
                if (StringUtils.isNotBlank(endTime)) {
                    taskWrapper.le(TMeasureTask::getCreateTime, endTime + " 23:59:59");
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
                    return Result.fail(YamlMessageSource.getMessageStatic("00205007"));
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

    @Override
    public Page<HistoryTaskVO> getHistoryTask(DeviceHistoryTaskDTO queryDto) throws BusinessException {

        if (StringUtils.isBlank(queryDto.getDeviceSerialNumber())) {
            log.warn("查询历史任务失败：仪器编号为空");
            throw new BusinessException(YamlMessageSource.getMessageStatic("00205001"));
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
