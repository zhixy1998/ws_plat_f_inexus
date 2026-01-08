package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

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
    public List<Double> calcResult(List<Double> X, List<Double> Y, List<Double> featurePoints) {
        List<Double> result = new ArrayList<>();
        if (!X.isEmpty() && X.size() == Y.size()) {
            Collections.reverse(X);
            Collections.reverse(Y);
            
            // 注意：这里需要实现或引用peakfactory类
            // peakfactory peakfactory = new peakfactory();
            // List<PeakInformations> peaks = (List<PeakInformations>) peakfactory.execute(
            //     peakfactory.MethodType.FIND_PEAK, X, Y, null);
            
            // 由于peakfactory和PeakInformations类未提供，这里使用占位符
            // 实际实现需要这些相关类
            for (double featurePoint : featurePoints) {
                double minimum = Double.MAX_VALUE;
                double ret = 0;
                // 实现寻峰逻辑
                result.add(ret);
            }
        }
        return result;
    }
}