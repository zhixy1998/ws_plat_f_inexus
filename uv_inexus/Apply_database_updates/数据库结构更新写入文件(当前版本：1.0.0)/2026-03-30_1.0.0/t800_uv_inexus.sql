UPDATE `t800_uv_inexus`.`t_device_type_info`
SET `scan_speed` = JSON_OBJECT(
    '0', '1',
    '1', '2',
    '2', '3',
    '111', '4'
)
WHERE `id` = 1;

-- 更新 T800 的 scan_speed
UPDATE `t800_uv_inexus`.`t_device_type_info`
SET `scan_speed` = JSON_OBJECT(
    '0', '3',
    '1', '2',
    '2', '1',
    '3', '0',
    '111', '4'
)
WHERE `id` = 2;


-- 添加最小换灯波长字段
ALTER TABLE `t800_uv_inexus`.`t_device_type_info`
ADD COLUMN `min_lamp_wavelength` INT(11) NULL COMMENT '最小换灯波长' AFTER `max_wavelength`;

-- 添加最大换灯波长字段
ALTER TABLE `t800_uv_inexus`.`t_device_type_info`
ADD COLUMN `max_lamp_wavelength` INT(11) NULL COMMENT '最大换灯波长' AFTER `min_lamp_wavelength`;

-- 更新 id=1 的数据（T600/T700）
UPDATE `t800_uv_inexus`.`t_device_type_info`
SET `min_lamp_wavelength` = 330,
    `max_lamp_wavelength` = 390
WHERE `id` = 1;

-- 更新 id=2 的数据（T800）
UPDATE `t800_uv_inexus`.`t_device_type_info`
SET `min_lamp_wavelength` = 330,
    `max_lamp_wavelength` = 390
WHERE `id` = 2;