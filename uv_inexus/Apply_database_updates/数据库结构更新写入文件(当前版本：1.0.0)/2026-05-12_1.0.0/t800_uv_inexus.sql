-- 提醒功能开关配置表（支持扩展）
CREATE TABLE IF NOT EXISTS `t_reminder_switch_config` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '主键ID',
  `reminder_type` INT NOT NULL COMMENT '提醒类型：0-总开关，1-检定提醒，2-校准提醒，3-期间核查提醒，4-标准物质效期提醒',
  `reminder_name` VARCHAR(100) NOT NULL COMMENT '提醒名称',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用：0-关闭，1-开启',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_user` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  UNIQUE KEY `uk_reminder_type` (`reminder_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒功能开关配置表';

-- 插入默认配置
INSERT INTO `t_reminder_switch_config` 
(`id`, `reminder_type`, `reminder_name`, `enabled`, `sort_order`, `description`, `create_user`, `update_user`)
VALUES 
('1', 0, '总开关', 1, 0, '提醒功能总开关，关闭后所有提醒将停止', 'system', 'system'),
('4', 1, '期间核查提醒', 1, 3, '期间核查任务提醒', 'system', 'system'),
('5', 2, '标准物质效期提醒', 1, 4, '标准物质有效期到期提醒', 'system', 'system');
