/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

ALTER TABLE `t800_uv_inexus`.`t_measure_method`
    CHANGE COLUMN `field2` `method_use_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段2' AFTER `method_creator`;

ALTER TABLE `t800_uv_inexus`.`t_measure_project`
    CHANGE COLUMN `field3` `owner_group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主责组' AFTER `last_updated_time`;