/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetection;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionConstants {

    /**
     * 实时监测标志量
     */
    public static volatile boolean isDeviceIdle = true;
    /**
     * 校零结果
     */
    public static volatile LinkedHashMap<String, ChangeParam> zeroResult = new LinkedHashMap<>();
    /**
     * 采集数据间隔,单位ms
     */
    public static final Integer interval = 3500;

    public static volatile ConcurrentHashMap<String, Object> detectionInfo = new ConcurrentHashMap<>();

    /**
     * 光度模式
     */
    public static volatile String luminosity = "1";
}
