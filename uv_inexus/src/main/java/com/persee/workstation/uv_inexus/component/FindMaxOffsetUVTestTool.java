package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 光谱扫描求最大最小值之差
 */
@Component
public class FindMaxOffsetUVTestTool extends IAlgorithm {
    @Override
    public List<BigDecimal> calcResult(List<BigDecimal> X, List<BigDecimal> Y, List<BigDecimal> featurePoints) {
        List<BigDecimal> result = new ArrayList<>();
        if (!X.isEmpty() && X.size() == Y.size()) {
            BigDecimal minimum = new BigDecimal(Double.MAX_VALUE);
            BigDecimal maximum = new BigDecimal(Double.MIN_VALUE);
            for (BigDecimal item : Y) {
                if (item.compareTo(minimum) < 0) {
                    minimum = item;
                }
                if (item.compareTo(maximum) > 0) {
                    maximum = item;
                }
            }
            BigDecimal diff = maximum.subtract(minimum);
            // 四舍五入到小数点后两位
            BigDecimal roundedDiff = diff.setScale(2, RoundingMode.HALF_UP);
            result.add(roundedDiff);
        }
        return result;
    }
}
