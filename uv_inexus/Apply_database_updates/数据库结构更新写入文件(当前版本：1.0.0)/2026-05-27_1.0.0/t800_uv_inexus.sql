-- 用户提醒屏蔽配置表
CREATE TABLE IF NOT EXISTS t_user_reminder_mute (
    id VARCHAR(36) PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL,
    reminder_type INT NOT NULL COMMENT '提醒类型：1-期间核查提醒，2-标准物到期提醒',
    muted INT DEFAULT 1 COMMENT '是否屏蔽弹窗：1-屏蔽，0-恢复',
    mute_scope INT DEFAULT 0 COMMENT '屏蔽范围：0-下次开机恢复，1-永久屏蔽需手动恢复',
    create_time DATETIME COMMENT '创建时间',
    UNIQUE KEY uk_user_type (user_name, reminder_type)
) COMMENT '用户提醒屏蔽配置';

-- 消息回收站表
CREATE TABLE IF NOT EXISTS t_message_recycle (
    id VARCHAR(36) PRIMARY KEY COMMENT '原消息ID',
    message_type INT COMMENT '消息类型',
    message_source VARCHAR(128) COMMENT '消息来源',
    message_content TEXT COMMENT '消息内容',
    message_status INT COMMENT '消息状态',
    receive_time DATETIME COMMENT '接收时间',
    message_summary VARCHAR(512) COMMENT '消息摘要',
    original_create_by VARCHAR(64) COMMENT '原始创建者',
    original_update_by VARCHAR(64) COMMENT '原始修改者',
    original_create_time DATETIME COMMENT '原始创建时间',
    deleted_time DATETIME COMMENT '删除时间'
) COMMENT '消息回收站';
