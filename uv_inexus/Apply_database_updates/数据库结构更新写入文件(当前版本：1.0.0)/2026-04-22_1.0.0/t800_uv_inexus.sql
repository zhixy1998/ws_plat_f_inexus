ALTER TABLE t_sys_operate_log
MODIFY COLUMN module TEXT COMMENT '操作模块',
MODIFY COLUMN method TEXT COMMENT '调用方法全路径',
MODIFY COLUMN result TEXT COMMENT '返回结果（JSON格式，仅recordResult=true时记录）',
MODIFY COLUMN error_msg TEXT COMMENT '错误信息（仅失败时记录）';

ALTER TABLE t_sys_quick_config
DROP COLUMN perm_ids_have;


ALTER TABLE t_sys_group_quick_config
ADD COLUMN perm_ids_have TEXT COMMENT '绑定权限ids拥有的回显（每个组各自拥有）';