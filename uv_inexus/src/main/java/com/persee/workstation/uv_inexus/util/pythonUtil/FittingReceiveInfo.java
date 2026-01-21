/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.pythonUtil;

import lombok.Data;

import java.util.List;

@Data
public class FittingReceiveInfo {

    /**
     * Code，状态码
     */
    private Integer code;

    /**
     * Message，状态信息
     */
    private String message;
    /**
     * Coefficients，修约后所有系数列表，顺序为 k0, k1, ..., kn
     */
    private List<Double> roundCoefficient;

    /**
     * Coefficients，所有系数列表，顺序为 k0, k1, ..., kn
     */
    private List<Double> coefficients;

    /**
     * 公式，拟合的曲线公式
     */
    private String equation;

    /**
     * 修约后，R，R值 （标准差, 代表拟合准确率）
     */
    private String roundR;


    /**
     * 修约后，R2，R2值 （决定系数, 代表拟合准确率）
     */
    private String roundR2;

    /**
     * R，R值 （标准差, 代表拟合准确率）
     */
    private String R;

    /**
     * R2，R2值 （决定系数, 代表拟合准确率）
     */
    private String R2;

    /**
     * R2 0，相关系数（r2_0），用于强调 Pearson 相关性
     */
    private Double r20;
    /**
     * R2 M，多变量相关系数（r2_M），与R2相同但强调多项式场景
     */
    private Double r2M;
    /**
     * Residual Std，残差标准偏差（R），表示残差的离散程度
     */
    private Double residualStd;
    /**
     * Rmse，评估标准误差（D），与 standard_error 等价
     */
    private Double rmse;
    /**
     * Standard Error，预测标准误差（S），表示拟合值对实际值的偏离程度
     */
    private Double standardError;

    /**
     * Degree，拟合的多项式次数
     */
    private Integer degree;

    /**
     * K0，常数项
     */
    private Double k0;
    /**
     * K1，一次项
     */
    private Double k1;
    /**
     * K2，二次项
     */
    private Double k2;
    /**
     * K3，三次项
     */
    private Double k3;
    /**
     * K4，四次项
     */
    private Double k4;

    private Double minX;

    private Double maxX;

}
