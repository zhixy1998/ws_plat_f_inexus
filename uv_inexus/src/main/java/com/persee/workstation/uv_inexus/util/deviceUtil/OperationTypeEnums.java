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

    Find(1, "查找仪器", "deviceIp,connectType", "", 4),
    Connect(2, "连接", "deviceIp,connectType", "",4),
    Init(3, "初始化仪器", "deviceIp,connectType", "", 1),
    Measure(4, "测量", "deviceIp,connectType", "",2),
    SetParam(5, "设置参数", "deviceIp,connectType,setParam", "", 4),
    ActionCmdEnum(6, "执行命令", "deviceIp,connectType,InputParam", "", 4),
    GetValue(7, "读取值", "deviceIp,connectType,InputParam", "", 4),
    Stop(8, "停止测量", "deviceIp,connectType", "", 4),
    TimeMeasure(9, "定时测量", "deviceIp,connectType,InputParam", "", 4),
    Zero(885, "零点校准", "deviceIp,connectType", "", 5),
    Calibration(886, "校准", "deviceIp,connectType", "", 4),
    LiveDetection(887, "实时检测", "deviceIp,connectType", "", 3),
    Disconnect(888, "断开连接", "deviceIp,connectType", "", 4),
    End(999, "枚举最后标志量，不做操作，方便添加", "deviceIp,connectType", "", 999);

    // 操作类型
    private final Integer type;
    // 描述
    private final String desc;
    // 输入参数
    private final String inputParam;
    // 输出参数
    private final String outputParam;
    /**
     * 仪器当前状态 0-空闲，1-初始化中，2-测量中，3-回显线程占用中，4其他指令-占用, 5-零点校准中
     */
    private final Integer status;

    private static final Map<Integer, OperationTypeEnums> enums = new HashMap<>();

    static {
        for(OperationTypeEnums e : OperationTypeEnums.values()) {
            enums.put(e.type, e);
        }
    }

    // 构造函数
    OperationTypeEnums(Integer type, String desc, String inputParam, String outputParam, Integer status) {
        this.type = type;
        this.desc = desc;
        this.inputParam = inputParam;
        this.outputParam = outputParam;
        this.status = status;
    }

    public static OperationTypeEnums getEnum(Integer type) {
        return enums.get(type);
    }

    public static List<OperationTypeEnums> getAll(){
        return Arrays.asList(values());
    }
}
