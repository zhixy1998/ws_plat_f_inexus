UPDATE `t800_uv_inexus`.`t_device_type_info` SET
`scan_speed` = '{\"0\": \"slow\", \"1\": \"medium\", \"2\": \"fast\", \"1001\": \"fastest\"}'
WHERE `id` = 1;

UPDATE `t800_uv_inexus`.`t_device_type_info` SET
`scan_speed` = '{\"0\": \"fast\", \"1\": \"medium\", \"2\": \"slow\", \"3\": \"slowest\", \"1001\": \"fastest\"}'
WHERE `id` = 2;


update `t800_uv_inexus`.`t_device_type_info` SET
`min_wavelength` = '185'
where `id` = 2;