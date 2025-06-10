package com.persee.workstation.uv_inexus.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePO implements Serializable {

    @TableId(value = "id",type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(value = "create_user")
    private String createBy;

    @TableField(value = "update_user")
    private String updateBy;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updated;

}
