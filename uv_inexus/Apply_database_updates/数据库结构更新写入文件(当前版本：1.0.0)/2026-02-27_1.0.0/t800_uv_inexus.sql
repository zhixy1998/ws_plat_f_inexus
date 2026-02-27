
ALTER TABLE `t800_uv_inexus`.`t_measure_sample`
ADD COLUMN `parallel_group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '平行样组id' AFTER `sample_name`;