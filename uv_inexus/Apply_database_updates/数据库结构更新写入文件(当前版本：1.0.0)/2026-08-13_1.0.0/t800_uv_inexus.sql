DELIMITER $$

CREATE PROCEDURE replace_admin_in_specific_fields()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE db_name VARCHAR(64);
    DECLARE tbl_name VARCHAR(64);
    DECLARE col_name VARCHAR(64);

    -- 游标：只获取三个数据库中含有指定字段的基表（且字段类型为字符型）
    DECLARE cur CURSOR FOR
        SELECT
            c.TABLE_SCHEMA,
            c.TABLE_NAME,
            c.COLUMN_NAME
        FROM information_schema.COLUMNS c
        JOIN information_schema.TABLES t
            ON c.TABLE_SCHEMA = t.TABLE_SCHEMA
            AND c.TABLE_NAME = t.TABLE_NAME
        WHERE c.TABLE_SCHEMA IN (
            't800_uv_inexus',
            't800_uv_inexus_concise',
            't800_uv_inexus_factory_inspection'
        )
        AND t.TABLE_TYPE = 'BASE TABLE'
        AND c.DATA_TYPE IN ('char','varchar','text','tinytext','mediumtext','longtext')
        AND c.COLUMN_NAME IN (
            'create_user',
            'update_user',
            'username',
            'user_name',
            'method_creator',
            'creator_key',
            'update_by',
            'user',
            'Last_updated_by'
        );

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO db_name, tbl_name, col_name;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 构造 UPDATE 语句，条件为完全匹配 'admin'
        SET @sql = CONCAT(
            'UPDATE `', db_name, '`.`', tbl_name, '` ',
            'SET `', col_name, '` = ''Admin'' ',
            'WHERE `', col_name, '` = ''admin'';'
        );

        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;

    CLOSE cur;
END$$

DELIMITER ;

CALL replace_admin_in_specific_fields();

DROP PROCEDURE replace_admin_in_specific_fields;