package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("excel_data_asset")
public class ExcelDataAsset extends BasePO {
    @TableId(value = "id", type = IdType.INPUT)
    @ApiModelProperty("主键id")
    @TableField(value = "id")
    private String  id;
    @ApiModelProperty("模板编号")
    @TableField(value = "template_code")
    private String templateCode;
    @TableField(value = "batch_no")
    private String batchNo;
    @TableField(value = "structured_data")
    private String structuredData; // JSON格式的结构化数据
    @TableField(value = "import_status")
    private String importStatus;

    // 获取结构化数据Map
    public Map<String, Object> getDataMap() {
        Map<String, Object> dataMap = JSON.parseObject(structuredData, Map.class);
        dataMap.put("id", this.id);
        return dataMap;
    }

}

