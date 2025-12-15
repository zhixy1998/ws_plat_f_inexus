package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 任务检测项数据传输对象
 * 用于工厂检查任务中检测项相关信息的传输
 */
@ApiModel("任务检测项DTO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCheckItemDto {

    /**
     * 唯一任务标识符
     * 用于标识特定的检查任务
     */
    @ApiModelProperty("唯一任务标识符")
    private String taskId;

    /**
     * 任务名称
     * 描述检查任务的名称信息
     */
    @ApiModelProperty("任务名称")
    private String taskName;

    /**
     * 模板标识符
     * 关联的任务模板唯一标识
     */
    @ApiModelProperty("模板标识符")
    private String templateId;

    /**
     * 模板名称
     * 关联的任务模板名称
     */
    @ApiModelProperty("模板名称")
    private String templateName;

    /**
     * 检测项列表
     * 包含该任务下的所有检测项目
     */
    @ApiModelProperty("检测项列表")
    private List<CheckItem> checkItems;

    /**
     * 检测项
     * 代表任务中的单个检测项目
     */
    @ApiModel("检测项")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckItem {

        /**
         * 检测项ID
         * 唯一标识一个检测项目
         */
        @ApiModelProperty("检测项id")
        private String itemId;

        /**
         * 检测项名称
         * 检测项目的显示名称
         */
        @ApiModelProperty("检测项名称")
        private String itemName;

        /**
         * 分组名称
         * 用于归类检测项，例如"波长示值误差与重复性检验"
         */
        @ApiModelProperty("分组名称（如'波长示值误差与重复性检验'）")
        private String groupName;

        /**
         * 选中的校零物质
         * 在检测过程中用于校准零点的标准物质
         */
        @ApiModelProperty("选中的校零物质")
        private Standard zeroStandard;

        /**
         * 选中的测量物质
         * 在检测过程中用于实际测量的标准物质
         */
        @ApiModelProperty("选中的测量物质")
        private Standard measurementStandard;

        /**
         * 校零物质选项列表
         * 可供选择的校零标准物质列表
         */
        @ApiModelProperty("校零物质选项列表")
        private List<Standard> zeroStandards;

        /**
         * 测量物质选项列表
         * 可供选择的测量标准物质列表
         */
        @ApiModelProperty("测量物质选项列表")
        private List<Standard> measurementStandards;

        /**
         * 状态
         * 检测项的当前状态，如"已完成"、"未完成"、"待检查"
         */
        @ApiModelProperty("状态（如'已完成'、'未完成'、'待检查'）")
        private String status;

        /**
         * 检查结果
         * 检测项的执行结果，如"通过"、"失败"、"无结果"
         */
        @ApiModelProperty("检查结果（如'通过'、'失败'、'无结果'）")
        private String result;

        /**
         * 执行时间
         * 检测项实际执行的时间，采用ISO 8601格式
         */
        @ApiModelProperty("执行时间（ISO 8601格式）")
        private Date executionTime;
    }

    /**
     * 标准物
     * 用于检测过程中的标准物质信息
     */
    @ApiModel("标准物")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Standard {

        /**
         * 标准物标识符
         * 唯一标识一个标准物质
         */
        @ApiModelProperty("标准物标识符")
        private String standardId;

        /**
         * 标准物名称
         * 标准物质的显示名称
         */
        @ApiModelProperty("标准物名称")
        private String standardName;
    }
}