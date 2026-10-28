/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.StepPoint;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.WorkTimeInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.GetParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DataNameValue;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 设备操作输入参数
 */

@Data
public class DevOperationInputParam {

    @ApiModelProperty("操作类型")
    private Integer operationType;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("连接类型")
    private Integer connectType;

    @ApiModelProperty("要连接的仪器")
    private SearchInfo deviceInfo;

    @ApiModelProperty("初始化是否从头开始")
    private Boolean isInitFromHead;

    @ApiModelProperty("设置参数命令")
    private SetParam param;

    @ApiModelProperty("操作命令")
    private ActionCmdEnum action;

    @ApiModelProperty("读取值")
    private GetParam readValue;

    @ApiModelProperty("校正方式")
    private Integer calibrationType;

    @ApiModelProperty("是否按实时方式接收（暗电流等流式指令，边收边解析并回调数据点）")
    private Boolean onTime;

    @ApiModelProperty("校准参数")
    private List<DataNameValue> calibrationData;

    @ApiModelProperty("波长信息")
    private List<String> wavelength;

    @ApiModelProperty("样品池位置")
    private Integer cellPos;

    @ApiModelProperty("单波长")
    private String singleWavelength;

    @ApiModelProperty("操作人")
    private String updateBy;

    @ApiModelProperty("时间扫描间隔（秒）")
    private Double intervalSeconds;

    @ApiModelProperty("扫描总时间")
    private Double totalTime;

    @ApiModelProperty("获取仪器、灯工作时长，指令")
    private List<WorkTimeInfo> workTimeInfo;

    @ApiModelProperty("设置波长列表")
    private List<StepPoint> setWlList;

}
