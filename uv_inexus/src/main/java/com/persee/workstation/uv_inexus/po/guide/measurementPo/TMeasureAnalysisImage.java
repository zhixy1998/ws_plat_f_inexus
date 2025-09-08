package com.persee.workstation.uv_inexus.po.guide.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("t_measure_analysis_image")
public class TMeasureAnalysisImage {

    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;  // 图片ID

    @TableField("analysis_id")
    private String analysisId;   // 关联的分析数据ID

    @TableField("image_base64")
    private String imageBase64;  // 图片的Base64编码

    @TableField("image_name")
    private String imageName;    // 图片名称

    @TableField("image_type")
    private String imageType;    // 图片类型(jpg/png等)

    @TableField("create_time")
    private Date createTime;     // 创建时间

    @TableField("remarks")
    private String remarks;      // 备注信息
}
