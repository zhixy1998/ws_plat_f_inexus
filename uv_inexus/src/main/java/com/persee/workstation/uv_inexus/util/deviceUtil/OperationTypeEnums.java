/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum OperationTypeEnums {

    Find(1, "查找仪器", "deviceIp,connectType", ""),
    Connect(2, "连接", "deviceIp,connectType", ""),
    Init(3, "初始化仪器", "deviceIp,connectType", ""),
    Measure(4, "测量", "deviceIp,connectType", ""),
    LiveDetection(887, "实时检测", "deviceIp,connectType", ""),
    SetParam(5, "设置参数", "deviceIp,connectType,setParam", ""),
    ActionCmdEnum(6, "执行命令", "deviceIp,connectType,InputParam", ""),
    GetValue(7, "读取值", "deviceIp,connectType,InputParam", ""),
    Disconnect(888, "断开连接", "deviceIp,connectType", ""),
    End(999, "枚举最后标志量，不做操作，方便添加", "deviceIp,connectType", "");

    // 操作类型
    private final Integer type;
    // 描述
    private final String desc;
    // 输入参数
    private final String inputParam;
    // 输出参数
    private final String outputParam;

    private static final Map<Integer, OperationTypeEnums> enums = new HashMap<>();

    static {
        for(OperationTypeEnums e : OperationTypeEnums.values()) {
            enums.put(e.type, e);
        }
    }

    // 构造函数
    OperationTypeEnums(Integer type, String desc, String inputParam, String outputParam) {
        this.type = type;
        this.desc = desc;
        this.inputParam = inputParam;
        this.outputParam = outputParam;
    }

    public static OperationTypeEnums getEnum(Integer type) {
        return enums.get(type);
    }

    public static List<OperationTypeEnums> getAll(){
        return Arrays.asList(values());
    }
}
