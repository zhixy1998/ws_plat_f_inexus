package com.persee.workstation.uv_inexus.util.deviceUtil.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
    public class SetParam {
        @ApiModelProperty("光度模式")
        private Integer photoMode;

        @ApiModelProperty("扫描速度")
        private Integer scanSpeed;

        @ApiModelProperty("扫描 wavelength")
        private Double startWl;

        @ApiModelProperty("扫描 wavelength")
        private Double endWl;

//        @ApiModelProperty("扫描间隔")
//        private Double scanInterval;

        @ApiModelProperty("增益")
        private Integer gain;

        @ApiModelProperty("狭缝")
        private Double slit1;

        @ApiModelProperty("灯位置")
        private Integer lampPos;

        @ApiModelProperty("扫描间隔")
        private Double ScanWlInterval;

        @ApiModelProperty("钨灯状态")
        private Integer WLampStatus;

        @ApiModelProperty("氘灯灯状态")
        private Integer DLampStatus;

        @ApiModelProperty("换灯波长")
        private Double changeLampWl;

        @ApiModelProperty("波长列表")
        private String wlList;

        @ApiModelProperty("设置波长")
        private Double setWl;

        @ApiModelProperty("滤色片")
        private Integer filter;

        @ApiModelProperty("响应时间")
        private Double responseTime;

        @ApiModelProperty("样品池位置")
        private Integer cellPos;

        @ApiModelProperty("初始化状态")
        private Integer initStatus;

        @ApiModelProperty("附件类型")
        private Integer accessoryType;

        @ApiModelProperty("读取序列号")
        private String sn;

        @ApiModelProperty("暗电流")
        private Integer dark;
    }
