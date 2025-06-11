package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

/**
 * 测量方法表(TMeasureMethod)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasureMethod extends Model<TMeasureMethod> {
//方法uuid
    private String id;
//测量标准Key
    private String standardKey;
//前处理Key
    private String preprocessKey;
//多个计算公式Key
    private String multipleCalculationFormulaKey;
//所属项目Key
    private String projectItemKey;
//方法名称
    private String methodName;
//测量类型
    private String measurementType;
//是否共享方法
    private String isSharingMethod;
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

