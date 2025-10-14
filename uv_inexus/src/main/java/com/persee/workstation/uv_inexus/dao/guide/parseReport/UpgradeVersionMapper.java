/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dao.guide.parseReport;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.persee.workstation.uv_inexus.po.guide.parseReport.UpgradeVersion;

@Mapper
public interface UpgradeVersionMapper {

    // 插入新版本号
    @Insert("INSERT INTO t_report_upgrade_version (version, update_description, create_time) " +
            "VALUES (#{version}, #{updateDescription}, #{createTime})")
    void insert(UpgradeVersion version);

    // 查询最新版本号
    @Select("SELECT * FROM t_report_upgrade_version " +
            "ORDER BY create_time DESC LIMIT 1")
    UpgradeVersion selectLatestVersion();

    // 新增：检查版本号是否已存在
    @Select("SELECT COUNT(*) FROM t_report_upgrade_version WHERE version = #{version}")
    int existsByVersion(@Param("version") String version);
}