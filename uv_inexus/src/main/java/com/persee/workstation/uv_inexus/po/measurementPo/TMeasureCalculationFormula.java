package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

/**
 * 计算公式表(TMeasureCalculationFormula)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasureCalculationFormula extends Model<TMeasureCalculationFormula> {
//计算公式ID
    private String id;
//测量标准uuid
    private String standardId;
//结果名称
    private String resultName;
//输入参数1
    private String inputParam1;
//输入参数2
    private String inputParam2;
//输入参数3
    private String inputParam3;
//输入参数4
    private String inputParam4;
//输入参数5
    private String inputParam5;
//输入参数6
    private String inputParam6;
//输入参数7
    private String inputParam7;
//输入参数8
    private String inputParam8;
//输入参数9
    private String inputParam9;
//输入参数10
    private String inputParam10;
//计算公式
    private String calculationFormula;
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

