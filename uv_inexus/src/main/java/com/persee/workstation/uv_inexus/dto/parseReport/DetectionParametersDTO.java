package com.persee.workstation.uv_inexus.dto.parseReport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 检测参数及子表字段详情DTO（含完整关联数据）
 */
@ApiModel("检测参数及子表字段详情DTO（含完整关联数据）")
@Data
public class DetectionParametersDTO {
    @ApiModelProperty("测量参数（格式化文本）")
    private String detectionParameters;

    // ========== 主表基础信息 ==========
    @ApiModelProperty("标准标识")
    private String standardIdentity;
    @ApiModelProperty("检测类型")
    private String detectionType;
    @ApiModelProperty("检测仪器")
    private String detectionInstrument;
    @ApiModelProperty("标准号")
    private String standardNumber;
    @ApiModelProperty("标准名称")
    private String standardName;
    @ApiModelProperty("检测项目")
    private String detectionItem;
    @ApiModelProperty("发布状态")
    private String releaseStatus;

    // ========== 关联子表汇总信息（原有） ==========
    @ApiModelProperty("前处理表-汇总描述（如：3种前处理方式）")
    private String preTreatmentDesc;
    @ApiModelProperty("质量控制表-汇总类型（如：空白试验；平行样）")
    private String qcType;
    @ApiModelProperty("计算公式表-汇总结果表示（如：结果1、结果2）")
    private String resultExpression;

    // ========== 关联子表完整数据列表 ==========
    @ApiModelProperty("前处理表-完整数据列表")
    private List<StandardPretreatmentSubDTO> preTreatmentList;
    @ApiModelProperty("质量控制表-完整数据列表")
    private List<QualityControlSubDTO> qualityControlList;
    @ApiModelProperty("计算公式表-完整数据列表")
    private List<CalculationFormulaSubDTO> calculationFormulaList;
    @ApiModelProperty("修约规则表-完整数据列表")
    private List<RoundingRulesSubDTO> roundingRulesList;

    // ========== 静态内部子DTO（避免类名冲突） ==========
    @ApiModel("前处理表子DTO")
    @Data
    public static class StandardPretreatmentSubDTO {
        @ApiModelProperty("检测ID（关联主表）")
        private String detectionId;
        @ApiModelProperty("前处理描述")
        private String pretreatmentDescription;
    }

    @ApiModel("质量控制表子DTO")
    @Data
    public static class QualityControlSubDTO {
        @ApiModelProperty("检测ID（关联主表）")
        private String detectionId;
        @ApiModelProperty("质控类型")
        private String controlType;
        @ApiModelProperty("质控描述")
        private String controlDescription;
        @ApiModelProperty("质控公式")
        private String controlFormula;
    }

