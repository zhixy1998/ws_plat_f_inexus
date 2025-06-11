package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_method")
public class TMethod {
    @TableField("id")
    private String id;
    @TableField("method_name")
    private String methodName;
    @TableField("luminosity")
    private String luminosity;
    @TableField("start_wave_length")
    private String startWaveLength;
    @TableField("end_wave_length")
    private String endWaveLength;
    @TableField("wave_length_interval")
    private String waveLengthInterval;
    @TableField("repetition_pattern")
    private String repetitionPattern;
    @TableField("repetition_time")
    private String repetitionTime;
    @TableField("time_interval")
    private String timeInterval;
    @TableField("spectral_bandwidth")
    private String spectralBandwidth;
    @TableField("response_time")
    private String responseTime;
    @TableField("change_lamp_wavelength")
    private String changeLampWaveLength;
    @TableField("energy_lamp")
    private String energyLamp;
    @TableField("gain")
    private String gain;
    @TableField("task_id")
    private String taskId;
    @TableField("field1")
    private String field1;
    @TableField("field2")
    private String field2;
    @TableField("field3")
    private String field3;
    @TableField("field4")
    private String field4;
    @TableField("field5")
    private String field5;
}
