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

 Date: 10/02/2026 10:06:46
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_ins_calibration_param
-- ----------------------------
DROP TABLE IF EXISTS `t_ins_calibration_param`;
CREATE TABLE `t_ins_calibration_param`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `param_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `param` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `default_value` json NULL COMMENT '参数默认数据',
  `param_type` int(11) NULL DEFAULT NULL COMMENT '参数类型，0-固定值，1-输入框，2-下拉框',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_ins_calibration_param
-- ----------------------------
INSERT INTO `t_ins_calibration_param` VALUES (1, '波长', 'wavelength', '[{\"dataKey\": null, \"dataValue\": \"600.00nm\"}]', 0);
INSERT INTO `t_ins_calibration_param` VALUES (2, '半峰宽', 'pwh', '[{\"dataKey\": null, \"dataValue\": \"0.00\"}]', 0);
INSERT INTO `t_ins_calibration_param` VALUES (3, '光谱带宽', 'wavebrand', '[{\"dataKey\": \"\", \"dataValue\": \"2.0\"}]', 2);
INSERT INTO `t_ins_calibration_param` VALUES (4, '起始波长', 'start', '[{\"dataKey\": \"min\", \"dataValue\": \"190\"}, {\"dataKey\": \"max\", \"dataValue\": \"1100\"}]', 1);
INSERT INTO `t_ins_calibration_param` VALUES (5, '终止波长', 'end', '[{\"dataKey\": \"min\", \"dataValue\": \"190\"}, {\"dataKey\": \"max\", \"dataValue\": \"1100\"}]', 1);
INSERT INTO `t_ins_calibration_param` VALUES (6, '扫描速度', 'scanSpeed', '[{\"dataKey\": \"慢速\", \"dataValue\": \"0\"}, {\"dataKey\": \"中速\", \"dataValue\": \"1\"}, {\"dataKey\": \"快速\", \"dataValue\": \"2\"}, {\"dataKey\": \"极快\", \"dataValue\": \"3\"}]', 2);
INSERT INTO `t_ins_calibration_param` VALUES (7, '扫描间隔', 'step', '[{\"dataKey\": null, \"dataValue\": \"0.5\"}]', 2);

SET FOREIGN_KEY_CHECKS = 1;
