ALTER TABLE `t800_uv_inexus`.`t_device_init_entry`
ADD COLUMN `scan_speed` JSON COMMENT '扫描速度';

-- 更新 id 为 1 至 8 的设备的扫描速度
UPDATE `t800_uv_inexus`.`t_device_init_entry`
SET `scan_speed` = '{"0": "slow", "1": "medium", "2": "fast", "1001": "fastest"}'
WHERE `id` IN (1,2,3,4,5,6,7,8);

-- 更新 id 为 9 至 17 的设备的扫描速度
UPDATE `t800_uv_inexus`.`t_device_init_entry`
SET `scan_speed` = '{"0": "fast", "1": "medium", "2": "slow", "3": "slowest", "1001": "fastest"}'
WHERE `id` IN (9,10,11,12,13,14,15,16,17);

-- 删除 t_device_type_info 表的 scan_speed 字段
ALTER TABLE `t800_uv_inexus`.`t_device_type_info`
DROP COLUMN `scan_speed`;
