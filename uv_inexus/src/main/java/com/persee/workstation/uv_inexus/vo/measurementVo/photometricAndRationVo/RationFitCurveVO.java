/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class RationFitCurveVO {

    @ApiModelProperty("任务ID")
    private String taskId;

    @ApiModelProperty("方程类型,Abs=f(C)..")
    private String equationType;

    @ApiModelProperty("数据")
    List<JSONObject> dataList;

    @ApiModelProperty("方程k值，从高次到低")
    private List<Double> coefficientList;

    @ApiModelProperty("曲线类型,一次曲线，二次曲线...")
    private String curveType;

    @ApiModelProperty("具体方程")
    private String equation;

    @ApiModelProperty("R值 （标准差, 代表拟合准确率）")
    private String R;

    @ApiModelProperty("R2值 （标准差, 代表拟合准确率）")
    private String R2;

    @ApiModelProperty("标准样品点")
    private List<JSONObject> standPoints;

    @ApiModelProperty("未知样品点")
    private List<JSONObject> unknowPoints;

}
