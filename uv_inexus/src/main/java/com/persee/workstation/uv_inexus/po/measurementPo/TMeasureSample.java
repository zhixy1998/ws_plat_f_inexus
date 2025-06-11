package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

/**
 * 样品信息表(TMeasureSample)表实体类
 *
 * @author makejava
 * @since 2025-06-11 09:14:55
 */
@Data
@SuppressWarnings("serial")
public class TMeasureSample extends Model<TMeasureSample> {
//样品uuid
    private String id;
//任务id
    private String taskId;
//样品类型
    private String sampleType;
//样品名称
    private String sampleName;
//稀释倍数
    private String dilutionRatio;
//取样量
    private String sampleVolume;
//定容体积
    private String constantVolume;
//标准浓度
    private String normalConcentration;
//样品位置
    private String sampleLocation;
//样品描述
    private String sampleDescription;
//输入数据
    private String inputData;
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

