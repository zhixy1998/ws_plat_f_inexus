package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 分辨率校验
 */
@Component
public class GetRatioOfMaxToMinUVTestTool extends IAlgorithm {
    @Override
    public List<Double> calcResult(List<Double> X, List<Double> Y, List<Double> featurePoints) {
        List<Double> result = new ArrayList<>();
        if (X.size() > 0 && X.size() == Y.size()) {
            // 1.寻峰
            // 2.找到最高波峰与最低波谷
            // 3.最大值与最小值之比
            
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            List<Double> findPeaks = new ArrayList<>();
            List<Double> findValleys = new ArrayList<>();
            
            Collections.reverse(X);
            Collections.reverse(Y);
            
            // 注意：这里需要实现或引用peakfactory类
            // peakfactory peakfactory = new peakfactory();
            // List<PeakInformations> peaks = (List<PeakInformations>) peakfactory.execute(
            //     peakfactory.MethodType.FIND_PEAK, X, Y, null);
            
            // 由于peakfactory和PeakInformations类未提供，这里使用占位符
            // 实际实现需要这些相关类
            if (findPeaks.size() > 0) {
                max = Collections.max(findPeaks);
            }
            if (findValleys.size() > 0) {
                min = Collections.min(findValleys);
            }
            
            result.add(max / min);
        }
        return result;
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