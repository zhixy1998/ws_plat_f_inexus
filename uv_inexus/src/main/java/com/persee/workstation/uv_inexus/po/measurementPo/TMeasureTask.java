package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;

/**
 * 测量任务表(TMeasureTask)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasureTask extends Model<TMeasureTask> {
//任务uuid
    private String id;
//用户ID
    private String userId;
//任务名称
    private String taskName;
//创建时间
    private Date createTime;
//采集起始时间
    private Date collectionStartTime;
//方法Key
    private String methodKey;
//所属项目Key
    private String projectItemKey;
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

