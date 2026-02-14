package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 光谱扫描求半峰宽（最小光谱带宽）
 */
@Component
public class FindHalfPeakUVTestTool extends IAlgorithm {
    @Override
    public List<BigDecimal> calcResult(List<BigDecimal> X, List<BigDecimal> Y, List<BigDecimal> featurePoints) {
        List<BigDecimal> result = new ArrayList<>();
        if (!X.isEmpty() && X.size() == Y.size()) {
            // 反转列表（注意：Collections.reverse 不支持 BigDecimal，需手动实现）
            reverseList(X);
            reverseList(Y);

            // TODO: 实现或引用 LCIntegration 类
            // LCIntegration lc = new LCIntegration();
            // List<PeakInformations> peaks = (List<PeakInformations>) lc.execute(X, Y, err, 0);

            // 遍历特征点，寻找半峰宽
            for (BigDecimal featurePoint : featurePoints) {
                BigDecimal minimum = new BigDecimal(Double.MAX_VALUE);
                BigDecimal single = BigDecimal.ZERO;

                // 示例逻辑：遍历Y值查找最接近featurePoint的峰值
                for (int i = 0; i < Y.size(); i++) {
                    BigDecimal diff = Y.get(i).subtract(featurePoint).abs();
                    if (diff.compareTo(minimum) < 0) {
                        minimum = diff;
                        single = X.get(i); // 假设X为波长，Y为强度
                    }
                }

                result.add(single);
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
