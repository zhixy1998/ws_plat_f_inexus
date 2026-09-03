
-- 添加积分球功能后 暗电流结构体变化 ，所以直接把旧的已用仪器暗电流 清空，防止联机时读取类型失败
-- 重新 操作 暗电流矫正后数据即可恢复正常
UPDATE `t800_uv_inexus`.t_ins_info_temp tiit
SET dark_info = NULL
WHERE tiit.dark_info NOT LIKE '%RS%';