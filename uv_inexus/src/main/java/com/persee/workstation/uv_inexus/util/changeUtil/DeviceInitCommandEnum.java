package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceInitCommandEnum {

//    GetAccessoryType(0, "Connect"),
    GetInitStatus(0, "GetInitStatus"),
    InitSampleCell(1, "InitSampleCell"),
    SetCellPos(2, "SetCellPos"),
    InitFilterMotor(3, "InitFilterMotor"),
    InitSlit1Motor(4, "InitSlit1Motor"),
    SetSlit1(5, "SetSlit1"),
    InitWlMotor(6, "InitWlMotor"),
    InitLampMotor(7, "InitLampMotor"),
    CheckWLamp(8, "CheckWLamp"),
    CheckDLamp(9, "CheckDLamp"),
    CheckWl(10, "CheckWl");


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
