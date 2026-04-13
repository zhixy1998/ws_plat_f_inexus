ALTER TABLE t_sys_operate_log
ADD COLUMN `before_data` text COMMENT '修改前数据JSON',
ADD COLUMN `after_data` text COMMENT '修改后数据JSON',
ADD COLUMN `change_content` text COMMENT '修改内容对比（中文）';