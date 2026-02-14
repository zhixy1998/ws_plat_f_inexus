package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 分辨率校验
 */
@Component
public class GetRatioOfMaxToMinUVTestTool extends IAlgorithm {
    @Override
    public List<BigDecimal> calcResult(List<BigDecimal> X, List<BigDecimal> Y, List<BigDecimal> featurePoints) {
        List<BigDecimal> result = new ArrayList<>();
        if (X.size() > 0 && X.size() == Y.size()) {
            // 1. 寻峰
            // 2. 找到最高波峰与最低波谷
            // 3. 最大值与最小值之比

            BigDecimal max = new BigDecimal(Double.MIN_VALUE);
            BigDecimal min = new BigDecimal(Double.MAX_VALUE);
            List<BigDecimal> findPeaks = new ArrayList<>();
            List<BigDecimal> findValleys = new ArrayList<>();

            reverseList(X);
            reverseList(Y);

            // 注意：这里需要实现或引用 peakfactory 类
            // peakfactory peakfactory = new peakfactory();
            // List<PeakInformations> peaks = (List<PeakInformations>) peakfactory.execute(
            //     peakfactory.MethodType.FIND_PEAK, X, Y, null);

            // 由于 peakfactory 和 PeakInformations 类未提供，这里使用占位符
            // 实际实现需要这些相关类
            if (!findPeaks.isEmpty()) {
                max = Collections.max(findPeaks);
            }
            if (!findValleys.isEmpty()) {
                min = Collections.min(findValleys);
            }

            // 计算最大值与最小值之比
            if (min.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal ratio = max.divide(min, 2, BigDecimal.ROUND_HALF_UP);
                result.add(ratio);
            }
        }
        return result;
    }

    /**
     * 手动反转列表（因为 Collections.reverse 不支持 BigDecimal）
     */
    private void reverseList(List<BigDecimal> list) {
        int size = list.size();
        for (int i = 0; i < size / 2; i++) {
            BigDecimal temp = list.get(i);
            list.set(i, list.get(size - 1 - i));
            list.set(size - 1 - i, temp);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
