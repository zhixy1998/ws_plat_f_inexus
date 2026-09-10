ALTER TABLE `t800_uv_inexus`.`t_system_panel_conf`
ADD COLUMN `is_full_screen` bit(1) NULL COMMENT '是否全屏，1-是，0-否' AFTER `is_simple_tip`,
ADD COLUMN `is_expand_nav_bar` bit(1) NULL COMMENT '是否展开导航栏，1-展开，0-收起' AFTER `is_full_screen`;

UPDATE `t800_uv_inexus`.`t_system_panel_conf` SET `is_full_screen` = b'1', `is_expand_nav_bar` = b'0' WHERE `id` = '00000000';

ALTER TABLE `t800_uv_inexus`.`t_system_business_param`
ADD COLUMN `measure_digits` int NULL COMMENT '测量值小数位数' AFTER `is_negative_show`;

UPDATE `t800_uv_inexus`.`t_system_business_param` SET `measure_digits` = 3 WHERE `id` = '00000000';

UPDATE `t800_uv_inexus`.`t_system_panel_conf` SET `is_auto_boot` = b'1' WHERE `id` = '00000000';
