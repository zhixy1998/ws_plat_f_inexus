package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

/**
 * 分析数据表(TMeasureAnalysisData)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasureAnalysisData extends Model<TMeasureAnalysisData> {
//分析数据uuid
    private String id;
//测量任务uuid
    private String taskUuid;
//分析参数
    private String analysisParam;
//分析结果
    private String analysisResult;
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
//预留字段6
    private String field6;
//预留字段7
    private String field7;
//预留字段8
    private String field8;
//预留字段9
    private String field9;
//预留字段10
    private String field10;
}

