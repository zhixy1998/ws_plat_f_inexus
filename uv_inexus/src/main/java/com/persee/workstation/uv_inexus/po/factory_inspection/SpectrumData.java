package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */


/**
 * 频谱数据实体类，用于存储和管理频谱相关的数据信息。
 * 该类使用JPA注解映射到数据库表，包含频谱数据的基本属性。
 */
@Entity // 标记这是一个JPA实体类，将被映射到数据库表
@Data // 指定映射的数据库表名为"spectrum_data"
@Table(name = "spectrum_data")


    // 频谱数据的唯一标识符
public class SpectrumData { // 标记此字段为主键
    @Id // 指定对应数据库表中的"id"字段
    @TableId("id") // API文档注解，说明此字段为id
    @ApiModelProperty(value = "id") // 主键ID，使用String类型存储
    private String id;


    // 任务标识符，关联到具体的检测任务
 // 指定对应数据库表中的"task_id"字段
    @TableField("task_id") // API文档注解，说明此字段为任务ID
    @ApiModelProperty(value = "任务ID") // 任务ID，使用String类型存储
    private String taskId;


    // 检测项标识符，关联到具体的检测项目
 // 指定对应数据库表中的"item_id"字段
    @TableField("item_id") // API文档注解，说明此字段为检测项ID
    @ApiModelProperty(value = "检测项ID") // 检测项ID，使用String类型存储
    private String itemId;


    // 频谱数据的具体内容，可能以字符串形式存储
 // 指定对应数据库表中的"spectrum_data"字段
    @TableField("spectrum_data") // API文档注解，说明此字段为频谱数据
    @ApiModelProperty(value = "频谱数据") // 频谱数据内容，使用String类型存储
    private String spectrumData;


    // 测量时间，记录频谱数据采集的具体时间
 // 指定对应数据库表中的"measurement_time"字段
    @TableField( "measurement_time") // API文档注解，说明此字段为测量时间
    @ApiModelProperty(value = "测量时间") // 测量时间，使用Date类型存储
    private Date measurementTime;


    // 记录数据创建的时间
 // 指定对应数据库表中的"create_time"字段
    @TableField( "create_time") // API文档注解，说明此字段为创建时间
    @ApiModelProperty(value = "创建时间") // 创建时间，使用Date类型存储
    private Date createTime;


    // 记录数据最后更新的时间
 // 指定对应数据库表中的"update_time"字段
    @TableField("update_time") // API文档注解，说明此字段为更新时间
    @ApiModelProperty(value = "更新时间") // 更新时间，使用Date类型存储
    private Date updateTime;
}
