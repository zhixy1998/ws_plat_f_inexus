/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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


    public LinearSegment(Double minActualStep, Double maxActualStep, Double minTheoryStep, Double maxTheoryStep, Double k, Double b) {
        this.minActualStep = minActualStep;
        this.maxActualStep = maxActualStep;
        this.minTheoryStep = minTheoryStep;
        this.maxTheoryStep = maxTheoryStep;
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
