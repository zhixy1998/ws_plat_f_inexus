UPDATE `t800_uv_inexus`.`t_device_type_info` SET `calibration_type` = '[1, 2, 4]' WHERE `id` = 1;
UPDATE `t800_uv_inexus`.`t_device_type_info` SET  `calibration_type` = '[1, 3, 4]' WHERE `id` = 2;

INSERT INTO `t800_uv_inexus`.`t_ins_calibration_config` (`id`, `calibration_name`, `calibration_type`, `param_list`) VALUES (4, '多点波长校正', 4, '[8, 9]');

ALTER TABLE `t800_uv_inexus`.`t_ins_calibration_param`
ADD COLUMN `table_data` json NULL AFTER `default_value`;
ALTER TABLE `t800_uv_inexus`.`t_ins_calibration_temp`
ADD COLUMN `table_data` json NULL AFTER `param_value`;

INSERT INTO `t800_uv_inexus`.`t_ins_calibration_param` (`id`, `param_name`, `param`, `default_value`, `table_data`, `param_type`) VALUES (8, '表格', 'table', NULL, '{\"head\": [{\"key\": \"name\", \"data\": [{\"dataKey\": \"汞灯\", \"dataValue\": \"mercury\"}, {\"dataKey\": \"镨钕滤光片\", \"dataValue\": \"filter\"}, {\"dataKey\": \"钬溶液\", \"dataValue\": \"holmium\"}, {\"dataKey\": \"氘灯\", \"dataValue\": \"deuterium\"}], \"name\": \"名称\", \"paramType\": \"2\"}, {\"key\": \"standLength\", \"data\": [{\"dataKey\": \"min\", \"dataValue\": \"190\"}, {\"dataKey\": \"max\", \"dataValue\": \"900\"}], \"name\": \"标准波长(nm)\", \"paramType\": \"1\"}, {\"key\": \"realLength\", \"data\": [{\"dataKey\": \"min\", \"dataValue\": \"190\"}, {\"dataKey\": \"max\", \"dataValue\": \"900\"}], \"name\": \"实际波长(nm)\", \"paramType\": \"1\"}, {\"key\": \"luminosity\", \"data\": [{\"dataKey\": \"Abs\", \"dataValue\": \"1\"}, {\"dataKey\": \"T%\", \"dataValue\": \"2\"}, {\"dataKey\": \"Es\", \"dataValue\": \"3\"}, {\"dataKey\": \"Er\", \"dataValue\": \"4\"}], \"name\": \"光度模式\", \"paramType\": \"2\"}, {\"key\": \"gain\", \"data\": [{\"dataKey\": \"1\", \"dataValue\": \"1\"}, {\"dataKey\": \"2\", \"dataValue\": \"2\"}, {\"dataKey\": \"3\", \"dataValue\": \"3\"}, {\"dataKey\": \"4\", \"dataValue\": \"4\"}], \"name\": \"增益\", \"paramType\": \"2\"}, {\"key\": \"lamp\", \"data\": [{\"dataKey\": \"钨灯\", \"dataValue\": \"0\"}, {\"dataKey\": \"氘灯\", \"dataValue\": \"1\"}, {\"dataKey\": \"汞灯\", \"dataValue\": \"2\"}], \"name\": \"光源\", \"paramType\": \"2\"}], \"content\": []}', 3);
INSERT INTO `t800_uv_inexus`.`t_ins_calibration_param` (`id`, `param_name`, `param`, `default_value`, `table_data`, `param_type`) VALUES (9, '谱图', 'graph', NULL, NULL, 4);
