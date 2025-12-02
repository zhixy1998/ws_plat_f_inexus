/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_measure_analysis_image")
public class TMeasureAnalysisImage {

    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;  // 图片ID

    @TableField("analysis_id")
    private String analysisId;   // 关联的分析数据ID

    @TableField("image_base64")
    private byte[] imageBase64;  // 图片的Base64编码

    @TableField("image_name")
    private String imageName;    // 图片名称

    @TableField("image_type")
    private String imageType;    // 图片类型(jpg/png等)

    @TableField("create_time")
    private LocalDateTime createTime;     // 创建时间

    @TableField("remarks")
    private String remarks;      // 备注信息
}
