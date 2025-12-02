/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 样品模板表头实体类
 */
@Data
@TableName("f_t_measure_sample_head")
public class FMeasureSampleHead extends BasePO {

    @TableField("template_name")
    @ApiModelProperty("模板名称")
    private String templateName;

    @TableField("template_desc")
    @ApiModelProperty("模板描述")
    private String templateDesc;

    @TableField("measure_type")
    @ApiModelProperty("测量类型")
    private String measureType;

    @TableField("heads")
    @ApiModelProperty("模板表头信息，后端存储字段")
    private String heads;

    @TableField(exist = false)
    @ApiModelProperty("模板表头信息，包含全部表头")
    private List<HeadVO> templateHeads;
}
