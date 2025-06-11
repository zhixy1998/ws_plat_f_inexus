package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

/**
 * 前处理信息表(TMeasurePreprocessInfo)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasurePreprocessInfo extends Model<TMeasurePreprocessInfo> {
//前处理信息ID
    private String id;
//测量标准uuid
    private String standardId;
//前处理描述
    private String preprocessDescription;
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

