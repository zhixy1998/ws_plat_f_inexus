/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.util.nettyUtil;

import java.util.Arrays;
import java.util.List;

public class CmdConstant {


    public static final List<String> connect = Arrays.asList("SearchAround","NewIP" ,"Connect","GetSn","GetAccessoryType","GetInitStatus");


    public static final List<String> init = Arrays.asList("GetAccessoryType", "GetInitStatus", "InitSampleCell", "SetCellPos", "InitFilterMotor", "InitSlit1Motor", "SetSlit1", "InitWlMotor", "InitLampMotor", "CheckWLamp", "CheckDLamp", "CheckWl");


    public static final List<String> setParam = Arrays.asList("GetAccessoryType", "GetInitStatus", "InitSampleCell", "SetCellPos", "InitFilterMotor", "InitSlit1Motor", "SetSlit1", "InitWlMotor", "InitLampMotor", "CheckWLamp", "CheckDLamp", "CheckWl");

}
