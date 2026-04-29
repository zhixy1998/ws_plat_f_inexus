-- 检定周期提醒相关字段
ALTER TABLE task_management ADD COLUMN reminder_enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用提醒：0-否，1-是';
ALTER TABLE task_management ADD COLUMN reminder_days INT DEFAULT 2 COMMENT '提前提醒天数';
ALTER TABLE task_management ADD COLUMN next_calibration_date DATE COMMENT '下次检定日期';
ALTER TABLE task_management ADD COLUMN last_reminder_date DATE COMMENT '上次提醒日期';




SET NAMES utf8mb4;

-- 添加有效期截止日期字段
ALTER TABLE `t_cal_artifacts`
ADD COLUMN `expiry_date` DATE NULL COMMENT '有效期截止日期' AFTER `remark`;

-- 添加是否启用效期提醒字段
ALTER TABLE `t_cal_artifacts`
ADD COLUMN `reminder_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否启用效期提醒：0-否，1-是' AFTER `expiry_date`;

-- 添加提前提醒天数字段
ALTER TABLE `t_cal_artifacts`
ADD COLUMN `reminder_days` INT DEFAULT 2 COMMENT '提前提醒天数（默认2天）' AFTER `reminder_enabled`;

-- 添加上次提醒日期字段（用于防重复提醒）
ALTER TABLE `t_cal_artifacts`
ADD COLUMN `last_reminder_date` DATE NULL COMMENT '上次提醒日期' AFTER `reminder_days`;

-- 为 expiry_date 添加索引，优化查询性能
ALTER TABLE `t_cal_artifacts` ADD INDEX `idx_expiry_date` (`expiry_date`) USING BTREE;

-- 为 reminder_enabled 和 expiry_date 添加联合索引
ALTER TABLE `t_cal_artifacts` ADD INDEX `idx_reminder_enabled_expiry` (`reminder_enabled`, `expiry_date`) USING BTREE;

-- 为 calibration_workbook_data 表添加黄色高亮列信息字段
ALTER TABLE calibration_workbook_data
ADD COLUMN yellow_highlighted_columns TEXT COMMENT '黄色高亮列信息(JSON格式)，存储RGB(255,192,0)的列名';
