/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetection;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum LiveDetectionCmdEnum {

    Connect(1, "Connect", null, 2),
    GetPhotoMode(2, "GetPhotoMode", null, 2),
    GetResponseTime(3, "GetResponseTime", null, 2),
    Measure(4, "Measure", null, 3),
    GetChangeLampWl(5, "GetChangeLampWl", null, 2),
    GetCellPos(6, "GetCellPos", null, 2),
    GetLampPos(7, "GetLampPos", null, 2),
    GetWLampStatus(8, "GetWLampStatus", null, 2),
    GetDLampStatus(9, "GetDLampStatus", null, 2),
    GetMLamp(10, "GetMLamp", null, 2);
//    GetAccessoryType(6, "GetAccessoryType", null, 2);

    // 顺序
    private final Integer num;
    // 指令
    private final String comId;
    //参数
    private final String param;
    //回应报文数量
    private final Integer count;

    private static final Map<Integer, String> map = new HashMap<>();
    private static final Map<Integer, LiveDetectionCmdEnum> enums = new HashMap<>();

    static {
        for(LiveDetectionCmdEnum e : LiveDetectionCmdEnum.values()) {
            map.put(e.num,e.comId);
        }
    }

    static {
        for(LiveDetectionCmdEnum e : LiveDetectionCmdEnum.values()) {
            enums.put(e.num,e);
        }
    }

    // 构造函数
    LiveDetectionCmdEnum(Integer num, String comId, String param, Integer count) {
        this.comId = comId;
        this.num = num;
        this.count = count;
        this.param = param;
    }

    public static String getComId(Integer num) {
        return map.get(num);
    }

    public static LiveDetectionCmdEnum getEnum(Integer num) {
        return enums.get(num);
    }

    public static List<LiveDetectionCmdEnum> getAll(){
        return  Arrays.asList(values());
    }

    public static Integer getLength(){
        return map.size();
    }
}
