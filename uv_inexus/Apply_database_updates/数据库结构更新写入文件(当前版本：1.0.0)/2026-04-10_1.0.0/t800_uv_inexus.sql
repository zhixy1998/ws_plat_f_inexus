ALTER TABLE `t800_uv_inexus`.`t_ins_info_temp`
ADD COLUMN `xml_key` varchar(255) NULL COMMENT '仪器连接的xmlKey' AFTER `device_model_name`;

ALTER TABLE `t800_uv_inexus`.`t_ins_info_temp`
ADD COLUMN `device_mac` varchar(255) NULL COMMENT '仪器的mac地址' AFTER `device_ip`;