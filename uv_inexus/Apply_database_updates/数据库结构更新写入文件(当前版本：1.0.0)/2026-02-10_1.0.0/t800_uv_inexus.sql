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
  `perm_name` varchar(100) NOT NULL COMMENT '快捷配置名称',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`) COMMENT '父级ID索引'
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='快捷配置表';