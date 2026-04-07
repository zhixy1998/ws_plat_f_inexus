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

 Date: 07/04/2026 09:42:22
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_measure_pool
-- ----------------------------
DROP TABLE IF EXISTS `t_measure_pool`;
CREATE TABLE `t_measure_pool`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `attachment_num` int(11) NULL DEFAULT NULL,
  `attachment_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sample_pool_num` int(11) NULL DEFAULT NULL,
  `attachment_type` int(11) NULL DEFAULT NULL COMMENT '附件种类',
  `dev_type` int(11) NULL DEFAULT NULL COMMENT '仪器类型，T800/t700',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_measure_pool
-- ----------------------------
INSERT INTO `t_measure_pool` VALUES (1, 0, 'T700固体样品池', 1, 1, 1);
INSERT INTO `t_measure_pool` VALUES (2, 1, 'T700镜面反射附件', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (3, 2, 'T700固定100mm长样池', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (4, 3, 'T700标准及微量样品池', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (5, 4, 'T700五联恒温池', 5, 3, 1);
INSERT INTO `t_measure_pool` VALUES (6, 5, 'T700五联100mm长样池', 5, 3, 1);
INSERT INTO `t_measure_pool` VALUES (7, 6, 'T700可变角度样品架', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (8, 7, 'T700超微量样品池', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (9, 8, 'T700八联池', 8, 2, 1);
INSERT INTO `t_measure_pool` VALUES (10, 9, 'T700试管架', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (11, 10, 'T700固定恒温池', 1, 1, 1);
INSERT INTO `t_measure_pool` VALUES (12, 11, 'T700积分椭球', 1, 4, 1);
INSERT INTO `t_measure_pool` VALUES (13, 12, '未识别', 0, -1, 1);
INSERT INTO `t_measure_pool` VALUES (14, 13, 'T600标准及微量样品池', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (15, 14, 'T600固定100mm长样池', 1, 1, 1);
INSERT INTO `t_measure_pool` VALUES (16, 15, 'T600固定恒温池', 1, 1, 1);
INSERT INTO `t_measure_pool` VALUES (17, 16, 'T600五联恒温池', 5, 3, 1);
INSERT INTO `t_measure_pool` VALUES (18, 17, 'T600固体样品池', 1, 1, 1);
INSERT INTO `t_measure_pool` VALUES (19, 18, 'T600试管架', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (20, 19, 'T600可变角度样品架', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (21, 20, 'T600超微量样品池', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (22, 21, 'T600五联100mm长样池', 5, 3, 1);
INSERT INTO `t_measure_pool` VALUES (23, 22, 'T600积分椭球', 1, 4, 1);
INSERT INTO `t_measure_pool` VALUES (24, 23, 'T600镜面反射附件', 1, 0, 1);
INSERT INTO `t_measure_pool` VALUES (25, 24, 'T600八联池', 8, 2, 1);
INSERT INTO `t_measure_pool` VALUES (26, 25, 'T800固体样品池', 1, 1, 1);
INSERT INTO `t_measure_pool` VALUES (27, 0, 'T800固体样品池', 1, 1, 2);
INSERT INTO `t_measure_pool` VALUES (28, 1, 'T1960镜面反射附件', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (29, 2, 'T1960固定100mm长样池', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (30, 3, 'T1960标准及微量样品池', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (31, 4, 'T1960五联恒温池', 5, 3, 2);
INSERT INTO `t_measure_pool` VALUES (32, 5, 'T1960五联100mm长样池', 5, 3, 2);
INSERT INTO `t_measure_pool` VALUES (33, 6, 'T1960可变角度样品架', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (34, 7, 'T1960超微量样品池', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (35, 8, 'T1960八联池', 8, 2, 2);
INSERT INTO `t_measure_pool` VALUES (36, 9, 'T1960试管架', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (37, 10, 'T1960固定恒温池', 1, 1, 2);
INSERT INTO `t_measure_pool` VALUES (38, 11, 'T1960积分椭球', 1, 4, 2);
INSERT INTO `t_measure_pool` VALUES (39, 12, '未识别', 0, -1, 2);
INSERT INTO `t_measure_pool` VALUES (40, 13, 'T1960标准及微量样品池', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (41, 14, 'T1960固定100mm长样池', 1, 1, 2);
INSERT INTO `t_measure_pool` VALUES (42, 15, 'T1960固定恒温池', 1, 1, 2);
INSERT INTO `t_measure_pool` VALUES (43, 16, 'T1960五联恒温池', 5, 3, 2);
INSERT INTO `t_measure_pool` VALUES (44, 17, 'T1960固体样品池', 1, 1, 2);
INSERT INTO `t_measure_pool` VALUES (45, 18, 'T1960试管架', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (46, 19, 'T1960可变角度样品架', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (47, 20, 'T1960超微量样品池', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (48, 21, 'T1960五联100mm长样池', 5, 3, 2);
INSERT INTO `t_measure_pool` VALUES (49, 22, 'T1960积分椭球', 1, 4, 2);
INSERT INTO `t_measure_pool` VALUES (50, 23, 'T1960镜面反射附件', 1, 0, 2);
INSERT INTO `t_measure_pool` VALUES (51, 24, 'T1960八联池', 8, 2, 2);
INSERT INTO `t_measure_pool` VALUES (52, 25, 'T1960固体样品池', 1, 1, 2);

SET FOREIGN_KEY_CHECKS = 1;
