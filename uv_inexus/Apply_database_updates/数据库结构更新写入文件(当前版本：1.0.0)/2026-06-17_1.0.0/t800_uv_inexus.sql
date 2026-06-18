ALTER TABLE `t800_uv_inexus`.`t_system_panel_conf` 
ADD COLUMN `is_simple_tip` bit(1) NULL COMMENT '是否精简提示信息，1-是，0-否' AFTER `is_auto_enter_experiment`;

UPDATE `t800_uv_inexus`.`t_system_panel_conf` SET  `is_simple_tip` = b'1'  WHERE `id` = '00000000';
