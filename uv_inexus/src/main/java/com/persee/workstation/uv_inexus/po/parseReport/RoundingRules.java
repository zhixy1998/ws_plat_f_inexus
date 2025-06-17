package com.persee.workstation.uv_inexus.po.parseReport;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

// import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_rounding_rules") // 修改了表名注解
public class RoundingRules extends BasePO { // 修改了类名

    @TableField("standard_identity")
    private String standardIdentity; // 标准标识

    @TableField("result_identity")
    private String resultIdentity; // 结果标识

    @TableField("rounding_type")
    private String roundingType; // 修约类型

    @TableField("rounding_condition")
    private String roundingCondition; // 修约条件

    @TableField("decimal_places")
    private Integer decimalPlaces; // 保留小数位数，使用 Integer 对应数据库的 int 类型

    // 如果 BasePO 类已经包含 create_time 和 update_time 字段，可以移除以下字段
    // @TableField("create_time")
    // private LocalDateTime createTime;

    // @TableField("update_time")
    // private LocalDateTime updateTime;
}
