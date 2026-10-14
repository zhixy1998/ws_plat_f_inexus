ALTER TABLE `t800_uv_inexus`.`t_system_panel_conf`
ADD COLUMN `default_enter_classic` bit(1) NOT NULL COMMENT '默认进入经典模式开关' AFTER `is_auto_enter_experiment`;

ALTER TABLE `t800_uv_inexus`.`t_system_database`
DROP COLUMN `default_enter_classic`;