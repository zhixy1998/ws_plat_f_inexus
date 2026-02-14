/* 新增快捷配置和组关联表 */
CREATE TABLE `t_sys_group_quick_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID（主键，自增数值类型）',
  `group_id` varchar(64) NOT NULL COMMENT '小组ID（关联t_sys_group表主键）',
  `quick_config_ids` text COMMENT '快捷配置ID列表（逗号分隔，如：1,2,3）',
  `create_user` varchar(64) DEFAULT '' COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(64) DEFAULT '' COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记（0=未删除，1=已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_group_id` (`group_id`) COMMENT '小组ID唯一索引（一个小组只存一条记录）',
  KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引',
  KEY `idx_update_time` (`update_time`) COMMENT '更新时间索引'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='小组-快捷配置关联表';

// 2026-02-11 perm_name设置唯一
ALTER TABLE `t_sys_permission`
ADD CONSTRAINT `uk_sys_permission_perm_name` UNIQUE (`perm_name`);
// 权限表新增字段
ALTER TABLE `t_sys_permission`
ADD COLUMN `quick_config_type` int(5) DEFAULT NULL COMMENT '0-普通，1-GMP' AFTER `redirect`;
// 新增表快捷配置
CREATE TABLE `t_sys_quick_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '快捷配置名称',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父级ID（0为一级配置）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` varchar(255) DEFAULT NULL,
  `update_user` varchar(255) DEFAULT NULL,
  `perm_ids` text COMMENT '快捷配置名称',
  `quick_config_type` int(5) DEFAULT NULL COMMENT '0-普通，1-GMP',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`) COMMENT '父级ID索引'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='快捷配置表';