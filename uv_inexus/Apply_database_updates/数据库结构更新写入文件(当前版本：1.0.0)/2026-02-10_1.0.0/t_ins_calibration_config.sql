/*
 Navicat Premium Dump SQL

 Source Server         : 本机docker-mysql5.7
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44)
 Source Host           : localhost:3308
 Source Schema         : t800_uv_inexus

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44)
 File Encoding         : 65001

 Date: 10/02/2026 10:05:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_ins_calibration_config
-- ----------------------------
DROP TABLE IF EXISTS `t_ins_calibration_config`;
CREATE TABLE `t_ins_calibration_config`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `calibration_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `calibration_type` int(11) NULL DEFAULT NULL COMMENT '类型序号',
  `param_list` json NULL COMMENT '参数列表',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '校正类型默认表，记录不同仪器所具备的校正类型' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_ins_calibration_config
-- ----------------------------
INSERT INTO `t_ins_calibration_config` VALUES (1, '波长校正', 1, '[1, 2, 3]');
INSERT INTO `t_ins_calibration_config` VALUES (2, '暗电流校正', 2, '[]');
INSERT INTO `t_ins_calibration_config` VALUES (3, '暗电流校正', 4, '[4, 5, 6, 3, 7]');

SET FOREIGN_KEY_CHECKS = 1;
