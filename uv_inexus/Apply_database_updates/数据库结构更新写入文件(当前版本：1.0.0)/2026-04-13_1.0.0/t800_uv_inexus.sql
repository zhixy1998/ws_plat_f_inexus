ALTER TABLE t_sys_operate_log
ADD COLUMN `before_data` text COMMENT '修改前数据JSON',
ADD COLUMN `after_data` text COMMENT '修改后数据JSON',
ADD COLUMN `change_content` text COMMENT '修改内容对比（中文）';

ALTER TABLE `t800_uv_inexus`.`t_sys_operate_log`
ADD COLUMN `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识（关联ButtonPermission注解）' AFTER `module`;