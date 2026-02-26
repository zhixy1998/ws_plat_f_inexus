/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 2026-02-03 新增付费功能字段
-- ----------------------------
ALTER TABLE `t_software_activation`
ADD COLUMN `features` VARCHAR(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '付费功能';
-- ----------------------------
-- Table structure for aerich
-- ----------------------------

ALTER TABLE `t800_uv_inexus`.`t_measure_method`
    CHANGE COLUMN `field2` `method_use_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预留字段2' AFTER `method_creator`;

ALTER TABLE `t800_uv_inexus`.`t_measure_project`
    CHANGE COLUMN `field3` `owner_group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主责组' AFTER `last_updated_time`;

DROP TABLE IF EXISTS `t_sample_pool_config`;
CREATE TABLE `t_sample_pool_config`  (
                                         `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键id',
                                         `device_serial_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '序列号',
                                         `attachment_type` int NULL DEFAULT NULL COMMENT '附件类型',
                                         `attachment_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '附件类型名称',
                                         `is_auto_change_pool` bit(1) NULL DEFAULT NULL COMMENT '是否自动切换样品池',
                                         `current_pool_num` int NULL DEFAULT NULL COMMENT '当前样品池位置',
                                         `pool_info` json NULL COMMENT '样品池信息',
                                         `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                         `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
                                         `create_user` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
                                         `update_user` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改者',
                                         PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


ALTER TABLE `t800_uv_inexus`.`t_measure_task`
DROP COLUMN `sample_auto_num`,
ADD COLUMN `sample_auto_num` int NULL COMMENT '样品自动编号' AFTER `is_save`;


ALTER TABLE `t800_uv_inexus`.`t_measure_method`
    CHANGE COLUMN `field3` `is_delete` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否删除(0:未删除 1:已删除)' AFTER `method_use_time`,
    MODIFY COLUMN `method_use_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法使用时间' AFTER `method_creator`;