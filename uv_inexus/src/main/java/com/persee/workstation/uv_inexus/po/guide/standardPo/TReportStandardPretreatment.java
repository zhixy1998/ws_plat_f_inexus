package com.persee.workstation.uv_inexus.po.guide.standardPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 标准前处理表
 */
@Data
@TableName("t_report_standard_pretreatment")
public class TReportStandardPretreatment {
    /**
     * 数据唯一标识，使用UUID自动分配
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private Integer id;

    /**
     * 检测标识ID
     */
    @TableField("detection_id")
    private String detectionId;

    /**
     * 前处理描述
     */
    @TableField("pretreatment_description")
    private String pretreatmentDescription;
}
