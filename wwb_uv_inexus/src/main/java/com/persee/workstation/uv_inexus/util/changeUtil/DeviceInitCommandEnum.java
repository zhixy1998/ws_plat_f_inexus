package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceInitCommandEnum {

    GetAccessoryType(0, "Connect"),
    GetInitStatus(1, "GetInitStatus"),
    InitSampleCell(2, "InitSampleCell"),
    SetCellPos(3, "SetCellPos"),
    InitFilterMotor(4, "InitFilterMotor"),
    InitSlit1Motor(5, "InitSlit1Motor"),
    SetSlit1(6, "SetSlit1"),
    InitWlMotor(7, "InitWlMotor"),
    InitLampMotor(8, "InitLampMotor"),
    CheckWLamp(9, "CheckWLamp"),
    CheckDLamp(10, "CheckDLamp"),
    CheckWl(11, "CheckWl");


    // 获取comId
    private final String comId;
    // 获取description
    private final Integer num;

    private static final Map<Integer, String> map = new HashMap<>();

    static {
        for(DeviceInitCommandEnum e : DeviceInitCommandEnum.values()) {
            map.put(e.num,e.comId);
        }
    }

    // 构造函数
    DeviceInitCommandEnum(Integer num, String comId) {
        this.comId = comId;
        this.num = num;
    }

    public static String getComId(Integer num) {
        return map.get(num);
    }

    public static Integer getLength(){
        return map.size();
    }
}
