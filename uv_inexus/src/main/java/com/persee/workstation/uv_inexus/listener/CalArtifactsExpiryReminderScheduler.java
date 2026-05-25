package com.persee.workstation.uv_inexus.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.enums.factoryInspection.ReminderTypeEnum;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalArtifacts;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalArtifactsService;
import com.persee.workstation.uv_inexus.service.factory_inspection.ReminderSwitchConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageInfoService;
import com.persee.workstation.uv_inexus.util.I18nMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.persee.workstation.uv_inexus.enums.messageEnums.MessageTypeEnum.TYPE_NOTICE_MESSAGE;
import static com.persee.workstation.uv_inexus.enums.messageEnums.MessageTypeEnum.TYPE_REMIND_MESSAGE;

@Slf4j
@Component
public class CalArtifactsExpiryReminderScheduler {

    @Autowired
    private CalArtifactsService calArtifactsService;

    @Autowired
    private MessageInfoService messageInfoService;

    @Autowired
    private ReminderSwitchConfigService reminderSwitchConfigService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private I18nMessageUtil i18nMessageUtil;

    @PostConstruct
    public void checkMissedRemindersOnStartup() {
        log.info(">>> 开始检查启动时错过的标准物质效期提醒...");

        if (!reminderSwitchConfigService.isReminderEnabled()) {
            log.info("提醒功能已关闭，跳过检查");
            return;
        }

        if (!reminderSwitchConfigService.isReminderTypeEnabled(ReminderTypeEnum.EXPIRY_REMINDER.getCode())) {
            log.info("标准物质效期提醒已关闭，跳过检查");
            return;
        }

        checkExpiryReminders();
    }

    @Scheduled(cron = "*/30 * * * * ?")
    public void scheduledCheckExpiryReminders() {
        log.info("开始执行标准物质效期定时提醒检查...");

        if (!reminderSwitchConfigService.isReminderEnabled()) {
            log.info("提醒功能已关闭，跳过检查");
            return;
        }

        if (!reminderSwitchConfigService.isReminderTypeEnabled(ReminderTypeEnum.EXPIRY_REMINDER.getCode())) {
            log.info("标准物质效期提醒已关闭，跳过检查");
            return;
        }

        checkExpiryReminders();
    }

    private void checkExpiryReminders() {
        try {
            LocalDate today = LocalDate.now();

            LambdaQueryWrapper<CalArtifacts> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CalArtifacts::getIsDelete, "0")
                    .eq(CalArtifacts::getReminderEnabled, 1)
                    .isNotNull(CalArtifacts::getExpiryDate);

            List<CalArtifacts> artifacts = calArtifactsService.list(wrapper);

            if (artifacts == null || artifacts.isEmpty()) {
                log.info("没有启用效期提醒的标准物质");
                return;
            }

            log.info("共找到 {} 个启用效期提醒的标准物质，开始检查...", artifacts.size());

            Set<String> onlineUsers = getOnlineUsersFromCache();
            if (onlineUsers.isEmpty()) {
                log.info("当前没有在线用户，跳过发送提醒");
                return;
            }
            log.info("当前在线用户: {}", onlineUsers);

            List<ExpiryReminder> allReminders = new ArrayList<>();

            for (CalArtifacts artifact : artifacts) {
                ExpiryCheckResult result = checkExpiryStatus(artifact, today);

                if (result.shouldNotify) {
                    allReminders.add(new ExpiryReminder(artifact, result));
                }
            }

            if (allReminders.isEmpty()) {
                log.info("✓ 今天没有需要提醒的标准物质效期");
                return;
            }

            int totalReminders = 0;
            for (String user : onlineUsers) {
                sendBatchNotification(user, allReminders, today);
                totalReminders++;

                for (ExpiryReminder reminder : allReminders) {
                    updateLastReminderDate(reminder.artifact);
                }
            }

            log.info("✓ 标准物质效期检查完成，共发送 {} 条提醒给 {} 个在线用户，涉及 {} 个标准物质",
                    totalReminders, onlineUsers.size(), allReminders.size());

        } catch (Exception e) {
            log.error("执行标准物质效期提醒检查异常", e);
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

    private ExpiryCheckResult checkExpiryStatus(CalArtifacts artifact, LocalDate today) {
        Date expiryDate = artifact.getExpiryDate();
        Integer reminderDays = artifact.getReminderDays();

        if (expiryDate == null) {
            return new ExpiryCheckResult(false, 0, null);
        }

        if (reminderDays == null || reminderDays <= 0) {
            reminderDays = 30;
        }

        LocalDate expiryLocalDate = expiryDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryLocalDate);

        boolean shouldNotify = false;
        String notifyReason = null;

        if (daysUntilExpiry < 0) {
            shouldNotify = true;
            notifyReason = "已过期";
        } else if (daysUntilExpiry <= reminderDays) {
            shouldNotify = true;
            notifyReason = "即将到期";
        }

        if (!shouldNotify) {
            return new ExpiryCheckResult(false, daysUntilExpiry, null);
        }

        Date lastReminderDate = artifact.getLastReminderDate();
        if (lastReminderDate != null) {
            LocalDate lastReminder = lastReminderDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if (lastReminder.isEqual(today)) {
                log.debug("标准物质 {} 今天已发送过效期提醒，跳过", artifact.getName());
                return new ExpiryCheckResult(false, daysUntilExpiry, null);
            }
        }

        log.debug("标准物质 {} 需要提醒: {}, 剩余{}天",
                artifact.getName(), notifyReason, daysUntilExpiry);

        return new ExpiryCheckResult(true, daysUntilExpiry, notifyReason);
    }

