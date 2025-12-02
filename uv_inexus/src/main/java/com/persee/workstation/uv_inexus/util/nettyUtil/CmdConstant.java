/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.nettyUtil;

import java.util.Arrays;
import java.util.List;

public class CmdConstant {


    public static final List<String> connect = Arrays.asList("SearchAround","NewIP" ,"Connect","GetSn","GetAccessoryType","GetInitStatus");


    public static final List<String> init = Arrays.asList("GetAccessoryType", "GetInitStatus", "InitSampleCell", "SetCellPos", "InitFilterMotor", "InitSlit1Motor", "SetSlit1", "InitWlMotor", "InitLampMotor", "CheckWLamp", "CheckDLamp", "CheckWl");


    public static final List<String> setParam = Arrays.asList("GetAccessoryType", "GetInitStatus", "InitSampleCell", "SetCellPos", "InitFilterMotor", "InitSlit1Motor", "SetSlit1", "InitWlMotor", "InitLampMotor", "CheckWLamp", "CheckDLamp", "CheckWl");

}
