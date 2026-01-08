package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 最大偏移量
 */
@Component
public class GetMaxDistanceUVTestTool extends IAlgorithm {
    @Override
    public List<Double> calcResult(List<Double> X, List<Double> Y, List<Double> featurePoints) {
        List<Double> result = new ArrayList<>();
        if (!X.isEmpty() && X.size() == Y.size()) {
            double[] outMax = new double[1];
            double[] outMin = new double[1];
            getMaxAndMin(Y, outMax, outMin);
            if (Math.pow(outMax[0], 2) > Math.pow(outMin[0], 2)) {
                result.add(outMax[0]);
            } else {
                result.add(outMin[0]);
            }
        }
        return result;
    }
    
    private void getMaxAndMin(List<Double> data, double[] outMax, double[] outMin) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (double value : data) {
            if (value > max) {
                max = value;
            }
            if (value < min) {
                min = value;
            }
        }
        outMax[0] = max;
        outMin[0] = min;
    }
}