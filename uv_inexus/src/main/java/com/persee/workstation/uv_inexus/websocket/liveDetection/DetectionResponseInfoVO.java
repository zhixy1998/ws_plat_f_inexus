/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetection;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DetectionResponseInfoVO {

    @ApiModelProperty("连接状态")
    private String connectStatus;

    @ApiModelProperty("仪器当前状态")
    private String devCurrentStatus;

    @ApiModelProperty("灯位置")
    private String lampPos;

    @ApiModelProperty("灯状态")
    private List<DetectionLampStatus> dataList;

    @ApiModelProperty("X坐标")
    private String x;

    @ApiModelProperty("Y坐标")
    private String y;

    @ApiModelProperty("样品池位置")
    private String pos;

    @ApiModelProperty("带宽")
    private String bandwidth;

    @ApiModelProperty("换灯波长")
    private String changeLampWl;

    @ApiModelProperty("响应时间")
    private String time;

    @ApiModelProperty("附件类型名")
    private String attachmentName;

    @ApiModelProperty("附件种类")
    private Integer attachmentKind;

    @ApiModelProperty("总池数")
    private Integer totalPoolNum;

    @ApiModelProperty("光度模式")
    private String luminosity;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        if (connectStatus != null) map.put("connectStatus", connectStatus);
        if (dataList != null) map.put("dataList", dataList);
        if (x != null) map.put("x", x);
        if (y != null) map.put("y", y);
        if (pos != null) map.put("pos", pos);
        if (bandwidth != null) map.put("bandwidth", bandwidth);
        if (changeLampWl != null) map.put("changeLampWl", changeLampWl);
        if (time != null) map.put("time", time);
        if (attachmentName != null) map.put("attachmentName", attachmentName);
        if (attachmentKind != null) map.put("attachmentKind", attachmentKind);
        if (totalPoolNum != null) map.put("totalPoolNum", totalPoolNum);
        if (luminosity != null) map.put("luminosity", luminosity);
        if (devCurrentStatus != null) map.put("devCurrentStatus", devCurrentStatus);
        if (lampPos != null) map.put("lampPos", lampPos);

        return Collections.unmodifiableMap(map);
    }

}
