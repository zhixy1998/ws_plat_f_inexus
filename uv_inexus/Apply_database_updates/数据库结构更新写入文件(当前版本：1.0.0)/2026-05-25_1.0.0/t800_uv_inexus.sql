ALTER TABLE t_reminder_switch_config
ADD COLUMN notification_mode INTEGER DEFAULT 0 COMMENT '通知模式：0-正常提醒，1-仅通知不提醒';
