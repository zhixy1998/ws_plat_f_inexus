package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 波长示值误差与重复性校验（扫描寻峰多次测量）
 */
@Component
public class FindPeakAndAverageUVTestTool extends IAlgorithm {
    /**
     * 计算寻峰
     */
    @Override
    public List<BigDecimal> calcResult(List<BigDecimal> X, List<BigDecimal> Y, List<BigDecimal> featurePoints) {
        List<BigDecimal> result = new ArrayList<>();
        if (!X.isEmpty() && X.size() == Y.size()) {
            // 手动反转列表（Collections.reverse 不支持 BigDecimal）
            reverseList(X);
            reverseList(Y);

            // 注意：这里需要实现或引用 peakfactory 类
            // peakfactory peakfactory = new peakfactory();
            // List<PeakInformations> peaks = (List<PeakInformations>) peakfactory.execute(
            //     peakfactory.MethodType.FIND_PEAK, X, Y, null);

            // 由于 peakfactory 和 PeakInformations 类未提供，这里使用占位符
            // 实际实现需要这些相关类
            for (BigDecimal featurePoint : featurePoints) {
                BigDecimal minimum = new BigDecimal(Double.MAX_VALUE);
                BigDecimal ret = BigDecimal.ZERO;

                // 示例逻辑：遍历 Y 值查找最接近 featurePoint 的峰值
                for (int i = 0; i < Y.size(); i++) {
                    BigDecimal diff = Y.get(i).subtract(featurePoint).abs();
                    if (diff.compareTo(minimum) < 0) {
                        minimum = diff;
                        ret = X.get(i); // 假设 X 为波长，Y 为强度
                    }
                }

                result.add(ret);
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
}
