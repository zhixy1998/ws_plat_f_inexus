ALTER TABLE `t800_uv_inexus`.`t_device_type_info`
ADD COLUMN `work_time_count` json NULL COMMENT '灯使用时间统计相关指令' AFTER `calibration_type`;
ALTER TABLE `t800_uv_inexus`.`t_ins_info_temp`
ADD COLUMN `work_time_info` json NULL COMMENT '使用时长' AFTER `linear_segment`;

UPDATE t_device_type_info
SET work_time_count = '[{"type": "仪器", "workTime": "GetDeviceCurrentTime", "updateTime": null, "totalWorkTime": "GetDeviceTotalTime"}, {"type": "钨灯", "workTime": "GetWLampCurrentTime", "updateTime": null, "totalWorkTime": "GetWLampTotalTime"}, {"type": "氘灯", "workTime": "GetDLampCurrentTime", "updateTime": null, "totalWorkTime": "GetDLampTotalTime"}]'
WHERE dev_type = 'T800';