    @ApiModel("计算公式表子DTO")
    @Data
    public static class CalculationFormulaSubDTO {
        @ApiModelProperty("主键ID")
        private String  id;
        @ApiModelProperty("检测ID（关联主表）")
        private String detectionId;
        @ApiModelProperty("结果标识")
        private String resultIdentity;
        @ApiModelProperty("结果名称")
        private String resultName;
        @ApiModelProperty("单位")
        private String unit;
        @ApiModelProperty("输入参数1名称")
        private String inputParam1Name;
        @ApiModelProperty("输入参数1标识")
        private String inputParam1Identity;
        @ApiModelProperty("参数1单位")
        private String inputParam1Unit;
        @ApiModelProperty("是否为样品输入参数1")
        private String isSampleAttribute1;
        @ApiModelProperty("软件绑定标识1")
        private String curveIdentifier1;
        @ApiModelProperty("输入参数2名称")
        private String inputParam2Name;
        @ApiModelProperty("输入参数2标识")
        private String inputParam2Identity;
        @ApiModelProperty("参数2单位")
        private String inputParam2Unit;
        @ApiModelProperty("是否为样品输入参数2")
        private String isSampleAttribute2;
        @ApiModelProperty("软件绑定标识2")
        private String curveIdentifier2;
        @ApiModelProperty("输入参数3名称")
        private String inputParam3Name;
        @ApiModelProperty("输入参数3标识")
        private String inputParam3Identity;
        @ApiModelProperty("参数3单位")
        private String inputParam3Unit;
        @ApiModelProperty("是否为样品输入参数3")
        private String isSampleAttribute3;
        @ApiModelProperty("软件绑定标识3")
        private String curveIdentifier3;
        @ApiModelProperty("输入参数4名称")
        private String inputParam4Name;
        @ApiModelProperty("输入参数4标识")
        private String inputParam4Identity;
        @ApiModelProperty("参数4单位")
        private String inputParam4Unit;
        @ApiModelProperty("是否为样品输入参数4")
        private String isSampleAttribute4;
        @ApiModelProperty("软件绑定标识4")
        private String curveIdentifier4;
        @ApiModelProperty("输入参数5名称")
        private String inputParam5Name;
        @ApiModelProperty("输入参数5标识")
        private String inputParam5Identity;
        @ApiModelProperty("参数5单位")
        private String inputParam5Unit;
        @ApiModelProperty("是否为样品输入参数5")
        private String isSampleAttribute5;
        @ApiModelProperty("软件绑定标识5")
        private String curveIdentifier5;
        @ApiModelProperty("输入参数6名称")
        private String inputParam6Name;
        @ApiModelProperty("输入参数6标识")
        private String inputParam6Identity;
        @ApiModelProperty("参数6单位")
        private String inputParam6Unit;
        @ApiModelProperty("是否为样品输入参数6")
        private String isSampleAttribute6;
        @ApiModelProperty("软件绑定标识6")
        private String curveIdentifier6;
        @ApiModelProperty("输入参数7名称")
        private String inputParam7Name;
        @ApiModelProperty("输入参数7标识")
        private String inputParam7Identity;
        @ApiModelProperty("参数7单位")
        private String inputParam7Unit;
        @ApiModelProperty("是否为样品输入参数7")
        private String isSampleAttribute7;
        @ApiModelProperty("软件绑定标识7")
        private String curveIdentifier7;
        @ApiModelProperty("输入参数8名称")
        private String inputParam8Name;
        @ApiModelProperty("输入参数8标识")
        private String inputParam8Identity;
        @ApiModelProperty("参数8单位")
        private String inputParam8Unit;
        @ApiModelProperty("是否为样品输入参数8")
        private String isSampleAttribute8;
        @ApiModelProperty("软件绑定标识8")
        private String curveIdentifier8;
        @ApiModelProperty("输入参数9名称")
        private String inputParam9Name;
        @ApiModelProperty("输入参数9标识")
        private String inputParam9Identity;
        @ApiModelProperty("参数9单位")
        private String inputParam9Unit;
        @ApiModelProperty("是否为样品输入参数9")
        private String isSampleAttribute9;
        @ApiModelProperty("软件绑定标识9")
        private String curveIdentifier9;
        @ApiModelProperty("输入参数10名称")
        private String inputParam10Name;
        @ApiModelProperty("输入参数10标识")
        private String inputParam10Identity;
        @ApiModelProperty("参数10单位")
        private String inputParam10Unit;
        @ApiModelProperty("是否为样品输入参数10")
        private String isSampleAttribute10;
        @ApiModelProperty("软件绑定标识10")
        private String curveIdentifier10;
        @ApiModelProperty("公式文本")
        private String formulaText;
        @ApiModelProperty("公式图片")
        private String formulaImage;
        @ApiModelProperty("检出限")
        private String detectionLimit;
    }

    @ApiModel("修约规则表子DTO")
    @Data
    public static class RoundingRulesSubDTO {
        @ApiModelProperty("检测ID（关联主表）")
        private String detectionId;
        @ApiModelProperty("结果标识")
        private String resultIdentity;
        @ApiModelProperty("修约类型")
        private String roundingType;
        @ApiModelProperty("修约条件")
        private String roundingCondition;
        @ApiModelProperty("保留小数位数")
        private Integer decimalPlaces;
    }
}