/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 返回结果实体类，包含所有返回参数
 */

@Data
@EqualsAndHashCode
public class OperationResultEntity {

    @ApiModelProperty("全局标志，操作成功还是失败")
    private Boolean isSuccess;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    @ApiModelProperty("本机ip")
    private String machineIp;

    @ApiModelProperty("连接类型")
    private Integer connectType;

    @ApiModelProperty("联机端口")
    private Integer connectPort;

    @ApiModelProperty("搜索仪器结果")
    private List<SearchInfo> searchInfo;

    @ApiModelProperty("连接状态")
    private Integer connectStatus;

    @ApiModelProperty("序列号")
    private String deviceSerialNumber;

    @ApiModelProperty(value = "自检状态值",example = "0：未完成；1：自检成功； 2：自检失败； 3：自检进行中")
    private Integer isInited;

    @ApiModelProperty(value = "开机状态",example = "0-无响应，1-已开机，2-开机中，3-软关机")
    private Integer isDevOpened;

    @ApiModelProperty("机器ip地址")
    private String ip;

    @ApiModelProperty("唯一标识")
    private String deviceId;

    @ApiModelProperty("附件类型")
    private Integer attachmentType;

    @ApiModelProperty("附件类型名称")
    private String attachmentName;

    @ApiModelProperty("样本池数量")
    private Integer samplePoolNum;

    @ApiModelProperty("附件种类")
    private Integer attachmentKind;

    @ApiModelProperty("当前样品池位置")
    private Integer currentPoolNum;

    @ApiModelProperty("仪器型号名称")
    private String deviceModelName;

    @ApiModelProperty("xmlKey")
    private String xmlKey;

    @ApiModelProperty("计算公式")
    private String linearSegment;

}
