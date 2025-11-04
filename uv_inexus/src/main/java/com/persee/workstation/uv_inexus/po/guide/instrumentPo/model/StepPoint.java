package com.persee.workstation.uv_inexus.po.guide.instrumentPo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepPoint {
    //实际步数x
    private double actualStep;
    //理想步数y
    private double theoryStep;
    private int n;
    public StepPoint(double actualStep, double theoryStep) {
        this.actualStep = actualStep;
        this.theoryStep = theoryStep;
    }

}