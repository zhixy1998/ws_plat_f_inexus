/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 草稿数据表实体类
 */
@Data
@TableName("f_t_measure_draft")
public class FMeasureDraft {

    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;          // 草稿ID

    @TableField("draft_name")
    private String draftName;   // 草稿名称

    @TableField("method_id")
    private String methodId;    // 方法ID

}
