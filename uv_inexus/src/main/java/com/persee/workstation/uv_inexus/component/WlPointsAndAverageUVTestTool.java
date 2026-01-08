package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 光度多次测量并求误差（透射比示值误差与重复性校验）
 */
@Component
public class WlPointsAndAverageUVTestTool extends IAlgorithm {
    @Override
    public List<Double> calcResult(List<Double> X, List<Double> Y, List<Double> featurePoints) {
        return Y;
    }
}