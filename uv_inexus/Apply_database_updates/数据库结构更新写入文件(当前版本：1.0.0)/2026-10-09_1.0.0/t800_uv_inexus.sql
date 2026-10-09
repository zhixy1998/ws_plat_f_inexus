-- ================================================================
-- T800 UV 简洁模式：清理 f_t_measure_analysis_data 重复脏数据（DELETE 法提速版）
-- 库：t800_uv_inexus_concise    版本：MySQL 5.7
-- 提速点：删前摘二级索引 / 批次加大到 5 万 / 仅保留删除必需的图片表索引
-- 注意：DELIMITER $$ 到 DELIMITER ; 必须整块一次执行，CALL 单独执行
-- ================================================================
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
USE t800_uv_inexus_concise;
SET SESSION unique_checks = 0;
SET SESSION foreign_key_checks = 0;

-- ---------------------------------------------------------------
-- 步骤 1：清理上次运行的残余
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS tmp_work;
DROP TABLE IF EXISTS tmp_has_img;
DROP TABLE IF EXISTS tmp_dup_keys;
DROP TABLE IF EXISTS tmp_del_analysis;
DROP TABLE IF EXISTS chk_del;
DROP PROCEDURE IF EXISTS sp_clean_dup_analysis;

-- ---------------------------------------------------------------
-- 步骤 2：图片表索引（删除 JOIN 必需；幂等，已存在自动跳过）
-- ---------------------------------------------------------------
SET @ddl = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'f_t_measure_analysis_image'
      AND index_name = 'idx_analysis_id') > 0,
  'SELECT ''idx_analysis_id 已存在'' AS msg',
  'ALTER TABLE f_t_measure_analysis_image ADD INDEX idx_analysis_id (analysis_id)'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------
-- 步骤 3：摘掉宽表的 App 索引（删除期间不再维护它们；删完步骤 8 重建）
--   不存在会自动跳过
-- ---------------------------------------------------------------
SET @ddl = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'f_t_measure_analysis_data'
      AND index_name = 'idx_dup_check') > 0,
  'ALTER TABLE f_t_measure_analysis_data DROP INDEX idx_dup_check',
  'SELECT ''idx_dup_check 不存在，无需摘除'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'f_t_measure_analysis_data'
      AND index_name = 'idx_task_sample') > 0,
  'ALTER TABLE f_t_measure_analysis_data DROP INDEX idx_task_sample',
  'SELECT ''idx_task_sample 不存在，无需摘除'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------