    private void sendBatchNotification(String updateUser, List<ExpiryReminder> reminders, LocalDate today) {
        if (reminders == null || reminders.isEmpty()) {
            return;
        }

        List<ExpiryReminder> expiringReminders = new ArrayList<>();
        List<ExpiryReminder> expiredReminders = new ArrayList<>();

        for (ExpiryReminder reminder : reminders) {
            if (reminder.result.daysUntilExpiry < 0) {
                expiredReminders.add(reminder);
            } else {
                expiringReminders.add(reminder);
            }
        }

        StringBuilder notification = new StringBuilder();

        if (!expiringReminders.isEmpty()) {
            notification.append(i18nMessageUtil.getMessage("00610003")).append("<br>");
            for (int i = 0; i < expiringReminders.size(); i++) {
                ExpiryReminder reminder = expiringReminders.get(i);
                long days = reminder.result.daysUntilExpiry;
                String expiryDateStr = reminder.artifact.getExpiryDate().toString().substring(0, 10);

                if (days == 0) {
                    notification.append(String.format("%d. %s<br>",
                            i + 1, i18nMessageUtil.getMessage("00610004", reminder.artifact.getName())));
                } else {
                    notification.append(String.format("%d. %s<br>",
                            i + 1, i18nMessageUtil.getMessage("00610005", reminder.artifact.getName(), days, expiryDateStr)));
                }
            }
        }

        if (!expiredReminders.isEmpty()) {
            notification.append(i18nMessageUtil.getMessage("00610006")).append("<br>");
            for (int i = 0; i < expiredReminders.size(); i++) {
                ExpiryReminder reminder = expiredReminders.get(i);
                long expiredDays = Math.abs(reminder.result.daysUntilExpiry);
                String expiryDateStr = reminder.artifact.getExpiryDate().toString().substring(0, 10);

                notification.append(String.format("%d. %s<br>",
                        i + 1, i18nMessageUtil.getMessage("00610007", reminder.artifact.getName(), expiredDays, expiryDateStr)));
            }
        }

        notification.append(i18nMessageUtil.getMessage("00610008"));

        log.warn("发送标准物质效期提醒给用户 {}: <br>{}", updateUser, notification.toString());

        Integer messageType = getNotificationModeForExpiryReminder();

        MessageInfoPO messageInfoPO = new MessageInfoPO();
        messageInfoPO.setMessageType(messageType);
        messageInfoPO.setMessageSummary(i18nMessageUtil.getMessage("00610009"));
        messageInfoPO.setMessageSource(Constants.MESSAGE_SOURCE_SYSTEM);
        messageInfoPO.setMessageContent(notification.toString());
        messageInfoPO.setCreateBy(updateUser);
        messageInfoPO.setUpdateBy(updateUser);

        Boolean saved = messageInfoService.saveMessageInfo(messageInfoPO);
        if (saved) {
            String typeDesc = messageType.equals(TYPE_REMIND_MESSAGE.getCode()) ?
                    i18nMessageUtil.getMessage("00610010") : i18nMessageUtil.getMessage("00610011");
            log.info("成功发送标准物质效期{}给用户: {}", typeDesc, updateUser);
        } else {
            log.error("发送标准物质效期提醒失败，用户: {}", updateUser);
        }
    }

    private Integer getNotificationModeForExpiryReminder() {
        try {
            Integer notificationMode = reminderSwitchConfigService.getNotificationMode(
                    ReminderTypeEnum.EXPIRY_REMINDER.getCode()
            );

            if (notificationMode != null && notificationMode == 1) {
                log.debug("标准物质效期提醒设置为'仅通知不提醒'模式");
                return TYPE_NOTICE_MESSAGE.getCode();
            }

            return TYPE_REMIND_MESSAGE.getCode();
        } catch (Exception e) {
            log.error("获取提醒配置失败，使用默认提醒类型", e);
            return TYPE_REMIND_MESSAGE.getCode();
        }
    }

    private void updateLastReminderDate(CalArtifacts artifact) {
        try {
            artifact.setLastReminderDate(new Date());
            calArtifactsService.updateById(artifact);
            log.debug("已更新标准物质 {} 的上次提醒日期", artifact.getName());
        } catch (Exception e) {
            log.error("更新标准物质提醒日期失败，artifactId: {}", artifact.getId(), e);
        }
    }

    public void manualCheckExpiryReminders() {
        log.info("手动触发标准物质效期提醒检查");
        checkExpiryReminders();
    }

    private static class ExpiryCheckResult {
        boolean shouldNotify;
        long daysUntilExpiry;
        String reason;

        ExpiryCheckResult(boolean shouldNotify, long daysUntilExpiry, String reason) {
            this.shouldNotify = shouldNotify;
            this.daysUntilExpiry = daysUntilExpiry;
            this.reason = reason;
        }
    }

    private static class ExpiryReminder {
        CalArtifacts artifact;
        ExpiryCheckResult result;

        ExpiryReminder(CalArtifacts artifact, ExpiryCheckResult result) {
            this.artifact = artifact;
            this.result = result;
        }
    }
}
