package com.persee.workstation.uv_inexus.dto.measurementDto;

import java.util.List;

public class TimeSweepParamter extends MeasurePublicParameters{
    // 光度模式
    private String spectralBandwidth;
    // 波长值
    private List<String> waveLength;
    // 限制条件
    private String limitCondition;
    // 扫描总时长
    private String scanTime;
    // 扫描单位(0.分 1.秒)
    private String scanUnit;
    // 时间间隔
    private String intervalTime;
    // 重复模式
    private String repeatMode;
    // 扫描逻辑(0.单池完成全程扫描后移动到下一个 1.依次走完待测池子后才进入定时逻辑)
    private String scanLogic;
}
