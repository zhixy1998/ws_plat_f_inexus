package com.persee.workstation.uv_inexus.po.guide.instrumentPo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * 表示单个分段线性函数 y = kx + b
 */
@Component
@Data
@NoArgsConstructor
public class LinearSegment {
    //最小最大实际波长
    private Double minActualStep;
    private Double maxActualStep;
    //最大最小理论波长
    private Double maxTheoryStep;
    private Double minTheoryStep;
    private Double k;
    private Double b;


    public LinearSegment(Double minActualStep, Double maxActualStep, Double maxTheoryStep, Double minTheoryStep, Double k, Double b) {
        this.minActualStep = minActualStep;
        this.maxActualStep = maxActualStep;
        this.maxTheoryStep = maxTheoryStep;
        this.minTheoryStep = minTheoryStep;
        this.k = k;
        this.b = b;
    }

    // 检查实际步长是否在区间内
    public boolean containsActualStep(double actualStep) {
        return actualStep >= minActualStep && actualStep < maxActualStep; // 左闭右开
    }

    // 检查理论步长是否在区间内
    public boolean containsWl(double wl) {
        return wl*50 >= minTheoryStep && wl*50 < maxTheoryStep; // 左闭右开
    }

}
