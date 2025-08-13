package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_measure_sample_head")
public class SampleHeadTemplatePO extends BasePO {

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
