package com.persee.workstation.uv_inexus.listener;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.enums.factoryInspection.ReminderTypeEnum;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.service.factory_inspection.ReminderSwitchConfigService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageInfoService;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.persee.workstation.uv_inexus.enums.messageEnums.MessageTypeEnum.TYPE_REMIND_MESSAGE;

@Slf4j
@Component
public class TaskReminderScheduler {

    @Autowired
    private TaskManagementService taskManagementService;
    @Autowired
    private MessageInfoService messageInfoService;
    @Autowired
    private ReminderSwitchConfigService reminderSwitchConfigService;
    @Autowired
    private CacheManager cacheManager;
    /**
     * 应用启动时检查错过的提醒
     */
    public void checkMissedRemindersOnStartup() {
        log.info(">>> 开始检查启动时错过的提醒...");
        // 检查提醒功能总开关
        if (!reminderSwitchConfigService.isReminderEnabled()) {
            log.info("提醒功能已关闭，跳过检查");
            return;
        }
        if (!reminderSwitchConfigService.isReminderTypeEnabled(ReminderTypeEnum.PERIODIC_INSPECTION.getCode())) {
            log.debug("期间核查提醒已关闭！！！");
            return;
        }
        try {
            LocalDate today = LocalDate.now();

            LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskManagement::getReminderEnabled, 1)
                    .isNotNull(TaskManagement::getFirstReminderDate)
                    .isNotNull(TaskManagement::getReminderIntervalDays);

            List<TaskManagement> tasks = taskManagementService.list(wrapper);

            if (tasks == null || tasks.isEmpty()) {
                log.info("没有启用提醒的任务");
                return;
            }

            log.info("共找到 {} 个启用提醒的任务，开始检查...", tasks.size());

            // 从缓存中获取在线用户列表
            Set<String> onlineUsers = getOnlineUsersFromCache();
            if (onlineUsers.isEmpty()) {
                log.info("当前没有在线用户，跳过发送提醒");
                return;
            }
            log.info("当前在线用户: {}", onlineUsers);

            // 收集所有需要提醒的任务
            List<TaskReminder> allReminders = new ArrayList<>();

            for (TaskManagement task : tasks) {
                ReminderResult result = checkTaskStatus(task, today);

                if (result.shouldNotify) {
                    TaskReminder reminder = new TaskReminder();
                    reminder.task = task;
                    reminder.result = result;
                    allReminders.add(reminder);
                }
            }

            if (allReminders.isEmpty()) {
                log.info("✓ 没有需要提醒的任务");
                return;
            }

            // 给所有在线用户发送相同的提醒
            int totalReminders = 0;
            for (String user : onlineUsers) {
                sendBatchNotification(user, allReminders, today);
                totalReminders++;

                // 更新所有任务的最后提醒日期
                for (TaskReminder reminder : allReminders) {
                    updateLastReminderDate(reminder.task);
                }
            }

            log.info("✓ 启动检查完成，共发送 {} 条提醒给 {} 个在线用户，涉及 {} 个任务",
                    totalReminders, onlineUsers.size(), allReminders.size());

        } catch (Exception e) {
            log.error("启动时检查错过提醒异常", e);
        }
    }

    /**
     * 每过三个小时执行定时检查任务提醒
     */
    @Scheduled(cron = "0 0 */3 * * ?")
    public void checkTaskReminders() {
        log.debug("开始执行定时任务提醒检查...");
        if (!reminderSwitchConfigService.isReminderTypeEnabled(ReminderTypeEnum.PERIODIC_INSPECTION.getCode())) {
            log.debug("期间核查提醒已关闭！！！");
        }
        try {
            LocalDate today = LocalDate.now();

            LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskManagement::getReminderEnabled, 1)
                    .isNotNull(TaskManagement::getFirstReminderDate)
                    .isNotNull(TaskManagement::getReminderIntervalDays);

            List<TaskManagement> tasks = taskManagementService.list(wrapper);

            if (tasks == null || tasks.isEmpty()) {
                log.info("没有需要检查的提醒任务");
                return;
            }

            log.info("共找到 {} 个启用提醒的任务", tasks.size());

            // 从缓存中获取在线用户列表
            Set<String> onlineUsers = getOnlineUsersFromCache();
            if (onlineUsers.isEmpty()) {
                log.info("当前没有在线用户，跳过发送提醒");
                return;
            }
            log.info("当前在线用户: {}", onlineUsers);

            // 收集所有需要提醒的任务
            List<TaskReminder> allReminders = new ArrayList<>();

            for (TaskManagement task : tasks) {
                ReminderResult result = checkTaskStatus(task, today);

                if (result.shouldNotify) {
                    TaskReminder reminder = new TaskReminder();
                    reminder.task = task;
                    reminder.result = result;
                    allReminders.add(reminder);
                }
            }

            if (allReminders.isEmpty()) {
                log.info("✓ 今天没有需要提醒的任务");
                return;
            }

            // 给所有在线用户发送相同的提醒
            int totalReminders = 0;
            for (String user : onlineUsers) {
                sendBatchNotification(user, allReminders, today);
                totalReminders++;

                // 更新所有任务的最后提醒日期
                for (TaskReminder reminder : allReminders) {
                    updateLastReminderDate(reminder.task);
                }
            }

            log.info("✓ 定时检查完成，共发送 {} 条提醒给 {} 个在线用户，涉及 {} 个任务",
                    totalReminders, onlineUsers.size(), allReminders.size());

        } catch (Exception e) {
            log.error("执行任务提醒检查异常", e);
        }
    }
    private Set<String> getOnlineUsersFromCache() {
        Set<String> onlineUsers = new HashSet<>();
        try {
            org.ehcache.Cache<String, Object> cache = cacheManager.getCache("defaultCache", String.class, Object.class);
            if (cache != null) {
                Object value = cache.get("message_subscribers");
                if (value != null) {
                    String subscribers = value.toString();
                    if (!subscribers.isEmpty()) {
                        // 订阅用户以逗号分隔
                        String[] users = subscribers.split(",");
                        for (String user : users) {
                            if (user != null && !user.trim().isEmpty()) {
                                onlineUsers.add(user.trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("从缓存获取在线用户失败", e);
        }
        return onlineUsers;
    }
    /**
     * 更新上次提醒日期
     */
    private void updateLastReminderDate(TaskManagement task) {
        try {
            task.setLastReminderDate(new Date());
            taskManagementService.updateById(task);
            log.debug("已更新任务 {} 的上次提醒日期", task.getId());
        } catch (Exception e) {
            log.error("更新任务提醒日期失败，taskId: {}", task.getId(), e);
        }
    }

    /**
     * 手动触发检查（用于测试）
     */
    public void manualCheckReminders() {
        log.info("手动触发任务提醒检查");
        checkMissedRemindersOnStartup();
    }

    /**
     * 内部类：提醒检查结果
     */
    private static class ReminderResult {
        boolean shouldNotify;
        long daysSinceLastReminder;
        String reason;

        ReminderResult(boolean shouldNotify, long daysSinceLastReminder, String reason) {
            this.shouldNotify = shouldNotify;
            this.daysSinceLastReminder = daysSinceLastReminder;
            this.reason = reason;
        }
    }

    /**
     * 内部类：任务提醒
     */
    private static class TaskReminder {
        TaskManagement task;
        ReminderResult result;
    }

    /**
     * 检查任务状态，判断是否需要提醒
     */
    private ReminderResult checkTaskStatus(TaskManagement task, LocalDate today) {
        Date firstReminderDate = task.getFirstReminderDate();
        Integer reminderIntervalDays = task.getReminderIntervalDays();

        if (firstReminderDate == null || reminderIntervalDays == null || reminderIntervalDays <= 0) {
            return new ReminderResult(false, 0, null);
        }

        LocalDate firstReminder = firstReminderDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        Date lastReminderDate = task.getLastReminderDate();

        if (lastReminderDate == null) {
            if (!today.isBefore(firstReminder)) {
                long daysSinceFirst = ChronoUnit.DAYS.between(firstReminder, today);
                log.debug("任务 {} 首次提醒，距离首次提醒日期已过{}天", task.getId(), daysSinceFirst);
                return new ReminderResult(true, daysSinceFirst, "首次提醒");
            } else {
                long daysUntilFirst = ChronoUnit.DAYS.between(today, firstReminder);
                return new ReminderResult(false, daysUntilFirst, null);
            }
        } else {
            LocalDate lastReminder = lastReminderDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            long daysSinceLastReminder = ChronoUnit.DAYS.between(lastReminder, today);

            if (daysSinceLastReminder >= reminderIntervalDays) {
                log.debug("任务 {} 距离上次提醒已过{}天，满足间隔{}天",
                        task.getId(), daysSinceLastReminder, reminderIntervalDays);
                return new ReminderResult(true, daysSinceLastReminder, "周期性提醒");
            } else {
                return new ReminderResult(false, daysSinceLastReminder, null);
            }
        }
    }

    /**
     * 批量发送通知给指定用户
     */
    private void sendBatchNotification(String updateUser, List<TaskReminder> reminders, LocalDate today) {
        if (reminders == null || reminders.isEmpty()) {
            return;
        }

        List<TaskReminder> firstReminders = new ArrayList<>();
        List<TaskReminder> periodicReminders = new ArrayList<>();

        for (TaskReminder reminder : reminders) {
            if ("首次提醒".equals(reminder.result.reason)) {
                firstReminders.add(reminder);
            } else {
                periodicReminders.add(reminder);
            }
        }

        StringBuilder notification = new StringBuilder();
        notification.append("📅 期间核查提醒\n\n");

        if (!firstReminders.isEmpty()) {
            notification.append("【首次提醒任务】\n");
            for (int i = 0; i < firstReminders.size(); i++) {
                TaskReminder reminder = firstReminders.get(i);
                notification.append(String.format("%d. 🆕 %s (首次提醒日期: %s)\n",
                        i + 1, reminder.task.getDetectName(),
                        reminder.task.getFirstReminderDate().toString().substring(0, 10)));
            }
            notification.append("\n");
        }

        if (!periodicReminders.isEmpty()) {
            notification.append("【周期性提醒任务】\n");
            for (int i = 0; i < periodicReminders.size(); i++) {
                TaskReminder reminder = periodicReminders.get(i);
                notification.append(String.format("%d. 🔄 %s (距上次提醒%d天)\n",
                        i + 1, reminder.task.getDetectName(),
                        reminder.result.daysSinceLastReminder));
            }
            notification.append("\n");
        }

        notification.append("请及时登录系统查看任务详情！");

        log.warn("发送期间核查提醒给用户 {}: \n{}", updateUser, notification.toString());

        MessageInfoPO messageInfoPO = new MessageInfoPO();
        messageInfoPO.setMessageType(TYPE_REMIND_MESSAGE.getCode());
        messageInfoPO.setMessageSummary("期间核查提醒");
        messageInfoPO.setMessageSource(Constants.MESSAGE_SOURCE_SYSTEM);
        messageInfoPO.setMessageContent(notification.toString());
        messageInfoPO.setUpdateBy(updateUser);
        Boolean b = messageInfoService.saveMessageInfo(messageInfoPO);
    }

}
