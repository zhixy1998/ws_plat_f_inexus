package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 光谱扫描求最大最小值之差
 */
@Component
public class FindMaxOffsetUVTestTool extends IAlgorithm {
    @Override
    public List<Double> calcResult(List<Double> X, List<Double> Y, List<Double> featurePoints) {
        List<Double> result = new ArrayList<>();
        if (!X.isEmpty() && X.size() == Y.size()) {
            double minimum = Double.MAX_VALUE;
            double maximum = Double.MIN_VALUE;
            for (double item : Y) {
                if (minimum > item) {
                    minimum = item;
                }
                if (maximum < item) {
                    maximum = item;
                }
            }
            double diff = maximum - minimum;
            // 使用BigDecimal进行四舍五入到小数点后两位
            java.math.BigDecimal bd = new java.math.BigDecimal(diff).setScale(2, java.math.RoundingMode.HALF_UP);
            result.add(bd.doubleValue());
        }
        return result;
    }
}