package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间扫描求漂移（漂移检验）
 */
@Component
public class GetDriftUVTestTool extends IAlgorithm {
    @Override
    public List<BigDecimal> calcResult(List<BigDecimal> X, List<BigDecimal> Y, List<BigDecimal> featurePoints) {
        List<BigDecimal> result = new ArrayList<>();
        if (X.size() > 0 && X.size() == Y.size()) {
            BigDecimal minimum = new BigDecimal(Double.MAX_VALUE);
            BigDecimal firstValue = Y.get(0);
            for (BigDecimal item : Y) {
                BigDecimal diff = firstValue.subtract(item);
                if (diff.compareTo(minimum) < 0) {
                    minimum = diff;
                }
            }
            result.add(minimum);
            return result;
        }
        return result;
    }
}
