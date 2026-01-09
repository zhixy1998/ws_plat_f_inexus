package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间扫描求漂移（漂移检验）
 */
@Component
public class GetDriftUVTestTool extends IAlgorithm {
    @Override
    public List<Double> calcResult(List<Double> X, List<Double> Y, List<Double> featurePoints) {
        List<Double> result = new ArrayList<>();
        if (X.size() > 0 && X.size() == Y.size()) {
            double minimum = Double.MAX_VALUE;
            for (double item : Y) {
                if (Y.get(0) - item > minimum) {
                    minimum = Y.get(0) - item;
                }
            }
            result.add(minimum);
            return result;
        }
        return result;
    }
}