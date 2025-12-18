/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

ALTER TABLE `t800_uv_inexus`.`t_system_business_param`
ADD COLUMN `concentration_digits` int(11) NULL COMMENT '浓度显示位数' AFTER `e_digits`;

ALTER TABLE `t800_uv_inexus`.`t_ins_info_temp`
ADD COLUMN `connect_type` int NULL COMMENT '连接方式' AFTER `device_ip`,
ADD COLUMN `device_id` varchar(255) NULL COMMENT '唯一标识' AFTER `connect_type`;