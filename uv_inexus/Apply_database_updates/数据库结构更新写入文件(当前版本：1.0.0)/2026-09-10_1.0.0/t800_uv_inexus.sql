-- 分析数据表 新增峰搜索参数JSON字段(存峰谷差+峰间距+是否选中)
ALTER TABLE `t_measure_analysis_data`
    ADD COLUMN `peak_search_param` json NULL COMMENT '峰搜索参数(峰谷差+峰间距+是否选中)' AFTER `grain`;