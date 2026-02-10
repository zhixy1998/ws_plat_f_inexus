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

 Date: 10/02/2026 10:16:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_device_type_info
-- ----------------------------
DROP TABLE IF EXISTS `t_device_type_info`;
CREATE TABLE `t_device_type_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `dev_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '设备类型， T600/T700',
  `luminosity` json NULL COMMENT '光度模式',
  `lamp_type` json NULL COMMENT '灯类型，钨灯，氘灯',
  `connect_mode` int(11) NULL DEFAULT NULL COMMENT '连接方式，0-串口，1-网口',
  `coefficient` int(11) NULL DEFAULT NULL COMMENT '没有波长列表时的默认系数',
  `gain` json NULL COMMENT '增益',
  `response_time` json NULL COMMENT '响应时间',
  `scan_speed` json NULL COMMENT '扫描速度',
  `min_wavelength` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最小波长',
  `max_wavelength` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最大波长',
  `scan_interval` json NULL COMMENT '扫描间隔',
  `calibration_type` json NULL COMMENT '仪器校正选项',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备类型信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_device_type_info
-- ----------------------------
INSERT INTO `t_device_type_info` VALUES (1, 'T600/T700', '[0, 1, 2, 3]', '[0, 1]', 1, 50, '[1, 2, 3, 4]', '[\"0.01\", \"0.02\", \"0.05\", \"0.1\", \"0.2\", \"0.5\", \"1\", \"2\"]', '[\"慢速\", \"中速\", \"快速\", \"极快\"]', '190', '1100', '[\"0.1\", \"0.2\", \"0.5\", \"1.0\", \"2.0\", \"5.0\"]', '[1, 2]');
INSERT INTO `t_device_type_info` VALUES (2, 'T800', '[0, 1, 2, 3]', '[0, 1]', 1, 40, '[1, 2, 3, 4]', '[\"0.01\", \"0.02\", \"0.05\", \"0.1\", \"0.2\", \"0.5\", \"1\", \"2\"]', '[\"慢速\", \"中速\", \"快速\", \"极快\"]', '190', '900', '[\"0.1\", \"0.2\", \"0.5\", \"1.0\", \"2.0\", \"5.0\"]', '[1, 3]');

SET FOREIGN_KEY_CHECKS = 1;
