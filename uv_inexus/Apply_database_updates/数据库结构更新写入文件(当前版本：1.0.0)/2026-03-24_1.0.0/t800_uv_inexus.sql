UPDATE t_device_type_info
SET work_time_count = '{"0": "慢速", "1": "中速", "2": "快速", "3": "极快"}'
WHERE dev_type = 'T600/T700';

UPDATE t_device_type_info
SET work_time_count = '{"0": "快速", "1": "中速", "2": "慢速", "3": "最慢"}'
WHERE dev_type = 'T800';