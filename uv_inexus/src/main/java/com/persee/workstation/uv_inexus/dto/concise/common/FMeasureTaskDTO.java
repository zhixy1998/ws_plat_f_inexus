package com.persee.workstation.uv_inexus.dto.concise.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FMeasureTaskDTO {
    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("方法名称")
    private String methodName;
    // 用户id
    @ApiModelProperty("用户id")
    private String userId;

    // 任务名称
    @ApiModelProperty("任务名称")
    private String taskName;

    // 创建时间
    @ApiModelProperty(" 创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 方法Key
    @ApiModelProperty("方法Key")
    private String methodKey;

    // 样品Key 由TaskSpecimen类转JSONString
    @ApiModelProperty("样品Key 由TaskSpecimen类转JSONString")
    private String sampleKey;

    // 测量类型
    @ApiModelProperty("测量类型")
    private String measureType;

    // 所属项目Key
    @ApiModelProperty("所属项目Key")
    private String projectItemKey;

    // 校零参数
    @ApiModelProperty("校零参数")
    private String zeroParam;

    // 拟合参数
    @ApiModelProperty("拟合参数")
    private String fittingParam;

    //  最后保存时间
    @ApiModelProperty("最后保存时间")
    private LocalDateTime endSaveTime;

    // 序列号
    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    // 图片数据
    @ApiModelProperty("图片数据")
    private String imageData;

    // 是否保存(0.未保存,1.已保存)
    @ApiModelProperty("是否保存(0.未保存,1.已保存)")
    private String isSave;

    // 是否删除
    @ApiModelProperty("是否删除")
    private String isDelete;


}
