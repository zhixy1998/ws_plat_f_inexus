/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceUsageLogDAO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceUsageLogPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceUsageLogService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceUsageLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class DeviceUsageLogServiceImpl implements DeviceUsageLogService {

    @Autowired
    DeviceUsageLogDAO dao;

    @Autowired
    DeviceInfoService infoService;


    @Override
    public Boolean saveDeviceUsageLog(DeviceUsageLogPO po) {

        if (!StringUtils.hasText(po.getDeviceSerialNumber())) {
            log.info("缺少仪器序列号");
            return false;
        }

        if (po.getStartTime() == null) {
            log.info("设备使用日志开始时间不能为空");
            return false;
        }
        DeviceInfoPO info = infoService.getInfo(po.getDeviceSerialNumber());
        if (!StringUtils.hasText(po.getUpdateBy()) && !StringUtils.hasText(info.getUpdateBy())) {
            log.info("缺少用户信息");
            return false;
        }
        po.setUpdateBy(po.getUpdateBy() == null ? info.getUpdateBy() : po.getUpdateBy());

        if (po.getCloseTime() == null) {
            po.setCloseTime(LocalDateTime.now());
        }
        int insert = dao.insert(po);

        return insert > 0;
    }

    @Override
    public Boolean deleteDeviceUsageLog(String deviceSerialNumber) {

        LambdaQueryWrapper<DeviceUsageLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceUsageLogPO::getDeviceSerialNumber, deviceSerialNumber);
        int delete = dao.delete(wrapper);
        if (delete > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean clearDeviceUsageLog() {
        int delete = dao.delete(null);
        return delete > 0;
    }

//    @Override
//    public List<DeviceUsageLogVO> deviceUsageLogStatistics(String startTime, String endTime) {
//        LambdaQueryWrapper<DeviceUsageLogPO> wrapper = new LambdaQueryWrapper<>();
//        wrapper.ge(DeviceUsageLogPO::getStartTime, startTime);
//        wrapper.le(DeviceUsageLogPO::getCloseTime, endTime);
//
//
//        return Collections.emptyList();
//    }


    /**
     * 设备使用率统计（按月），返回每个设备每个月的使用率（工作日8小时为分母）
     */
    @Override
    public List<DeviceUsageLogVO> deviceUsageLogStatistics(String startTime, String endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start;
        LocalDateTime end;

        // ---------- 处理空值参数 ----------
        if (startTime == null || startTime.trim().isEmpty()) {
            if (endTime == null || endTime.trim().isEmpty()) {
                // 都为空：最近一年（当前时间往前一年）
                end = LocalDateTime.now();
            } else {
                // start为空，end有值：从end前一年到end
                end = LocalDateTime.parse(endTime + " 23:59:00", formatter);
            }
            start = end.minusYears(1);
        } else {
            start = LocalDateTime.parse(startTime + " 00:00:00", formatter);
            if (endTime == null || endTime.trim().isEmpty()) {
                // start有值，end为空：从start到现在
                end = LocalDateTime.now();
            } else {
                // 两者均有值
                end = LocalDateTime.parse(endTime + " 23:59:00", formatter);
            }
        }

        // 确保 start 不晚于 end
        if (start.isAfter(end)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }
        // ---------- 参数处理结束 ----------

        // 1. 查询符合条件的日志（时间段有交集的记录）
        LambdaQueryWrapper<DeviceUsageLogPO> wrapper = Wrappers.lambdaQuery();
        wrapper.lt(DeviceUsageLogPO::getStartTime, end)
                .gt(DeviceUsageLogPO::getCloseTime, start);
        List<DeviceUsageLogPO> logs = dao.selectList(wrapper);

        // 2. 累计每个设备每个月的使用分钟数 + 所有设备总使用分钟数（按月）
        Map<String, Map<YearMonth, Long>> deviceMonthUsage = new HashMap<>();
        Map<YearMonth, Long> totalMonthUsage = new HashMap<>();

        for (DeviceUsageLogPO log : logs) {
            LocalDateTime logStart = log.getStartTime();
            LocalDateTime logEnd = log.getCloseTime();
            // 计算交集
            LocalDateTime intersectStart = logStart.isAfter(start) ? logStart : start;
            LocalDateTime intersectEnd = logEnd.isBefore(end) ? logEnd : end;
            if (intersectStart.isBefore(intersectEnd)) {
                // 按设备累加
                splitAndAddUsage(deviceMonthUsage, log.getDeviceSerialNumber(), intersectStart, intersectEnd);
                // 按所有设备总时长累加
                splitAndAddToMonthMap(totalMonthUsage, intersectStart, intersectEnd);
            }
        }

        // 3. 计算每个月份在查询区间内的工作日可用分钟数（工作日8小时）
        Map<YearMonth, Long> monthAvailable = computeWorkdayMonthAvailable(start, end);

        // 4. 排序月份
        List<YearMonth> sortedMonths = new ArrayList<>(monthAvailable.keySet());
        Collections.sort(sortedMonths);

        // 5. 构建结果 VO
        List<DeviceUsageLogVO> result = new ArrayList<>();
        for (Map.Entry<String, Map<YearMonth, Long>> entry : deviceMonthUsage.entrySet()) {
            String deviceSn = entry.getKey();
            Map<YearMonth, Long> deviceMonth = entry.getValue();

            DeviceUsageLogVO vo = new DeviceUsageLogVO();
            vo.setDeviceSerialNumber(deviceSn);
            JSONObject obj = new JSONObject(true);
            for (YearMonth month : sortedMonths) {

                long usageMinutes = deviceMonth.getOrDefault(month, 0L);
                long totalMinutes = monthAvailable.getOrDefault(month, 0L);
                double rate = totalMinutes > 0 ? (usageMinutes * 100.0 / totalMinutes) : 0.0;
                BigDecimal bd = BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP);
                obj.put(month.toString(), bd.doubleValue());

            }
            vo.setUsageStatistics(obj);
            result.add(vo);
        }

        return result;
    }

    /**
     * 用户使用率统计（整个时间段），返回每个用户的使用率（工作日8小时为分母）
     */
    @Override
    public JSONObject userUsageLogStatistics(String startTime, String endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start;
        LocalDateTime end;

        // ---------- 处理空值参数 ----------
        if (startTime == null || startTime.trim().isEmpty()) {
            if (endTime == null || endTime.trim().isEmpty()) {
                end = LocalDateTime.now();
                start = end.minusYears(1);
            } else {
                end = LocalDateTime.parse(endTime + " 23:59:00", formatter);
                start = end.minusYears(1);
            }
        } else {
            start = LocalDateTime.parse(startTime + " 00:00:00", formatter);
            if (endTime == null || endTime.trim().isEmpty()) {
                end = LocalDateTime.now();
            } else {
                end = LocalDateTime.parse(endTime + " 23:59:00", formatter);
            }
        }

        if (start.isAfter(end)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }
        // ---------- 参数处理结束 ----------

        // 1. 查询符合条件的日志
        LambdaQueryWrapper<DeviceUsageLogPO> wrapper = Wrappers.lambdaQuery();
        wrapper.lt(DeviceUsageLogPO::getStartTime, end)
                .gt(DeviceUsageLogPO::getCloseTime, start);
        List<DeviceUsageLogPO> logs = dao.selectList(wrapper);

        // 2. 按用户累加使用分钟数
        Map<String, Long> userUsageMinutes = new HashMap<>();
        Map<YearMonth, Long> totalMonthUsage = new HashMap<>();

        for (DeviceUsageLogPO log : logs) {
            LocalDateTime logStart = log.getStartTime();
            LocalDateTime logEnd = log.getCloseTime();
            LocalDateTime intersectStart = logStart.isAfter(start) ? logStart : start;
            LocalDateTime intersectEnd = logEnd.isBefore(end) ? logEnd : end;
            if (intersectStart.isBefore(intersectEnd)) {
                long minutes = ChronoUnit.MINUTES.between(intersectStart, intersectEnd);
                userUsageMinutes.merge(log.getUpdateBy(), minutes, Long::sum);
                splitAndAddToMonthMap(totalMonthUsage, intersectStart, intersectEnd);
            }
        }

        // 3. 计算总可用工作日分钟数（工作日8小时总和）
        Map<YearMonth, Long> monthWorkMinutes = computeWorkdayMonthAvailable(start, end);
        long totalAvailableWorkMinutes = monthWorkMinutes.values().stream()
                .mapToLong(Long::longValue).sum();

        // 4. 构建结果 JSON
        JSONObject result = new JSONObject();
        for (Map.Entry<String, Long> entry : userUsageMinutes.entrySet()) {

            double rate = totalAvailableWorkMinutes > 0 ?
                    (entry.getValue() * 100.0 / totalAvailableWorkMinutes) : 0.0;
            BigDecimal bd = BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP);
            result.put(entry.getKey(), bd.doubleValue());
        }

        return result;
    }

    // ================== 辅助方法 ==================

    /**
     * 将时间段按月份拆分，并累加到指定设备月份的分钟数映射中
     */
    private void splitAndAddUsage(Map<String, Map<YearMonth, Long>> deviceMonthUsage,
                                  String deviceSn, LocalDateTime start, LocalDateTime end) {
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            YearMonth month = YearMonth.from(current);
            LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();
            if (monthEnd.isAfter(end)) {
                monthEnd = end;
            }
            long minutes = ChronoUnit.MINUTES.between(current, monthEnd);
            if (minutes > 0) {
                deviceMonthUsage.computeIfAbsent(deviceSn, k -> new HashMap<>())
                        .merge(month, minutes, Long::sum);
            }
            current = monthEnd;
        }
    }

    /**
     * 将时间段按月份拆分，并累加到总时长映射中（不区分设备）
     */
    private void splitAndAddToMonthMap(Map<YearMonth, Long> monthMap,
                                       LocalDateTime start, LocalDateTime end) {
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            YearMonth month = YearMonth.from(current);
            LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();
            if (monthEnd.isAfter(end)) {
                monthEnd = end;
            }
            long minutes = ChronoUnit.MINUTES.between(current, monthEnd);
            if (minutes > 0) {
                monthMap.merge(month, minutes, Long::sum);
            }
            current = monthEnd;
        }
    }

    /**
     * 计算查询区间内每个月的工作日可用分钟数（工作日周一至周五，每天8小时）
     */
    private Map<YearMonth, Long> computeWorkdayMonthAvailable(LocalDateTime start, LocalDateTime end) {
        Map<YearMonth, Long> monthWorkMinutes = new HashMap<>();
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dow = currentDate.getDayOfWeek();
            // 周一至周五为工作日
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                YearMonth month = YearMonth.from(currentDate);
                // 480分钟
                long dailyMinutes = 8 * 60;
                monthWorkMinutes.merge(month, dailyMinutes, Long::sum);
            }
            currentDate = currentDate.plusDays(1);
        }
        return monthWorkMinutes;
    }
}
