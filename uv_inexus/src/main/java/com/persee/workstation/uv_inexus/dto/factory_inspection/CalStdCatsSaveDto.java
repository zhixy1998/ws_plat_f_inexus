/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

@ApiModel("标准物质分类")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalStdCatsSaveDto {
    /**
     * 标准物质ID
     */
    @ApiModelProperty(value="标准物质ID")
    private String id;

    /**
     * 标准物质名称
     */
    @ApiModelProperty(value="标准物质名称")
    @Size(max = 32,message = "标准物质名称长度不能大于32")
    private String name;

    /**
     * 标准物质描述
     */
    @ApiModelProperty(value="标准物质描述")
    @Size(max = 128,message = "标准物质描述长度不能大于128")
    private String remark ;

    /**
     * 特征值
     */
    @ApiModelProperty(value="特征值")
    //增加校验字符串用,分隔开来，每一个都得是能转为数字
    private String characteristicValue;

    /**
     * 标准值
     */
    @ApiModelProperty(value="标准值")
    private String standardValue;
    /**
     * 修改者
     */
    @ApiModelProperty(value="修改者")
    private String updateBy;
    /*
    * 标准值单位
    * */
    @ApiModelProperty(value="标准值单位")
    private String standardUnit;

}