-- 步骤 4：物化窄工作表（唯一一次大表扫描，Sending data 正常别中断）
-- 判重键：task_id+sample_key+measure_data_id+analysis_number
--         +analysis_type+analysis_name+data_version
-- ---------------------------------------------------------------
CREATE TABLE tmp_work (
  id        varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  k         char(32)    CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  is_delete char(1)     CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0',
  has_image tinyint     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_k (k, is_delete, has_image)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO tmp_work (id, k, is_delete)
SELECT id,
       MD5(CONCAT_WS('|',
             IFNULL(task_id, ''), IFNULL(sample_key, ''), IFNULL(measure_data_id, ''),
             IFNULL(analysis_number, ''), IFNULL(analysis_type, ''),
             IFNULL(analysis_name, ''), IFNULL(data_version, ''))),
       IFNULL(is_delete, '0')
FROM f_t_measure_analysis_data;

CREATE TABLE tmp_has_img (
  analysis_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (analysis_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT IGNORE INTO tmp_has_img (analysis_id)
SELECT DISTINCT analysis_id
FROM f_t_measure_analysis_image
WHERE analysis_id IS NOT NULL AND analysis_id <> '';

UPDATE tmp_work w
JOIN tmp_has_img h ON h.analysis_id = w.id
SET w.has_image = 1;

-- ---------------------------------------------------------------
-- 步骤 5：生成清单 + 备份待删行（备份表保留作回滚保险）
-- ---------------------------------------------------------------
CREATE TABLE tmp_dup_keys (
  k       char(32)    CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  cnt     int         NOT NULL,
  keep_id varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (k)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO tmp_dup_keys (k, cnt, keep_id)
SELECT k,
       COUNT(*) AS cnt,
       COALESCE(
         MIN(CASE WHEN is_delete = '0' AND has_image = 1 THEN id END),
         MIN(CASE WHEN is_delete = '0' THEN id END),
         MIN(CASE WHEN has_image = 1 THEN id END),
         MIN(id)
       ) AS keep_id
FROM tmp_work
GROUP BY k
HAVING COUNT(*) > 1;

CREATE TABLE tmp_del_analysis (
  id   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  done tinyint     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_done (done)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO tmp_del_analysis (id)
SELECT w.id
FROM tmp_work w
JOIN tmp_dup_keys d ON d.k = w.k
WHERE w.id <> d.keep_id;

CREATE TABLE bak_del_analysis AS
SELECT a.* FROM f_t_measure_analysis_data a
JOIN tmp_del_analysis t ON t.id = a.id;

CREATE TABLE bak_del_analysis_image AS
SELECT i.* FROM f_t_measure_analysis_image i
JOIN tmp_del_analysis t ON t.id = i.analysis_id;

-- ---------------------------------------------------------------
-- 步骤 6：分批删除（批次 5 万；中断可重复 CALL，幂等自愈）
-- ---------------------------------------------------------------
DELIMITER $$
CREATE PROCEDURE sp_clean_dup_analysis(IN p_batch INT)
BEGIN
  DECLARE v_batch INT DEFAULT 1;
  WHILE v_batch > 0 DO
    DROP TEMPORARY TABLE IF EXISTS tmp_batch;
    CREATE TEMPORARY TABLE tmp_batch (
      id varchar(64) NOT NULL,
      PRIMARY KEY (id)
    ) ENGINE = InnoDB;

    INSERT INTO tmp_batch (id)
    SELECT id FROM tmp_del_analysis WHERE done = 0 LIMIT p_batch;
    SET v_batch = ROW_COUNT();

    IF v_batch > 0 THEN
      DELETE i FROM f_t_measure_analysis_image i
      JOIN tmp_batch b ON b.id = i.analysis_id;

      DELETE a FROM f_t_measure_analysis_data a
      JOIN tmp_batch b ON b.id = a.id;

      UPDATE tmp_del_analysis d
      JOIN tmp_batch b ON b.id = d.id
      SET d.done = 1;
    END IF;
  END WHILE;
END$$
DELIMITER ;

CALL sp_clean_dup_analysis(50000);

DROP PROCEDURE IF EXISTS sp_clean_dup_analysis;

-- ---------------------------------------------------------------
-- 步骤 7：重建宽表的 App 索引
-- ---------------------------------------------------------------
ALTER TABLE f_t_measure_analysis_data
  ADD INDEX idx_dup_check (measure_data_id, analysis_number),
  ADD INDEX idx_task_sample (task_id, sample_key);

-- ---------------------------------------------------------------
-- 步骤 8：收尾（保留 bak_del_* 备份表）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS tmp_work;
DROP TABLE IF EXISTS tmp_has_img;
DROP TABLE IF EXISTS tmp_dup_keys;
DROP TABLE IF EXISTS tmp_del_analysis;
DROP TABLE IF EXISTS chk_del;
DROP TEMPORARY TABLE IF EXISTS tmp_batch;
SET SESSION unique_checks = 1;
SET SESSION foreign_key_checks = 1;
-- OPTIMIZE TABLE f_t_measure_analysis_data;   -- 空间回收，业务空闲期再跑

-- 回滚（发现删错时按 id 补回，不会产生新重复）：
-- INSERT IGNORE INTO f_t_measure_analysis_data SELECT * FROM bak_del_analysis;
-- INSERT IGNORE INTO f_t_measure_analysis_image SELECT * FROM bak_del_analysis_image;
-- 确认无误、观察一个周期后再删备份：
-- DROP TABLE IF EXISTS bak_del_analysis;
-- DROP TABLE IF EXISTS bak_del_analysis_image;