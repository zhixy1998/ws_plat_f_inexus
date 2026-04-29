-- 测量数据表
CREATE INDEX idx_md_task_sample_serial
ON f_t_measure_data (task_id, sample_key, serial_number);

-- 分析数据表
CREATE INDEX idx_mad_task_sample_anumber
ON f_t_measure_analysis_data (task_id, sample_key, analysis_number);