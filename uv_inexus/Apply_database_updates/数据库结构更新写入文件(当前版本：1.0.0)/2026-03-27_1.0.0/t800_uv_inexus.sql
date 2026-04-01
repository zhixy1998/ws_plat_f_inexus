-- 添加滤色片字段
ALTER TABLE `t800_uv_inexus`.`t_device_init_entry`
ADD COLUMN `filter_wavelengths` JSON COMMENT '滤色片波长点' AFTER `init_entry`;

UPDATE `t800_uv_inexus`.`t_device_init_entry`
SET `filter_wavelengths` = '[760, 532, 389, 350, 225, 184]'
WHERE `dev_type_id` = 2;


UPDATE `t800_uv_inexus`.`t_device_init_entry`
SET `filter_wavelengths` = '[762, 555, 359]'
WHERE `dev_type_id` = 1;
