package com.persee.workstation.uv_inexus.po.parseReport;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_report_upgrade_version")
public class UpgradeVersion {
    private Long id;

    @TableField("version")
    private String version; // 版本号（如v2.1.3）

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间

    @TableField("update_description")
    private String updateDescription; // 上传的文件名
}