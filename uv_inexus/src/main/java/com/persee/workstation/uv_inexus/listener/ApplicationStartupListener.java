package com.persee.workstation.uv_inexus.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Slf4j
@Component
public class ApplicationStartupListener implements CommandLineRunner {

    @Autowired
    private TaskReminderScheduler taskReminderScheduler;
    @Autowired
    private CalArtifactsExpiryReminderScheduler calArtifactsExpiryReminderScheduler;

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("应用启动完成，开始执行启动检查...");
        log.info("========================================");

        try {
            taskReminderScheduler.checkMissedRemindersOnStartup();
            calArtifactsExpiryReminderScheduler.checkMissedRemindersOnStartup();
        } catch (Exception e) {
            log.error("启动时检查错过提醒失败", e);
        }

        log.info("========================================");
        log.info("启动检查完成");
        log.info("========================================");
    }
}