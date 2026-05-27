package com.persee.workstation.uv_inexus.listener;

import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageRecycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageRecycleCleanScheduler {

    @Autowired
    private MessageRecycleService messageRecycleService;

/**
 * 定时清理消息回收站的任务
 * 使用cron表达式设置为每天凌晨2点执行
 * cron表达式 "0 0 2 * * ?" 表示：
 * 0秒 0分 2点 每日 ?表示不指定具体星期几
 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanup() {
    // 记录开始执行日志
        log.info("开始执行消息回收站定时清理...");
        try {
        // 执行超过限制的消息清理
            messageRecycleService.cleanupOverLimit();
        // 执行过期记录清理
            messageRecycleService.cleanupOldRecords();
        // 记录完成执行日志
            log.info("消息回收站定时清理完成");
        } catch (Exception e) {
        // 记录异常日志
            log.error("消息回收站定时清理异常", e);
        }
    }
}
