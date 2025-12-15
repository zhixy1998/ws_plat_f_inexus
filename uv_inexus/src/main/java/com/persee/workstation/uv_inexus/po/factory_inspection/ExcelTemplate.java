package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("excel_template")
public class ExcelTemplate extends BasePO {
    @TableId
    @ApiModelProperty("模板ID")

    private String id;
    /**
     * 模板编码
     */
    @ApiModelProperty("模板编码")
    @TableField("template_code")
    private String templateCode;
    /**
     * 模板名称
     */
    @ApiModelProperty("模板名称")
    @TableField("template_name")
    private String templateName;
    /**
     * 列配置
     */
    @ApiModelProperty("列配置")
    @TableField("column_config")
    private String columnConfig; // JSON格式的列配置
    /**
     * 状态1：正常，0：禁用
     */
    @ApiModelProperty("状态")
    @TableField("status")
    private Integer status;
    /**
     * 适用于哪个型号的仪器
     */
    @TableField(value = "model")
    @ApiModelProperty(value = "适用于哪个型号的仪器")
    private String model;


    /**
     * 模板类型
     */
    @TableField(value = "category")
    @ApiModelProperty(value = "模板类型")
    private String category;

    /**
     * 最近一次使用时间
     */
    @TableField(value = "last_used")
    @ApiModelProperty(value = "最近一次使用时间")
    private Date lastUsed;

    /**
     * 模板描述
     */
    @TableField(value = "remark")
    @ApiModelProperty(value = "模板描述")
    private String remark;
    /**
     * 父模板id
     */
    @TableField(value = "parent_id")
    @ApiModelProperty(value = "模板描述")
    private String parentId;

    @TableField(value = "use_count")
    @ApiModelProperty(value = "使用次数")
    private Integer useCount;

    // 获取列配置列表
    public List<ColumnConfig> getColumnConfigList() {
        return JSON.parseArray(columnConfig, ColumnConfig.class);
    }

    @Data
    public class ColumnConfig {
        private Integer order;    // 排序
        private String excelHeader;  // Excel表头名
        private String dataType;     // 数据类型:TEXT, DROPDOWN
        private List<String> dropdownOptions; // 下拉框选项列表
    }
}
