package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import springfox.documentation.spring.web.json.Json;

/**
 * 测量标准表(TMeasureStandard)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasureStandard extends Model<TMeasureStandard> {
//标准ID
    private String id;
//标准名称
    private String standardName;
//标准类型（内置/自定义）
    private String standardType;
//行业
    private String industry;
//测试类型（如：紫外-定量）
    private String testType;
//标准号
    private String standardCode;
//检测方法全称
    private String detectionMethodName;
//检测项目
    private String detectionItem;
//测量参数信息
    private Json measurementParamInfo;
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

