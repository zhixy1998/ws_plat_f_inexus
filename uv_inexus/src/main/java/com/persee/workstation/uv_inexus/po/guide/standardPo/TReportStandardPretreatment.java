/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
