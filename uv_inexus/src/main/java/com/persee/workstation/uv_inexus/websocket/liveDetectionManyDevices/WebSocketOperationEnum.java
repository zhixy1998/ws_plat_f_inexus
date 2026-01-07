/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum WebSocketOperationEnum {

    LiveDetection("liveDetection", "实时回显测量结果"),
    ConcisePhotoMetric("concisePhotoMetric", "经典-光度测量"),
    ConciseRationMeasure("conciseRationMeasure", "经典-定量测定"),
    ConciseBaseLine("conciseBaseLine", "经典-基线测量"),
    ConciseSpectrum("conciseSpectrum", "经典-光谱扫描"),
    ConciseTimeMeasure("conciseTimeMeasure", "经典-时间测量"),

    PhotoMetric("photoMetric", "向导-光度测量"),
    RationMeasure("rationMeasure", "向导-定量测定"),
    Spectrum("spectrum", "向导-光谱扫描"),
    BaseLine("baseLine", "向导-基线测量"),
    TimeMeasure("timeMeasure", "向导-时间测量"),
    DeviceStatus("deviceStatus", "仪器实时状态值"),
    //通用
    Common("Common", "通用");

    // 操作类型
    private final String operation;

    private static final Map<String, WebSocketOperationEnum> enums = new HashMap<>();

    static {
        for(WebSocketOperationEnum e : WebSocketOperationEnum.values()) {
            enums.put(e.operation, e);
        }
    }

    // 构造函数
    WebSocketOperationEnum(String operation, String desc) {
        this.operation = operation;
        // 描述
    }

    public static WebSocketOperationEnum getEnum(String operation) {
        return enums.get(operation);
    }

    public static List<WebSocketOperationEnum> getAll(){
        return  Arrays.asList(values());
    }


}
