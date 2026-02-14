package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 最大偏移量
 */
@Component
public class GetMaxDistanceUVTestTool extends IAlgorithm {
    public List<BigDecimal> calcResult(List<BigDecimal> X, List<BigDecimal> Y, List<BigDecimal> featurePoints) {
        List<BigDecimal> result = new ArrayList<>();
        if (!X.isEmpty() && X.size() == Y.size()) {
            BigDecimal[] outMax = new BigDecimal[1];
            BigDecimal[] outMin = new BigDecimal[1];
            this.getMaxAndMin(Y, outMax, outMin);
            if (outMax[0].pow(2).compareTo(outMin[0].pow(2)) > 0) {
                result.add(outMax[0]);
            } else {
                result.add(outMin[0]);
            }
        }
        return result;
    }

    private void getMaxAndMin(List<BigDecimal> data, BigDecimal[] outMax, BigDecimal[] outMin) {
        BigDecimal max = new BigDecimal(Double.MIN_VALUE);
        BigDecimal min = new BigDecimal(Double.MAX_VALUE);
        for (BigDecimal value : data) {
            if (value.compareTo(max) > 0) {
                max = value;
            }
            if (value.compareTo(min) < 0) {
                min = value;
            }
        }
        outMax[0] = max;
        outMin[0] = min;
    }
}
