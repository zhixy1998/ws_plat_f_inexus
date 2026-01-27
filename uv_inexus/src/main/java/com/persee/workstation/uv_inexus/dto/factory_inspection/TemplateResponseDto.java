package com.persee.workstation.uv_inexus.dto.factory_inspection;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
public class TemplateResponseDto {
    /**
     * 模板编码
     */
    @ApiModelProperty("模板编码")
    private String templateCode;
    /**
     * 模板名称
     */
    @ApiModelProperty("模板名称")
    private String templateName;

    /**
     * 模板类型
     */
    @ApiModelProperty(value = "模板类型")
    private String category;
    /**
     * 最近一次使用时间
     */
    @ApiModelProperty(value = "最近一次使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    //格式化成yyyy-MM-dd HH:mm:ss
    private Date lastUsed;

    /**
     * 模板描述
     */
    @ApiModelProperty(value = "模板描述")
    private String remark;
    @ApiModelProperty(value = "使用次数")
    private Integer useCount;
}
