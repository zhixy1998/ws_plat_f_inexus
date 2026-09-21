-- 测量任务表 新增分析样品选中状态JSON字段(存分析数据id+是否选中)
ALTER TABLE `f_t_measure_task`
    ADD COLUMN `analysis_sample` json NULL COMMENT '分析样品选中状态(分析数据id+是否选中)' AFTER `table_info`;
