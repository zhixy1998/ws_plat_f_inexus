/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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