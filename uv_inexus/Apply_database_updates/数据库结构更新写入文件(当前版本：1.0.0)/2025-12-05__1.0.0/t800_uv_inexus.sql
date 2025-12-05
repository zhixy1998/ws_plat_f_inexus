/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

ALTER TABLE `t800_uv_inexus`.`t_ins_config_setting`
ADD COLUMN `message_port_change` bit NULL COMMENT '通讯端口是否可改' AFTER `band_width`,
ADD COLUMN `annex_setting_change` bit NULL COMMENT '附件设置是否可改' AFTER `message_port_change`;


ALTER TABLE `t800_uv_inexus`.`t_system_business_param`
ADD COLUMN `abs_digits` INT DEFAULT NULL COMMENT 'Abs显示位数',
ADD COLUMN `t_digits` INT DEFAULT NULL COMMENT 'T%、R%显示位数',
ADD COLUMN `e_digits` INT DEFAULT NULL COMMENT 'E显示位数',
ADD COLUMN `sd_digits` INT DEFAULT NULL COMMENT 'SD和RSD显示位数',
ADD COLUMN `k_digits` INT DEFAULT NULL COMMENT 'K显示位数',
ADD COLUMN `r_square_digits` INT DEFAULT NULL COMMENT 'R/R平方显示位数',
ADD COLUMN `is_negative_show` bit(1) DEFAULT NULL COMMENT '浓度负值是否显示未检出';


ALTER TABLE `t800_uv_inexus`.`t_system_business_param`
MODIFY COLUMN `is_negative_show` tinyint(1) NULL DEFAULT NULL COMMENT '浓度负值是否显示未检出' AFTER `r_square_digits`,
MODIFY COLUMN `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间' AFTER `is_negative_show`,
MODIFY COLUMN `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间' AFTER `create_time`,
MODIFY COLUMN `create_user` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者' AFTER `update_time`,
MODIFY COLUMN `update_user` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改者' AFTER `create_user`;

UPDATE `t800_uv_inexus`.`t_system_business_param`
SET
    `abs_digits` = 4,
    `t_digits` = 4,
    `e_digits` = 4,
    `sd_digits` = 4,
    `k_digits` = 4,
    `r_square_digits` = 4,
    `is_negative_show` = 0
WHERE id = '00000000';