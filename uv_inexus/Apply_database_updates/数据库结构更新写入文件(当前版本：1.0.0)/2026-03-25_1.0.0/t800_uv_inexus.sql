-- T600/T700 设备：将 key=3 的值改为"极快"，并添加 key=111 也显示"极快"
UPDATE `t800_uv_inexus`.`t_device_type_info` 
SET `scan_speed` = '{"0": "慢速", "1": "中速", "2": "快速", "111": "极快"}'
WHERE `id` = 1;

-- T800 设备：添加 key=111 显示"极快"
UPDATE `t800_uv_inexus`.`t_device_type_info` 
SET `scan_speed` = '{"0": "快速", "1": "中速", "2": "慢速", "3": "最慢", "111": "极快"}' 
WHERE `id` = 2;