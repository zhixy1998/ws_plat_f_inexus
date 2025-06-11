package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;

/**
 * 项目表(TMeasureProject)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasureProject extends Model<TMeasureProject> {
//项目uuid
    private String id;
//项目名称
    private String projectName;
//创建时间
    private Date createTime;
//创建人Key
    private String creatorKey;
//备注
    private String remark;
//预留字段1
    private String field1;
//预留字段2
    private String field2;
//预留字段3
    private String field3;
//预留字段4
    private String field4;
//预留字段5
    private String field5;
}

